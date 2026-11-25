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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class AuraFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val localStore: LocalOfflineStore? = null,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        localStore = LocalOfflineStore(context.applicationContext),
        appContext = context.applicationContext
    )

    init {
        runCatching {
            val settings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
        }
    }

    private fun isPlaceholderMemberName(raw: String?): Boolean {
        val clean = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return clean.isBlank() ||
            clean == "você" ||
            clean == "voce" ||
            clean == "vo" ||
            clean == "você (omni)" ||
            clean == "família" ||
            clean == "familia" ||
            clean == "membro" ||
            clean == "membro da família"
    }

    private fun resolveRealAuthenticatedName(vararg candidates: String?): String {
        for (c in candidates) {
            val trimmed = c?.trim().orEmpty()
            if (!isPlaceholderMemberName(trimmed)) {
                return trimmed.take(80)
            }
        }
        val firebaseUser = auth.currentUser
        val authDisplay = firebaseUser?.displayName?.trim().orEmpty()
        if (!isPlaceholderMemberName(authDisplay)) {
            return authDisplay.take(80)
        }
        val emailPrefix = firebaseUser?.email?.substringBefore('@')?.trim().orEmpty()
        if (emailPrefix.isNotBlank()) {
            return emailPrefix.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString()
            }.take(80)
        }
        return "Marcus"
    }

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
        val deduplicated = linkedMapOf<String, FamilyMemberBadge>()
        for (m in members) {
            val cleanName = m.displayName.trim()
            if (isPlaceholderMemberName(cleanName)) continue
            val key = m.userId.trim().ifBlank { cleanName.lowercase(Locale.ROOT) }
            val existing = deduplicated[key]
            deduplicated[key] = FamilyMemberBadge(
                userId = m.userId.trim().ifBlank { existing?.userId.orEmpty() },
                displayName = cleanName,
                email = m.email.trim().ifBlank { existing?.email.orEmpty() },
                avatarBase64 = m.avatarBase64.ifBlank { existing?.avatarBase64.orEmpty() },
                colorHex = if (m.colorHex != 0L) m.colorHex else (existing?.colorHex ?: 0xFF0E7C5B),
                avatarEmoji = m.avatarEmoji.ifBlank { existing?.avatarEmoji ?: "🌿" },
                role = m.role.ifBlank { existing?.role ?: "ADMIN" }
            )
        }
        return deduplicated.values.map { m ->
            mapOf(
                "userId" to m.userId.take(128),
                "displayName" to m.displayName.take(80),
                "email" to m.email.take(160),
                "avatarBase64" to m.avatarBase64,
                "colorHex" to m.colorHex,
                "avatarEmoji" to m.avatarEmoji.take(16),
                "role" to m.role.take(24)
            )
        }
    }

    private fun bankAccountsToFirestoreList(accounts: List<BankAccount>): List<Map<String, Any>> {
        return accounts.map { rawAcc ->
            val acc = rawAcc.sanitized()
            val isCash = acc.isCashOrWallet()
            val safeLimit = if (isCash) 0.0 else acc.creditLimitBrl.coerceAtLeast(0.0)
            val safeClosing = if (isCash || safeLimit <= 0.0) 0 else acc.closingDay.coerceIn(1, 31)
            val safeDue = if (isCash || safeLimit <= 0.0) 0 else acc.dueDay.coerceIn(1, 31)
            mapOf(
                "id" to acc.id.take(64).ifBlank { "acc_" + UUID.randomUUID().toString().take(8) },
                "name" to acc.name.take(80),
                "institution" to acc.institution.take(60),
                "initialBalanceBrl" to acc.initialBalanceBrl,
                "creditLimitBrl" to safeLimit,
                "closingDay" to safeClosing,
                "dueDay" to safeDue,
                "colorHex" to acc.colorHex,
                "iconEmoji" to acc.iconEmoji.take(16),
                "privacyScope" to acc.privacyScope.take(24)
            )
        }
    }

    private fun muralItemsToFirestoreList(items: List<FamilyMuralItem>): List<Map<String, Any>> {
        return items.filterNot { it.id.startsWith("mural_seed_") }.take(200).map { item ->
            mapOf(
                "id" to item.id.take(64).ifBlank { "mural_" + UUID.randomUUID().toString().take(8) },
                "title" to item.title.take(240),
                "notes" to item.notes.take(800),
                "category" to item.category.take(40).ifBlank { "Compras" },
                "addedBy" to item.addedBy.take(80).ifBlank { "Família" },
                "addedByColorHex" to item.addedByColorHex,
                "checkedBy" to item.checkedBy.take(80),
                "checkedByColorHex" to item.checkedByColorHex,
                "completed" to item.completed,
                "pinned" to item.pinned,
                "isNote" to item.isNote,
                "createdAtEpochMs" to item.createdAtEpochMs
            )
        }
    }

    fun defaultHouseholdIdForUser(userId: String): String {
        if (isLocalOfflineMode()) {
            return localStore?.userProfileFlow?.value?.householdId ?: "FAM_OMNI_LOCAL"
        }
        val suffix = userId.replace(Regex("[^a-zA-Z0-9]"), "").take(8).uppercase(Locale.ROOT)
        return "FAM_${suffix.ifBlank { "OMNI01" }}"
    }

    // 1. UserProfile Stream & Initialization
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

    /**
     * Fluxo "Nuvem Primeiro" (Cloud-First) com Fusão Segura de Famílias (ex.: FAM_SKRKRVKS -> FAM_OMNI2448),
     * eliminação de "Você" / "VO", limpeza de conta Dinheiro/Carteira e download integral antes de abrir o Hub.
     */
    suspend fun ensureUserProfileAndHousehold(
        displayNameRaw: String?,
        avatarBase64Raw: String = ""
    ): Result<UserProfile> {
        if (isLocalOfflineMode()) {
            return Result.success(localStore!!.userProfileFlow.value)
        }
        var currentPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userEmail = auth.currentUser?.email?.trim().orEmpty()
            val userDocRef = db.collection("users").document(uid)
            currentPath = userDocRef.path
            val userSnap = userDocRef.get().await()
            val existingUserProfile = if (userSnap.exists()) {
                userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null

            val resolvedName = resolveRealAuthenticatedName(
                existingUserProfile?.displayName,
                displayNameRaw,
                auth.currentUser?.displayName
            )
            val initials = resolvedName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { "OM" }
            val resolvedAvatarBase64 = avatarBase64Raw.ifBlank { existingUserProfile?.avatarBase64.orEmpty() }

            // 1. Consultar famílias do usuário usando exclusivamente queries filtradas compatíveis com Security Rules
            // (NUNCA fazer listagem aberta db.collection("households").get() sem filtro)
            currentPath = "households"
            val userGeneratedCode = defaultHouseholdIdForUser(uid)
            val ownedHouseholdsSnap = runCatching {
                db.collection("households").whereEqualTo("ownerId", uid).get().await()
            }.getOrNull()
            val memberHouseholdsSnap = runCatching {
                db.collection("households").whereArrayContains("memberUids", uid).get().await()
            }.getOrNull()
            val profileHouseholdSnap = existingUserProfile?.householdId?.takeIf { it.isNotBlank() }?.let { savedId ->
                runCatching { db.collection("households").document(savedId).get().await() }.getOrNull()
            }
            val directOfficialSnap = runCatching {
                db.collection("households").document(OFFICIAL_HOUSEHOLD_ID).get().await()
            }.getOrNull()
            val directLegacyDupSnap = runCatching {
                db.collection("households").document(LEGACY_DUPLICATE_HOUSEHOLD_ID).get().await()
            }.getOrNull()
            val directGeneratedSnap = runCatching {
                db.collection("households").document(userGeneratedCode).get().await()
            }.getOrNull()

            val discoveredById = linkedMapOf<String, DocumentSnapshot>()
            fun recordIfValid(doc: DocumentSnapshot?) {
                if (doc != null && doc.exists()) {
                    discoveredById[doc.id] = doc
                }
            }
            ownedHouseholdsSnap?.documents?.forEach { recordIfValid(it) }
            memberHouseholdsSnap?.documents?.forEach { recordIfValid(it) }
            recordIfValid(profileHouseholdSnap)
            recordIfValid(directOfficialSnap)
            recordIfValid(directLegacyDupSnap)
            recordIfValid(directGeneratedSnap)

            val activeHouseholdDocs = discoveredById.values.filter { doc ->
                val migratedTo = doc.getString("migratedTo")?.trim().orEmpty()
                migratedTo.isBlank()
            }

            // Identificar famílias ligadas ao usuário (por ownerId, memberUids, members, userProfile.householdId ou FAM_OMNI2448 / FAM_SKRKRVKS)
            val candidateDocs = activeHouseholdDocs.filter { doc ->
                val house = doc.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val docId = doc.id
                val matchesOwner = house?.ownerId == uid
                val matchesMemberUid = house?.memberUids?.contains(uid) == true
                val matchesMember = house?.members?.any { m ->
                    m.userId == uid ||
                        (userEmail.isNotBlank() && m.email.equals(userEmail, ignoreCase = true)) ||
                        (!isPlaceholderMemberName(m.displayName) && m.displayName.equals(resolvedName, ignoreCase = true))
                } == true
                val matchesProfileHouse = existingUserProfile?.householdId?.isNotBlank() == true &&
                    docId.equals(existingUserProfile.householdId, ignoreCase = true)
                val matchesKnownOfficialOrDup = docId == OFFICIAL_HOUSEHOLD_ID ||
                    docId == LEGACY_DUPLICATE_HOUSEHOLD_ID ||
                    docId == userGeneratedCode
                matchesOwner || matchesMemberUid || matchesMember || matchesProfileHouse || matchesKnownOfficialOrDup
            }

            // Determinar a família oficial alvo:
            // Se FAM_OMNI2448 estiver acessível para este usuário (ou se veio de FAM_SKRKRVKS), ela é a família oficial alvo.
            val canUseOfficialHousehold = (directOfficialSnap != null && directOfficialSnap.exists() &&
                directOfficialSnap.getString("migratedTo").isNullOrBlank()) ||
                (directLegacyDupSnap != null && directLegacyDupSnap.exists()) ||
                existingUserProfile?.householdId == OFFICIAL_HOUSEHOLD_ID ||
                existingUserProfile?.householdId == LEGACY_DUPLICATE_HOUSEHOLD_ID

            val primaryHouseholdId: String = when {
                canUseOfficialHousehold -> OFFICIAL_HOUSEHOLD_ID
                candidateDocs.any { it.id == OFFICIAL_HOUSEHOLD_ID } -> OFFICIAL_HOUSEHOLD_ID
                candidateDocs.isNotEmpty() -> {
                    // Priorizar família que não seja o código auto-gerado FAM_<UID> se houver outra, ou a que tiver mais membros
                    candidateDocs.sortedWith(
                        compareByDescending<DocumentSnapshot> { it.id == OFFICIAL_HOUSEHOLD_ID }
                            .thenByDescending { it.id != userGeneratedCode && it.id != LEGACY_DUPLICATE_HOUSEHOLD_ID }
                            .thenByDescending {
                                val h = it.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                                h?.members?.size ?: 0
                            }
                    ).first().id
                }
                existingUserProfile?.householdId?.isNotBlank() == true &&
                    existingUserProfile.householdId != LEGACY_DUPLICATE_HOUSEHOLD_ID -> {
                    val savedId = existingUserProfile.householdId
                    val savedSnap = profileHouseholdSnap
                        ?: runCatching { db.collection("households").document(savedId).get().await() }.getOrNull()
                    if (savedSnap != null && savedSnap.exists() && !savedSnap.getString("migratedTo").isNullOrBlank()) {
                        savedSnap.getString("migratedTo")!!.trim()
                    } else if (savedSnap != null) {
                        savedId
                    } else {
                        userGeneratedCode
                    }
                }
                directOfficialSnap != null && !directOfficialSnap.exists() -> OFFICIAL_HOUSEHOLD_ID
                else -> userGeneratedCode
            }

            // 2. Fusão Segura Automática: se existirem famílias duplicadas (ex.: FAM_SKRKRVKS ou FAM_<UID>) além da oficial
            val duplicateDocsToFuse = candidateDocs.filter { doc ->
                doc.id != primaryHouseholdId &&
                    (doc.id == LEGACY_DUPLICATE_HOUSEHOLD_ID ||
                        doc.id == userGeneratedCode ||
                        doc.getString("ownerId") == uid)
            }.toMutableList()

            // Garantir verificação direta do documento FAM_SKRKRVKS mesmo que não tenha vindo no filtro
            if (primaryHouseholdId == OFFICIAL_HOUSEHOLD_ID &&
                duplicateDocsToFuse.none { it.id == LEGACY_DUPLICATE_HOUSEHOLD_ID } &&
                directLegacyDupSnap != null &&
                directLegacyDupSnap.exists() &&
                directLegacyDupSnap.getString("migratedTo").isNullOrBlank()
            ) {
                duplicateDocsToFuse.add(directLegacyDupSnap)
            }

            for (dupDoc in duplicateDocsToFuse) {
                runCatching {
                    fuseDuplicateHouseholdIntoOfficial(
                        duplicateSnap = dupDoc,
                        targetHouseholdId = primaryHouseholdId
                    )
                }
            }

            // 3. Atualizar ou criar apenas o documento users/{uid} do próprio usuário autenticado apontando para primaryHouseholdId
            val finalProfile = UserProfile(
                id = uid,
                userId = uid,
                displayName = resolvedName,
                email = userEmail,
                avatarBase64 = resolvedAvatarBase64,
                householdId = primaryHouseholdId,
                themeMode = existingUserProfile?.themeMode?.ifBlank { "SYSTEM" } ?: "SYSTEM",
                baseCurrency = existingUserProfile?.baseCurrency?.ifBlank { "BRL" } ?: "BRL",
                memberColorHex = existingUserProfile?.memberColorHex?.takeIf { it != 0L } ?: 0xFF0E7C5BL,
                avatarEmoji = existingUserProfile?.avatarEmoji?.ifBlank { "🌿" } ?: "🌿",
                avatarInitials = initials,
                memberRole = existingUserProfile?.memberRole?.ifBlank { "ADMIN" } ?: "ADMIN",
                defaultSplitPercentage = existingUserProfile?.defaultSplitPercentage ?: 50.0
            )
            currentPath = userDocRef.path
            val userPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to finalProfile.displayName,
                "email" to finalProfile.email,
                "householdId" to primaryHouseholdId,
                "themeMode" to finalProfile.themeMode,
                "baseCurrency" to finalProfile.baseCurrency,
                "memberColorHex" to finalProfile.memberColorHex,
                "avatarEmoji" to finalProfile.avatarEmoji,
                "avatarInitials" to finalProfile.avatarInitials,
                "memberRole" to finalProfile.memberRole,
                "defaultSplitPercentage" to finalProfile.defaultSplitPercentage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (finalProfile.avatarBase64.isNotBlank()) {
                userPayload["avatarBase64"] = finalProfile.avatarBase64
            }
            if (!userSnap.exists()) {
                userPayload["createdAt"] = FieldValue.serverTimestamp()
            }
            userDocRef.set(userPayload, SetOptions.merge()).await()

            // 4. Garantir documento households/{primaryHouseholdId} normalizado com memberUids e inviteOpen
            val houseRef = db.collection("households").document(primaryHouseholdId)
            currentPath = houseRef.path
            val houseSnap = runCatching { houseRef.get().await() }.getOrNull()
            val myBadge = FamilyMemberBadge(
                userId = uid,
                displayName = finalProfile.displayName,
                email = finalProfile.email,
                avatarBase64 = finalProfile.avatarBase64,
                colorHex = finalProfile.memberColorHex,
                avatarEmoji = finalProfile.avatarEmoji,
                role = finalProfile.memberRole,
                isCurrentUser = true
            )

            if (houseSnap == null || !houseSnap.exists()) {
                val defaultFamilyName = if (primaryHouseholdId == OFFICIAL_HOUSEHOLD_ID) {
                    "Família Vaz"
                } else {
                    "Família ${finalProfile.displayName.split(" ").firstOrNull() ?: "Omni"}"
                }
                val initialMemberUids = extractValidMemberUids(listOf(myBadge), emptyList(), uid)
                val housePayload = mapOf(
                    "householdId" to primaryHouseholdId,
                    "name" to defaultFamilyName,
                    "ownerId" to uid,
                    "memberUids" to initialMemberUids,
                    "inviteOpen" to false,
                    "monthlyIncomeBrl" to 0.0,
                    "needsPercentage" to 60L,
                    "wantsPercentage" to 20L,
                    "savingsPercentage" to 20L,
                    "defaultSplitMode" to SplitMode.EQUAL,
                    "members" to memberBadgesToFirestoreList(listOf(myBadge)),
                    "bankAccounts" to bankAccountsToFirestoreList(BankAccountCatalog.defaultAccounts()),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                houseRef.set(housePayload, SetOptions.merge()).await()
            } else {
                val existingHouse = houseSnap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingHouse != null) {
                    val normalizedMembers = normalizeHouseholdMembersList(
                        rawMembers = existingHouse.members,
                        currentUid = uid,
                        currentName = finalProfile.displayName,
                        currentEmail = finalProfile.email,
                        currentAvatarBase64 = finalProfile.avatarBase64,
                        currentColorHex = finalProfile.memberColorHex,
                        currentEmoji = finalProfile.avatarEmoji,
                        currentRole = finalProfile.memberRole
                    )
                    val sanitizedAccounts = existingHouse.bankAccounts
                        .ifEmpty { BankAccountCatalog.defaultAccounts() }
                        .map { it.sanitized() }
                    val cleanMural = existingHouse.muralItems.filterNot { it.id.startsWith("mural_seed_") }
                    val isLegacy503020 = existingHouse.needsPercentage == 50L &&
                        existingHouse.wantsPercentage == 30L &&
                        existingHouse.savingsPercentage == 20L
                    val needsPct = if (isLegacy503020) 60L else existingHouse.needsPercentage
                    val wantsPct = if (isLegacy503020) 20L else existingHouse.wantsPercentage
                    val savingsPct = if (isLegacy503020) 20L else existingHouse.savingsPercentage
                    val syncedMemberUids = extractValidMemberUids(
                        members = normalizedMembers,
                        existingMemberUids = existingHouse.memberUids,
                        uid,
                        existingHouse.ownerId
                    )

                    val updates = mutableMapOf<String, Any>(
                        "householdId" to primaryHouseholdId,
                        "memberUids" to FieldValue.arrayUnion(*syncedMemberUids.toTypedArray()),
                        "members" to memberBadgesToFirestoreList(normalizedMembers),
                        "bankAccounts" to bankAccountsToFirestoreList(sanitizedAccounts),
                        "muralItems" to muralItemsToFirestoreList(cleanMural),
                        "needsPercentage" to needsPct,
                        "wantsPercentage" to wantsPct,
                        "savingsPercentage" to savingsPct,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!houseSnap.contains("inviteOpen")) {
                        updates["inviteOpen"] = false
                    }
                    houseRef.set(updates, SetOptions.merge()).await()
                }
            }

            // 5. Realizar download completo (Pull Inicial) das subcoleções antes de abrir o Hub
            pullFullHouseholdSnapshotToLocal(primaryHouseholdId, finalProfile)

            Result.success(finalProfile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    private fun normalizeHouseholdMembersList(
        rawMembers: List<FamilyMemberBadge>,
        currentUid: String,
        currentName: String,
        currentEmail: String,
        currentAvatarBase64: String,
        currentColorHex: Long,
        currentEmoji: String,
        currentRole: String
    ): List<FamilyMemberBadge> {
        val merged = linkedMapOf<String, FamilyMemberBadge>()
        for (m in rawMembers) {
            val isMe = (m.userId.isNotBlank() && m.userId == currentUid) ||
                (currentEmail.isNotBlank() && m.email.equals(currentEmail, ignoreCase = true)) ||
                (!isPlaceholderMemberName(m.displayName) && m.displayName.equals(currentName, ignoreCase = true))

            val resolvedName = when {
                isMe -> currentName
                !isPlaceholderMemberName(m.displayName) -> m.displayName.trim()
                m.email.isNotBlank() -> m.email.substringBefore('@').replaceFirstChar { it.titlecase(Locale.ROOT) }
                else -> ""
            }
            if (resolvedName.isBlank()) continue

            val resolvedUid = if (isMe) currentUid else m.userId.trim()
            val key = resolvedUid.ifBlank { resolvedName.lowercase(Locale.ROOT) }
            val prev = merged[key]
            merged[key] = FamilyMemberBadge(
                userId = resolvedUid.ifBlank { prev?.userId.orEmpty() },
                displayName = resolvedName,
                email = if (isMe && currentEmail.isNotBlank()) currentEmail else m.email.ifBlank { prev?.email.orEmpty() },
                avatarBase64 = when {
                    isMe && currentAvatarBase64.isNotBlank() -> currentAvatarBase64
                    m.avatarBase64.isNotBlank() -> m.avatarBase64
                    else -> prev?.avatarBase64.orEmpty()
                },
                colorHex = when {
                    isMe && currentColorHex != 0L -> currentColorHex
                    m.colorHex != 0L -> m.colorHex
                    else -> prev?.colorHex ?: 0xFF0E7C5B
                },
                avatarEmoji = if (isMe && currentEmoji.isNotBlank()) currentEmoji else m.avatarEmoji.ifBlank { prev?.avatarEmoji ?: "🌿" },
                role = m.role.ifBlank { prev?.role ?: currentRole.ifBlank { "ADMIN" } },
                isCurrentUser = isMe
            )
        }

        if (merged.values.none { it.userId == currentUid || it.displayName.equals(currentName, ignoreCase = true) }) {
            merged[currentUid] = FamilyMemberBadge(
                userId = currentUid,
                displayName = currentName,
                email = currentEmail,
                avatarBase64 = currentAvatarBase64,
                colorHex = if (currentColorHex != 0L) currentColorHex else 0xFF0E7C5B,
                avatarEmoji = currentEmoji.ifBlank { "🌿" },
                role = currentRole.ifBlank { "ADMIN" },
                isCurrentUser = true
            )
        }
        return merged.values.toList()
    }

    /**
     * Fusão Segura de Família Duplicada (ex.: FAM_SKRKRVKS -> FAM_OMNI2448):
     * Copia todas as subcoleções preservando o ID original de cada documento com SetOptions.merge(),
     * mescla contas bancárias, categorias, orçamentos, mural e membros, atualiza users/{uid} vinculados
     * e marca a família antiga com migratedTo = targetHouseholdId sem apagá-la.
     */
    private suspend fun fuseDuplicateHouseholdIntoOfficial(
        duplicateSnap: DocumentSnapshot,
        targetHouseholdId: String
    ) {
        val dupId = duplicateSnap.id
        if (dupId.isBlank() || dupId == targetHouseholdId) return
        val alreadyMigrated = duplicateSnap.getString("migratedTo")?.trim().orEmpty()
        if (alreadyMigrated == targetHouseholdId) return

        val dupRef = db.collection("households").document(dupId)
        val targetRef = db.collection("households").document(targetHouseholdId)

        val subcollections = listOf(
            "tasks",
            "transactions",
            "subscriptions",
            "savings_goals",
            "health_logs",
            "insulin_records",
            "diary_entries"
        )

        for (sub in subcollections) {
            runCatching {
                val subSnap = dupRef.collection(sub).get().await()
                for (doc in subSnap.documents) {
                    val docId = doc.id
                    if (docId.startsWith("task_demo_") ||
                        docId.startsWith("tx_demo_") ||
                        docId.startsWith("sub_demo_") ||
                        docId.startsWith("goal_demo_") ||
                        docId.startsWith("diary_seed_")
                    ) continue
                    val data = doc.data?.toMutableMap() ?: continue
                    data["householdId"] = targetHouseholdId
                    targetRef.collection(sub).document(docId).set(data, SetOptions.merge()).await()
                }
            }
        }

        // Mesclar campos do documento household (membros, contas, categorias, orçamentos e mural)
        val dupHouse = duplicateSnap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val targetSnap = targetRef.get().await()
        val targetHouse = if (targetSnap.exists()) {
            targetSnap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else null

        if (dupHouse != null) {
            val mergedMembers = (targetHouse?.members.orEmpty() + dupHouse.members)
                .filterNot { isPlaceholderMemberName(it.displayName) }
            val existingAccountIds = targetHouse?.bankAccounts.orEmpty().map { it.id }.toSet()
            val mergedAccounts = (targetHouse?.bankAccounts.orEmpty() +
                dupHouse.bankAccounts.filterNot { it.id in existingAccountIds })
                .ifEmpty { BankAccountCatalog.defaultAccounts() }
                .map { it.sanitized() }
            val mergedCategories = (targetHouse?.customCategories.orEmpty() + dupHouse.customCategories)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val mergedBudgets = dupHouse.categoryBudgetsBrl + targetHouse?.categoryBudgetsBrl.orEmpty()
            val existingMuralIds = targetHouse?.muralItems.orEmpty().map { it.id }.toSet()
            val mergedMural = (targetHouse?.muralItems.orEmpty() +
                dupHouse.muralItems.filterNot { it.id in existingMuralIds })
                .filterNot { it.id.startsWith("mural_seed_") }

            val currentUid = auth.currentUser?.uid?.trim().orEmpty()
            val syncedMemberUids = extractValidMemberUids(
                members = mergedMembers,
                existingMemberUids = targetHouse?.memberUids.orEmpty() + dupHouse.memberUids,
                currentUid,
                targetHouse?.ownerId.orEmpty(),
                dupHouse.ownerId
            )

            val mergePayload = mutableMapOf<String, Any>(
                "householdId" to targetHouseholdId,
                "memberUids" to FieldValue.arrayUnion(*syncedMemberUids.toTypedArray()),
                "members" to memberBadgesToFirestoreList(mergedMembers),
                "bankAccounts" to bankAccountsToFirestoreList(mergedAccounts),
                "customCategories" to mergedCategories,
                "categoryBudgetsBrl" to mergedBudgets,
                "muralItems" to muralItemsToFirestoreList(mergedMural),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!targetSnap.contains("inviteOpen")) {
                mergePayload["inviteOpen"] = false
            }
            if ((targetHouse?.monthlyIncomeBrl ?: 0.0) <= 0.0 && dupHouse.monthlyIncomeBrl > 0.0 && dupHouse.monthlyIncomeBrl != 8500.0) {
                mergePayload["monthlyIncomeBrl"] = dupHouse.monthlyIncomeBrl
            }
            if (targetHouse?.pregnancyDppDate.isNullOrBlank() && dupHouse.pregnancyDppDate.isNotBlank()) {
                mergePayload["pregnancyDppDate"] = dupHouse.pregnancyDppDate
            }
            if (targetHouse?.insulinConfigs.isNullOrEmpty() && dupHouse.insulinConfigs.isNotEmpty()) {
                mergePayload["insulinConfigs"] = dupHouse.insulinConfigs.map { cfg ->
                    mapOf(
                        "id" to cfg.id,
                        "memberId" to cfg.memberId,
                        "insulinType" to cfg.insulinType,
                        "defaultUnits" to cfg.defaultUnits,
                        "alarmHour" to cfg.alarmHour,
                        "alarmMinute" to cfg.alarmMinute,
                        "alarmEnabled" to cfg.alarmEnabled
                    )
                }
            }
            targetRef.set(mergePayload, SetOptions.merge()).await()

            // Atualizar APENAS o documento users/{currentUid} do próprio usuário autenticado (nunca gravar em users/{otherUid} de terceiros)
            if (currentUid.isNotEmpty()) {
                runCatching {
                    db.collection("users").document(currentUid)
                        .set(
                            mapOf(
                                "householdId" to targetHouseholdId,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        ).await()
                }
            }
        }

        // Marcar documento duplicado com migratedTo = targetHouseholdId sem apagá-lo
        val currentUidForDup = auth.currentUser?.uid?.trim().orEmpty()
        val dupUpdates = mutableMapOf<String, Any>(
            "migratedTo" to targetHouseholdId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (currentUidForDup.isNotEmpty()) {
            dupUpdates["memberUids"] = FieldValue.arrayUnion(currentUidForDup)
        }
        dupRef.set(dupUpdates, SetOptions.merge()).await()
    }

    private fun extractValidMemberUids(
        members: List<FamilyMemberBadge>,
        existingMemberUids: List<String> = emptyList(),
        vararg extraUids: String
    ): List<String> {
        return (extraUids.toList() + existingMemberUids + members.map { it.userId })
            .map { it.trim() }
            .filter {
                it.isNotEmpty() &&
                    !it.startsWith("mem_") &&
                    it != LocalOfflineStore.LOCAL_USER_ID &&
                    it != "local_offline_user" &&
                    it != "member_admin"
            }
            .distinct()
    }

    /**
     * Faz o download completo (get()) de households/{householdId} e de todas as subcoleções
     * para o armazenamento local antes de liberar o Hub.
     */
    suspend fun pullFullHouseholdSnapshotToLocal(
        householdId: String,
        profile: UserProfile
    ) {
        if (householdId.isBlank()) return
        val uid = auth.currentUser?.uid ?: profile.uid
        val houseRef = db.collection("households").document(householdId)
        val houseSnap = runCatching { houseRef.get().await() }.getOrNull() ?: return
        val remoteHousehold = houseSnap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.let { h ->
                h.copy(
                    id = houseSnap.id,
                    householdId = houseSnap.id,
                    bankAccounts = h.bankAccounts.ifEmpty { BankAccountCatalog.defaultAccounts() }.map { it.sanitized() },
                    muralItems = h.muralItems.filterNot { it.id.startsWith("mural_seed_") }
                )
            } ?: Household(id = householdId, householdId = householdId)

        val tasksSnap = runCatching { houseRef.collection("tasks").get().await() }.getOrNull()
        val remoteTasks = tasksSnap?.documents.orEmpty().mapNotNull { doc ->
            if (doc.id.startsWith("task_demo_")) {
                runCatching { doc.reference.delete() }
                return@mapNotNull null
            }
            val base = doc.toObject(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            base?.copy(
                id = doc.id,
                isInbox = doc.getBoolean("isInbox") ?: doc.getBoolean("inbox") ?: base.isInbox,
                isEssential = doc.getBoolean("isEssential") ?: doc.getBoolean("essential") ?: base.isEssential
            )
        }.filter { task ->
            isVisibleForMember(uid, task.userId, task.assignedMemberId, task.privacyScope, task.visibleToMemberIds)
        }

        val txSnap = runCatching { houseRef.collection("transactions").get().await() }.getOrNull()
        val remoteTx = txSnap?.documents.orEmpty().mapNotNull { doc ->
            if (doc.id.startsWith("tx_demo_")) {
                runCatching { doc.reference.delete() }
                return@mapNotNull null
            }
            doc.toObject(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(id = doc.id)
        }.filter { tx ->
            isVisibleForMember(uid, tx.userId, tx.responsibleMemberId, tx.privacyScope, tx.visibleToMemberIds)
        }

        val subsSnap = runCatching { houseRef.collection("subscriptions").get().await() }.getOrNull()
        val remoteSubs = subsSnap?.documents.orEmpty().mapNotNull { doc ->
            if (doc.id.startsWith("sub_demo_")) {
                runCatching { doc.reference.delete() }
                return@mapNotNull null
            }
            doc.toObject(SubscriptionBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(id = doc.id)
        }.filter { sub ->
            isVisibleForMember(uid, sub.userId, sub.responsibleMemberId, sub.privacyScope, sub.visibleToMemberIds)
        }

        val goalsSnap = runCatching { houseRef.collection("savings_goals").get().await() }.getOrNull()
        val remoteGoals = goalsSnap?.documents.orEmpty().mapNotNull { doc ->
            if (doc.id.startsWith("goal_demo_")) {
                runCatching { doc.reference.delete() }
                return@mapNotNull null
            }
            doc.toObject(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(id = doc.id)
        }.filter { goal ->
            isVisibleForMember(uid, goal.userId, goal.responsibleMemberId, goal.privacyScope, emptyList())
        }

        localStore?.replaceAllWithCloudPull(
            profile = profile,
            household = remoteHousehold,
            tasks = remoteTasks,
            transactions = remoteTx,
            subscriptions = remoteSubs,
            savingsGoals = remoteGoals
        )

        val ctx = appContext
        if (ctx != null) {
            runCatching {
                val healthRepo = HealthRepository(ctx)
                val healthSnap = runCatching { houseRef.collection("health_logs").get().await() }.getOrNull()
                val remoteGlucose = mutableListOf<GlucoseRecord>()
                val remoteBp = mutableListOf<BloodPressureRecord>()
                for (doc in healthSnap?.documents.orEmpty()) {
                    val data = doc.data ?: continue
                    val logType = (data["logType"] as? String).orEmpty()
                    if (logType == "BLOOD_PRESSURE" || data.containsKey("systolic")) {
                        val systolic = (data["systolic"] as? Number)?.toInt() ?: continue
                        val diastolic = (data["diastolic"] as? Number)?.toInt() ?: continue
                        val pulse = (data["pulseBpm"] as? Number)?.toInt() ?: 75
                        val dateIso = (data["dateIso"] as? String).orEmpty()
                        val timeFormatted = (data["timeFormatted"] as? String).orEmpty()
                        val memberId = (data["memberId"] as? String)?.trim().orEmpty()
                        val notes = (data["notes"] as? String).orEmpty()
                        val epochMs = (data["timestampEpochMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        remoteBp.add(
                            BloodPressureRecord(
                                id = doc.id,
                                dateIso = dateIso.ifBlank { DateCalendarUtils.todayIsoDate() },
                                timeFormatted = timeFormatted.ifBlank { "08:00" },
                                systolic = systolic,
                                diastolic = diastolic,
                                pulseBpm = pulse,
                                memberId = memberId.ifBlank { profile.displayName.ifBlank { "Marcus" } },
                                notes = notes,
                                timestampEpochMillis = epochMs
                            )
                        )
                    } else if (data.containsKey("valueMgDl")) {
                        val valueMgDl = (data["valueMgDl"] as? Number)?.toInt() ?: continue
                        val rawSlot = (data["slot"] as? String).orEmpty()
                        val dateIso = (data["dateIso"] as? String).orEmpty()
                        val timeFormatted = (data["timeFormatted"] as? String).orEmpty()
                        val memberId = (data["memberId"] as? String)?.trim().orEmpty()
                        val notes = (data["notes"] as? String).orEmpty()
                        val epochMs = (data["timestampEpochMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        remoteGlucose.add(
                            GlucoseRecord(
                                id = doc.id,
                                dateIso = dateIso.ifBlank { DateCalendarUtils.todayIsoDate() },
                                timeFormatted = timeFormatted.ifBlank { "08:00" },
                                slot = DiabetesSlotCatalog.normalizeSlot(rawSlot),
                                valueMgDl = valueMgDl,
                                memberId = memberId.ifBlank { profile.displayName.ifBlank { "Marcus" } },
                                notes = notes,
                                timestampEpochMillis = epochMs
                            )
                        )
                    }
                }

                val insulinSnap = runCatching { houseRef.collection("insulin_records").get().await() }.getOrNull()
                val remoteInsulin = insulinSnap?.documents.orEmpty().mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val units = (data["units"] as? Number)?.toInt() ?: return@mapNotNull null
                    val insulinType = (data["insulinType"] as? String)?.trim().orEmpty()
                    val site = (data["injectionSite"] as? String)?.trim().orEmpty()
                    val dateIso = (data["dateIso"] as? String)?.trim().orEmpty()
                    val timeFormatted = (data["timeFormatted"] as? String)?.trim().orEmpty()
                    val memberId = (data["memberId"] as? String)?.trim().orEmpty()
                    val notes = (data["notes"] as? String)?.trim().orEmpty()
                    val epochMs = (data["timestampEpochMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    InsulinRecord(
                        id = doc.id,
                        dateIso = dateIso.ifBlank { DateCalendarUtils.todayIsoDate() },
                        timeFormatted = timeFormatted.ifBlank { "08:00" },
                        insulinType = insulinType.ifBlank { "Basal / Glargina" },
                        units = units,
                        injectionSite = site.ifBlank { InsulinSiteCatalog.ABDOMEN_ESQ },
                        memberId = memberId.ifBlank { profile.displayName.ifBlank { "Marcus" } },
                        notes = notes,
                        timestampEpochMillis = epochMs
                    )
                }

                healthRepo.replaceWithCloudHealthPull(
                    glucoseRecords = remoteGlucose,
                    bloodPressureRecords = remoteBp,
                    insulinRecords = remoteInsulin,
                    insulinConfigs = remoteHousehold.insulinConfigs,
                    loggedUserId = uid,
                    loggedUserName = profile.displayName
                )

                val customRepo = CustomizationRepository(ctx)
                val currentCustom = customRepo.configFlow.value
                val cloudDpp = remoteHousehold.pregnancyDppDate.trim()
                val cloudMode = remoteHousehold.pregnancyComparisonMode.trim().uppercase(Locale.ROOT)
                if (cloudDpp.isNotBlank() || cloudMode.isNotBlank()) {
                    customRepo.updatePregnancyModule(
                        enabled = if (cloudDpp.isNotBlank()) true else currentCustom.pregnancyEnabled,
                        dppDate = cloudDpp.ifBlank { currentCustom.pregnancyDppDate },
                        notes = currentCustom.pregnancyNotes,
                        comparisonMode = cloudMode.ifBlank { currentCustom.pregnancyComparisonMode }
                    )
                }
            }
        }
    }

    /**
     * Botão "Forçar Sincronização Agora" no card "Status da Nuvem (Firestore)" do Perfil.
     */
    suspend fun forceFullCloudSyncNow(
        displayNameHint: String = "",
        avatarBase64Hint: String = ""
    ): Result<String> {
        if (isLocalOfflineMode()) {
            return Result.failure(IllegalStateException("Conecte sua conta Google para sincronizar com a nuvem."))
        }
        return try {
            val profileRes = ensureUserProfileAndHousehold(
                displayNameRaw = displayNameHint,
                avatarBase64Raw = avatarBase64Hint
            )
            val profile = profileRes.getOrThrow()
            val houseId = profile.householdId.ifBlank { OFFICIAL_HOUSEHOLD_ID }
            Result.success("Sincronização concluída com a família $houseId!")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatarBase64(
        householdId: String,
        currentHousehold: Household?,
        avatarBase64: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            val prof = localStore!!.userProfileFlow.value
            localStore.updateUserMemberIdentity(
                displayName = prof.displayName,
                memberColorHex = prof.memberColorHex,
                avatarEmoji = prof.avatarEmoji,
                avatarInitials = prof.avatarInitials,
                memberRole = prof.memberRole,
                defaultSplitPercentage = prof.defaultSplitPercentage,
                avatarBase64 = avatarBase64,
                email = prof.email
            )
            return Result.success(Unit)
        }
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            userRef.set(
                mapOf(
                    "avatarBase64" to avatarBase64,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            if (householdId.isNotBlank()) {
                val houseRef = db.collection("households").document(householdId)
                docPath = houseRef.path
                val snap = houseRef.get().await()
                val existingHouse = snap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: currentHousehold
                if (existingHouse != null) {
                    val updatedMembers = existingHouse.members.map { m ->
                        if (m.userId == uid || m.isCurrentUser) {
                            m.copy(avatarBase64 = avatarBase64)
                        } else {
                            m
                        }
                    }
                    val syncedMemberUids = extractValidMemberUids(
                        members = updatedMembers,
                        existingMemberUids = existingHouse.memberUids,
                        uid,
                        existingHouse.ownerId
                    )
                    houseRef.set(
                        mapOf(
                            "memberUids" to FieldValue.arrayUnion(*syncedMemberUids.toTypedArray()),
                            "members" to memberBadgesToFirestoreList(updatedMembers),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
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

    suspend fun updateUserMemberIdentity(
        displayName: String,
        memberColorHex: Long,
        avatarEmoji: String,
        avatarInitials: String = "",
        memberRole: String = "ADMIN",
        defaultSplitPercentage: Double = 50.0,
        avatarBase64: String = ""
    ): Result<Unit> {
        val cleanName = resolveRealAuthenticatedName(displayName)
        val cleanEmoji = avatarEmoji.trim().take(16).ifBlank { "🌿" }
        val cleanInitials = avatarInitials.trim().take(4).ifBlank {
            cleanName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { "OM" }
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
                defaultSplitPercentage = safeSplitPct,
                avatarBase64 = avatarBase64.ifBlank { localStore.userProfileFlow.value.avatarBase64 }
            )
            return Result.success(Unit)
        }

        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userEmail = auth.currentUser?.email?.trim().orEmpty()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "email" to userEmail,
                "memberColorHex" to memberColorHex,
                "avatarEmoji" to cleanEmoji,
                "avatarInitials" to cleanInitials,
                "memberRole" to validRole,
                "defaultSplitPercentage" to safeSplitPct,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (avatarBase64.isNotBlank()) {
                userUpdates["avatarBase64"] = avatarBase64
            }
            ref.set(userUpdates, SetOptions.merge()).await()

            // Sincronizar também na lista members do household atual para atualizar o chip imediatamente em todos os aparelhos
            val userSnap = ref.get().await()
            val houseId = userSnap.getString("householdId").orEmpty()
            if (houseId.isNotBlank()) {
                val houseRef = db.collection("households").document(houseId)
                val houseSnap = houseRef.get().await()
                val existingHouse = houseSnap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existingHouse != null) {
                    val normalized = normalizeHouseholdMembersList(
                        rawMembers = existingHouse.members,
                        currentUid = uid,
                        currentName = cleanName,
                        currentEmail = userEmail,
                        currentAvatarBase64 = avatarBase64.ifBlank { userSnap.getString("avatarBase64").orEmpty() },
                        currentColorHex = memberColorHex,
                        currentEmoji = cleanEmoji,
                        currentRole = validRole
                    )
                    val syncedMemberUids = extractValidMemberUids(
                        members = normalized,
                        existingMemberUids = existingHouse.memberUids,
                        uid,
                        existingHouse.ownerId
                    )
                    houseRef.set(
                        mapOf(
                            "memberUids" to FieldValue.arrayUnion(*syncedMemberUids.toTypedArray()),
                            "members" to memberBadgesToFirestoreList(normalized),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
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

    suspend fun updateThemeMode(themeMode: String): Result<Unit> {
        val validTheme = if (themeMode in listOf("SYSTEM", "LIGHT", "DARK", "AMOLED")) themeMode else "SYSTEM"
        localStore?.updateThemeMode(validTheme)
        if (isLocalOfflineMode()) {
            return Result.success(Unit)
        }
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            docPath = ref.path
            ref.set(
                mapOf(
                    "themeMode" to validTheme,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateHouseholdInviteOpen(
        householdId: String,
        inviteOpen: Boolean
    ): Result<Unit> {
        localStore?.updateHouseholdInviteOpen(inviteOpen)
        if (isLocalOfflineMode() || householdId.isBlank()) {
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "inviteOpen" to inviteOpen,
                    "memberUids" to FieldValue.arrayUnion(uid),
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

    suspend fun switchOrJoinHousehold(
        newHouseholdCodeRaw: String,
        familyNameRaw: String,
        isCreatingNewFamily: Boolean = false
    ): Result<String> {
        if (isLocalOfflineMode()) {
            val targetCode = sanitizeHouseholdCode(newHouseholdCodeRaw)
            val savedCode = localStore!!.switchHousehold(targetCode, familyNameRaw)
            return Result.success(savedCode)
        }
        var currentPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userEmail = auth.currentUser?.email?.trim().orEmpty()
            val targetHouseholdId = sanitizeHouseholdCode(newHouseholdCodeRaw)
            val familyName = familyNameRaw.trim().take(80).ifBlank {
                if (targetHouseholdId == OFFICIAL_HOUSEHOLD_ID) "Família Vaz" else "Família Omni"
            }

            val userRef = db.collection("users").document(uid)
            currentPath = userRef.path
            val userSnap = userRef.get().await()
            val userProf = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val cleanMemberName = resolveRealAuthenticatedName(userProf?.displayName)

            val myMemberBadge = FamilyMemberBadge(
                userId = uid,
                displayName = cleanMemberName,
                email = userEmail,
                avatarBase64 = userProf?.avatarBase64.orEmpty(),
                colorHex = userProf?.memberColorHex ?: 0xFF0E7C5B,
                avatarEmoji = userProf?.avatarEmoji?.ifBlank { "🌿" } ?: "🌿",
                role = userProf?.memberRole?.ifBlank { "ADMIN" } ?: "ADMIN",
                isCurrentUser = true
            )

            val houseRef = db.collection("households").document(targetHouseholdId)
            currentPath = houseRef.path

            val closedOrInvalidMessage =
                "Convite fechado ou código inválido. Peça para um membro da família ativar 'Aceitar novos membros' no Perfil."

            val snap = try {
                houseRef.get().await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return Result.failure(IllegalStateException(closedOrInvalidMessage))
                }
                throw e
            }

            if (!snap.exists()) {
                if (!isCreatingNewFamily) {
                    return Result.failure(IllegalStateException(closedOrInvalidMessage))
                }
                val housePayload = mapOf(
                    "householdId" to targetHouseholdId,
                    "name" to familyName,
                    "ownerId" to uid,
                    "memberUids" to listOf(uid),
                    "inviteOpen" to false,
                    "monthlyIncomeBrl" to 0.0,
                    "needsPercentage" to 60L,
                    "wantsPercentage" to 20L,
                    "savingsPercentage" to 20L,
                    "defaultSplitMode" to SplitMode.EQUAL,
                    "members" to memberBadgesToFirestoreList(listOf(myMemberBadge)),
                    "bankAccounts" to bankAccountsToFirestoreList(BankAccountCatalog.defaultAccounts()),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                houseRef.set(housePayload, SetOptions.merge()).await()
            } else {
                val existingHouse = snap.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return Result.failure(IllegalStateException(closedOrInvalidMessage))

                val isAlreadyMemberOrOwner = existingHouse.ownerId == uid ||
                    existingHouse.memberUids.contains(uid) ||
                    existingHouse.members.any { it.userId == uid }

                if (!isAlreadyMemberOrOwner && !existingHouse.inviteOpen) {
                    return Result.failure(IllegalStateException(closedOrInvalidMessage))
                }

                val normalized = normalizeHouseholdMembersList(
                    rawMembers = existingHouse.members,
                    currentUid = uid,
                    currentName = cleanMemberName,
                    currentEmail = userEmail,
                    currentAvatarBase64 = userProf?.avatarBase64.orEmpty(),
                    currentColorHex = userProf?.memberColorHex ?: 0xFF0E7C5B,
                    currentEmoji = userProf?.avatarEmoji?.ifBlank { "🌿" } ?: "🌿",
                    currentRole = userProf?.memberRole?.ifBlank { "ADMIN" } ?: "ADMIN"
                )
                try {
                    houseRef.update(
                        mapOf(
                            "memberUids" to FieldValue.arrayUnion(uid),
                            "members" to memberBadgesToFirestoreList(normalized),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: FirebaseFirestoreException) {
                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        return Result.failure(IllegalStateException(closedOrInvalidMessage))
                    }
                    throw e
                }
            }

            // Gravar householdId em users/{uid} do próprio usuário autenticado após sucesso na família
            currentPath = userRef.path
            userRef.set(
                mapOf(
                    "householdId" to targetHouseholdId,
                    "displayName" to cleanMemberName,
                    "email" to userEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            val effectiveProfile = (userProf ?: UserProfile(id = uid, userId = uid)).copy(
                householdId = targetHouseholdId,
                displayName = cleanMemberName,
                email = userEmail
            )
            pullFullHouseholdSnapshotToLocal(targetHouseholdId, effectiveProfile)

            Result.success(targetHouseholdId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun saveFamilyMember(
        householdId: String,
        currentHousehold: Household?,
        member: FamilyMemberBadge
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.saveFamilyMember(member)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            val uid = requireUserId()
            val cleanId = member.userId.ifBlank { "mem_" + UUID.randomUUID().toString().take(8) }
            val cleanMember = member.copy(
                userId = cleanId,
                displayName = member.displayName.trim().take(80).ifBlank { "Familiar" },
                avatarEmoji = member.avatarEmoji.trim().take(16).ifBlank { "👤" },
                role = member.role.ifBlank { "ADULT" }
            )
            val existing = currentHousehold?.members.orEmpty().toMutableList()
            val idx = existing.indexOfFirst { it.userId == cleanId }
            if (idx >= 0) {
                existing[idx] = cleanMember
            } else {
                existing.add(cleanMember)
            }
            val syncedMemberUids = extractValidMemberUids(
                members = existing,
                existingMemberUids = currentHousehold?.memberUids.orEmpty(),
                uid,
                currentHousehold?.ownerId.orEmpty()
            )
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(*syncedMemberUids.toTypedArray()),
                    "members" to memberBadgesToFirestoreList(existing),
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

    suspend fun removeFamilyMember(
        householdId: String,
        currentHousehold: Household?,
        memberUserId: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.removeFamilyMember(memberUserId)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            val uid = requireUserId()
            if (memberUserId == uid) {
                return Result.failure(IllegalArgumentException("Você não pode remover a si mesmo da família."))
            }
            val updated = currentHousehold?.members.orEmpty().filterNot { it.userId == memberUserId }
            val remainingMemberUids = extractValidMemberUids(
                members = updated,
                existingMemberUids = currentHousehold?.memberUids.orEmpty().filterNot { it == memberUserId },
                uid,
                currentHousehold?.ownerId.orEmpty()
            )
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "memberUids" to remainingMemberUids,
                    "members" to memberBadgesToFirestoreList(updated),
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

    suspend fun saveBankAccount(
        householdId: String,
        currentHousehold: Household?,
        account: BankAccount
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.saveBankAccount(account)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            val cleanId = account.id.ifBlank { "acc_" + UUID.randomUUID().toString().take(8) }
            val cleanAcc = account.copy(
                id = cleanId,
                name = account.name.trim().take(80).ifBlank { account.institution.ifBlank { "Conta" } },
                institution = account.institution.trim().take(60).ifBlank { "Banco" },
                iconEmoji = account.iconEmoji.trim().take(16).ifBlank { "🏦" }
            )
            val baseAccounts = currentHousehold?.bankAccounts.orEmpty().ifEmpty { BankAccountCatalog.defaultAccounts() }.toMutableList()
            val idx = baseAccounts.indexOfFirst { it.id == cleanId }
            if (idx >= 0) {
                baseAccounts[idx] = cleanAcc
            } else {
                baseAccounts.add(cleanAcc)
            }
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "bankAccounts" to bankAccountsToFirestoreList(baseAccounts),
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

    suspend fun deleteBankAccount(
        householdId: String,
        currentHousehold: Household?,
        accountId: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.deleteBankAccount(accountId)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            val baseAccounts = currentHousehold?.bankAccounts.orEmpty().ifEmpty { BankAccountCatalog.defaultAccounts() }
            val remaining = baseAccounts.filterNot { it.id == accountId }.ifEmpty { BankAccountCatalog.defaultAccounts() }
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "bankAccounts" to bankAccountsToFirestoreList(remaining),
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
                        val loaded = snapshot.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (loaded != null) {
                            val sanitizedAccounts = loaded.bankAccounts
                                .ifEmpty { BankAccountCatalog.defaultAccounts() }
                                .map { it.sanitized() }
                            loaded.copy(
                                bankAccounts = sanitizedAccounts,
                                muralItems = loaded.muralItems.filterNot { it.id.startsWith("mural_seed_") }
                            )
                        } else {
                            null
                        }
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

    suspend fun updateCategoryBudget(
        householdId: String,
        currentHousehold: Household?,
        category: String,
        limitBrl: Double
    ): Result<Unit> {
        val cleanCat = category.trim().take(50)
        if (cleanCat.isEmpty()) return Result.success(Unit)
        localStore?.updateCategoryBudget(cleanCat, limitBrl)
        if (isLocalOfflineMode()) {
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            val updatedMap = currentHousehold?.categoryBudgetsBrl.orEmpty().toMutableMap()
            if (limitBrl <= 0.0) {
                updatedMap.remove(cleanCat)
            } else {
                updatedMap[cleanCat] = limitBrl.coerceAtMost(1_000_000_000.0)
            }
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "categoryBudgetsBrl" to updatedMap,
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

    suspend fun saveCustomCategories(
        householdId: String,
        categories: List<String>
    ): Result<Unit> {
        val cleanList = categories.map { it.trim().take(40) }.filter { it.isNotEmpty() }.distinct().take(60)
        localStore?.saveCustomCategories(cleanList)
        if (isLocalOfflineMode()) {
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "customCategories" to cleanList,
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

    suspend fun syncHouseholdMuralItems(
        householdId: String,
        items: List<FamilyMuralItem>
    ): Result<Unit> {
        localStore?.syncHouseholdMuralItems(items)
        if (isLocalOfflineMode() || householdId.isBlank()) {
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "muralItems" to muralItemsToFirestoreList(items),
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

    suspend fun updateHouseholdPregnancyConfig(
        householdId: String,
        dppDate: String,
        comparisonMode: String
    ): Result<Unit> {
        val cleanDpp = dppDate.trim().take(20)
        val cleanMode = when (comparisonMode.trim().uppercase(Locale.ROOT)) {
            "DOCE", "FRUTA", "ANIMAL", "TODOS" -> comparisonMode.trim().uppercase(Locale.ROOT)
            else -> "TODOS"
        }
        if (isLocalOfflineMode() || householdId.isBlank()) {
            return Result.success(Unit)
        }
        var docPath = "households/$householdId"
        return try {
            requireUserId()
            val ref = db.collection("households").document(householdId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "pregnancyDppDate" to cleanDpp,
                    "pregnancyComparisonMode" to cleanMode,
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

    // 3. Tasks Observation & Mutations
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
                    snapshot.documents.mapNotNull { doc ->
                        val base = doc.toObject(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        base?.copy(
                            id = doc.id,
                            isInbox = doc.getBoolean("isInbox") ?: doc.getBoolean("inbox") ?: base.isInbox,
                            isEssential = doc.getBoolean("isEssential") ?: doc.getBoolean("essential") ?: base.isEssential
                        )
                    }
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
        location: String = "",
        category: String,
        priority: String,
        dateEpochDay: Long,
        endDateEpochDay: Long = 0L,
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
        recurrenceWeekdays: List<Int> = emptyList(),
        subtasks: List<SubtaskItem> = emptyList(),
        ringtoneCategory: String = "APP_DEFAULT"
    ): Result<String> {
        val validScope = sanitizePrivacyScope(privacyScope)
        val validSplit = sanitizeSplitMode(splitMode)
        val safeCost = estimatedCostBrl.coerceIn(0.0, 1_000_000_000.0)
        val safeEndEpoch = if (endDateEpochDay >= dateEpochDay) endDateEpochDay else 0L
        val safeWeekdays = recurrenceWeekdays.filter { it in 1..7 }.distinct().sorted()

        if (isLocalOfflineMode()) {
            val id = localStore!!.addTask(
                householdId = householdId,
                title = title,
                notes = notes,
                location = location,
                category = category,
                priority = priority,
                dateEpochDay = dateEpochDay,
                endDateEpochDay = safeEndEpoch,
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
                recurrenceWeekdays = safeWeekdays,
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
            val cleanLocation = location.trim().take(160)
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
                "location" to cleanLocation,
                "category" to cleanCategory,
                "priority" to validPriority,
                "isEssential" to isEssential,
                "dateEpochDay" to dateEpochDay.coerceIn(0L, 100000L),
                "endDateEpochDay" to safeEndEpoch.coerceIn(0L, 100000L),
                "yearMonth" to yearMonth,
                "alarmHour" to alarmHour.coerceIn(0, 23).toLong(),
                "alarmMinute" to alarmMinute.coerceIn(0, 59).toLong(),
                "alarmEnabled" to (if (isInbox) false else alarmEnabled),
                "completed" to false,
                "completedDates" to emptyList<String>(),
                "recurrenceWeekdays" to safeWeekdays,
                "isInbox" to isInbox,
                "durationMinutes" to durationMinutes.coerceIn(5, 720),
                "iconEmoji" to iconEmoji.trim().take(16).ifBlank { "📋" },
                "taskColorHex" to taskColorHex,
                "recurrence" to recurrence.trim().take(24).ifBlank { TaskRecurrence.ONCE },
                "subtasks" to subtasksToFirestoreList(subtasks),
                "ringtoneCategory" to ringtoneCategory.trim().take(256).ifBlank { "APP_DEFAULT" },
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

    suspend fun updateTask(
        householdId: String,
        taskId: String,
        title: String,
        notes: String,
        location: String = "",
        category: String,
        priority: String,
        dateEpochDay: Long,
        endDateEpochDay: Long = 0L,
        alarmHour: Int,
        alarmMinute: Int,
        alarmEnabled: Boolean,
        privacyScope: String,
        estimatedCostBrl: Double,
        autoCreateExpenseOnComplete: Boolean,
        isEssential: Boolean,
        isInbox: Boolean,
        durationMinutes: Int,
        iconEmoji: String,
        taskColorHex: Long,
        recurrence: String,
        recurrenceWeekdays: List<Int> = emptyList(),
        subtasks: List<SubtaskItem>,
        ringtoneCategory: String
    ): Result<Unit> {
        val validScope = sanitizePrivacyScope(privacyScope)
        val safeCost = estimatedCostBrl.coerceIn(0.0, 1_000_000_000.0)
        val safeEndEpoch = if (endDateEpochDay >= dateEpochDay) endDateEpochDay else 0L
        val safeWeekdays = recurrenceWeekdays.filter { it in 1..7 }.distinct().sorted()
        if (isLocalOfflineMode()) {
            localStore!!.updateTask(
                taskId = taskId,
                title = title,
                notes = notes,
                location = location,
                category = category,
                priority = priority,
                dateEpochDay = dateEpochDay,
                endDateEpochDay = safeEndEpoch,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute,
                alarmEnabled = alarmEnabled,
                privacyScope = validScope,
                estimatedCostBrl = safeCost,
                autoCreateExpenseOnComplete = autoCreateExpenseOnComplete,
                isEssential = isEssential,
                isInbox = isInbox,
                durationMinutes = durationMinutes,
                iconEmoji = iconEmoji,
                taskColorHex = taskColorHex,
                recurrence = recurrence,
                recurrenceWeekdays = safeWeekdays,
                subtasks = subtasks,
                ringtoneCategory = ringtoneCategory
            )
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/tasks/$taskId"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe o título da tarefa." }
            val cleanNotes = notes.trim().take(500)
            val cleanLocation = location.trim().take(160)
            val cleanCategory = category.trim().take(50).ifBlank { "Geral" }
            val validPriority = if (priority in listOf("LOW", "MEDIUM", "HIGH")) priority else "MEDIUM"
            val yearMonth = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
            val ref = db.collection("households").document(householdId).collection("tasks").document(taskId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "title" to cleanTitle,
                    "notes" to cleanNotes,
                    "location" to cleanLocation,
                    "category" to cleanCategory,
                    "priority" to validPriority,
                    "isEssential" to isEssential,
                    "dateEpochDay" to dateEpochDay.coerceIn(0L, 100000L),
                    "endDateEpochDay" to safeEndEpoch.coerceIn(0L, 100000L),
                    "yearMonth" to yearMonth,
                    "alarmHour" to alarmHour.coerceIn(0, 23).toLong(),
                    "alarmMinute" to alarmMinute.coerceIn(0, 59).toLong(),
                    "alarmEnabled" to (if (isInbox) false else alarmEnabled),
                    "isInbox" to isInbox,
                    "durationMinutes" to durationMinutes.coerceIn(5, 720),
                    "iconEmoji" to iconEmoji.trim().take(16).ifBlank { "📋" },
                    "taskColorHex" to taskColorHex,
                    "recurrence" to recurrence.trim().take(24).ifBlank { TaskRecurrence.ONCE },
                    "recurrenceWeekdays" to safeWeekdays,
                    "subtasks" to subtasksToFirestoreList(subtasks),
                    "ringtoneCategory" to ringtoneCategory.trim().take(256).ifBlank { "APP_DEFAULT" },
                    "privacyScope" to validScope,
                    "estimatedCostBrl" to safeCost,
                    "autoCreateExpenseOnComplete" to autoCreateExpenseOnComplete,
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
        targetDateIso: String = "",
        completedByUserId: String = "",
        completedByName: String = "Família",
        taskForAutoExpense: TaskItem? = null
    ): Result<Unit> {
        var generatedTxId: String? = null

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
                targetDateIso = targetDateIso,
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
            val currentDates = taskForAutoExpense?.completedDates.orEmpty()
            val updatedDates = if (targetDateIso.isNotBlank()) {
                if (completed) {
                    (currentDates + targetDateIso).distinct().sorted()
                } else {
                    currentDates.filterNot { it == targetDateIso }
                }
            } else {
                currentDates
            }
            val isRecurring = taskForAutoExpense?.isRecurringOrMultiDay ?: false
            val globalCompleted = if (isRecurring) false else completed
            val updates = mutableMapOf<String, Any>(
                "completed" to globalCompleted,
                "completedDates" to updatedDates,
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

    // 4. Finance Transactions Observation & Mutations
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
                    snapshot.documents.mapNotNull { doc ->
                        val base = doc.toObject(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        base?.copy(
                            id = doc.id,
                            isRecurring = doc.getBoolean("isRecurring") ?: doc.getBoolean("recurring") ?: base.isRecurring,
                            isCreditCard = doc.getBoolean("isCreditCard") ?: doc.getBoolean("creditCard") ?: base.isCreditCard,
                            isPaid = doc.getBoolean("isPaid") ?: doc.getBoolean("paid") ?: base.isPaid
                        )
                    }
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
        generateCalendarTask: Boolean = false,
        accountId: String = "acc_nubank",
        accountName: String = "Nubank",
        destinationAccountId: String = "",
        destinationAccountName: String = "",
        isRecurring: Boolean = false,
        isCreditCard: Boolean = false,
        installmentCount: Int = 1,
        isPaid: Boolean = !isCreditCard,
        accountClosingDay: Int = 25
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
                generateCalendarTask = generateCalendarTask,
                accountId = accountId,
                accountName = accountName,
                destinationAccountId = destinationAccountId,
                destinationAccountName = destinationAccountName,
                isRecurring = isRecurring,
                isCreditCard = isCreditCard,
                installmentCount = installmentCount,
                isPaid = isPaid
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
            val validType = if (type in listOf("EXPENSE", "INCOME", "TRANSFER")) type else "EXPENSE"
            val validBucket = when (validType) {
                "INCOME" -> "INCOME"
                "TRANSFER" -> "TRANSFER"
                else -> if (budgetBucket in listOf("NEEDS", "WANTS", "SAVINGS")) budgetBucket else "NEEDS"
            }
            val cleanCategory = category.trim().take(50).ifBlank { "Geral" }
            val cleanAuthor = createdByName.trim().take(80).ifBlank { "Família" }
            val cleanResponsible = responsibleMemberName.trim().take(80).ifBlank { cleanAuthor }

            val safeInstallments = if (validType == "EXPENSE" && isCreditCard) installmentCount.coerceIn(1, 48) else 1
            val parentGroupId = if (safeInstallments > 1) "grp_" + UUID.randomUUID().toString().take(8) else ""
            val rawBaseYm = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
            val (_, _, baseDay) = DateCalendarUtils.fromEpochDay(dateEpochDay)
            val safeClosingDay = accountClosingDay.coerceIn(1, 31)
            // Compras no crédito lançadas após o dia de fechamento do cartão entram automaticamente na fatura do mês seguinte
            val baseYm = if (validType == "EXPENSE" && isCreditCard && baseDay > safeClosingDay) {
                DateCalendarUtils.shiftYearMonth(rawBaseYm, 1)
            } else {
                rawBaseYm
            }

            val perInstallmentOriginal = (Math.round((amountOriginal / safeInstallments) * 100.0) / 100.0).coerceAtLeast(0.01)
            val perInstallmentBrl = (Math.round(perInstallmentOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)

            var firstDocId = ""
            for (idx in 0 until safeInstallments) {
                val docRef = db.collection("households").document(householdId).collection("transactions").document()
                if (idx == 0) {
                    firstDocId = docRef.id
                    docPath = docRef.path
                }
                val targetYm = if (idx == 0) baseYm else DateCalendarUtils.shiftYearMonth(baseYm, idx)
                val targetEpochDay = if (idx == 0) {
                    dateEpochDay
                } else {
                    val parts = targetYm.split("-")
                    val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
                    val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
                    val maxDay = DateCalendarUtils.daysInMonth(y, m)
                    DateCalendarUtils.toEpochDay(y, m, baseDay.coerceIn(1, maxDay))
                }
                val displayTitle = if (safeInstallments > 1) {
                    "$cleanTitle (${idx + 1}/$safeInstallments)".take(140)
                } else {
                    cleanTitle
                }
                val payload = mapOf(
                    "userId" to uid,
                    "householdId" to householdId,
                    "title" to displayTitle,
                    "amountOriginal" to perInstallmentOriginal,
                    "currency" to validCurrency,
                    "exchangeRateToBrl" to safeRate,
                    "amountBrl" to perInstallmentBrl,
                    "type" to validType,
                    "budgetBucket" to validBucket,
                    "category" to cleanCategory,
                    "dateEpochDay" to targetEpochDay.coerceIn(0L, 100000L),
                    "yearMonth" to targetYm,
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
                    "linkedTaskId" to (if (idx == 0) linkedTaskId.take(128) else ""),
                    "generateCalendarTask" to (if (idx == 0) generateCalendarTask else false),
                    "accountId" to accountId.take(64).ifBlank { "acc_nubank" },
                    "accountName" to accountName.take(80).ifBlank { "Nubank" },
                    "destinationAccountId" to destinationAccountId.take(64),
                    "destinationAccountName" to destinationAccountName.take(80),
                    "isRecurring" to isRecurring,
                    "isCreditCard" to (validType == "EXPENSE" && isCreditCard),
                    "installmentCount" to safeInstallments,
                    "installmentIndex" to (idx + 1),
                    "parentTransactionId" to parentGroupId,
                    "isPaid" to (if (validType == "EXPENSE" && isCreditCard) isPaid else true),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }

            if (generateCalendarTask && linkedTaskId.isBlank() && firstDocId.isNotBlank()) {
                val scopeLabel = if (validScope == PrivacyScope.PERSONAL) "Conta Individual" else "Conta Familiar"
                val taskRes = addTask(
                    householdId = householdId,
                    title = "Pagar: $cleanTitle",
                    notes = "$scopeLabel • Valor: ${formatCurrencyOriginal(perInstallmentOriginal, validCurrency)}",
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
                    linkedTransactionId = firstDocId,
                    estimatedCostBrl = perInstallmentBrl,
                    splitMode = validSplit,
                    splitShares = splitShares
                )
                val newTaskId = taskRes.getOrNull()
                if (!newTaskId.isNullOrBlank()) {
                    runCatching {
                        db.collection("households").document(householdId)
                            .collection("transactions").document(firstDocId)
                            .update(
                                mapOf(
                                    "linkedTaskId" to newTaskId,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                    }
                }
            }
            Result.success(firstDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        householdId: String,
        transactionId: String,
        title: String,
        amountOriginal: Double,
        currency: String,
        exchangeRateToBrl: Double,
        type: String,
        budgetBucket: String,
        category: String,
        dateEpochDay: Long,
        privacyScope: String,
        accountId: String,
        accountName: String,
        destinationAccountId: String = "",
        destinationAccountName: String = "",
        isRecurring: Boolean = false,
        isCreditCard: Boolean = false,
        isPaid: Boolean = !isCreditCard
    ): Result<Unit> {
        val validScope = sanitizePrivacyScope(privacyScope)
        if (isLocalOfflineMode()) {
            localStore!!.updateTransaction(
                txId = transactionId,
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = exchangeRateToBrl,
                type = type,
                budgetBucket = budgetBucket,
                category = category,
                dateEpochDay = dateEpochDay,
                privacyScope = validScope,
                accountId = accountId,
                accountName = accountName,
                destinationAccountId = destinationAccountId,
                destinationAccountName = destinationAccountName,
                isRecurring = isRecurring,
                isCreditCard = isCreditCard,
                isPaid = isPaid
            )
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/transactions/$transactionId"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe a descrição." }
            require(amountOriginal > 0.0) { "O valor deve ser maior que zero." }
            val validCurrency = if (currency in listOf("BRL", "USD", "EUR", "GBP")) currency else "BRL"
            val safeRate = if (validCurrency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
            val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)
            val validType = if (type in listOf("EXPENSE", "INCOME", "TRANSFER")) type else "EXPENSE"
            val validBucket = when (validType) {
                "INCOME" -> "INCOME"
                "TRANSFER" -> "TRANSFER"
                else -> if (budgetBucket in listOf("NEEDS", "WANTS", "SAVINGS")) budgetBucket else "NEEDS"
            }
            val ym = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
            val ref = db.collection("households").document(householdId).collection("transactions").document(transactionId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "title" to cleanTitle,
                    "amountOriginal" to amountOriginal,
                    "currency" to validCurrency,
                    "exchangeRateToBrl" to safeRate,
                    "amountBrl" to amountBrl,
                    "type" to validType,
                    "budgetBucket" to validBucket,
                    "category" to category.trim().take(50).ifBlank { "Geral" },
                    "dateEpochDay" to dateEpochDay.coerceIn(0L, 100000L),
                    "yearMonth" to ym,
                    "privacyScope" to validScope,
                    "accountId" to accountId.take(64).ifBlank { "acc_nubank" },
                    "accountName" to accountName.take(80).ifBlank { "Nubank" },
                    "destinationAccountId" to destinationAccountId.take(64),
                    "destinationAccountName" to destinationAccountName.take(80),
                    "isRecurring" to isRecurring,
                    "isCreditCard" to (validType == "EXPENSE" && isCreditCard),
                    "isPaid" to (if (validType == "EXPENSE" && isCreditCard) isPaid else true),
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

    suspend fun setCreditTransactionPaid(
        householdId: String,
        transactionId: String,
        isPaid: Boolean,
        accountId: String,
        accountName: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.setCreditTransactionPaid(transactionId, isPaid, accountId, accountName)
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/transactions/$transactionId"
        return try {
            requireUserId()
            val ref = db.collection("households").document(householdId).collection("transactions").document(transactionId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "isPaid" to isPaid,
                    "accountId" to accountId.take(64).ifBlank { "acc_nubank" },
                    "accountName" to accountName.take(80).ifBlank { "Nubank" },
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

    // 5. Subscriptions & Fixed Bills Observation & Mutations
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

    suspend fun updateSubscriptionBill(
        householdId: String,
        subscriptionId: String,
        title: String,
        amountOriginal: Double,
        currency: String,
        exchangeRateToBrl: Double,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String
    ): Result<Unit> {
        val validScope = sanitizePrivacyScope(privacyScope)
        if (isLocalOfflineMode()) {
            localStore!!.updateSubscriptionBill(
                subId = subscriptionId,
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = exchangeRateToBrl,
                category = category,
                budgetBucket = budgetBucket,
                dueDayOfMonth = dueDayOfMonth,
                reminderDaysBefore = reminderDaysBefore,
                autoPostEnabled = autoPostEnabled,
                privacyScope = validScope
            )
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/subscriptions/$subscriptionId"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe o nome da assinatura." }
            require(amountOriginal > 0.0) { "O valor deve ser maior que zero." }
            val validCurrency = if (currency in listOf("BRL", "USD", "EUR", "GBP")) currency else "BRL"
            val safeRate = if (validCurrency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
            val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)
            val validBucket = if (budgetBucket in listOf("NEEDS", "WANTS", "SAVINGS")) budgetBucket else "NEEDS"

            val ref = db.collection("households").document(householdId).collection("subscriptions").document(subscriptionId)
            docPath = ref.path
            ref.update(
                mapOf(
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
                    "privacyScope" to validScope,
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

    // 6. Savings Goals Observation & Mutations
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

    suspend fun updateSavingsGoal(
        householdId: String,
        goalId: String,
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String
    ): Result<Unit> {
        if (isLocalOfflineMode()) {
            localStore!!.updateSavingsGoal(
                goalId = goalId,
                title = title,
                targetAmountBrl = targetAmountBrl,
                currentAmountBrl = currentAmountBrl,
                incomePercentage = incomePercentage,
                category = category,
                targetYearMonth = targetYearMonth
            )
            return Result.success(Unit)
        }
        var docPath = "households/$householdId/savings_goals/$goalId"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Informe o nome da meta." }
            require(targetAmountBrl > 0.0) { "O valor alvo deve ser maior que zero." }
            val ref = db.collection("households").document(householdId).collection("savings_goals").document(goalId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "title" to cleanTitle,
                    "targetAmountBrl" to targetAmountBrl.coerceIn(1.0, 1_000_000_000.0),
                    "currentAmountBrl" to currentAmountBrl.coerceIn(0.0, 1_000_000_000.0),
                    "incomePercentage" to incomePercentage.coerceIn(0, 100).toLong(),
                    "category" to category.trim().take(50).ifBlank { "Reserva" },
                    "targetYearMonth" to targetYearMonth.trim().take(7),
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

    companion object {
        const val OFFICIAL_HOUSEHOLD_ID = "FAM_OMNI2448"
        const val LEGACY_DUPLICATE_HOUSEHOLD_ID = "FAM_SKRKRVKS"
    }
}
