package com.example.data

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.R
import com.example.widget.OmniHomeWidgetProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

data class FamilyMuralItem(
    val id: String = "mural_" + UUID.randomUUID().toString().take(8),
    val title: String = "",
    val notes: String = "",
    val category: String = "Compras", // "Compras", "Mural", "Casa", "Lembretes"
    val addedBy: String = "Família",
    val addedByColorHex: Long = 0xFF0E7C5B,
    val checkedBy: String = "",
    val checkedByColorHex: Long = 0L,
    val completed: Boolean = false,
    val pinned: Boolean = false,
    val isNote: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val done: Boolean
        get() = completed
}

data class OmniMigrationResult(
    val tasksMerged: Int = 0,
    val transactionsMerged: Int = 0,
    val accountsMerged: Int = 0,
    val healthMerged: Int = 0,
    val diaryMerged: Int = 0,
    val muralMerged: Int = 0
) {
    val totalMerged: Int
        get() = tasksMerged + transactionsMerged + accountsMerged + healthMerged + diaryMerged + muralMerged

    fun toSummaryMessagePt(isRestore: Boolean = false): String {
        val actionWord = if (isRestore) "Restauração concluída" else "Recuperação concluída"
        if (totalMerged == 0) {
            return "$actionWord: todos os seus dados antigos já estão sincronizados na família atual!"
        }
        val parts = mutableListOf<String>()
        if (tasksMerged > 0) parts.add("$tasksMerged tarefa(s)")
        if (transactionsMerged > 0) parts.add("$transactionsMerged lançamento(s)")
        if (accountsMerged > 0) parts.add("$accountsMerged conta(s)")
        if (healthMerged > 0) parts.add("$healthMerged medição(ões) de saúde")
        if (diaryMerged > 0) parts.add("$diaryMerged registro(s) do diário")
        if (muralMerged > 0) parts.add("$muralMerged item(ns) da lista")
        return "$actionWord: ${parts.joinToString(", ")} mesclado(s) com sucesso!"
    }
}

object OmniBackupAndMigrationManager {
    private const val PREFS_MIGRATION = "omni_migration_and_mural_prefs"
    private const val KEY_FAMILY_MURAL_JSON = "family_mural_items_json"
    private const val KEY_AUTO_MIGRATION_V4_DONE_PREFIX = "auto_migration_v4_done_"
    private const val KEY_WIDGET_FINANCE_HIDE_BALANCE = "widget_finance_hide_balance"

    private val _muralItemsFlow = MutableStateFlow<List<FamilyMuralItem>>(emptyList())
    val muralItemsFlow: StateFlow<List<FamilyMuralItem>> = _muralItemsFlow.asStateFlow()

