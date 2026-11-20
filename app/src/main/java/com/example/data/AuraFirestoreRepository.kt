package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AuraFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val localStore: LocalOfflineStore? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        localStore = LocalOfflineStore(context.applicationContext)
    )

    private fun isLocalOfflineMode(): Boolean {
        return auth.currentUser == null && localStore != null
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado com Google para acessar o Firestore.")
    }

    private fun currentEffectiveUserId(): String {
        return auth.currentUser?.uid ?: LocalOfflineStore.LOCAL_USER_ID
    }

    private fun sanitizeHouseholdCode(rawCode: String): String {
        val cleaned = rawCode.trim()
            .uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9_\\-]"), "")
        return cleaned.take(64).ifBlank { "FAM_PADRAO" }
    }

    private fun sanitizePrivacyScope(rawScope: String): String {
        return when (rawScope) {
            PrivacyScope.PERSONAL,
            PrivacyScope.COUPLE,
            PrivacyScope.ASSIGNED_ONLY,
            PrivacyScope.FAMILY -> rawScope
            else -> PrivacyScope.FAMILY
        }
    }

    private fun sanitizeSplitMode(rawMode: String): String {
        return when (rawMode) {
            SplitMode.NONE,
            SplitMode.EQUAL,
            SplitMode.PERCENTAGE,
            SplitMode.CUSTOM_AMOUNT -> rawMode
            else -> SplitMode.NONE
        }
    }

    private fun isVisibleForMember(
        currentUserId: String,
        ownerUserId: String,
        assignedOrResponsibleMemberId: String,
        privacyScope: String,
        visibleToMemberIds: List<String>
    ): Boolean {
        if (privacyScope == PrivacyScope.FAMILY) return true
        if (ownerUserId.isBlank() || ownerUserId == currentUserId) return true
        if (visibleToMemberIds.contains(currentUserId)) return true
        return when (privacyScope) {
            PrivacyScope.PERSONAL -> ownerUserId == currentUserId
            PrivacyScope.ASSIGNED_ONLY -> assignedOrResponsibleMemberId == currentUserId || ownerUserId == currentUserId
            PrivacyScope.COUPLE -> true
            else -> true
        }
    }

    private fun splitSharesToFirestoreList(shares: List<ExpenseSplitShare>): List<Map<String, Any>> {
        return shares.map { s ->
            mapOf(
                "memberId" to s.memberId.take(128),
                "memberName" to s.memberName.take(80),
                "memberColorHex" to s.memberColorHex,
                "memberEmoji" to s.memberEmoji.take(16),
                "percentage" to s.percentage.coerceIn(0.0, 100.0),
                "shareAmountBrl" to s.shareAmountBrl.coerceIn(0.0, 1_000_000_000.0),
                "settled" to s.settled
            )
        }
    }

    private fun memberBadgesToFirestoreList(members: List<FamilyMemberBadge>): List<Map<String, Any>> {
        return members.map { m ->
            mapOf(
                "userId" to m.userId.take(128),
                "displayName" to m.displayName.take(80),
                "colorHex" to m.colorHex,
                "avatarEmoji" to m.avatarEmoji.take(16),
                "role" to m.role.take(24)
            )
        }
    }

    fun defaultHouseholdIdForUser(userId: String): String {
        if (isLocalOfflineMode()) {
            return localStore?.userProfileFlow?.value?.householdId ?: "FAM_AURA_LOCAL"
        }
        val suffix = userId.replace(Regex("[^a-zA-Z0-9]"), "").take(8).uppercase(Locale.ROOT)
        return "FAM_${suffix.ifBlank { "AURA01" }}"
    }

    // 1. UserProfile Stream & Initialization (including Member Visual Identity)
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (isLocalOfflineMode()) {
            emitAll(localStore!!.userProfileFlow)
            return@flow
        }
        val uid = requireUserId()
        check(uid == userId) { "Sessão de usuário alterada." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileAndHousehold(displayNameRaw: String?): Result<UserProfile> {
        if (isLocalOfflineMode()) {
            return Result.success(localStore!!.userProfileFlow.value)
        }
        var currentPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userDocRef = db.collection("users").document(uid)
            currentPath = userDocRef.path
            val snapshot = userDocRef.get().await()

            val displayName = (displayNameRaw?.trim()?.take(80)).takeUnless { it.isNullOrBlank() } ?: "Membro da Família"
            val initials = displayName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { "FA" }

            val profile = if (snapshot.exists()) {
                val loaded = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (loaded != null && loaded.householdId.isNotBlank()) {
                    loaded
                } else {
                    val defaultHouseId = defaultHouseholdIdForUser(uid)
                    userDocRef.update(
                        mapOf(
                            "householdId" to defaultHouseId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    UserProfile(
                        id = uid,
                        userId = uid,
                        displayName = displayName,
                        householdId = defaultHouseId,
                        memberColorHex = loaded?.memberColorHex ?: 0xFF0E7C5B,
                        avatarEmoji = loaded?.avatarEmoji ?: "🌿",
                        avatarInitials = loaded?.avatarInitials?.ifBlank { initials } ?: initials,
                        memberRole = loaded?.memberRole ?: "ADMIN",
                        defaultSplitPercentage = loaded?.defaultSplitPercentage ?: 50.0
                    )
                }
            } else {
                val defaultHouseId = defaultHouseholdIdForUser(uid)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "householdId" to defaultHouseId,
                    "themeMode" to "SYSTEM",
                    "baseCurrency" to "BRL",
                    "memberColorHex" to 0xFF0E7C5BL,
                    "avatarEmoji" to "🌿",
                    "avatarInitials" to initials,
                    "memberRole" to "ADMIN",
                    "defaultSplitPercentage" to 50.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(payload).await()
                UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName,
                    householdId = defaultHouseId,
                    themeMode = "SYSTEM",
                    baseCurrency = "BRL",
                    memberColorHex = 0xFF0E7C5BL,
                    avatarEmoji = "🌿",
                    avatarInitials = initials,
                    memberRole = "ADMIN",
                    defaultSplitPercentage = 50.0
                )
            }

            // Ensure household document exists
            val houseId = profile.householdId.ifBlank { defaultHouseholdIdForUser(uid) }
            val houseRef = db.collection("households").document(houseId)
            currentPath = houseRef.path
            val houseSnap = houseRef.get().await()
            if (!houseSnap.exists()) {
                val initialMember = FamilyMemberBadge(
                    userId = uid,
                    displayName = profile.displayName.ifBlank { displayName },
                    colorHex = profile.memberColorHex,
                    avatarEmoji = profile.avatarEmoji,
                    role = profile.memberRole
                )
                val housePayload = mapOf(
                    "householdId" to houseId,
                    "name" to "Família ${displayName.split(" ").firstOrNull() ?: "Aura"}",
                    "ownerId" to uid,
                    "monthlyIncomeBrl" to 8500.0,
                    "needsPercentage" to 50L,
                    "wantsPercentage" to 30L,
                    "savingsPercentage" to 20L,
                    "defaultSplitMode" to SplitMode.EQUAL,
                    "members" to memberBadgesToFirestoreList(listOf(initialMember)),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                houseRef.set(housePayload).await()
            }

            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserMemberIdentity(
        displayName: String,
        memberColorHex: Long,
        avatarEmoji: String,
        avatarInitials: String = "",
        memberRole: String = "ADMIN",
        defaultSplitPercentage: Double = 50.0
    ): Result<Unit> {
        val cleanName = displayName.trim().take(80).ifBlank { "Membro da Família" }
        val cleanEmoji = avatarEmoji.trim().take(16).ifBlank { "🌿" }
        val cleanInitials = avatarInitials.trim().take(4).ifBlank {
            cleanName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { "FA" }
        }
        val validRole = if (memberRole in listOf("ADMIN", "ADULT", "TEEN", "CHILD")) memberRole else "ADMIN"
        val safeSplitPct = defaultSplitPercentage.coerceIn(0.0, 100.0)

        if (isLocalOfflineMode()) {
            localStore!!.updateUserMemberIdentity(
                displayName = cleanName,
                memberColorHex = memberColorHex,
                avatarEmoji = cleanEmoji,
                avatarInitials = cleanInitials,
                memberRole = validRole,
                defaultSplitPercentage = safeSplitPct
            )
            return Result.success(Unit)
        }

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "displayName" to cleanName,
                    "memberColorHex" to memberColorHex,
                    "avatarEmoji" to cleanEmoji,
                    "avatarInitials" to cleanInitials,
                    "memberRole" to validRole,
                    "defaultSplitPercentage" to safeSplitPct,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateThemeMode(themeMode: String): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.updateThemeMode(themeMode)
            return Result.success(Unit)
        }
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val validTheme = if (themeMode in listOf("SYSTEM", "LIGHT", "DARK")) themeMode else "SYSTEM"
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.update(
                mapOf(
                    "themeMode" to validTheme,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun switchOrJoinHousehold(newHouseholdCodeRaw: String, familyNameRaw: String): Result<String> {
        if (isLocalOfflineMode()) {
            val targetCode = sanitizeHouseholdCode(newHouseholdCodeRaw)
            val savedCode = localStore!!.switchHousehold(targetCode, familyNameRaw)
            return Result.success(savedCode)
        }
        var currentPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val targetHouseholdId = sanitizeHouseholdCode(newHouseholdCodeRaw)
            val familyName = familyNameRaw.trim().take(80).ifBlank { "Orçamento Familiar" }

            val userRef = db.collection("users").document(uid)
            currentPath = userRef.path
            userRef.update(
                mapOf(
                    "householdId" to targetHouseholdId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val houseRef = db.collection("households").document(targetHouseholdId)
            currentPath = houseRef.path
            val snap = houseRef.get().await()
            if (!snap.exists()) {
                val housePayload = mapOf(
                    "householdId" to targetHouseholdId,
                    "name" to familyName,
                    "ownerId" to uid,
                    "monthlyIncomeBrl" to 8500.0,
                    "needsPercentage" to 50L,
                    "wantsPercentage" to 30L,
                    "savingsPercentage" to 20L,
                    "defaultSplitMode" to SplitMode.EQUAL,
                    "members" to emptyList<Map<String, Any>>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                houseRef.set(housePayload).await()
            }

            Result.success(targetHouseholdId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    // 2. Household Observation & Budget 50/30/20 Settings
    fun observeHousehold(householdId: String): Flow<Household?> = flow {
        if (isLocalOfflineMode()) {
            emitAll(localStore!!.householdFlow)
            return@flow
        }
        val uid = requireUserId()
        require(householdId.isNotBlank()) { "Household ID inválido." }
        val path = "households/$householdId"
        emitAll(
            db.collection("households").document(householdId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateHouseholdBudgetPlan(
        householdId: String,
        name: String,
        monthlyIncomeBrl: Double,
        needsPct: Int,
        wantsPct: Int,
        savingsPct: Int,
        defaultSplitMode: String = SplitMode.EQUAL
    ): Result<Unit> {
        val validSplitMode = sanitizeSplitMode(defaultSplitMode)
        if (isLocalOfflineMode()) {
            localStore!!.updateHouseholdBudgetPlan(
                name = name,
                monthlyIncomeBrl = monthlyIncomeBrl,
                needsPct = needsPct,
                wantsPct = wantsPct,
                savingsPct = savingsPct,
                defaultSplitMode = validSplitMode
            )
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            require(householdId.isNotBlank()) { "Código da família obrigatório." }
            val safeName = name.trim().take(80).ifBlank { "Orçamento Familiar" }
            val safeIncome = monthlyIncomeBrl.coerceIn(0.0, 1_000_000_000.0)
            require(needsPct + wantsPct + savingsPct == 100) {
                "A soma das porcentagens (Necessidades + Desejos + Economia) deve ser exatamente 100%."
            }
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "name" to safeName,
                    "monthlyIncomeBrl" to safeIncome,
                    "needsPercentage" to needsPct.toLong(),
                    "wantsPercentage" to wantsPct.toLong(),
                    "savingsPercentage" to savingsPct.toLong(),
                    "defaultSplitMode" to validSplitMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // 3. Tasks Observation & Mutations (under /households/{householdId}/tasks)
    fun observeHouseholdTasks(householdId: String): Flow<List<TaskItem>> = flow {
        val currentUid = currentEffectiveUserId()
        if (isLocalOfflineMode()) {
            emitAll(
                localStore!!.tasksFlow.map { list ->
                    list.filter { task ->
                        isVisibleForMember(
                            currentUserId = currentUid,
                            ownerUserId = task.userId,
                            assignedOrResponsibleMemberId = task.assignedMemberId,
                            privacyScope = task.privacyScope,
                            visibleToMemberIds = task.visibleToMemberIds
                        )
                    }
                }
            )
            return@flow
        }
        val uid = requireUserId()
        require(householdId.isNotBlank()) { "Household ID inválido." }
        val path = "households/$householdId/tasks"
        emitAll(
            db.collection("households").document(householdId).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { task ->
                            isVisibleForMember(
                                currentUserId = uid,
                                ownerUserId = task.userId,
                                assignedOrResponsibleMemberId = task.assignedMemberId,
                                privacyScope = task.privacyScope,
                                visibleToMemberIds = task.visibleToMemberIds
                            )
                        }
                        .sortedWith(compareBy<TaskItem> { it.completed }.thenBy { it.dateEpochDay }.thenBy { it.alarmHour })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun subtasksToFirestoreList(subtasks: List<SubtaskItem>): List<Map<String, Any>> {
        return subtasks.take(30).mapIndexed { idx, st ->
            mapOf(
                "id" to st.id.take(64).ifBlank { "st_$idx" },
                "title" to st.title.trim().take(160),
                "done" to st.done
            )
        }
    }

    suspend fun addTask(
        householdId: String,
        title: String,
        notes: String,
        category: String,
        priority: String,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        alarmEnabled: Boolean,
        privacyScope: String = PrivacyScope.FAMILY,
        visibleToMemberIds: List<String> = emptyList(),
        assignedMemberId: String = "",
        assignedMemberName: String = "Família",
        assignedMemberColorHex: Long = 0xFF0E7C5B,
        assignedMemberEmoji: String = "🌿",
        createdByName: String = "Família",
        createdByColorHex: Long = 0xFF0E7C5B,
        linkedTransactionId: String = "",
        linkedSubscriptionId: String = "",
        estimatedCostBrl: Double = 0.0,
        autoCreateExpenseOnComplete: Boolean = false,
        splitMode: String = SplitMode.NONE,
        splitShares: List<ExpenseSplitShare> = emptyList(),
        isEssential: Boolean = (priority == "HIGH"),
        isInbox: Boolean = false,
        durationMinutes: Int = 30,
        iconEmoji: String = "📋",
        taskColorHex: Long = assignedMemberColorHex,
        recurrence: String = TaskRecurrence.ONCE,
        subtasks: List<SubtaskItem> = emptyList(),
        ringtoneCategory: String = "SUAVE"
    ): Result<String> {
        val validScope = sanitizePrivacyScope(privacyScope)
        val validSplit = sanitizeSplitMode(splitMode)
        val safeCost = estimatedCostBrl.coerceIn(0.0, 1_000_000_000.0)

        if (isLocalOfflineMode()) {
            val id = localStore!!.addTask(
                householdId = householdId,
                title = title,
                notes = notes,
                category = category,
                priority = priority,
                dateEpochDay = dateEpochDay,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute,
                alarmEnabled = if (isInbox) false else alarmEnabled,
                privacyScope = validScope,
                visibleToMemberIds = visibleToMemberIds,
                assignedMemberId = assignedMemberId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                assignedMemberName = assignedMemberName.ifBlank { "Família" },
                assignedMemberColorHex = assignedMemberColorHex,
                assignedMemberEmoji = assignedMemberEmoji.ifBlank { "🌿" },
                createdByName = createdByName.ifBlank { "Família" },
                createdByColorHex = createdByColorHex,
                linkedTransactionId = linkedTransactionId,
                linkedSubscriptionId = linkedSubscriptionId,
                estimatedCostBrl = safeCost,
                autoCreateExpenseOnComplete = autoCreateExpenseOnComplete,
                splitMode = validSplit,
                splitShares = splitShares,
                isEssential = isEssential,
                isInbox = isInbox,
                durationMinutes = durationMinutes,
                iconEmoji = iconEmoji,
                taskColorHex = taskColorHex,
                recurrence = recurrence,
                subtasks = subtasks,
                ringtoneCategory = ringtoneCategory
            )
            return Result.success(id)
        }
        var docPath = "households/$householdId/tasks"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe o título da tarefa." }
            val cleanNotes = notes.trim().take(500)
            val cleanCategory = category.trim().take(50).ifBlank { "Geral" }
            val validPriority = if (priority in listOf("LOW", "MEDIUM", "HIGH")) priority else "MEDIUM"
            val yearMonth = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)

            val docRef = db.collection("households").document(householdId).collection("tasks").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "householdId" to householdId,
                "title" to cleanTitle,
                "notes" to cleanNotes,
                "category" to cleanCategory,
                "priority" to validPriority,
                "isEssential" to isEssential,
                "dateEpochDay" to dateEpochDay.coerceIn(0L, 100000L),
                "yearMonth" to yearMonth,
                "alarmHour" to alarmHour.coerceIn(0, 23).toLong(),
                "alarmMinute" to alarmMinute.coerceIn(0, 59).toLong(),
                "alarmEnabled" to (if (isInbox) false else alarmEnabled),
                "completed" to false,
                "isInbox" to isInbox,
                "durationMinutes" to durationMinutes.coerceIn(5, 720),
                "iconEmoji" to iconEmoji.trim().take(16).ifBlank { "📋" },
                "taskColorHex" to taskColorHex,
                "recurrence" to recurrence.trim().take(24).ifBlank { TaskRecurrence.ONCE },
                "subtasks" to subtasksToFirestoreList(subtasks),
                "ringtoneCategory" to ringtoneCategory.trim().take(24).ifBlank { "SUAVE" },
                "privacyScope" to validScope,
                "visibleToMemberIds" to visibleToMemberIds.take(20),
                "assignedMemberId" to assignedMemberId.take(128).ifBlank { uid },
                "assignedMemberName" to assignedMemberName.trim().take(80).ifBlank { "Família" },
                "assignedMemberColorHex" to assignedMemberColorHex,
                "assignedMemberEmoji" to assignedMemberEmoji.trim().take(16).ifBlank { "🌿" },
                "createdByName" to createdByName.trim().take(80).ifBlank { "Família" },
                "createdByColorHex" to createdByColorHex,
                "completedByUserId" to "",
                "completedByName" to "",
                "linkedTransactionId" to linkedTransactionId.take(128),
                "linkedSubscriptionId" to linkedSubscriptionId.take(128),
                "estimatedCostBrl" to safeCost,
                "autoCreateExpenseOnComplete" to autoCreateExpenseOnComplete,
                "splitMode" to validSplit,
                "splitShares" to splitSharesToFirestoreList(splitShares),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskEssential(
        householdId: String,
        taskId: String,
        newEssential: Boolean
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.toggleTaskEssential(taskId)
            return Result.success(Unit)
        }
        return try {
            val ref = db.collection("households").document(householdId).collection("tasks").document(taskId)
            ref.update(
                mapOf(
                    "isEssential" to newEssential,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleSubtaskDone(
        householdId: String,
        task: TaskItem,
        subtaskId: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.toggleSubtaskDone(task.id, subtaskId)
            return Result.success(Unit)
        }
        return try {
            val updatedSubtasks = task.subtasks.map { st ->
                if (st.id == subtaskId) st.copy(done = !st.done) else st
            }
            val ref = db.collection("households").document(householdId).collection("tasks").document(task.id)
            ref.update(
                mapOf(
                    "subtasks" to subtasksToFirestoreList(updatedSubtasks),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scheduleInboxTask(
        householdId: String,
        taskId: String,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        durationMinutes: Int,
        alarmEnabled: Boolean = true
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.scheduleInboxTask(
                taskId = taskId,
                dateEpochDay = dateEpochDay,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute,
                durationMinutes = durationMinutes,
                alarmEnabled = alarmEnabled
            )
            return Result.success(Unit)
        }
        return try {
            val ym = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
            val ref = db.collection("households").document(householdId).collection("tasks").document(taskId)
            ref.update(
                mapOf(
                    "isInbox" to false,
                    "dateEpochDay" to dateEpochDay.coerceIn(0L, 100000L),
                    "yearMonth" to ym,
                    "alarmHour" to alarmHour.coerceIn(0, 23).toLong(),
                    "alarmMinute" to alarmMinute.coerceIn(0, 59).toLong(),
                    "durationMinutes" to durationMinutes.coerceIn(5, 720),
                    "alarmEnabled" to alarmEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionRingtone(
        householdId: String,
        subscriptionId: String,
        ringtoneCategory: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.updateSubscriptionRingtone(subscriptionId, ringtoneCategory)
            return Result.success(Unit)
        }
        return try {
            val ref = db.collection("households").document(householdId)
                .collection("subscriptions").document(subscriptionId)
            ref.update(
                mapOf(
                    "ringtoneCategory" to ringtoneCategory.take(24),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(
        householdId: String,
        taskId: String,
        completed: Boolean,
        completedByUserId: String = "",
        completedByName: String = "Família",
        taskForAutoExpense: TaskItem? = null
    ): Result<Unit> {
        var generatedTxId: String? = null

        // If task is configured to automatically create a linked expense when completed
        if (completed && taskForAutoExpense != null &&
            taskForAutoExpense.autoCreateExpenseOnComplete &&
            taskForAutoExpense.estimatedCostBrl > 0.0 &&
            taskForAutoExpense.linkedTransactionId.isBlank()
        ) {
            val txRes = addTransaction(
                householdId = householdId,
                title = taskForAutoExpense.title.removePrefix("Pagar: ").trim(),
                amountOriginal = taskForAutoExpense.estimatedCostBrl,
                currency = "BRL",
                exchangeRateToBrl = 1.0,
                type = "EXPENSE",
                budgetBucket = "NEEDS",
                category = taskForAutoExpense.category,
                dateEpochDay = taskForAutoExpense.dateEpochDay,
                recurringSourceId = taskForAutoExpense.linkedSubscriptionId,
                createdByName = taskForAutoExpense.assignedMemberName.ifBlank { completedByName },
                createdByColorHex = taskForAutoExpense.assignedMemberColorHex,
                createdByEmoji = taskForAutoExpense.assignedMemberEmoji,
                privacyScope = taskForAutoExpense.privacyScope,
                visibleToMemberIds = taskForAutoExpense.visibleToMemberIds,
                responsibleMemberId = taskForAutoExpense.assignedMemberId,
                responsibleMemberName = taskForAutoExpense.assignedMemberName,
                responsibleMemberColorHex = taskForAutoExpense.assignedMemberColorHex,
                responsibleMemberEmoji = taskForAutoExpense.assignedMemberEmoji,
                splitMode = taskForAutoExpense.splitMode,
                splitShares = taskForAutoExpense.splitShares,
                linkedTaskId = taskId
            )
            generatedTxId = txRes.getOrNull()
        }

        if (isLocalOfflineMode()) {
            localStore!!.toggleTaskCompleted(
                taskId = taskId,
                completed = completed,
                completedByUserId = completedByUserId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                completedByName = completedByName,
                linkedTransactionId = generatedTxId
            )
            if (completed && taskForAutoExpense != null && taskForAutoExpense.linkedSubscriptionId.isNotBlank()) {
                localStore.markSubscriptionPosted(taskForAutoExpense.linkedSubscriptionId, taskForAutoExpense.yearMonth)
            }
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(householdId.isNotBlank() && taskId.isNotBlank()) { "ID da tarefa inválido." }
            val ref = db.collection("households").document(householdId).collection("tasks").document(taskId)
            docPath = ref.path
            val updates = mutableMapOf<String, Any>(
                "completed" to completed,
                "completedByUserId" to if (completed) completedByUserId.ifBlank { uid } else "",
                "completedByName" to if (completed) completedByName.take(80) else "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (generatedTxId != null) {
                updates["linkedTransactionId"] = generatedTxId
            }
            ref.update(updates).await()
            if (completed && taskForAutoExpense != null && taskForAutoExpense.linkedSubscriptionId.isNotBlank()) {
                val subRef = db.collection("households").document(householdId)
                    .collection("subscriptions").document(taskForAutoExpense.linkedSubscriptionId)
                runCatching {
                    subRef.update(
                        mapOf(
                            "lastPostedYearMonth" to taskForAutoExpense.yearMonth,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(householdId: String, taskId: String): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.deleteTask(taskId)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/tasks/$taskId"
        return try {
            requireUserId()
            require(householdId.isNotBlank() && taskId.isNotBlank()) { "ID da tarefa inválido." }
            val ref = db.collection("households").document(householdId).collection("tasks").document(taskId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // 4. Finance Transactions Observation & Mutations (under /households/{householdId}/transactions)
    fun observeHouseholdTransactions(householdId: String): Flow<List<FinanceTransaction>> = flow {
        val currentUid = currentEffectiveUserId()
        if (isLocalOfflineMode()) {
            emitAll(
                localStore!!.transactionsFlow.map { list ->
                    list.filter { tx ->
                        isVisibleForMember(
                            currentUserId = currentUid,
                            ownerUserId = tx.userId,
                            assignedOrResponsibleMemberId = tx.responsibleMemberId,
                            privacyScope = tx.privacyScope,
                            visibleToMemberIds = tx.visibleToMemberIds
                        )
                    }
                }
            )
            return@flow
        }
        val uid = requireUserId()
        require(householdId.isNotBlank()) { "Household ID inválido." }
        val path = "households/$householdId/transactions"
        emitAll(
            db.collection("households").document(householdId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { tx ->
                            isVisibleForMember(
                                currentUserId = uid,
                                ownerUserId = tx.userId,
                                assignedOrResponsibleMemberId = tx.responsibleMemberId,
                                privacyScope = tx.privacyScope,
                                visibleToMemberIds = tx.visibleToMemberIds
                            )
                        }
                        .sortedByDescending { it.dateEpochDay }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTransaction(
        householdId: String,
        title: String,
        amountOriginal: Double,
        currency: String,
        exchangeRateToBrl: Double,
        type: String,
        budgetBucket: String,
        category: String,
        dateEpochDay: Long,
        recurringSourceId: String = "",
        createdByName: String = "Membro",
        createdByColorHex: Long = 0xFF0E7C5B,
        createdByEmoji: String = "🌿",
        privacyScope: String = PrivacyScope.FAMILY,
        visibleToMemberIds: List<String> = emptyList(),
        responsibleMemberId: String = "",
        responsibleMemberName: String = createdByName,
        responsibleMemberColorHex: Long = createdByColorHex,
        responsibleMemberEmoji: String = createdByEmoji,
        splitMode: String = SplitMode.NONE,
        splitWithMemberIds: List<String> = emptyList(),
        splitSharesMap: Map<String, Double> = emptyMap(),
        splitShares: List<ExpenseSplitShare> = emptyList(),
        linkedTaskId: String = "",
        generateCalendarTask: Boolean = false
    ): Result<String> {
        val validScope = sanitizePrivacyScope(privacyScope)
        val validSplit = sanitizeSplitMode(splitMode)

        if (isLocalOfflineMode()) {
            var resolvedLinkedTaskId = linkedTaskId
            val txId = localStore!!.addTransaction(
                householdId = householdId,
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = exchangeRateToBrl,
                type = type,
                budgetBucket = budgetBucket,
                category = category,
                dateEpochDay = dateEpochDay,
                recurringSourceId = recurringSourceId,
                createdByName = createdByName,
                createdByColorHex = createdByColorHex,
                createdByEmoji = createdByEmoji,
                privacyScope = validScope,
                visibleToMemberIds = visibleToMemberIds,
                responsibleMemberId = responsibleMemberId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                responsibleMemberName = responsibleMemberName.ifBlank { createdByName },
                responsibleMemberColorHex = responsibleMemberColorHex,
                responsibleMemberEmoji = responsibleMemberEmoji.ifBlank { "🌿" },
                splitMode = validSplit,
                splitWithMemberIds = splitWithMemberIds,
                splitSharesMap = splitSharesMap,
                splitShares = splitShares,
                linkedTaskId = resolvedLinkedTaskId,
                generateCalendarTask = generateCalendarTask
            )
            if (generateCalendarTask && resolvedLinkedTaskId.isBlank()) {
                val scopeLabel = if (validScope == PrivacyScope.PERSONAL) "Conta Individual" else "Conta Familiar"
                resolvedLinkedTaskId = localStore.addTask(
                    householdId = householdId,
                    title = "Pagar: $title",
                    notes = "$scopeLabel • Valor: ${formatCurrencyOriginal(amountOriginal, currency)}",
                    category = "Finanças",
                    priority = "HIGH",
                    dateEpochDay = dateEpochDay,
                    alarmHour = 9,
                    alarmMinute = 0,
                    alarmEnabled = true,
                    privacyScope = validScope,
                    visibleToMemberIds = visibleToMemberIds,
                    assignedMemberId = responsibleMemberId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                    assignedMemberName = responsibleMemberName.ifBlank { createdByName },
                    assignedMemberColorHex = responsibleMemberColorHex,
                    assignedMemberEmoji = if (validScope == PrivacyScope.PERSONAL) "🔒" else responsibleMemberEmoji,
                    createdByName = createdByName,
                    createdByColorHex = createdByColorHex,
                    linkedTransactionId = txId,
                    estimatedCostBrl = amountOriginal * (if (currency == "BRL") 1.0 else exchangeRateToBrl),
                    splitMode = validSplit,
                    splitShares = splitShares
                )
                localStore.linkTransactionToTask(txId, resolvedLinkedTaskId)
            } else if (resolvedLinkedTaskId.isNotBlank()) {
                localStore.linkTaskToTransaction(resolvedLinkedTaskId, txId)
            }
            return Result.success(txId)
        }

        var docPath = "households/$householdId/transactions"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe a descrição do lançamento." }
            require(amountOriginal > 0.0) { "O valor deve ser maior que zero." }

            val validCurrency = if (currency in listOf("BRL", "USD", "EUR", "GBP")) currency else "BRL"
            val safeRate = if (validCurrency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
            val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)
            val validType = if (type in listOf("EXPENSE", "INCOME")) type else "EXPENSE"
            val validBucket = if (validType == "INCOME") {
                "INCOME"
            } else if (budgetBucket in listOf("NEEDS", "WANTS", "SAVINGS")) {
                budgetBucket
            } else {
                "NEEDS"
            }
            val cleanCategory = category.trim().take(50).ifBlank { "Geral" }
            val yearMonth = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
            val cleanAuthor = createdByName.trim().take(80).ifBlank { "Família" }
            val cleanResponsible = responsibleMemberName.trim().take(80).ifBlank { cleanAuthor }

            val docRef = db.collection("households").document(householdId).collection("transactions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "householdId" to householdId,
                "title" to cleanTitle,
                "amountOriginal" to amountOriginal,
                "currency" to validCurrency,
                "exchangeRateToBrl" to safeRate,
                "amountBrl" to amountBrl,
                "type" to validType,
                "budgetBucket" to validBucket,
                "category" to cleanCategory,
                "dateEpochDay" to dateEpochDay.coerceIn(0L, 100000L),
                "yearMonth" to yearMonth,
                "recurringSourceId" to recurringSourceId.take(128),
                "createdByName" to cleanAuthor,
                "createdByColorHex" to createdByColorHex,
                "createdByEmoji" to createdByEmoji.trim().take(16).ifBlank { "🌿" },
                "privacyScope" to validScope,
                "visibleToMemberIds" to visibleToMemberIds.take(20),
                "responsibleMemberId" to responsibleMemberId.take(128).ifBlank { uid },
                "responsibleMemberName" to cleanResponsible,
                "responsibleMemberColorHex" to responsibleMemberColorHex,
                "responsibleMemberEmoji" to responsibleMemberEmoji.trim().take(16).ifBlank { "🌿" },
                "splitMode" to validSplit,
                "splitWithMemberIds" to splitWithMemberIds.take(20),
                "splitSharesMap" to splitSharesMap,
                "splitShares" to splitSharesToFirestoreList(splitShares),
                "linkedTaskId" to linkedTaskId.take(128),
                "generateCalendarTask" to generateCalendarTask,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            if (generateCalendarTask && linkedTaskId.isBlank()) {
                val scopeLabel = if (validScope == PrivacyScope.PERSONAL) "Conta Individual" else "Conta Familiar"
                val taskRes = addTask(
                    householdId = householdId,
                    title = "Pagar: $cleanTitle",
                    notes = "$scopeLabel • Valor: ${formatCurrencyOriginal(amountOriginal, validCurrency)}",
                    category = "Finanças",
                    priority = "HIGH",
                    dateEpochDay = dateEpochDay,
                    alarmHour = 9,
                    alarmMinute = 0,
                    alarmEnabled = true,
                    privacyScope = validScope,
                    visibleToMemberIds = visibleToMemberIds,
                    assignedMemberId = responsibleMemberId.ifBlank { uid },
                    assignedMemberName = cleanResponsible,
                    assignedMemberColorHex = responsibleMemberColorHex,
                    assignedMemberEmoji = if (validScope == PrivacyScope.PERSONAL) "🔒" else responsibleMemberEmoji,
                    createdByName = cleanAuthor,
                    createdByColorHex = createdByColorHex,
                    linkedTransactionId = docRef.id,
                    estimatedCostBrl = amountBrl,
                    splitMode = validSplit,
                    splitShares = splitShares
                )
                val newTaskId = taskRes.getOrNull()
                if (!newTaskId.isNullOrBlank()) {
                    runCatching {
                        docRef.update(
                            mapOf(
                                "linkedTaskId" to newTaskId,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(householdId: String, transactionId: String): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.deleteTransaction(transactionId)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/transactions/$transactionId"
        return try {
            requireUserId()
            require(householdId.isNotBlank() && transactionId.isNotBlank()) { "ID da transação inválido." }
            val ref = db.collection("households").document(householdId).collection("transactions").document(transactionId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // 5. Subscriptions & Fixed Bills Observation & Mutations (under /households/{householdId}/subscriptions)
    fun observeHouseholdSubscriptions(householdId: String): Flow<List<SubscriptionBill>> = flow {
        val currentUid = currentEffectiveUserId()
        if (isLocalOfflineMode()) {
            emitAll(
                localStore!!.subscriptionsFlow.map { list ->
                    list.filter { sub ->
                        isVisibleForMember(
                            currentUserId = currentUid,
                            ownerUserId = sub.userId,
                            assignedOrResponsibleMemberId = sub.responsibleMemberId,
                            privacyScope = sub.privacyScope,
                            visibleToMemberIds = sub.visibleToMemberIds
                        )
                    }
                }
            )
            return@flow
        }
        val uid = requireUserId()
        require(householdId.isNotBlank()) { "Household ID inválido." }
        val path = "households/$householdId/subscriptions"
        emitAll(
            db.collection("households").document(householdId).collection("subscriptions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SubscriptionBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { sub ->
                            isVisibleForMember(
                                currentUserId = uid,
                                ownerUserId = sub.userId,
                                assignedOrResponsibleMemberId = sub.responsibleMemberId,
                                privacyScope = sub.privacyScope,
                                visibleToMemberIds = sub.visibleToMemberIds
                            )
                        }
                        .sortedBy { it.dueDayOfMonth }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addSubscriptionBill(
        householdId: String,
        title: String,
        amountOriginal: Double,
        currency: String,
        exchangeRateToBrl: Double,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String = PrivacyScope.FAMILY,
        visibleToMemberIds: List<String> = emptyList(),
        responsibleMemberId: String = "",
        responsibleMemberName: String = "Família",
        responsibleMemberColorHex: Long = 0xFF0E7C5B,
        responsibleMemberEmoji: String = "🌿",
        splitMode: String = SplitMode.NONE,
        splitWithMemberIds: List<String> = emptyList(),
        splitSharesMap: Map<String, Double> = emptyMap(),
        splitShares: List<ExpenseSplitShare> = emptyList(),
        linkedTaskId: String = "",
        syncDueDayToCalendar: Boolean = true,
        targetYearMonth: String = DateCalendarUtils.currentYearMonth()
    ): Result<String> {
        val validScope = sanitizePrivacyScope(privacyScope)
        val validSplit = sanitizeSplitMode(splitMode)

        if (isLocalOfflineMode()) {
            val id = localStore!!.addSubscriptionBill(
                householdId = householdId,
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = exchangeRateToBrl,
                category = category,
                budgetBucket = budgetBucket,
                dueDayOfMonth = dueDayOfMonth,
                reminderDaysBefore = reminderDaysBefore,
                autoPostEnabled = autoPostEnabled,
                privacyScope = validScope,
                visibleToMemberIds = visibleToMemberIds,
                responsibleMemberId = responsibleMemberId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                responsibleMemberName = responsibleMemberName.ifBlank { "Família" },
                responsibleMemberColorHex = responsibleMemberColorHex,
                responsibleMemberEmoji = responsibleMemberEmoji.ifBlank { "🌿" },
                splitMode = validSplit,
                splitWithMemberIds = splitWithMemberIds,
                splitSharesMap = splitSharesMap,
                splitShares = splitShares,
                linkedTaskId = linkedTaskId,
                syncDueDayToCalendar = syncDueDayToCalendar,
                targetYearMonth = targetYearMonth
            )
            return Result.success(id)
        }
        var docPath = "households/$householdId/subscriptions"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe o nome da assinatura ou conta fixa." }
            require(amountOriginal > 0.0) { "O valor deve ser maior que zero." }

            val validCurrency = if (currency in listOf("BRL", "USD", "EUR", "GBP")) currency else "BRL"
            val safeRate = if (validCurrency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
            val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)
            val validBucket = if (budgetBucket in listOf("NEEDS", "WANTS", "SAVINGS")) budgetBucket else "NEEDS"

            val docRef = db.collection("households").document(householdId).collection("subscriptions").document()
            docPath = docRef.path

            var resolvedLinkedTaskId = linkedTaskId.take(128)
            if (syncDueDayToCalendar && resolvedLinkedTaskId.isBlank()) {
                val parts = targetYearMonth.split("-")
                val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
                val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
                val maxDay = DateCalendarUtils.daysInMonth(year, month)
                val dueEpochDay = DateCalendarUtils.toEpochDay(year, month, dueDayOfMonth.coerceIn(1, maxDay))
                val scopeLabel = if (validScope == PrivacyScope.PERSONAL) "Conta Individual" else "Custo Mensal Familiar"
                val taskRes = addTask(
                    householdId = householdId,
                    title = "Pagar: $cleanTitle",
                    notes = "$scopeLabel • Vence todo dia $dueDayOfMonth (${formatCurrencyOriginal(amountOriginal, validCurrency)})",
                    category = "Finanças",
                    priority = "HIGH",
                    dateEpochDay = dueEpochDay,
                    alarmHour = 9,
                    alarmMinute = 0,
                    alarmEnabled = true,
                    privacyScope = validScope,
                    visibleToMemberIds = visibleToMemberIds,
                    assignedMemberId = responsibleMemberId.take(128).ifBlank { uid },
                    assignedMemberName = responsibleMemberName.trim().take(80).ifBlank { "Família" },
                    assignedMemberColorHex = responsibleMemberColorHex,
                    assignedMemberEmoji = if (validScope == PrivacyScope.PERSONAL) "🔒" else responsibleMemberEmoji,
                    createdByName = responsibleMemberName.trim().take(80).ifBlank { "Família" },
                    createdByColorHex = responsibleMemberColorHex,
                    linkedSubscriptionId = docRef.id,
                    estimatedCostBrl = amountBrl,
                    autoCreateExpenseOnComplete = true,
                    splitMode = validSplit,
                    splitShares = splitShares
                )
                resolvedLinkedTaskId = taskRes.getOrNull().orEmpty().take(128)
            }

            val payload = mapOf(
                "userId" to uid,
                "householdId" to householdId,
                "title" to cleanTitle,
                "amountOriginal" to amountOriginal,
                "currency" to validCurrency,
                "exchangeRateToBrl" to safeRate,
                "amountBrl" to amountBrl,
                "category" to category.trim().take(50).ifBlank { "Assinatura" },
                "budgetBucket" to validBucket,
                "dueDayOfMonth" to dueDayOfMonth.coerceIn(1, 31).toLong(),
                "reminderDaysBefore" to reminderDaysBefore.coerceIn(0, 15).toLong(),
                "autoPostEnabled" to autoPostEnabled,
                "lastPostedYearMonth" to "",
                "active" to true,
                "privacyScope" to validScope,
                "visibleToMemberIds" to visibleToMemberIds.take(20),
                "responsibleMemberId" to responsibleMemberId.take(128).ifBlank { uid },
                "responsibleMemberName" to responsibleMemberName.trim().take(80).ifBlank { "Família" },
                "responsibleMemberColorHex" to responsibleMemberColorHex,
                "responsibleMemberEmoji" to responsibleMemberEmoji.trim().take(16).ifBlank { "🌿" },
                "splitMode" to validSplit,
                "splitWithMemberIds" to splitWithMemberIds.take(20),
                "splitSharesMap" to splitSharesMap,
                "splitShares" to splitSharesToFirestoreList(splitShares),
                "linkedTaskId" to resolvedLinkedTaskId,
                "syncDueDayToCalendar" to syncDueDayToCalendar,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun postSubscriptionForMonth(
        subscription: SubscriptionBill,
        yearMonth: String,
        createdByName: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            val parts = yearMonth.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
            val dueEpochDay = DateCalendarUtils.toEpochDay(year, month, subscription.dueDayOfMonth.toInt().coerceIn(1, 28))
            localStore!!.addTransaction(
                householdId = subscription.householdId,
                title = "${subscription.title} (Conta Recorrente)",
                amountOriginal = subscription.amountOriginal,
                currency = subscription.currency,
                exchangeRateToBrl = subscription.exchangeRateToBrl,
                type = "EXPENSE",
                budgetBucket = subscription.budgetBucket,
                category = subscription.category,
                dateEpochDay = dueEpochDay,
                recurringSourceId = subscription.id,
                createdByName = subscription.responsibleMemberName.ifBlank { createdByName },
                createdByColorHex = subscription.responsibleMemberColorHex,
                createdByEmoji = subscription.responsibleMemberEmoji,
                privacyScope = subscription.privacyScope,
                visibleToMemberIds = subscription.visibleToMemberIds,
                responsibleMemberId = subscription.responsibleMemberId,
                responsibleMemberName = subscription.responsibleMemberName.ifBlank { createdByName },
                responsibleMemberColorHex = subscription.responsibleMemberColorHex,
                responsibleMemberEmoji = subscription.responsibleMemberEmoji,
                splitMode = subscription.splitMode,
                splitWithMemberIds = subscription.splitWithMemberIds,
                splitSharesMap = subscription.splitSharesMap,
                splitShares = subscription.splitShares,
                linkedTaskId = subscription.linkedTaskId
            )
            localStore.markSubscriptionPosted(subscription.id, yearMonth)
            if (subscription.linkedTaskId.isNotBlank()) {
                localStore.toggleTaskCompleted(
                    taskId = subscription.linkedTaskId,
                    completed = true,
                    completedByName = createdByName
                )
            }
            return Result.success(Unit)
        }
        var docPath = "households/${subscription.householdId}/subscriptions/${subscription.id}"
        return try {
            requireUserId()
            require(subscription.id.isNotBlank() && subscription.householdId.isNotBlank()) { "Assinatura inválida." }
            val parts = yearMonth.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
            val dueEpochDay = DateCalendarUtils.toEpochDay(year, month, subscription.dueDayOfMonth.toInt().coerceIn(1, 28))

            // 1. Add transaction entry preserving responsibility, privacy scope, and split shares
            val txResult = addTransaction(
                householdId = subscription.householdId,
                title = "${subscription.title} (Conta Recorrente)",
                amountOriginal = subscription.amountOriginal,
                currency = subscription.currency,
                exchangeRateToBrl = subscription.exchangeRateToBrl,
                type = "EXPENSE",
                budgetBucket = subscription.budgetBucket,
                category = subscription.category,
                dateEpochDay = dueEpochDay,
                recurringSourceId = subscription.id,
                createdByName = subscription.responsibleMemberName.ifBlank { createdByName },
                createdByColorHex = subscription.responsibleMemberColorHex,
                createdByEmoji = subscription.responsibleMemberEmoji,
                privacyScope = subscription.privacyScope,
                visibleToMemberIds = subscription.visibleToMemberIds,
                responsibleMemberId = subscription.responsibleMemberId,
                responsibleMemberName = subscription.responsibleMemberName.ifBlank { createdByName },
                responsibleMemberColorHex = subscription.responsibleMemberColorHex,
                responsibleMemberEmoji = subscription.responsibleMemberEmoji,
                splitMode = subscription.splitMode,
                splitWithMemberIds = subscription.splitWithMemberIds,
                splitSharesMap = subscription.splitSharesMap,
                splitShares = subscription.splitShares,
                linkedTaskId = subscription.linkedTaskId
            )
            if (txResult.isFailure) {
                return Result.failure(txResult.exceptionOrNull() ?: Exception("Falha ao lançar conta fixa"))
            }

            // 2. Mark subscription lastPostedYearMonth
            val subRef = db.collection("households").document(subscription.householdId)
                .collection("subscriptions").document(subscription.id)
            docPath = subRef.path
            subRef.update(
                mapOf(
                    "lastPostedYearMonth" to yearMonth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSubscriptionBill(householdId: String, subscriptionId: String): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.deleteSubscription(subscriptionId)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/subscriptions/$subscriptionId"
        return try {
            requireUserId()
            require(householdId.isNotBlank() && subscriptionId.isNotBlank()) { "ID da assinatura inválido." }
            val ref = db.collection("households").document(householdId).collection("subscriptions").document(subscriptionId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // 6. Savings Goals Observation & Mutations (under /households/{householdId}/savings_goals)
    fun observeHouseholdSavingsGoals(householdId: String): Flow<List<SavingsGoal>> = flow {
        val currentUid = currentEffectiveUserId()
        if (isLocalOfflineMode()) {
            emitAll(
                localStore!!.savingsGoalsFlow.map { list ->
                    list.filter { goal ->
                        isVisibleForMember(
                            currentUserId = currentUid,
                            ownerUserId = goal.userId,
                            assignedOrResponsibleMemberId = goal.responsibleMemberId,
                            privacyScope = goal.privacyScope,
                            visibleToMemberIds = emptyList()
                        )
                    }
                }
            )
            return@flow
        }
        val uid = requireUserId()
        require(householdId.isNotBlank()) { "Household ID inválido." }
        val path = "households/$householdId/savings_goals"
        emitAll(
            db.collection("households").document(householdId).collection("savings_goals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { goal ->
                            isVisibleForMember(
                                currentUserId = uid,
                                ownerUserId = goal.userId,
                                assignedOrResponsibleMemberId = goal.responsibleMemberId,
                                privacyScope = goal.privacyScope,
                                visibleToMemberIds = emptyList()
                            )
                        }
                        .sortedByDescending { it.incomePercentage }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addSavingsGoal(
        householdId: String,
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String,
        privacyScope: String = PrivacyScope.FAMILY,
        responsibleMemberId: String = "",
        responsibleMemberName: String = "Família",
        responsibleMemberColorHex: Long = 0xFF0E7C5B,
        responsibleMemberEmoji: String = "🌿",
        splitMode: String = SplitMode.EQUAL,
        memberContributionsMap: Map<String, Double> = emptyMap(),
        linkedTaskId: String = ""
    ): Result<String> {
        val validScope = sanitizePrivacyScope(privacyScope)
        val validSplit = sanitizeSplitMode(splitMode)

        if (isLocalOfflineMode()) {
            val id = localStore!!.addSavingsGoal(
                householdId = householdId,
                title = title,
                targetAmountBrl = targetAmountBrl,
                currentAmountBrl = currentAmountBrl,
                incomePercentage = incomePercentage,
                category = category,
                targetYearMonth = targetYearMonth,
                privacyScope = validScope,
                responsibleMemberId = responsibleMemberId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                responsibleMemberName = responsibleMemberName.ifBlank { "Família" },
                responsibleMemberColorHex = responsibleMemberColorHex,
                responsibleMemberEmoji = responsibleMemberEmoji.ifBlank { "🌿" },
                splitMode = validSplit,
                memberContributionsMap = memberContributionsMap,
                linkedTaskId = linkedTaskId
            )
            return Result.success(id)
        }
        var docPath = "households/$householdId/savings_goals"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe o nome da meta de economia." }
            require(targetAmountBrl > 0.0) { "O valor alvo deve ser maior que zero." }

            val docRef = db.collection("households").document(householdId).collection("savings_goals").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "householdId" to householdId,
                "title" to cleanTitle,
                "targetAmountBrl" to targetAmountBrl.coerceIn(1.0, 1_000_000_000.0),
                "currentAmountBrl" to currentAmountBrl.coerceIn(0.0, 1_000_000_000.0),
                "incomePercentage" to incomePercentage.coerceIn(0, 100).toLong(),
                "category" to category.trim().take(50).ifBlank { "Reserva" },
                "targetYearMonth" to targetYearMonth.trim().take(7),
                "privacyScope" to validScope,
                "responsibleMemberId" to responsibleMemberId.take(128).ifBlank { uid },
                "responsibleMemberName" to responsibleMemberName.trim().take(80).ifBlank { "Família" },
                "responsibleMemberColorHex" to responsibleMemberColorHex,
                "responsibleMemberEmoji" to responsibleMemberEmoji.trim().take(16).ifBlank { "🌿" },
                "splitMode" to validSplit,
                "memberContributionsMap" to memberContributionsMap,
                "linkedTaskId" to linkedTaskId.take(128),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun contributeToSavingsGoal(
        goal: SavingsGoal,
        addedAmountBrl: Double,
        recordAsExpenseInMonth: Boolean,
        createdByName: String,
        memberColorHex: Long = goal.responsibleMemberColorHex,
        memberEmoji: String = goal.responsibleMemberEmoji
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.contributeToSavingsGoal(
                goalId = goal.id,
                addedAmountBrl = addedAmountBrl,
                memberId = LocalOfflineStore.LOCAL_USER_ID
            )
            if (recordAsExpenseInMonth) {
                localStore.addTransaction(
                    householdId = goal.householdId,
                    title = "Aporte Meta: ${goal.title}",
                    amountOriginal = addedAmountBrl,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    type = "EXPENSE",
                    budgetBucket = "SAVINGS",
                    category = goal.category,
                    dateEpochDay = DateCalendarUtils.todayEpochDay(),
                    createdByName = createdByName,
                    createdByColorHex = memberColorHex,
                    createdByEmoji = memberEmoji,
                    privacyScope = goal.privacyScope,
                    responsibleMemberId = goal.responsibleMemberId.ifBlank { LocalOfflineStore.LOCAL_USER_ID },
                    responsibleMemberName = goal.responsibleMemberName.ifBlank { createdByName },
                    responsibleMemberColorHex = memberColorHex,
                    responsibleMemberEmoji = memberEmoji,
                    splitMode = goal.splitMode,
                    linkedTaskId = goal.linkedTaskId
                )
            }
            return Result.success(Unit)
        }
        var docPath = "households/${goal.householdId}/savings_goals/${goal.id}"
        return try {
            val uid = requireUserId()
            require(goal.id.isNotBlank() && goal.householdId.isNotBlank()) { "Meta inválida." }
            require(addedAmountBrl > 0.0) { "O aporte deve ser maior que zero." }
            val newTotal = (goal.currentAmountBrl + addedAmountBrl).coerceIn(0.0, 1_000_000_000.0)
            val updatedContributions = goal.memberContributionsMap.toMutableMap()
            updatedContributions[uid] = (updatedContributions[uid] ?: 0.0) + addedAmountBrl

            val ref = db.collection("households").document(goal.householdId)
                .collection("savings_goals").document(goal.id)
            docPath = ref.path
            ref.update(
                mapOf(
                    "currentAmountBrl" to newTotal,
                    "memberContributionsMap" to updatedContributions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (recordAsExpenseInMonth) {
                addTransaction(
                    householdId = goal.householdId,
                    title = "Aporte Meta: ${goal.title}",
                    amountOriginal = addedAmountBrl,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    type = "EXPENSE",
                    budgetBucket = "SAVINGS",
                    category = goal.category,
                    dateEpochDay = DateCalendarUtils.todayEpochDay(),
                    createdByName = createdByName,
                    createdByColorHex = memberColorHex,
                    createdByEmoji = memberEmoji,
                    privacyScope = goal.privacyScope,
                    responsibleMemberId = uid,
                    responsibleMemberName = createdByName,
                    responsibleMemberColorHex = memberColorHex,
                    responsibleMemberEmoji = memberEmoji,
                    splitMode = goal.splitMode,
                    linkedTaskId = goal.linkedTaskId
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavingsGoal(householdId: String, goalId: String): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.deleteSavingsGoal(goalId)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/savings_goals/$goalId"
        return try {
            requireUserId()
            require(householdId.isNotBlank() && goalId.isNotBlank()) { "ID da meta inválido." }
            val ref = db.collection("households").document(householdId).collection("savings_goals").document(goalId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