    fun isWidgetFinanceBalanceHidden(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WIDGET_FINANCE_HIDE_BALANCE, false)
    }

    fun setWidgetFinanceBalanceHidden(context: Context, hidden: Boolean) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WIDGET_FINANCE_HIDE_BALANCE, hidden).apply()
        OmniHomeWidgetProvider.requestUpdate(appContext)
    }

    fun toggleWidgetFinanceBalanceHidden(context: Context): Boolean {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        val next = !prefs.getBoolean(KEY_WIDGET_FINANCE_HIDE_BALANCE, false)
        prefs.edit().putBoolean(KEY_WIDGET_FINANCE_HIDE_BALANCE, next).apply()
        OmniHomeWidgetProvider.requestUpdate(appContext)
        return next
    }

    fun loadFamilyMuralItems(context: Context): List<FamilyMuralItem> = getFamilyMuralItems(context)

    fun getFamilyMuralItems(context: Context): List<FamilyMuralItem> {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FAMILY_MURAL_JSON, null)
        if (raw.isNullOrBlank()) {
            _muralItemsFlow.value = emptyList()
            return emptyList()
        }
        var hadSeedItems = false
        val parsed = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "mural_$i")
                    if (id.startsWith("mural_seed_")) {
                        hadSeedItems = true
                        continue
                    }
                    val title = obj.optString("title", "").trim()
                    if (title.isNotEmpty()) {
                        add(
                            FamilyMuralItem(
                                id = id,
                                title = title,
                                notes = obj.optString("notes", ""),
                                category = obj.optString("category", "Compras"),
                                addedBy = obj.optString("addedBy", "Família"),
                                addedByColorHex = obj.optLong("addedByColorHex", 0xFF0E7C5B),
                                checkedBy = obj.optString("checkedBy", ""),
                                checkedByColorHex = obj.optLong("checkedByColorHex", 0L),
                                completed = obj.optBoolean("completed", false),
                                pinned = obj.optBoolean("pinned", false),
                                isNote = obj.optBoolean("isNote", false),
                                createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        }.getOrElse { emptyList() }
        if (hadSeedItems) {
            saveFamilyMuralItems(appContext, parsed, notifyWidgets = false)
        }
        _muralItemsFlow.value = parsed
        return parsed
    }

    private fun defaultFamilyMuralItems(): List<FamilyMuralItem> = emptyList()

    fun saveFamilyMuralItems(
        context: Context,
        items: List<FamilyMuralItem>,
        notifyWidgets: Boolean = true
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("notes", item.notes)
                    put("category", item.category)
                    put("addedBy", item.addedBy)
                    put("addedByColorHex", item.addedByColorHex)
                    put("checkedBy", item.checkedBy)
                    put("checkedByColorHex", item.checkedByColorHex)
                    put("completed", item.completed)
                    put("pinned", item.pinned)
                    put("isNote", item.isNote)
                    put("createdAtEpochMs", item.createdAtEpochMs)
                }
            )
        }
        prefs.edit().putString(KEY_FAMILY_MURAL_JSON, arr.toString()).apply()
        _muralItemsFlow.value = items
        if (notifyWidgets) {
            OmniHomeWidgetProvider.requestUpdate(appContext)
        }
    }

    fun syncMuralItemsFromRemote(context: Context, remoteItems: List<FamilyMuralItem>) {
        if (remoteItems.isEmpty()) return
        saveFamilyMuralItems(context, remoteItems, notifyWidgets = true)
    }

    fun addFamilyMuralItem(
        context: Context,
        title: String,
        category: String = "Compras",
        addedBy: String = "Família",
        addedByColorHex: Long = 0xFF0E7C5B,
        notes: String = "",
        pinned: Boolean = false,
        isNote: Boolean = false
    ): FamilyMuralItem? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        val current = getFamilyMuralItems(context).toMutableList()
        val newItem = FamilyMuralItem(
            id = "mural_" + UUID.randomUUID().toString().take(8),
            title = cleanTitle,
            notes = notes.trim(),
            category = category.ifBlank { "Compras" },
            addedBy = addedBy.ifBlank { "Família" },
            addedByColorHex = if (addedByColorHex != 0L) addedByColorHex else 0xFF0E7C5B,
            pinned = pinned,
            isNote = isNote,
            completed = false
        )
        current.add(0, newItem)
        val sorted = current.sortedWith(compareByDescending<FamilyMuralItem> { it.pinned }.thenBy { it.completed })
        saveFamilyMuralItems(context, sorted, notifyWidgets = true)
        return newItem
    }

    fun toggleFamilyMuralItem(
        context: Context,
        itemId: String,
        checkedByName: String = "Família",
        checkedByColorHex: Long = 0xFF0E7C5B
    ): List<FamilyMuralItem> {
        val current = getFamilyMuralItems(context).map { item ->
            if (item.id == itemId) {
                val nextDone = !item.completed
                item.copy(
                    completed = nextDone,
                    checkedBy = if (nextDone) checkedByName else "",
                    checkedByColorHex = if (nextDone) checkedByColorHex else 0L
                )
            } else item
        }.sortedWith(compareByDescending<FamilyMuralItem> { it.pinned }.thenBy { it.completed })
        saveFamilyMuralItems(context, current, notifyWidgets = true)
        return current
    }

    fun togglePinMuralItem(context: Context, itemId: String): List<FamilyMuralItem> {
        val current = getFamilyMuralItems(context).map { item ->
            if (item.id == itemId) item.copy(pinned = !item.pinned) else item
        }.sortedWith(compareByDescending<FamilyMuralItem> { it.pinned }.thenBy { it.completed })
        saveFamilyMuralItems(context, current, notifyWidgets = true)
        return current
    }

    fun clearCompletedMuralItems(context: Context, categoryFilter: String? = null): List<FamilyMuralItem> {
        val current = getFamilyMuralItems(context).filterNot { item ->
            item.completed && !item.isNote && (categoryFilter.isNullOrBlank() || categoryFilter == "ALL" || item.category.equals(categoryFilter, ignoreCase = true))
        }
        saveFamilyMuralItems(context, current, notifyWidgets = true)
        return current
    }

    fun deleteFamilyMuralItem(context: Context, itemId: String): List<FamilyMuralItem> {
        val current = getFamilyMuralItems(context).filterNot { it.id == itemId }
        saveFamilyMuralItems(context, current, notifyWidgets = true)
        return current
    }

    /**
     * Runs once automatically on app startup per active householdId to scan legacy local storage
     * and previous Firestore households linked to the user's UID, merging any missing records.
     */
    suspend fun autoRunStartupMigrationIfNeeded(
        context: Context,
        activeHouseholdId: String,
        currentHousehold: Household,
        currentTasks: List<TaskItem>,
        currentTransactions: List<FinanceTransaction>,
        repository: AuraFirestoreRepository,
        healthRepo: HealthRepository,
        diaryRepo: PrivateDiaryRepository
    ): OmniMigrationResult? {
        if (activeHouseholdId.isBlank()) return null
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        val flagKey = "$KEY_AUTO_MIGRATION_V4_DONE_PREFIX$activeHouseholdId"
        if (prefs.getBoolean(flagKey, false)) {
            return null
        }
        val result = recoverAndMergeLegacyData(
            context = appContext,
            activeHouseholdId = activeHouseholdId,
            currentHousehold = currentHousehold,
            currentTasks = currentTasks,
            currentTransactions = currentTransactions,
            repository = repository,
            healthRepo = healthRepo,
            diaryRepo = diaryRepo
        )
        prefs.edit().putBoolean(flagKey, true).apply()
        return result
    }

    /**
     * Comprehensive scanner that inspects:
     * 1. All local SharedPreferences files (`aura_*`, `aura_local_offline_store`, `aura_health_module_prefs`, `aura_private_diary_*`)
     * 2. Previous Firestore documents/households linked to the user's UID (`ownerId == uid` or `defaultHouseholdIdForUser(uid)`)
     * 3. Merges tasks, transactions, bank accounts, glucose/BP readings, and diary entries into the active family without duplicates.
     */
    suspend fun recoverAndMergeLegacyData(
        context: Context,
        activeHouseholdId: String,
        currentHousehold: Household,
        currentTasks: List<TaskItem>,
        currentTransactions: List<FinanceTransaction>,
        repository: AuraFirestoreRepository,
        healthRepo: HealthRepository,
        diaryRepo: PrivateDiaryRepository
    ): OmniMigrationResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        var tasksMerged = 0
        var transactionsMerged = 0
        var accountsMerged = 0
        var healthMerged = 0
        var diaryMerged = 0

        val knownTaskSignatures = currentTasks.map { taskSignature(it.title, it.dateEpochDay, it.isInbox) }.toMutableSet()
        val knownTxSignatures = currentTransactions.map { txSignature(it.title, it.amountBrl, it.dateEpochDay, it.type) }.toMutableSet()
        val knownAccountNames = currentHousehold.bankAccounts.map { it.name.trim().lowercase() }.toMutableSet()

        val recoveredTasks = mutableListOf<TaskItem>()
        val recoveredTransactions = mutableListOf<FinanceTransaction>()
        val recoveredAccounts = mutableListOf<BankAccount>()
        val recoveredGlucose = mutableListOf<GlucoseRecord>()
        val recoveredBp = mutableListOf<BloodPressureRecord>()
        val recoveredInsulin = mutableListOf<InsulinRecord>()
        val recoveredDiaryEntries = mutableListOf<PrivateDiaryEntry>()
        val recoveredSupportLetters = mutableListOf<SupportLetterRecord>()

        // 1. Scan all SharedPreferences files in shared_prefs directory with "aura_" or "omni_" prefix
        val sharedPrefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
        val prefFiles = sharedPrefsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".xml") &&
                (file.name.startsWith("aura_") || file.name.startsWith("omni_") || file.name.contains("health") || file.name.contains("diary"))
        }?.map { it.name.removeSuffix(".xml") }
            ?: listOf(
                "aura_local_offline_store",
                "aura_health_module_prefs",
                "aura_private_diary_LOCAL_TEST_USER",
                "aura_customization_prefs"
            )

        for (prefName in prefFiles.distinct()) {
            runCatching {
                val sp = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                val allEntries = sp.all

                // Check for tasks_json
                (allEntries["tasks_json"] as? String)?.takeIf { it.isNotBlank() }?.let { rawTasks ->
                    recoveredTasks.addAll(parseTasksFromJsonArray(JSONArray(rawTasks)))
                }

                // Check for tx_json
                (allEntries["tx_json"] as? String)?.takeIf { it.isNotBlank() }?.let { rawTxs ->
                    recoveredTransactions.addAll(parseTransactionsFromJsonArray(JSONArray(rawTxs)))
                }

                // Check for bank_accounts_json
                (allEntries["bank_accounts_json"] as? String)?.takeIf { it.isNotBlank() }?.let { rawAccs ->
                    recoveredAccounts.addAll(parseBankAccountsFromJsonArray(JSONArray(rawAccs)))
                }

                // Check for health_module_state_json
                (allEntries["health_module_state_json"] as? String)?.takeIf { it.isNotBlank() }?.let { rawHealth ->
                    val root = JSONObject(rawHealth)
                    root.optJSONArray("glucoseRecords")?.let { arr ->
                        recoveredGlucose.addAll(parseGlucoseFromJsonArray(arr))
                    }
                    root.optJSONArray("bloodPressureRecords")?.let { arr ->
                        recoveredBp.addAll(parseBpFromJsonArray(arr))
                    }
                    root.optJSONArray("insulinRecords")?.let { arr ->
                        recoveredInsulin.addAll(parseInsulinFromJsonArray(arr))
                    }
                }

                // Check for private_diary_state_json
                (allEntries["private_diary_state_json"] as? String)?.takeIf { it.isNotBlank() }?.let { rawDiary ->
                    val root = JSONObject(rawDiary)
                    root.optJSONArray("entries")?.let { arr ->
                        recoveredDiaryEntries.addAll(parseDiaryEntriesFromJsonArray(arr))
                    }
                    root.optJSONArray("savedSupportLetters")?.let { arr ->
                        recoveredSupportLetters.addAll(parseSupportLettersFromJsonArray(arr))
                    }
                }
            }
        }

        // 2. If authenticated with Google in Firestore, also scan previous households linked to this user's UID
        val authUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        val db = runCatching {
            FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
        }.getOrNull()

        if (authUser != null && db != null) {
            val uid = authUser.uid
            val candidateHouseholdIds = mutableSetOf<String>()
            val defaultUidHouseId = repository.defaultHouseholdIdForUser(uid)
            if (defaultUidHouseId != activeHouseholdId) {
                candidateHouseholdIds.add(defaultUidHouseId)
            }

            // Query households where ownerId == uid
            runCatching {
                val ownedSnap = db.collection("households")
                    .whereEqualTo("ownerId", uid)
                    .get()
                    .await()
                ownedSnap.documents.forEach { doc ->
                    if (doc.id != activeHouseholdId) {
                        candidateHouseholdIds.add(doc.id)
                    }
                }
            }

            // Query households where memberUids contains uid
            runCatching {
                val memberSnap = db.collection("households")
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                memberSnap.documents.forEach { doc ->
                    if (doc.id != activeHouseholdId) {
                        candidateHouseholdIds.add(doc.id)
                    }
                }
            }

            for (oldHouseId in candidateHouseholdIds) {
                // Pull tasks from old household
                runCatching {
                    val tasksSnap = db.collection("households")
                        .document(oldHouseId)
                        .collection("tasks")
                        .get()
                        .await()
                    tasksSnap.documents.forEach { doc ->
                        val base = doc.toObject(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (base != null) {
                            recoveredTasks.add(
                                base.copy(
                                    id = doc.id,
                                    isInbox = doc.getBoolean("isInbox") ?: doc.getBoolean("inbox") ?: base.isInbox,
                                    isEssential = doc.getBoolean("isEssential") ?: doc.getBoolean("essential") ?: base.isEssential
                                )
                            )
                        }
                    }
                }
                // Pull transactions from old household
                runCatching {
                    val txSnap = db.collection("households")
                        .document(oldHouseId)
                        .collection("transactions")
                        .get()
                        .await()
                    txSnap.documents.forEach { doc ->
                        val base = doc.toObject(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (base != null) {
                            recoveredTransactions.add(
                                base.copy(
                                    id = doc.id,
                                    isRecurring = doc.getBoolean("isRecurring") ?: doc.getBoolean("recurring") ?: base.isRecurring,
                                    isCreditCard = doc.getBoolean("isCreditCard") ?: doc.getBoolean("creditCard") ?: base.isCreditCard,
                                    isPaid = doc.getBoolean("isPaid") ?: doc.getBoolean("paid") ?: base.isPaid
                                )
                            )
                        }
                    }
                }
            }

            // Also check /users/{uid}/private_diary in Firestore
            runCatching {
                val diarySnap = db.collection("users")
                    .document(uid)
                    .collection("private_diary")
                    .get()
                    .await()
                diarySnap.documents.forEach { doc ->
                    val freeText = doc.getString("freeText").orEmpty()
                    if (freeText.isNotBlank()) {
                        val tags = (doc.get("symptomTags") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
                        recoveredDiaryEntries.add(
                            PrivateDiaryEntry(
                                id = doc.id,
                                userId = uid,
                                dateIso = doc.getString("dateIso") ?: LocalDate.now().toString(),
                                epochDay = doc.getLong("epochDay") ?: LocalDate.now().toEpochDay(),
                                hour = (doc.getLong("hour") ?: 9L).toInt(),
                                minute = (doc.getLong("minute") ?: 0L).toInt(),
                                moodId = doc.getString("moodId") ?: "CALM",
                                symptomTags = tags,
                                freeText = freeText,
                                createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        // 3. Merge recovered Health records (glucose, blood pressure & insulin)
        if (recoveredGlucose.isNotEmpty() || recoveredBp.isNotEmpty() || recoveredInsulin.isNotEmpty()) {
            healthMerged += healthRepo.mergeImportedHealthRecords(
                importedGlucose = recoveredGlucose,
                importedBp = recoveredBp,
                importedInsulin = recoveredInsulin
            )
        }

        // 4. Merge recovered Diary entries & support letters
        if (recoveredDiaryEntries.isNotEmpty() || recoveredSupportLetters.isNotEmpty()) {
            diaryMerged += diaryRepo.mergeImportedDiaryEntries(
                importedEntries = recoveredDiaryEntries,
                importedLetters = recoveredSupportLetters
            )
        }

        // 5. Merge recovered Bank Accounts into active household
        var workingHousehold = currentHousehold
        for (acc in recoveredAccounts) {
            val cleanNameKey = acc.name.trim().lowercase()
            if (cleanNameKey.isNotEmpty() && cleanNameKey !in knownAccountNames) {
                knownAccountNames.add(cleanNameKey)
                repository.saveBankAccount(
                    householdId = activeHouseholdId,
                    currentHousehold = workingHousehold,
                    account = acc
                )
                workingHousehold = workingHousehold.copy(
                    bankAccounts = workingHousehold.bankAccounts + acc
                )
                accountsMerged++
            }
        }

        // 6. Merge recovered Tasks into active household (avoiding duplicates by title + dateEpochDay + isInbox)
        for (task in recoveredTasks) {
            val sig = taskSignature(task.title, task.dateEpochDay, task.isInbox)
            if (task.title.isNotBlank() && sig !in knownTaskSignatures) {
                knownTaskSignatures.add(sig)
                val addRes = repository.addTask(
                    householdId = activeHouseholdId,
                    title = task.title,
                    notes = task.notes,
                    location = task.location,
                    category = task.category,
                    priority = task.priority,
                    dateEpochDay = task.dateEpochDay,
                    endDateEpochDay = task.endDateEpochDay,
                    alarmHour = task.alarmHour.toInt(),
                    alarmMinute = task.alarmMinute.toInt(),
                    alarmEnabled = task.alarmEnabled,
                    privacyScope = task.privacyScope,
                    assignedMemberId = task.assignedMemberId,
                    assignedMemberName = task.assignedMemberName,
                    assignedMemberColorHex = task.assignedMemberColorHex,
                    assignedMemberEmoji = task.assignedMemberEmoji,
                    createdByName = task.createdByName,
                    createdByColorHex = task.createdByColorHex,
                    estimatedCostBrl = task.estimatedCostBrl,
                    autoCreateExpenseOnComplete = task.autoCreateExpenseOnComplete,
                    isEssential = task.isEssential,
                    isInbox = task.isInbox,
                    durationMinutes = task.durationMinutes,
                    iconEmoji = task.iconEmoji,
                    taskColorHex = task.taskColorHex,
                    recurrence = task.recurrence,
                    recurrenceWeekdays = task.recurrenceWeekdays,
                    subtasks = task.subtasks,
                    ringtoneCategory = task.ringtoneCategory
                )
                if (addRes.isSuccess) {
                    tasksMerged++
                }
            }
        }

        // 7. Merge recovered Financial Transactions into active household
        for (tx in recoveredTransactions) {
            val sig = txSignature(tx.title, tx.amountBrl, tx.dateEpochDay, tx.type)
            if (tx.title.isNotBlank() && tx.amountBrl > 0.0 && sig !in knownTxSignatures) {
                knownTxSignatures.add(sig)
                val addRes = repository.addTransaction(
                    householdId = activeHouseholdId,
                    title = tx.title,
                    amountOriginal = tx.amountBrl,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    type = tx.type,
                    budgetBucket = tx.budgetBucket,
                    category = tx.category,
                    dateEpochDay = tx.dateEpochDay,
                    privacyScope = tx.privacyScope,
                    responsibleMemberId = tx.responsibleMemberId,
                    responsibleMemberName = tx.responsibleMemberName,
                    responsibleMemberColorHex = tx.responsibleMemberColorHex,
                    responsibleMemberEmoji = tx.responsibleMemberEmoji,
                    splitMode = tx.splitMode,
                    splitShares = tx.splitShares,
                    accountId = tx.accountId,
                    accountName = tx.accountName,
                    isRecurring = tx.isRecurring,
                    isCreditCard = tx.isCreditCard,
                    installmentCount = 1,
                    isPaid = tx.isPaid
                )
                if (addRes.isSuccess) {
                    transactionsMerged++
                }
            }
        }

        OmniHomeWidgetProvider.requestUpdate(appContext)

        OmniMigrationResult(
            tasksMerged = tasksMerged,
            transactionsMerged = transactionsMerged,
            accountsMerged = accountsMerged,
            healthMerged = healthMerged,
            diaryMerged = diaryMerged
        )
    }

    /**
     * Builds a complete 1-Tap Backup JSON string with all Family, Agenda, Finance, Health, Diary, and Mural data.
     */
    fun exportFullBackupJson(
        context: Context,
        household: Household,
        tasks: List<TaskItem>,
        transactions: List<FinanceTransaction>,
        subscriptions: List<SubscriptionBill>,
        savingsGoals: List<SavingsGoal>,
        healthState: HealthModuleState,
        diaryState: PrivateDiaryState
    ): String {
        val root = JSONObject()
        root.put("backupFormatVersion", 1)
        root.put("appName", "Omni")
        root.put("appVersionName", BuildConfig.VERSION_NAME)
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        root.put("householdId", household.id)
        root.put("householdName", household.name)
        root.put("pregnancyDppDate", household.pregnancyDppDate)
        root.put("pregnancyComparisonMode", household.pregnancyComparisonMode)
        root.put("babyName", household.babyName)
        root.put("babyGender", household.babyGender)

        // Family Members
        val membersArr = JSONArray()
        household.membersWithAssignedColors().forEach { m ->
            membersArr.put(
                JSONObject().apply {
                    put("userId", m.userId)
                    put("displayName", m.displayName)
                    put("colorHex", m.colorHex)
                    put("avatarEmoji", m.avatarEmoji)
                    put("role", m.role)
                }
            )
        }
        root.put("familyMembers", membersArr)

        // Bank Accounts
        val accountsArr = JSONArray()
        household.bankAccounts.forEach { acc ->
            accountsArr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("institution", acc.institution)
                    put("initialBalanceBrl", acc.initialBalanceBrl)
                    put("colorHex", acc.colorHex)
                    put("iconEmoji", acc.iconEmoji)
                    put("privacyScope", acc.privacyScope)
                }
            )
        }
        root.put("bankAccounts", accountsArr)

        // Tasks
        val tasksArr = JSONArray()
        tasks.forEach { t ->
            tasksArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("notes", t.notes)
                    put("location", t.location)
                    put("category", t.category)
                    put("priority", t.priority)
                    put("isEssential", t.isEssential)
                    put("dateEpochDay", t.dateEpochDay)
                    put("endDateEpochDay", t.endDateEpochDay)
                    put("alarmHour", t.alarmHour)
                    put("alarmMinute", t.alarmMinute)
                    put("alarmEnabled", t.alarmEnabled)
                    put("completed", t.completed)
                    put("isInbox", t.isInbox)
                    put("durationMinutes", t.durationMinutes)
                    put("iconEmoji", t.iconEmoji)
                    put("taskColorHex", t.taskColorHex)
                    put("recurrence", t.recurrence)
                    put("privacyScope", t.privacyScope)
                    put("assignedMemberId", t.assignedMemberId)
                    put("assignedMemberName", t.assignedMemberName)
                    put("assignedMemberColorHex", t.assignedMemberColorHex)
                    put("assignedMemberEmoji", t.assignedMemberEmoji)
                    put("estimatedCostBrl", t.estimatedCostBrl)
                    put("autoCreateExpenseOnComplete", t.autoCreateExpenseOnComplete)
                    put("ringtoneCategory", t.ringtoneCategory)
                    val weekdaysArr = JSONArray()
                    t.recurrenceWeekdays.forEach { weekdaysArr.put(it) }
                    put("recurrenceWeekdays", weekdaysArr)
                    val subtasksArr = JSONArray()
                    t.subtasks.forEach { st ->
                        subtasksArr.put(
                            JSONObject().apply {
                                put("id", st.id)
                                put("title", st.title)
                                put("done", st.done)
                            }
                        )
                    }
                    put("subtasks", subtasksArr)
                }
            )
        }
        root.put("tasks", tasksArr)

        // Transactions
        val txArr = JSONArray()
        transactions.forEach { tx ->
            txArr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amountBrl", tx.amountBrl)
                    put("type", tx.type)
                    put("category", tx.category)
                    put("budgetBucket", tx.budgetBucket)
                    put("isRecurring", tx.isRecurring)
                    put("dateEpochDay", tx.dateEpochDay)
                    put("privacyScope", tx.privacyScope)
                    put("responsibleMemberId", tx.responsibleMemberId)
                    put("responsibleMemberName", tx.responsibleMemberName)
                    put("responsibleMemberColorHex", tx.responsibleMemberColorHex)
                    put("responsibleMemberEmoji", tx.responsibleMemberEmoji)
                    put("accountId", tx.accountId)
                    put("accountName", tx.accountName)
                    put("isCreditCard", tx.isCreditCard)
                    put("installmentIndex", tx.installmentIndex)
                    put("installmentCount", tx.installmentCount)
                    put("isPaid", tx.isPaid)
                }
            )
        }
        root.put("transactions", txArr)

        // Subscriptions
        val subsArr = JSONArray()
        subscriptions.forEach { s ->
            subsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("amountBrl", s.amountBrl)
                    put("category", s.category)
                    put("budgetBucket", s.budgetBucket)
                    put("dueDayOfMonth", s.dueDayOfMonth)
                    put("active", s.active)
                    put("privacyScope", s.privacyScope)
                    put("ringtoneCategory", s.ringtoneCategory)
                }
            )
        }
        root.put("subscriptions", subsArr)

        // Savings Goals
        val goalsArr = JSONArray()
        savingsGoals.forEach { g ->
            goalsArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("targetAmountBrl", g.targetAmountBrl)
                    put("currentAmountBrl", g.currentAmountBrl)
                    put("category", g.category)
                    put("privacyScope", g.privacyScope)
                }
            )
        }
        root.put("savingsGoals", goalsArr)

        // Health: Glucose & Blood Pressure
        val glucoseArr = JSONArray()
        healthState.glucoseRecords.forEach { rec ->
            glucoseArr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("valueMgDl", rec.valueMgDl)
                    put("slot", rec.slot)
                    put("dateIso", rec.dateIso)
                    put("dateEpochDay", rec.dateEpochDay)
                    put("timeHHmm", rec.timeHHmm)
                    put("notes", rec.notes)
                    put("privacyScope", rec.privacyScope)
                    put("timestampMillis", rec.createdAtMillis)
                    put("createdAtMillis", rec.createdAtMillis)
                    put("memberId", rec.memberId)
                    put("memberName", rec.memberName)
                }
            )
        }
        root.put("glucoseRecords", glucoseArr)

        val bpArr = JSONArray()
        healthState.bloodPressureRecords.forEach { rec ->
            bpArr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("systolic", rec.systolic)
                    put("diastolic", rec.diastolic)
                    put("heartRateBpm", rec.pulseBpm)
                    put("pulseBpm", rec.pulseBpm)
                    put("dateIso", rec.dateIso)
                    put("dateEpochDay", rec.dateEpochDay)
                    put("timeHHmm", rec.timeHHmm)
                    put("notes", rec.notes)
                    put("privacyScope", rec.privacyScope)
                    put("timestampMillis", rec.createdAtMillis)
                    put("createdAtMillis", rec.createdAtMillis)
                    put("memberId", rec.memberId)
                    put("memberName", rec.memberName)
                }
            )
        }
        root.put("bloodPressureRecords", bpArr)

        val insulinArr = JSONArray()
        healthState.insulinRecords.forEach { rec ->
            insulinArr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("dateIso", rec.dateIso)
                    put("timeFormatted", rec.timeFormatted)
                    put("insulinType", rec.insulinType)
                    put("units", rec.units)
                    put("injectionSite", rec.injectionSite)
                    put("memberId", rec.memberId)
                    put("notes", rec.notes)
                    put("timestampEpochMillis", rec.timestampEpochMillis)
                }
            )
        }
        root.put("insulinRecords", insulinArr)

        // Medical Documents (structured metadata without Base64 page bloat)
        val medDocsArr = JSONArray()
        runCatching {
            MedicalDocumentsRepository(context).documentsFlow.value.forEach { doc ->
                medDocsArr.put(MedicalDocumentsRepository.recordToJson(doc))
            }
        }
        root.put("healthDocuments", medDocsArr)

        // Private Diary
        val diaryArr = JSONArray()
        diaryState.entries.forEach { entry ->
            diaryArr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("userId", entry.userId)
                    put("dateIso", entry.dateIso)
                    put("epochDay", entry.epochDay)
                    put("hour", entry.hour)
                    put("minute", entry.minute)
                    put("moodId", entry.moodId)
                    val tagsArr = JSONArray()
                    entry.symptomTags.forEach { tagsArr.put(it) }
                    put("symptomTags", tagsArr)
                    put("freeText", entry.freeText)
                    put("createdAtEpochMs", entry.createdAtEpochMs)
                }
            )
        }
        root.put("diaryEntries", diaryArr)

        // Family Mural & Shopping List
        val muralArr = JSONArray()
        getFamilyMuralItems(context).forEach { item ->
            muralArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("category", item.category)
                    put("addedBy", item.addedBy)
                    put("completed", item.completed)
                    put("createdAtEpochMs", item.createdAtEpochMs)
                }
            )
        }
        root.put("familyMuralItems", muralArr)

        return root.toString(2)
    }

    suspend fun writeBackupToUri(
        context: Context,
        targetUri: Uri,
        jsonContent: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = context.contentResolver.openOutputStream(targetUri)
                ?: throw IllegalStateException("Não foi possível abrir o arquivo para gravação.")
            out.use { stream ->
                stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    suspend fun restoreBackupFromUri(
        context: Context,
        sourceUri: Uri,
        activeHouseholdId: String,
        currentHousehold: Household,
        currentTasks: List<TaskItem>,
        currentTransactions: List<FinanceTransaction>,
        repository: AuraFirestoreRepository,
        healthRepo: HealthRepository,
        diaryRepo: PrivateDiaryRepository
    ): Result<OmniMigrationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val rawJson = appContext.contentResolver.openInputStream(sourceUri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Não foi possível ler o arquivo de backup selecionado.")

            val root = JSONObject(rawJson)
            var tasksMerged = 0
            var transactionsMerged = 0
            var accountsMerged = 0
            var healthMerged = 0
            var diaryMerged = 0
            var muralMerged = 0

            // 1. Bank Accounts
            val importedAccounts = root.optJSONArray("bankAccounts")?.let { parseBankAccountsFromJsonArray(it) }.orEmpty()
            val knownAccNames = currentHousehold.bankAccounts.map { it.name.trim().lowercase() }.toMutableSet()
            var workingHouse = currentHousehold
            for (acc in importedAccounts) {
                val key = acc.name.trim().lowercase()
                if (key.isNotEmpty() && key !in knownAccNames) {
                    knownAccNames.add(key)
                    repository.saveBankAccount(activeHouseholdId, workingHouse, acc)
                    workingHouse = workingHouse.copy(bankAccounts = workingHouse.bankAccounts + acc)
                    accountsMerged++
                }
            }

            // 2. Tasks
            val importedTasks = root.optJSONArray("tasks")?.let { parseTasksFromJsonArray(it) }.orEmpty()
            val knownTaskSigs = currentTasks.map { taskSignature(it.title, it.dateEpochDay, it.isInbox) }.toMutableSet()
            for (task in importedTasks) {
                val sig = taskSignature(task.title, task.dateEpochDay, task.isInbox)
                if (task.title.isNotBlank() && sig !in knownTaskSigs) {
                    knownTaskSigs.add(sig)
                    val res = repository.addTask(
                        householdId = activeHouseholdId,
                        title = task.title,
                        notes = task.notes,
                        location = task.location,
                        category = task.category,
                        priority = task.priority,
                        dateEpochDay = task.dateEpochDay,
                        endDateEpochDay = task.endDateEpochDay,
                        alarmHour = task.alarmHour.toInt(),
                        alarmMinute = task.alarmMinute.toInt(),
                        alarmEnabled = task.alarmEnabled,
                        privacyScope = task.privacyScope,
                        assignedMemberId = task.assignedMemberId,
                        assignedMemberName = task.assignedMemberName,
                        assignedMemberColorHex = task.assignedMemberColorHex,
                        assignedMemberEmoji = task.assignedMemberEmoji,
                        estimatedCostBrl = task.estimatedCostBrl,
                        autoCreateExpenseOnComplete = task.autoCreateExpenseOnComplete,
                        isEssential = task.isEssential,
                        isInbox = task.isInbox,
                        durationMinutes = task.durationMinutes,
                        iconEmoji = task.iconEmoji,
                        taskColorHex = task.taskColorHex,
                        recurrence = task.recurrence,
                        recurrenceWeekdays = task.recurrenceWeekdays,
                        subtasks = task.subtasks,
                        ringtoneCategory = task.ringtoneCategory
                    )
                    if (res.isSuccess) tasksMerged++
                }
            }

            // 3. Transactions
            val importedTxs = root.optJSONArray("transactions")?.let { parseTransactionsFromJsonArray(it) }.orEmpty()
            val knownTxSigs = currentTransactions.map { txSignature(it.title, it.amountBrl, it.dateEpochDay, it.type) }.toMutableSet()
            for (tx in importedTxs) {
                val sig = txSignature(tx.title, tx.amountBrl, tx.dateEpochDay, tx.type)
                if (tx.title.isNotBlank() && tx.amountBrl > 0.0 && sig !in knownTxSigs) {
                    knownTxSigs.add(sig)
                    val res = repository.addTransaction(
                        householdId = activeHouseholdId,
                        title = tx.title,
                        amountOriginal = tx.amountBrl,
                        currency = "BRL",
                        exchangeRateToBrl = 1.0,
                        type = tx.type,
                        budgetBucket = tx.budgetBucket,
                        category = tx.category,
                        dateEpochDay = tx.dateEpochDay,
                        privacyScope = tx.privacyScope,
                        responsibleMemberId = tx.responsibleMemberId,
                        responsibleMemberName = tx.responsibleMemberName,
                        responsibleMemberColorHex = tx.responsibleMemberColorHex,
                        responsibleMemberEmoji = tx.responsibleMemberEmoji,
                        accountId = tx.accountId,
                        accountName = tx.accountName,
                        isRecurring = tx.isRecurring,
                        isCreditCard = tx.isCreditCard,
                        installmentCount = 1,
                        isPaid = tx.isPaid
                    )
                    if (res.isSuccess) transactionsMerged++
                }
            }

            // 4. Health (Glucose, Blood Pressure & Insulin)
            val importedGlucose = root.optJSONArray("glucoseRecords")?.let { parseGlucoseFromJsonArray(it) }.orEmpty()
            val importedBp = root.optJSONArray("bloodPressureRecords")?.let { parseBpFromJsonArray(it) }.orEmpty()
            val importedInsulin = root.optJSONArray("insulinRecords")?.let { parseInsulinFromJsonArray(it) }.orEmpty()
            if (importedGlucose.isNotEmpty() || importedBp.isNotEmpty() || importedInsulin.isNotEmpty()) {
                healthMerged += healthRepo.mergeImportedHealthRecords(
                    importedGlucose = importedGlucose,
                    importedBp = importedBp,
                    importedInsulin = importedInsulin
                )
            }

            // 4b. Medical Documents & Baby Identity
            root.optJSONArray("healthDocuments")?.let { docsArr ->
                val importedDocs = buildList {
                    for (i in 0 until docsArr.length()) {
                        val obj = docsArr.optJSONObject(i) ?: continue
                        runCatching { MedicalDocumentsRepository.parseRecordFromJson(obj) }.getOrNull()?.let { add(it) }
                    }
                }
                if (importedDocs.isNotEmpty()) {
                    healthMerged += MedicalDocumentsRepository(appContext).mergeImportedDocuments(importedDocs)
                }
            }
            val importedBabyName = root.optString("babyName", "").trim()
            val importedBabyGender = root.optString("babyGender", "").trim()
            val importedDpp = root.optString("pregnancyDppDate", "").trim()
            val importedCompMode = root.optString("pregnancyComparisonMode", "").trim()
            if (importedBabyName.isNotBlank() || importedBabyGender.isNotBlank() || importedDpp.isNotBlank()) {
                CustomizationRepository(appContext).syncSharedPregnancyDetailsFromCloud(
                    dppDate = importedDpp,
                    comparisonMode = importedCompMode,
                    babyName = importedBabyName,
                    babyGender = importedBabyGender
                )
            }

            // 5. Private Diary
            val importedDiary = root.optJSONArray("diaryEntries")?.let { parseDiaryEntriesFromJsonArray(it) }.orEmpty()
            val importedLetters = root.optJSONArray("supportLetters")?.let { parseSupportLettersFromJsonArray(it) }.orEmpty()
            if (importedDiary.isNotEmpty() || importedLetters.isNotEmpty()) {
                diaryMerged += diaryRepo.mergeImportedDiaryEntries(importedDiary, importedLetters)
            }

            // 6. Family Mural Items
            root.optJSONArray("familyMuralItems")?.let { arr ->
                val currentMural = getFamilyMuralItems(appContext).toMutableList()
                val existingTitles = currentMural.map { it.title.trim().lowercase() }.toMutableSet()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "").trim()
                    if (title.isNotEmpty() && title.lowercase() !in existingTitles) {
                        existingTitles.add(title.lowercase())
                        currentMural.add(
                            FamilyMuralItem(
                                id = obj.optString("id", "mural_" + UUID.randomUUID().toString().take(8)),
                                title = title,
                                category = obj.optString("category", "Compras"),
                                addedBy = obj.optString("addedBy", "Família"),
                                completed = obj.optBoolean("completed", false),
                                createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                            )
                        )
                        muralMerged++
                    }
                }
                if (muralMerged > 0) {
                    saveFamilyMuralItems(appContext, currentMural, notifyWidgets = true)
                }
            }

            OmniHomeWidgetProvider.requestUpdate(appContext)

            OmniMigrationResult(
                tasksMerged = tasksMerged,
                transactionsMerged = transactionsMerged,
                accountsMerged = accountsMerged,
                healthMerged = healthMerged,
                diaryMerged = diaryMerged,
                muralMerged = muralMerged
            )
        }
    }

    private fun taskSignature(title: String, dateEpochDay: Long, isInbox: Boolean): String {
        return "${title.trim().lowercase()}_${if (isInbox) "inbox" else dateEpochDay.toString()}"
    }

    private fun txSignature(title: String, amountBrl: Double, dateEpochDay: Long, type: String): String {
        val roundedCents = kotlin.math.round(amountBrl * 100).toLong()
        return "${title.trim().lowercase()}_${roundedCents}_${dateEpochDay}_${type.uppercase()}"
    }

    private fun parseTasksFromJsonArray(arr: JSONArray): List<TaskItem> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                if (title.isEmpty()) continue
                val subtasksArr = obj.optJSONArray("subtasks") ?: JSONArray()
                val subtasks = buildList {
                    for (j in 0 until subtasksArr.length()) {
                        val st = subtasksArr.optJSONObject(j) ?: continue
                        add(
                            SubtaskItem(
                                id = st.optString("id", "st_$j"),
                                title = st.optString("title", ""),
                                done = st.optBoolean("done", false)
                            )
                        )
                    }
                }
                val weekdaysArr = obj.optJSONArray("recurrenceWeekdays") ?: JSONArray()
                val weekdays = (0 until weekdaysArr.length()).map { weekdaysArr.optInt(it) }.filter { it in 1..7 }
                val dateEpoch = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay())
                add(
                    TaskItem(
                        id = obj.optString("id", "task_$i"),
                        title = title,
                        notes = obj.optString("notes", ""),
                        location = obj.optString("location", ""),
                        category = obj.optString("category", "Rotina"),
                        priority = obj.optString("priority", "MEDIUM"),
                        isEssential = obj.optBoolean("isEssential", false),
                        dateEpochDay = dateEpoch,
                        endDateEpochDay = obj.optLong("endDateEpochDay", 0L),
                        yearMonth = DateCalendarUtils.yearMonthFromEpochDay(dateEpoch),
                        alarmHour = obj.optLong("alarmHour", 9L),
                        alarmMinute = obj.optLong("alarmMinute", 0L),
                        alarmEnabled = obj.optBoolean("alarmEnabled", false),
                        completed = obj.optBoolean("completed", false),
                        isInbox = obj.optBoolean("isInbox", false),
                        durationMinutes = obj.optInt("durationMinutes", 30),
                        iconEmoji = obj.optString("iconEmoji", "📋"),
                        taskColorHex = obj.optLong("taskColorHex", 0xFF0E7C5B),
                        recurrence = obj.optString("recurrence", TaskRecurrence.ONCE),
                        recurrenceWeekdays = weekdays,
                        subtasks = subtasks,
                        ringtoneCategory = obj.optString("ringtoneCategory", "APP_DEFAULT"),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                        assignedMemberId = obj.optString("assignedMemberId", ""),
                        assignedMemberName = obj.optString("assignedMemberName", "Família"),
                        assignedMemberColorHex = obj.optLong("assignedMemberColorHex", 0xFF0E7C5B),
                        assignedMemberEmoji = obj.optString("assignedMemberEmoji", "🌿"),
                        estimatedCostBrl = obj.optDouble("estimatedCostBrl", 0.0),
                        autoCreateExpenseOnComplete = obj.optBoolean("autoCreateExpenseOnComplete", false)
                    )
                )
            }
        }
    }

    private fun parseTransactionsFromJsonArray(arr: JSONArray): List<FinanceTransaction> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val amount = obj.optDouble("amountBrl", obj.optDouble("amountOriginal", 0.0))
                if (title.isEmpty() || amount <= 0.0) continue
                val epoch = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay())
                add(
                    FinanceTransaction(
                        id = obj.optString("id", "tx_$i"),
                        title = title,
                        amountOriginal = amount,
                        amountBrl = amount,
                        type = obj.optString("type", "EXPENSE"),
                        category = obj.optString("category", "Geral"),
                        budgetBucket = obj.optString("budgetBucket", obj.optString("bucket503020", "NEEDS")),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        dateEpochDay = epoch,
                        yearMonth = DateCalendarUtils.yearMonthFromEpochDay(epoch),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                        responsibleMemberId = obj.optString("responsibleMemberId", ""),
                        responsibleMemberName = obj.optString("responsibleMemberName", "Família"),
                        responsibleMemberColorHex = obj.optLong("responsibleMemberColorHex", 0xFF0E7C5B),
                        responsibleMemberEmoji = obj.optString("responsibleMemberEmoji", "🌿"),
                        accountId = obj.optString("accountId", obj.optString("bankAccountId", "acc_nubank")),
                        accountName = obj.optString("accountName", obj.optString("bankAccountName", "Nubank")),
                        isCreditCard = obj.optBoolean("isCreditCard", false),
                        installmentIndex = obj.optInt("installmentIndex", obj.optInt("installmentNumber", 1)),
                        installmentCount = obj.optInt("installmentCount", 1),
                        isPaid = obj.optBoolean("isPaid", true)
                    )
                )
            }
        }
    }

    private fun parseBankAccountsFromJsonArray(arr: JSONArray): List<BankAccount> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                add(
                    BankAccount(
                        id = obj.optString("id", "acc_$i"),
                        name = name,
                        institution = obj.optString("institution", "Banco"),
                        initialBalanceBrl = obj.optDouble("initialBalanceBrl", 0.0),
                        colorHex = obj.optLong("colorHex", 0xFF0E7C5B),
                        iconEmoji = obj.optString("iconEmoji", "🏦"),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY)
                    )
                )
            }
        }
    }

    private fun parseGlucoseFromJsonArray(arr: JSONArray): List<GlucoseRecord> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val value = obj.optInt("valueMgDl", 0)
                if (value <= 0) continue
                val rawDateIso = obj.optString("dateIso", "")
                val epochDay = obj.optLong(
                    "dateEpochDay",
                    runCatching { java.time.LocalDate.parse(rawDateIso).toEpochDay() }
                        .getOrDefault(DateCalendarUtils.todayEpochDay())
                )
                val dateIso = rawDateIso.ifBlank { DateCalendarUtils.formatIsoDate(epochDay) }
                val createdAt = obj.optLong(
                    "createdAtMillis",
                    obj.optLong("timestampMillis", System.currentTimeMillis())
                )
                add(
                    GlucoseRecord(
                        id = obj.optString("id", "glu_$i"),
                        dateIso = dateIso,
                        dateEpochDay = epochDay,
                        timeHHmm = obj.optString("timeHHmm", "08:00"),
                        slot = DiabetesSlotCatalog.normalizeSlot(obj.optString("slot", DiabetesSlotCatalog.JEJUM)),
                        valueMgDl = value,
                        notes = obj.optString("notes", ""),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.PERSONAL),
                        memberId = obj.optString("memberId", ""),
                        memberName = obj.optString("memberName", "Você"),
                        createdAtMillis = createdAt
                    )
                )
            }
        }
    }

    private fun parseBpFromJsonArray(arr: JSONArray): List<BloodPressureRecord> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val sys = obj.optInt("systolic", 0)
                val dia = obj.optInt("diastolic", 0)
                if (sys <= 0 || dia <= 0) continue
                val rawDateIso = obj.optString("dateIso", "")
                val epochDay = obj.optLong(
                    "dateEpochDay",
                    runCatching { java.time.LocalDate.parse(rawDateIso).toEpochDay() }
                        .getOrDefault(DateCalendarUtils.todayEpochDay())
                )
                val dateIso = rawDateIso.ifBlank { DateCalendarUtils.formatIsoDate(epochDay) }
                val createdAt = obj.optLong(
                    "createdAtMillis",
                    obj.optLong("timestampMillis", System.currentTimeMillis())
                )
                val pulse = obj.optInt("pulseBpm", obj.optInt("heartRateBpm", 72))
                add(
                    BloodPressureRecord(
                        id = obj.optString("id", "bp_$i"),
                        dateIso = dateIso,
                        dateEpochDay = epochDay,
                        timeHHmm = obj.optString("timeHHmm", "08:00"),
                        systolic = sys,
                        diastolic = dia,
                        pulseBpm = pulse,
                        notes = obj.optString("notes", ""),
                        privacyScope = obj.optString("privacyScope", PrivacyScope.PERSONAL),
                        memberId = obj.optString("memberId", ""),
                        memberName = obj.optString("memberName", "Marcus"),
                        createdAtMillis = createdAt
                    )
                )
            }
        }
    }

    private fun parseInsulinFromJsonArray(arr: JSONArray): List<InsulinRecord> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val units = obj.optInt("units", 0)
                if (units <= 0) continue
                add(
                    InsulinRecord(
                        id = obj.optString("id", "ins_$i"),
                        dateIso = obj.optString("dateIso", DateCalendarUtils.todayIsoDate()),
                        timeFormatted = obj.optString("timeFormatted", "08:00"),
                        insulinType = obj.optString("insulinType", "Basal / Glargina"),
                        units = units,
                        injectionSite = obj.optString("injectionSite", InsulinSiteCatalog.ABDOMEN_ESQ),
                        memberId = obj.optString("memberId", "Marcus"),
                        notes = obj.optString("notes", ""),
                        timestampEpochMillis = obj.optLong("timestampEpochMillis", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun parseDiaryEntriesFromJsonArray(arr: JSONArray): List<PrivateDiaryEntry> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val text = obj.optString("freeText", "").trim()
                if (text.isEmpty()) continue
                val tagsArr = obj.optJSONArray("symptomTags") ?: JSONArray()
                val tags = (0 until tagsArr.length()).map { tagsArr.optString(it) }.filter { it.isNotBlank() }
                add(
                    PrivateDiaryEntry(
                        id = obj.optString("id", "diary_$i"),
                        userId = obj.optString("userId", LocalOfflineStore.LOCAL_USER_ID),
                        dateIso = obj.optString("dateIso", LocalDate.now().toString()),
                        epochDay = obj.optLong("epochDay", LocalDate.now().toEpochDay()),
                        hour = obj.optInt("hour", 9),
                        minute = obj.optInt("minute", 0),
                        moodId = obj.optString("moodId", "CALM"),
                        symptomTags = tags,
                        freeText = text,
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun parseSupportLettersFromJsonArray(arr: JSONArray): List<SupportLetterRecord> {
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val content = obj.optString("content", "").trim()
                if (content.isEmpty()) continue
                add(
                    SupportLetterRecord(
                        id = obj.optString("id", "letter_$i"),
                        userId = obj.optString("userId", LocalOfflineStore.LOCAL_USER_ID),
                        dateIso = obj.optString("dateIso", LocalDate.now().toString()),
                        timeFormatted = obj.optString("timeFormatted", "12:00"),
                        title = obj.optString("title", "Carta de Apoio"),
                        content = content,
                        moodSummaryUsed = obj.optString("moodSummaryUsed", ""),
                        generatedViaCloudAi = obj.optBoolean("generatedViaCloudAi", false),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }
    }
}
