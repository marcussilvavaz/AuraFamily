package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local on-device reactive store used when login is disabled / bypassed for fast testing.
 * Persists tasks, transactions, subscriptions, savings goals, bank accounts, and family members in SharedPreferences.
 */
class LocalOfflineStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfileFlow = MutableStateFlow(loadUserProfile())
    val userProfileFlow: StateFlow<UserProfile> = _userProfileFlow.asStateFlow()

    private val _householdFlow = MutableStateFlow(loadHousehold())
    val householdFlow: StateFlow<Household> = _householdFlow.asStateFlow()

    private val _tasksFlow = MutableStateFlow(loadTasks())
    val tasksFlow: StateFlow<List<TaskItem>> = _tasksFlow.asStateFlow()

    private val _transactionsFlow = MutableStateFlow(loadTransactions())
    val transactionsFlow: StateFlow<List<FinanceTransaction>> = _transactionsFlow.asStateFlow()

    private val _subscriptionsFlow = MutableStateFlow(loadSubscriptions())
    val subscriptionsFlow: StateFlow<List<SubscriptionBill>> = _subscriptionsFlow.asStateFlow()

    private val _savingsGoalsFlow = MutableStateFlow(loadSavingsGoals())
    val savingsGoalsFlow: StateFlow<List<SavingsGoal>> = _savingsGoalsFlow.asStateFlow()

    private fun loadUserProfile(): UserProfile {
        val houseId = prefs.getString(KEY_HOUSEHOLD_ID, "FAM_OMNI_LOCAL") ?: "FAM_OMNI_LOCAL"
        val theme = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        val rawName = prefs.getString(KEY_MEMBER_NAME, "Você (Omni)") ?: "Você (Omni)"
        val displayName = rawName.replace("Família Aura (Modo Local)", "Você (Omni)").replace("Aura", "Omni")
        val colorHex = prefs.getLong(KEY_MEMBER_COLOR_HEX, 0xFF0E7C5B)
        val emoji = prefs.getString(KEY_MEMBER_EMOJI, "🌿") ?: "🌿"
        val initials = prefs.getString(KEY_MEMBER_INITIALS, "OM") ?: "OM"
        val role = prefs.getString(KEY_MEMBER_ROLE, "ADMIN") ?: "ADMIN"
        val splitPct = prefs.getFloat(KEY_MEMBER_SPLIT_PCT, 50f).toDouble()
        return UserProfile(
            id = LOCAL_USER_ID,
            userId = LOCAL_USER_ID,
            displayName = displayName,
            householdId = houseId,
            themeMode = theme,
            baseCurrency = "BRL",
            memberColorHex = colorHex,
            avatarEmoji = emoji,
            avatarInitials = initials,
            memberRole = role,
            defaultSplitPercentage = splitPct
        )
    }

    fun updateUserMemberIdentity(
        displayName: String,
        memberColorHex: Long,
        avatarEmoji: String,
        avatarInitials: String,
        memberRole: String,
        defaultSplitPercentage: Double
    ) {
        val cleanName = displayName.trim().ifBlank { "Você" }
        prefs.edit()
            .putString(KEY_MEMBER_NAME, cleanName)
            .putLong(KEY_MEMBER_COLOR_HEX, memberColorHex)
            .putString(KEY_MEMBER_EMOJI, avatarEmoji.ifBlank { "🌿" })
            .putString(KEY_MEMBER_INITIALS, avatarInitials.take(3).ifBlank { "OM" })
            .putString(KEY_MEMBER_ROLE, memberRole.ifBlank { "ADMIN" })
            .putFloat(KEY_MEMBER_SPLIT_PCT, defaultSplitPercentage.toFloat())
            .apply()
        _userProfileFlow.value = loadUserProfile()
        _householdFlow.value = loadHousehold()

        val updatedTasks = _tasksFlow.value.map { task ->
            if (task.userId == LOCAL_USER_ID || task.assignedMemberId == LOCAL_USER_ID || task.assignedMemberId.isBlank()) {
                task.copy(
                    assignedMemberColorHex = memberColorHex,
                    createdByColorHex = memberColorHex,
                    assignedMemberName = if (task.privacyScope == PrivacyScope.PERSONAL) "Só Meu ($cleanName)" else cleanName
                )
            } else {
                task
            }
        }
        saveTasks(updatedTasks)
        _tasksFlow.value = updatedTasks
    }

    private fun loadExtraMembers(ownerProfile: UserProfile): List<FamilyMemberBadge> {
        val ownerBadge = FamilyMemberBadge(
            userId = ownerProfile.userId,
            displayName = ownerProfile.displayName,
            colorHex = ownerProfile.memberColorHex,
            avatarEmoji = ownerProfile.avatarEmoji,
            role = ownerProfile.memberRole
        )
        val raw = prefs.getString(KEY_FAMILY_MEMBERS_JSON, null) ?: return listOf(ownerBadge)
        return try {
            val arr = JSONArray(raw)
            val others = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val uid = obj.optString("userId", "")
                    if (uid.isNotBlank() && uid != ownerProfile.userId) {
                        add(
                            FamilyMemberBadge(
                                userId = uid,
                                displayName = obj.optString("displayName", "Membro"),
                                colorHex = obj.optLong("colorHex", 0xFF2962FF),
                                avatarEmoji = obj.optString("avatarEmoji", "👤"),
                                role = obj.optString("role", "ADULT")
                            )
                        )
                    }
                }
            }
            listOf(ownerBadge) + others
        } catch (_: Exception) {
            listOf(ownerBadge)
        }
    }

    private fun saveExtraMembers(members: List<FamilyMemberBadge>) {
        val arr = JSONArray()
        members.filter { it.userId != LOCAL_USER_ID }.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("userId", m.userId)
                    put("displayName", m.displayName)
                    put("colorHex", m.colorHex)
                    put("avatarEmoji", m.avatarEmoji)
                    put("role", m.role)
                }
            )
        }
        prefs.edit().putString(KEY_FAMILY_MEMBERS_JSON, arr.toString()).apply()
    }

    fun saveFamilyMember(member: FamilyMemberBadge) {
        val current = loadExtraMembers(loadUserProfile()).toMutableList()
        if (member.userId == LOCAL_USER_ID) {
            updateUserMemberIdentity(
                displayName = member.displayName,
                memberColorHex = member.colorHex,
                avatarEmoji = member.avatarEmoji,
                avatarInitials = member.displayName.take(2).uppercase(),
                memberRole = member.role,
                defaultSplitPercentage = 50.0
            )
            return
        }
        val cleanId = member.userId.ifBlank { "mem_" + UUID.randomUUID().toString().take(8) }
        val cleanMember = member.copy(
            userId = cleanId,
            displayName = member.displayName.trim().ifBlank { "Familiar" },
            avatarEmoji = member.avatarEmoji.ifBlank { "👤" }
        )
        val idx = current.indexOfFirst { it.userId == cleanId }
        if (idx >= 0) {
            current[idx] = cleanMember
        } else {
            current.add(cleanMember)
        }
        saveExtraMembers(current)
        _householdFlow.value = loadHousehold()
    }

    fun removeFamilyMember(memberUserId: String) {
        if (memberUserId == LOCAL_USER_ID) return
        val current = loadExtraMembers(loadUserProfile()).filterNot { it.userId == memberUserId }
        saveExtraMembers(current)
        _householdFlow.value = loadHousehold()
    }

    private fun loadBankAccounts(): List<BankAccount> {
        val raw = prefs.getString(KEY_BANK_ACCOUNTS_JSON, null)
        if (raw == null) {
            val defaults = BankAccountCatalog.defaultAccounts()
            saveBankAccounts(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val loaded = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    if (id.isNotBlank()) {
                        add(
                            BankAccount(
                                id = id,
                                name = obj.optString("name", "Conta"),
                                institution = obj.optString("institution", "Outro"),
                                initialBalanceBrl = obj.optDouble("initialBalanceBrl", 0.0),
                                creditLimitBrl = obj.optDouble("creditLimitBrl", 5000.0),
                                closingDay = obj.optInt("closingDay", 25).coerceIn(1, 31),
                                dueDay = obj.optInt("dueDay", 5).coerceIn(1, 31),
                                colorHex = obj.optLong("colorHex", 0xFF0E7C5B),
                                iconEmoji = obj.optString("iconEmoji", "🏦"),
                                privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY)
                            )
                        )
                    }
                }
            }
            loaded.ifEmpty { BankAccountCatalog.defaultAccounts() }
        } catch (_: Exception) {
            BankAccountCatalog.defaultAccounts()
        }
    }

    private fun saveBankAccounts(accounts: List<BankAccount>) {
        val arr = JSONArray()
        accounts.forEach { acc ->
            arr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("institution", acc.institution)
                    put("initialBalanceBrl", acc.initialBalanceBrl)
                    put("creditLimitBrl", acc.creditLimitBrl)
                    put("closingDay", acc.closingDay)
                    put("dueDay", acc.dueDay)
                    put("colorHex", acc.colorHex)
                    put("iconEmoji", acc.iconEmoji)
                    put("privacyScope", acc.privacyScope)
                }
            )
        }
        prefs.edit().putString(KEY_BANK_ACCOUNTS_JSON, arr.toString()).apply()
    }

    fun saveBankAccount(account: BankAccount) {
        val current = loadBankAccounts().toMutableList()
        val cleanId = account.id.ifBlank { "acc_" + UUID.randomUUID().toString().take(8) }
        val cleanAccount = account.copy(
            id = cleanId,
            name = account.name.trim().ifBlank { account.institution.ifBlank { "Conta" } },
            institution = account.institution.trim().ifBlank { "Banco" },
            iconEmoji = account.iconEmoji.ifBlank { "🏦" }
        )
        val idx = current.indexOfFirst { it.id == cleanId }
        if (idx >= 0) {
            current[idx] = cleanAccount
        } else {
            current.add(cleanAccount)
        }
        saveBankAccounts(current)
        _householdFlow.value = loadHousehold()
    }

    fun deleteBankAccount(accountId: String) {
        val current = loadBankAccounts().filterNot { it.id == accountId }
        val safeList = current.ifEmpty { BankAccountCatalog.defaultAccounts() }
        saveBankAccounts(safeList)
        _householdFlow.value = loadHousehold()
    }

    private fun loadHousehold(): Household {
        val houseId = prefs.getString(KEY_HOUSEHOLD_ID, "FAM_OMNI_LOCAL") ?: "FAM_OMNI_LOCAL"
        val rawName = prefs.getString(KEY_HOUSEHOLD_NAME, "Família Omni") ?: "Família Omni"
        val name = rawName.replace("Família Aura", "Família Omni")
        val income = prefs.getFloat(KEY_MONTHLY_INCOME, 8500f).toDouble()
        val needs = prefs.getInt(KEY_NEEDS_PCT, 50).toLong()
        val wants = prefs.getInt(KEY_WANTS_PCT, 30).toLong()
        val savings = prefs.getInt(KEY_SAVINGS_PCT, 20).toLong()
        val defaultSplit = prefs.getString(KEY_DEFAULT_SPLIT_MODE, SplitMode.EQUAL) ?: SplitMode.EQUAL
        val profile = loadUserProfile()
        val budgetsRaw = prefs.getString(KEY_CATEGORY_BUDGETS_JSON, null)
        val budgetsMap = runCatching {
            if (budgetsRaw.isNullOrBlank()) emptyMap() else deserializeDoubleMap(JSONObject(budgetsRaw))
        }.getOrDefault(emptyMap())
        val customCatsRaw = prefs.getString(KEY_CUSTOM_CATEGORIES_JSON, null)
        val customCatsList = runCatching {
            if (customCatsRaw.isNullOrBlank()) emptyList() else deserializeStringList(JSONArray(customCatsRaw))
        }.getOrDefault(emptyList())
        return Household(
            id = houseId,
            householdId = houseId,
            name = name,
            ownerId = LOCAL_USER_ID,
            monthlyIncomeBrl = income,
            needsPercentage = needs,
            wantsPercentage = wants,
            savingsPercentage = savings,
            defaultSplitMode = defaultSplit,
            members = loadExtraMembers(profile),
            bankAccounts = loadBankAccounts(),
            categoryBudgetsBrl = budgetsMap,
            customCategories = customCatsList,
            muralItems = OmniBackupAndMigrationManager.getFamilyMuralItems(appContext)
        )
    }

    fun updateCategoryBudget(category: String, limitBrl: Double) {
        val cleanCat = category.trim()
        if (cleanCat.isEmpty()) return
        val current = _householdFlow.value.categoryBudgetsBrl.toMutableMap()
        if (limitBrl <= 0.0) {
            current.remove(cleanCat)
        } else {
            current[cleanCat] = limitBrl
        }
        prefs.edit().putString(KEY_CATEGORY_BUDGETS_JSON, serializeDoubleMap(current).toString()).apply()
        _householdFlow.value = loadHousehold()
    }

    fun saveCustomCategories(categories: List<String>) {
        val clean = categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES_JSON, serializeStringList(clean).toString()).apply()
        _householdFlow.value = loadHousehold()
    }

    fun syncHouseholdMuralItems(items: List<FamilyMuralItem>) {
        OmniBackupAndMigrationManager.saveFamilyMuralItems(appContext, items, notifyWidgets = true)
        _householdFlow.value = loadHousehold()
    }

    fun updateThemeMode(themeMode: String) {
        val valid = if (themeMode in listOf("SYSTEM", "LIGHT", "DARK", "AMOLED")) themeMode else "SYSTEM"
        prefs.edit().putString(KEY_THEME_MODE, valid).apply()
        _userProfileFlow.value = loadUserProfile()
    }

    fun switchHousehold(code: String, familyName: String): String {
        val cleanCode = code.trim().uppercase().ifBlank { "FAM_OMNI_LOCAL" }
        val cleanName = familyName.trim().ifBlank { "Família Omni" }
        prefs.edit()
            .putString(KEY_HOUSEHOLD_ID, cleanCode)
            .putString(KEY_HOUSEHOLD_NAME, cleanName)
            .apply()
        _userProfileFlow.value = loadUserProfile()
        _householdFlow.value = loadHousehold()
        return cleanCode
    }

    fun updateHouseholdBudgetPlan(
        name: String,
        monthlyIncomeBrl: Double,
        needsPct: Int,
        wantsPct: Int,
        savingsPct: Int,
        defaultSplitMode: String = SplitMode.EQUAL
    ) {
        prefs.edit()
            .putString(KEY_HOUSEHOLD_NAME, name.trim().ifBlank { "Família Omni" })
            .putFloat(KEY_MONTHLY_INCOME, monthlyIncomeBrl.toFloat())
            .putInt(KEY_NEEDS_PCT, needsPct)
            .putInt(KEY_WANTS_PCT, wantsPct)
            .putInt(KEY_SAVINGS_PCT, savingsPct)
            .putString(KEY_DEFAULT_SPLIT_MODE, defaultSplitMode)
            .apply()
        _householdFlow.value = loadHousehold()
    }

    // JSON Helpers
    private fun serializeStringList(list: List<String>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr
    }

    private fun deserializeStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }

    private fun serializeIntList(list: List<Int>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr
    }

    private fun deserializeIntList(arr: JSONArray?): List<Int> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optInt(i, 0)
                if (v in 1..7) add(v)
            }
        }
    }

    private fun serializeDoubleMap(map: Map<String, Double>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj
    }

    private fun deserializeDoubleMap(obj: JSONObject?): Map<String, Double> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, Double>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            result[k] = obj.optDouble(k, 0.0)
        }
        return result
    }

    private fun serializeSplitShares(shares: List<ExpenseSplitShare>): JSONArray {
        val arr = JSONArray()
        shares.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("memberId", s.memberId)
                    put("memberName", s.memberName)
                    put("memberColorHex", s.memberColorHex)
                    put("memberEmoji", s.memberEmoji)
                    put("percentage", s.percentage)
                    put("shareAmountBrl", s.shareAmountBrl)
                    put("settled", s.settled)
                }
            )
        }
        return arr
    }

    private fun deserializeSplitShares(arr: JSONArray?): List<ExpenseSplitShare> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    ExpenseSplitShare(
                        memberId = obj.optString("memberId", ""),
                        memberName = obj.optString("memberName", "Família"),
                        memberColorHex = obj.optLong("memberColorHex", 0xFF0E7C5B),
                        memberEmoji = obj.optString("memberEmoji", "🌿"),
                        percentage = obj.optDouble("percentage", 100.0),
                        shareAmountBrl = obj.optDouble("shareAmountBrl", 0.0),
                        settled = obj.optBoolean("settled", false)
                    )
                )
            }
        }
    }

    private fun serializeSubtasks(subtasks: List<SubtaskItem>): JSONArray {
        val arr = JSONArray()
        subtasks.forEach { st ->
            arr.put(
                JSONObject().apply {
                    put("id", st.id.ifBlank { "st_" + UUID.randomUUID().toString().take(6) })
                    put("title", st.title)
                    put("done", st.done)
                }
            )
        }
        return arr
    }

    private fun deserializeSubtasks(arr: JSONArray?): List<SubtaskItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                if (title.isNotEmpty()) {
                    add(
                        SubtaskItem(
                            id = obj.optString("id", "st_$i"),
                            title = title,
                            done = obj.optBoolean("done", false)
                        )
                    )
                }
            }
        }
    }

    // Tasks
    private fun loadTasks(): List<TaskItem> {
        val raw = prefs.getString(KEY_TASKS_JSON, null)
        if (raw == null) {
            val today = DateCalendarUtils.todayEpochDay()
            val ym = DateCalendarUtils.yearMonthFromEpochDay(today)
            val initial = listOf(
                TaskItem(
                    id = "task_demo_1",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Revisar orçamento familiar da semana",
                    notes = "Conferir contas fixas, regra 50/30/20 e metas do mês",
                    category = "Finanças",
                    priority = "HIGH",
                    isEssential = true,
                    dateEpochDay = today,
                    yearMonth = ym,
                    alarmHour = 9,
                    alarmMinute = 0,
                    alarmEnabled = true,
                    completed = false,
                    isInbox = false,
                    durationMinutes = 45,
                    iconEmoji = "📊",
                    taskColorHex = 0xFF0E7C5B,
                    recurrence = TaskRecurrence.WEEKLY,
                    ringtoneCategory = "FOCO",
                    subtasks = listOf(
                        SubtaskItem(id = "st_1", title = "Checar saldo disponível do mês", done = true),
                        SubtaskItem(id = "st_2", title = "Confirmar faturas e contas da semana", done = false),
                        SubtaskItem(id = "st_3", title = "Separar 20% da meta de reserva", done = false)
                    ),
                    privacyScope = PrivacyScope.FAMILY,
                    assignedMemberId = LOCAL_USER_ID,
                    assignedMemberName = "Família",
                    assignedMemberColorHex = 0xFF0E7C5B,
                    assignedMemberEmoji = "🌿"
                ),
                TaskItem(
                    id = "task_demo_3",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Pagar academia / plano pessoal",
                    notes = "Conta individual (visível só no meu filtro Individual ou Tudo)",
                    category = "Finanças",
                    priority = "HIGH",
                    isEssential = true,
                    dateEpochDay = today,
                    yearMonth = ym,
                    alarmHour = 11,
                    alarmMinute = 30,
                    alarmEnabled = true,
                    completed = false,
                    isInbox = false,
                    durationMinutes = 30,
                    iconEmoji = "🏋️",
                    taskColorHex = 0xFF7C4DFF,
                    recurrence = TaskRecurrence.MONTHLY,
                    ringtoneCategory = "CONTAS",
                    subtasks = listOf(
                        SubtaskItem(id = "st_4", title = "Copiar código PIX no app da academia", done = false),
                        SubtaskItem(id = "st_5", title = "Dar check para lançar automaticamente nas Finanças", done = false)
                    ),
                    privacyScope = PrivacyScope.PERSONAL,
                    assignedMemberId = LOCAL_USER_ID,
                    assignedMemberName = "Só Meu",
                    assignedMemberColorHex = 0xFF7C4DFF,
                    assignedMemberEmoji = "🔒",
                    estimatedCostBrl = 119.90,
                    autoCreateExpenseOnComplete = true
                ),
                TaskItem(
                    id = "task_demo_2",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Compras da despensa e frutas da semana",
                    notes = "Passar no mercado após o expediente",
                    category = "Casa",
                    priority = "MEDIUM",
                    isEssential = false,
                    dateEpochDay = today,
                    yearMonth = ym,
                    alarmHour = 18,
                    alarmMinute = 0,
                    alarmEnabled = true,
                    completed = false,
                    isInbox = false,
                    durationMinutes = 60,
                    iconEmoji = "🛒",
                    taskColorHex = 0xFFD4AF37,
                    recurrence = TaskRecurrence.WEEKLY,
                    ringtoneCategory = "SUAVE",
                    subtasks = listOf(
                        SubtaskItem(id = "st_6", title = "Frutas, verduras e ovos", done = true),
                        SubtaskItem(id = "st_7", title = "Café e itens de limpeza", done = false)
                    ),
                    privacyScope = PrivacyScope.FAMILY,
                    assignedMemberId = LOCAL_USER_ID,
                    assignedMemberName = "Família",
                    assignedMemberColorHex = 0xFFD4AF37,
                    assignedMemberEmoji = "🛒",
                    estimatedCostBrl = 250.0,
                    autoCreateExpenseOnComplete = true
                )
            )
            saveTasks(initial)
            return initial
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val prio = obj.optString("priority", "MEDIUM")
                    val memberCol = obj.optLong("assignedMemberColorHex", 0xFF0E7C5B)
                    add(
                        TaskItem(
                            id = obj.optString("id"),
                            userId = obj.optString("userId", LOCAL_USER_ID),
                            householdId = obj.optString("householdId", "FAM_OMNI_LOCAL"),
                            title = obj.optString("title"),
                            notes = obj.optString("notes"),
                            location = obj.optString("location", ""),
                            category = obj.optString("category", "Geral"),
                            priority = prio,
                            isEssential = obj.optBoolean("isEssential", prio == "HIGH"),
                            dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                            endDateEpochDay = obj.optLong("endDateEpochDay", 0L),
                            yearMonth = obj.optString("yearMonth", DateCalendarUtils.currentYearMonth()),
                            alarmHour = obj.optLong("alarmHour", 9L),
                            alarmMinute = obj.optLong("alarmMinute", 0L),
                            alarmEnabled = obj.optBoolean("alarmEnabled", true),
                            completed = obj.optBoolean("completed", false),
                            completedDates = deserializeStringList(obj.optJSONArray("completedDates")),
                            isInbox = obj.optBoolean("isInbox", false),
                            durationMinutes = obj.optInt("durationMinutes", 30).coerceIn(5, 720),
                            iconEmoji = obj.optString("iconEmoji", "📋").ifBlank { "📋" },
                            taskColorHex = obj.optLong("taskColorHex", memberCol),
                            recurrence = obj.optString("recurrence", TaskRecurrence.ONCE),
                            recurrenceWeekdays = deserializeIntList(obj.optJSONArray("recurrenceWeekdays")),
                            subtasks = deserializeSubtasks(obj.optJSONArray("subtasks")),
                            ringtoneCategory = obj.optString("ringtoneCategory", "APP_DEFAULT"),
                            privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                            visibleToMemberIds = deserializeStringList(obj.optJSONArray("visibleToMemberIds")),
                            assignedMemberId = obj.optString("assignedMemberId", ""),
                            assignedMemberName = obj.optString("assignedMemberName", "Família"),
                            assignedMemberColorHex = memberCol,
                            assignedMemberEmoji = obj.optString("assignedMemberEmoji", "🌿"),
                            createdByName = obj.optString("createdByName", "Família"),
                            createdByColorHex = obj.optLong("createdByColorHex", 0xFF0E7C5B),
                            completedByUserId = obj.optString("completedByUserId", ""),
                            completedByName = obj.optString("completedByName", ""),
                            linkedTransactionId = obj.optString("linkedTransactionId", ""),
                            linkedSubscriptionId = obj.optString("linkedSubscriptionId", ""),
                            estimatedCostBrl = obj.optDouble("estimatedCostBrl", 0.0),
                            autoCreateExpenseOnComplete = obj.optBoolean("autoCreateExpenseOnComplete", false),
                            splitMode = obj.optString("splitMode", SplitMode.NONE),
                            splitShares = deserializeSplitShares(obj.optJSONArray("splitShares"))
                        )
                    )
                }
            }.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenBy { it.dateEpochDay }
                    .thenBy { it.alarmHour }
                    .thenBy { it.alarmMinute }
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveTasks(list: List<TaskItem>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("userId", t.userId)
                    put("householdId", t.householdId)
                    put("title", t.title)
                    put("notes", t.notes)
                    put("location", t.location)
                    put("category", t.category)
                    put("priority", t.priority)
                    put("isEssential", t.isEssential)
                    put("dateEpochDay", t.dateEpochDay)
                    put("endDateEpochDay", t.endDateEpochDay)
                    put("yearMonth", t.yearMonth)
                    put("alarmHour", t.alarmHour)
                    put("alarmMinute", t.alarmMinute)
                    put("alarmEnabled", t.alarmEnabled)
                    put("completed", t.completed)
                    put("completedDates", serializeStringList(t.completedDates))
                    put("isInbox", t.isInbox)
                    put("durationMinutes", t.durationMinutes)
                    put("iconEmoji", t.iconEmoji)
                    put("taskColorHex", t.taskColorHex)
                    put("recurrence", t.recurrence)
                    put("recurrenceWeekdays", serializeIntList(t.recurrenceWeekdays))
                    put("subtasks", serializeSubtasks(t.subtasks))
                    put("ringtoneCategory", t.ringtoneCategory)
                    put("privacyScope", t.privacyScope)
                    put("visibleToMemberIds", serializeStringList(t.visibleToMemberIds))
                    put("assignedMemberId", t.assignedMemberId)
                    put("assignedMemberName", t.assignedMemberName)
                    put("assignedMemberColorHex", t.assignedMemberColorHex)
                    put("assignedMemberEmoji", t.assignedMemberEmoji)
                    put("createdByName", t.createdByName)
                    put("createdByColorHex", t.createdByColorHex)
                    put("completedByUserId", t.completedByUserId)
                    put("completedByName", t.completedByName)
                    put("linkedTransactionId", t.linkedTransactionId)
                    put("linkedSubscriptionId", t.linkedSubscriptionId)
                    put("estimatedCostBrl", t.estimatedCostBrl)
                    put("autoCreateExpenseOnComplete", t.autoCreateExpenseOnComplete)
                    put("splitMode", t.splitMode)
                    put("splitShares", serializeSplitShares(t.splitShares))
                }
            )
        }
        prefs.edit().putString(KEY_TASKS_JSON, arr.toString()).apply()
        com.example.widget.OmniHomeWidgetProvider.requestUpdate(appContext)
    }

    fun addTask(
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
        assignedMemberId: String = LOCAL_USER_ID,
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
    ): String {
        val id = "task_" + UUID.randomUUID().toString().take(8)
        val safeEndEpoch = if (endDateEpochDay >= dateEpochDay) endDateEpochDay else 0L
        val newItem = TaskItem(
            id = id,
            userId = LOCAL_USER_ID,
            householdId = householdId,
            title = title.trim(),
            notes = notes.trim(),
            location = location.trim(),
            category = category.trim().ifBlank { "Geral" },
            priority = priority,
            isEssential = isEssential,
            dateEpochDay = dateEpochDay,
            endDateEpochDay = safeEndEpoch,
            yearMonth = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay),
            alarmHour = alarmHour.toLong(),
            alarmMinute = alarmMinute.toLong(),
            alarmEnabled = if (isInbox) false else alarmEnabled,
            completed = false,
            completedDates = emptyList(),
            isInbox = isInbox,
            durationMinutes = durationMinutes.coerceIn(5, 720),
            iconEmoji = iconEmoji.ifBlank { "📋" },
            taskColorHex = taskColorHex,
            recurrence = recurrence.ifBlank { TaskRecurrence.ONCE },
            recurrenceWeekdays = recurrenceWeekdays,
            subtasks = subtasks,
            ringtoneCategory = ringtoneCategory.ifBlank { "APP_DEFAULT" },
            privacyScope = privacyScope,
            visibleToMemberIds = visibleToMemberIds,
            assignedMemberId = assignedMemberId,
            assignedMemberName = assignedMemberName,
            assignedMemberColorHex = assignedMemberColorHex,
            assignedMemberEmoji = assignedMemberEmoji,
            createdByName = createdByName,
            createdByColorHex = createdByColorHex,
            linkedTransactionId = linkedTransactionId,
            linkedSubscriptionId = linkedSubscriptionId,
            estimatedCostBrl = estimatedCostBrl,
            autoCreateExpenseOnComplete = autoCreateExpenseOnComplete,
            splitMode = splitMode,
            splitShares = splitShares
        )
        val updated = (_tasksFlow.value + newItem)
            .sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenBy { it.dateEpochDay }
                    .thenBy { it.alarmHour }
                    .thenBy { it.alarmMinute }
            )
        saveTasks(updated)
        _tasksFlow.value = updated
        return id
    }

    fun updateTask(
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
    ) {
        val safeEndEpoch = if (endDateEpochDay >= dateEpochDay) endDateEpochDay else 0L
        val updated = _tasksFlow.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    title = title.trim().ifBlank { task.title },
                    notes = notes.trim(),
                    location = location.trim(),
                    category = category.trim().ifBlank { "Geral" },
                    priority = priority,
                    isEssential = isEssential,
                    dateEpochDay = dateEpochDay,
                    endDateEpochDay = safeEndEpoch,
                    yearMonth = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay),
                    alarmHour = alarmHour.coerceIn(0, 23).toLong(),
                    alarmMinute = alarmMinute.coerceIn(0, 59).toLong(),
                    alarmEnabled = if (isInbox) false else alarmEnabled,
                    isInbox = isInbox,
                    durationMinutes = durationMinutes.coerceIn(5, 720),
                    iconEmoji = iconEmoji.ifBlank { "📋" },
                    taskColorHex = taskColorHex,
                    recurrence = recurrence.ifBlank { TaskRecurrence.ONCE },
                    recurrenceWeekdays = recurrenceWeekdays,
                    subtasks = subtasks,
                    ringtoneCategory = ringtoneCategory.ifBlank { "APP_DEFAULT" },
                    privacyScope = privacyScope,
                    estimatedCostBrl = estimatedCostBrl.coerceAtLeast(0.0),
                    autoCreateExpenseOnComplete = autoCreateExpenseOnComplete
                )
            } else {
                task
            }
        }.sortedWith(
            compareBy<TaskItem> { it.completed }
                .thenBy { it.dateEpochDay }
                .thenBy { it.alarmHour }
                .thenBy { it.alarmMinute }
        )
        saveTasks(updated)
        _tasksFlow.value = updated
    }

    fun toggleTaskEssential(taskId: String) {
        val updated = _tasksFlow.value.map {
            if (it.id == taskId) it.copy(isEssential = !it.isEssential) else it
        }
        saveTasks(updated)
        _tasksFlow.value = updated
    }

    fun toggleSubtaskDone(taskId: String, subtaskId: String): List<SubtaskItem>? {
        var updatedSubtasks: List<SubtaskItem>? = null
        val updated = _tasksFlow.value.map { task ->
            if (task.id == taskId) {
                val newSubtasks = task.subtasks.map { st ->
                    if (st.id == subtaskId) st.copy(done = !st.done) else st
                }
                updatedSubtasks = newSubtasks
                task.copy(subtasks = newSubtasks)
            } else {
                task
            }
        }
        saveTasks(updated)
        _tasksFlow.value = updated
        return updatedSubtasks
    }

    fun scheduleInboxTask(
        taskId: String,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        durationMinutes: Int,
        alarmEnabled: Boolean = true
    ) {
        val ym = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
        val updated = _tasksFlow.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    isInbox = false,
                    dateEpochDay = dateEpochDay,
                    yearMonth = ym,
                    alarmHour = alarmHour.coerceIn(0, 23).toLong(),
                    alarmMinute = alarmMinute.coerceIn(0, 59).toLong(),
                    durationMinutes = durationMinutes.coerceIn(5, 720),
                    alarmEnabled = alarmEnabled
                )
            } else {
                task
            }
        }.sortedWith(
            compareBy<TaskItem> { it.completed }
                .thenBy { it.dateEpochDay }
                .thenBy { it.alarmHour }
                .thenBy { it.alarmMinute }
        )
        saveTasks(updated)
        _tasksFlow.value = updated
    }

    fun toggleTaskCompleted(
        taskId: String,
        completed: Boolean,
        completedByUserId: String = LOCAL_USER_ID,
        completedByName: String = "Família",
        linkedTransactionId: String? = null,
        targetDateIso: String = ""
    ) {
        val updated = _tasksFlow.value.map { task ->
            if (task.id == taskId) {
                val updatedDates = task.completedDates.toMutableSet()
                if (targetDateIso.isNotBlank()) {
                    if (completed) {
                        updatedDates.add(targetDateIso)
                    } else {
                        updatedDates.remove(targetDateIso)
                    }
                }
                val newGlobalCompleted = if (task.isRecurringOrMultiDay) {
                    false
                } else {
                    completed
                }
                task.copy(
                    completed = newGlobalCompleted,
                    completedDates = updatedDates.toList(),
                    completedByUserId = if (completed) completedByUserId else "",
                    completedByName = if (completed) completedByName else "",
                    linkedTransactionId = linkedTransactionId ?: task.linkedTransactionId
                )
            } else task
        }.sortedWith(
            compareBy<TaskItem> { it.completed }
                .thenBy { it.dateEpochDay }
                .thenBy { it.alarmHour }
                .thenBy { it.alarmMinute }
        )
        saveTasks(updated)
        _tasksFlow.value = updated
    }

    fun linkTaskToTransaction(taskId: String, transactionId: String) {
        val updated = _tasksFlow.value.map {
            if (it.id == taskId) it.copy(linkedTransactionId = transactionId) else it
        }
        saveTasks(updated)
        _tasksFlow.value = updated
    }

    fun deleteTask(taskId: String) {
        val updated = _tasksFlow.value.filterNot { it.id == taskId }
        saveTasks(updated)
        _tasksFlow.value = updated
    }

    // Transactions
    private fun loadTransactions(): List<FinanceTransaction> {
        val raw = prefs.getString(KEY_TX_JSON, null)
        if (raw == null) {
            val today = DateCalendarUtils.todayEpochDay()
            val ym = DateCalendarUtils.yearMonthFromEpochDay(today)
            val initial = listOf(
                FinanceTransaction(
                    id = "tx_demo_1",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Supermercado da Família",
                    amountOriginal = 480.0,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    amountBrl = 480.0,
                    type = "EXPENSE",
                    budgetBucket = "NEEDS",
                    category = "Alimentação",
                    dateEpochDay = today,
                    yearMonth = ym,
                    createdByName = "Família",
                    createdByColorHex = 0xFF0E7C5B,
                    createdByEmoji = "🌿",
                    privacyScope = PrivacyScope.FAMILY,
                    responsibleMemberName = "Família",
                    responsibleMemberColorHex = 0xFF0E7C5B,
                    splitMode = SplitMode.EQUAL,
                    accountId = "acc_nubank",
                    accountName = "Nubank",
                    isPaid = true
                ),
                FinanceTransaction(
                    id = "tx_demo_2",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Jantar de Fim de Semana",
                    amountOriginal = 165.0,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    amountBrl = 165.0,
                    type = "EXPENSE",
                    budgetBucket = "WANTS",
                    category = "Lazer",
                    dateEpochDay = today,
                    yearMonth = ym,
                    createdByName = "Família",
                    createdByColorHex = 0xFFD4AF37,
                    createdByEmoji = "✨",
                    privacyScope = PrivacyScope.FAMILY,
                    responsibleMemberName = "Família",
                    responsibleMemberColorHex = 0xFFD4AF37,
                    splitMode = SplitMode.EQUAL,
                    accountId = "acc_itau",
                    accountName = "Itaú",
                    isPaid = true
                )
            )
            saveTransactions(initial)
            return initial
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isCredit = obj.optBoolean("isCreditCard", false)
                    add(
                        FinanceTransaction(
                            id = obj.optString("id"),
                            userId = obj.optString("userId", LOCAL_USER_ID),
                            householdId = obj.optString("householdId", "FAM_OMNI_LOCAL"),
                            title = obj.optString("title"),
                            amountOriginal = obj.optDouble("amountOriginal", 0.0),
                            currency = obj.optString("currency", "BRL"),
                            exchangeRateToBrl = obj.optDouble("exchangeRateToBrl", 1.0),
                            amountBrl = obj.optDouble("amountBrl", 0.0),
                            type = obj.optString("type", "EXPENSE"),
                            budgetBucket = obj.optString("budgetBucket", "NEEDS"),
                            category = obj.optString("category", "Geral"),
                            dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                            yearMonth = obj.optString("yearMonth", DateCalendarUtils.currentYearMonth()),
                            recurringSourceId = obj.optString("recurringSourceId", ""),
                            createdByName = obj.optString("createdByName", "Família"),
                            createdByColorHex = obj.optLong("createdByColorHex", 0xFF0E7C5B),
                            createdByEmoji = obj.optString("createdByEmoji", "🌿"),
                            privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                            visibleToMemberIds = deserializeStringList(obj.optJSONArray("visibleToMemberIds")),
                            responsibleMemberId = obj.optString("responsibleMemberId", ""),
                            responsibleMemberName = obj.optString("responsibleMemberName", "Família"),
                            responsibleMemberColorHex = obj.optLong("responsibleMemberColorHex", 0xFF0E7C5B),
                            responsibleMemberEmoji = obj.optString("responsibleMemberEmoji", "🌿"),
                            splitMode = obj.optString("splitMode", SplitMode.NONE),
                            splitWithMemberIds = deserializeStringList(obj.optJSONArray("splitWithMemberIds")),
                            splitSharesMap = deserializeDoubleMap(obj.optJSONObject("splitSharesMap")),
                            splitShares = deserializeSplitShares(obj.optJSONArray("splitShares")),
                            linkedTaskId = obj.optString("linkedTaskId", ""),
                            generateCalendarTask = obj.optBoolean("generateCalendarTask", false),
                            accountId = obj.optString("accountId", "acc_nubank"),
                            accountName = obj.optString("accountName", "Nubank"),
                            destinationAccountId = obj.optString("destinationAccountId", ""),
                            destinationAccountName = obj.optString("destinationAccountName", ""),
                            isRecurring = obj.optBoolean("isRecurring", false),
                            isCreditCard = isCredit,
                            installmentCount = obj.optInt("installmentCount", 1).coerceIn(1, 48),
                            installmentIndex = obj.optInt("installmentIndex", 1).coerceIn(1, 48),
                            parentTransactionId = obj.optString("parentTransactionId", ""),
                            isPaid = obj.optBoolean("isPaid", !isCredit)
                        )
                    )
                }
            }.sortedByDescending { it.dateEpochDay }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveTransactions(list: List<FinanceTransaction>) {
        val arr = JSONArray()
        list.forEach { tx ->
            arr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("userId", tx.userId)
                    put("householdId", tx.householdId)
                    put("title", tx.title)
                    put("amountOriginal", tx.amountOriginal)
                    put("currency", tx.currency)
                    put("exchangeRateToBrl", tx.exchangeRateToBrl)
                    put("amountBrl", tx.amountBrl)
                    put("type", tx.type)
                    put("budgetBucket", tx.budgetBucket)
                    put("category", tx.category)
                    put("dateEpochDay", tx.dateEpochDay)
                    put("yearMonth", tx.yearMonth)
                    put("recurringSourceId", tx.recurringSourceId)
                    put("createdByName", tx.createdByName)
                    put("createdByColorHex", tx.createdByColorHex)
                    put("createdByEmoji", tx.createdByEmoji)
                    put("privacyScope", tx.privacyScope)
                    put("visibleToMemberIds", serializeStringList(tx.visibleToMemberIds))
                    put("responsibleMemberId", tx.responsibleMemberId)
                    put("responsibleMemberName", tx.responsibleMemberName)
                    put("responsibleMemberColorHex", tx.responsibleMemberColorHex)
                    put("responsibleMemberEmoji", tx.responsibleMemberEmoji)
                    put("splitMode", tx.splitMode)
                    put("splitWithMemberIds", serializeStringList(tx.splitWithMemberIds))
                    put("splitSharesMap", serializeDoubleMap(tx.splitSharesMap))
                    put("splitShares", serializeSplitShares(tx.splitShares))
                    put("linkedTaskId", tx.linkedTaskId)
                    put("generateCalendarTask", tx.generateCalendarTask)
                    put("accountId", tx.accountId)
                    put("accountName", tx.accountName)
                    put("destinationAccountId", tx.destinationAccountId)
                    put("destinationAccountName", tx.destinationAccountName)
                    put("isRecurring", tx.isRecurring)
                    put("isCreditCard", tx.isCreditCard)
                    put("installmentCount", tx.installmentCount)
                    put("installmentIndex", tx.installmentIndex)
                    put("parentTransactionId", tx.parentTransactionId)
                    put("isPaid", tx.isPaid)
                }
            )
        }
        prefs.edit().putString(KEY_TX_JSON, arr.toString()).apply()
    }

    fun addTransaction(
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
        createdByName: String = "Família",
        createdByColorHex: Long = 0xFF0E7C5B,
        createdByEmoji: String = "🌿",
        privacyScope: String = PrivacyScope.FAMILY,
        visibleToMemberIds: List<String> = emptyList(),
        responsibleMemberId: String = LOCAL_USER_ID,
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
        isPaid: Boolean = !isCreditCard
    ): String {
        val safeRate = if (currency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
        val validBucket = when (type) {
            "INCOME" -> "INCOME"
            "TRANSFER" -> "TRANSFER"
            else -> budgetBucket.ifBlank { "NEEDS" }
        }
        val safeInstallments = if (type == "EXPENSE" && isCreditCard) installmentCount.coerceIn(1, 48) else 1
        val parentGroupId = if (safeInstallments > 1) "grp_" + UUID.randomUUID().toString().take(8) else ""

        val rawBaseYm = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
        val (_, _, baseDay) = DateCalendarUtils.fromEpochDay(dateEpochDay)
        val matchedAccount = loadBankAccounts().firstOrNull { it.id == accountId }
        val closingDay = (matchedAccount?.closingDay ?: 25).coerceIn(1, 31)
        // Se for compra no crédito lançada após o dia de fechamento do cartão, entra automaticamente na fatura do mês seguinte
        val baseYm = if (type == "EXPENSE" && isCreditCard && baseDay > closingDay) {
            DateCalendarUtils.shiftYearMonth(rawBaseYm, 1)
        } else {
            rawBaseYm
        }

        val perInstallmentOriginal = (Math.round((amountOriginal / safeInstallments) * 100.0) / 100.0).coerceAtLeast(0.01)
        val perInstallmentBrl = (Math.round(perInstallmentOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)

        val generatedList = mutableListOf<FinanceTransaction>()
        var firstId = ""

        for (idx in 0 until safeInstallments) {
            val id = "tx_" + UUID.randomUUID().toString().take(8)
            if (idx == 0) firstId = id
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
                "${title.trim()} (${idx + 1}/$safeInstallments)"
            } else {
                title.trim()
            }
            generatedList.add(
                FinanceTransaction(
                    id = id,
                    userId = LOCAL_USER_ID,
                    householdId = householdId,
                    title = displayTitle,
                    amountOriginal = perInstallmentOriginal,
                    currency = currency,
                    exchangeRateToBrl = safeRate,
                    amountBrl = perInstallmentBrl,
                    type = type,
                    budgetBucket = validBucket,
                    category = category,
                    dateEpochDay = targetEpochDay,
                    yearMonth = targetYm,
                    recurringSourceId = recurringSourceId,
                    createdByName = createdByName,
                    createdByColorHex = createdByColorHex,
                    createdByEmoji = createdByEmoji,
                    privacyScope = privacyScope,
                    visibleToMemberIds = visibleToMemberIds,
                    responsibleMemberId = responsibleMemberId,
                    responsibleMemberName = responsibleMemberName,
                    responsibleMemberColorHex = responsibleMemberColorHex,
                    responsibleMemberEmoji = responsibleMemberEmoji,
                    splitMode = splitMode,
                    splitWithMemberIds = splitWithMemberIds,
                    splitSharesMap = splitSharesMap,
                    splitShares = splitShares,
                    linkedTaskId = if (idx == 0) linkedTaskId else "",
                    generateCalendarTask = if (idx == 0) generateCalendarTask else false,
                    accountId = accountId,
                    accountName = accountName,
                    destinationAccountId = destinationAccountId,
                    destinationAccountName = destinationAccountName,
                    isRecurring = isRecurring,
                    isCreditCard = (type == "EXPENSE" && isCreditCard),
                    installmentCount = safeInstallments,
                    installmentIndex = idx + 1,
                    parentTransactionId = parentGroupId,
                    isPaid = if (type == "EXPENSE" && isCreditCard) isPaid else true
                )
            )
        }

        val updated = (_transactionsFlow.value + generatedList).sortedByDescending { it.dateEpochDay }
        saveTransactions(updated)
        _transactionsFlow.value = updated
        return firstId
    }

    fun updateTransaction(
        txId: String,
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
    ) {
        val safeRate = if (currency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
        val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)
        val validBucket = when (type) {
            "INCOME" -> "INCOME"
            "TRANSFER" -> "TRANSFER"
            else -> budgetBucket.ifBlank { "NEEDS" }
        }
        val ym = DateCalendarUtils.yearMonthFromEpochDay(dateEpochDay)
        val updated = _transactionsFlow.value.map { tx ->
            if (tx.id == txId) {
                tx.copy(
                    title = title.trim().ifBlank { tx.title },
                    amountOriginal = amountOriginal,
                    currency = currency,
                    exchangeRateToBrl = safeRate,
                    amountBrl = amountBrl,
                    type = type,
                    budgetBucket = validBucket,
                    category = category.ifBlank { "Geral" },
                    dateEpochDay = dateEpochDay,
                    yearMonth = ym,
                    privacyScope = privacyScope,
                    accountId = accountId,
                    accountName = accountName,
                    destinationAccountId = destinationAccountId,
                    destinationAccountName = destinationAccountName,
                    isRecurring = isRecurring,
                    isCreditCard = (type == "EXPENSE" && isCreditCard),
                    isPaid = if (type == "EXPENSE" && isCreditCard) isPaid else true
                )
            } else {
                tx
            }
        }.sortedByDescending { it.dateEpochDay }
        saveTransactions(updated)
        _transactionsFlow.value = updated
    }

    fun setCreditTransactionPaid(
        txId: String,
        isPaid: Boolean,
        accountId: String,
        accountName: String
    ) {
        val updated = _transactionsFlow.value.map { tx ->
            if (tx.id == txId) {
                tx.copy(
                    isPaid = isPaid,
                    accountId = accountId.ifBlank { tx.accountId },
                    accountName = accountName.ifBlank { tx.accountName }
                )
            } else {
                tx
            }
        }
        saveTransactions(updated)
        _transactionsFlow.value = updated
    }

    fun payAllCreditForMonth(
        yearMonth: String,
        accountId: String,
        accountName: String
    ): Int {
        var count = 0
        val updated = _transactionsFlow.value.map { tx ->
            val matchesAccount = accountId.isBlank() || tx.accountId == accountId
            if (tx.yearMonth == yearMonth && tx.type == "EXPENSE" && tx.isCreditCard && !tx.isPaid && matchesAccount) {
                count++
                tx.copy(
                    isPaid = true,
                    accountId = accountId.ifBlank { tx.accountId },
                    accountName = accountName.ifBlank { tx.accountName }
                )
            } else {
                tx
            }
        }
        saveTransactions(updated)
        _transactionsFlow.value = updated
        return count
    }

    fun linkTransactionToTask(txId: String, taskId: String) {
        val updated = _transactionsFlow.value.map {
            if (it.id == txId) it.copy(linkedTaskId = taskId) else it
        }
        saveTransactions(updated)
        _transactionsFlow.value = updated
    }

    fun deleteTransaction(txId: String) {
        val updated = _transactionsFlow.value.filterNot { it.id == txId }
        saveTransactions(updated)
        _transactionsFlow.value = updated
    }

    // Subscriptions
    private fun loadSubscriptions(): List<SubscriptionBill> {
        val raw = prefs.getString(KEY_SUBS_JSON, null)
        if (raw == null) {
            val todayDayOfMonth = DateCalendarUtils.fromEpochDay(DateCalendarUtils.todayEpochDay()).third.toLong()
            val initial = listOf(
                SubscriptionBill(
                    id = "sub_demo_1",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Internet Fibra da Casa",
                    amountOriginal = 129.90,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    amountBrl = 129.90,
                    category = "Moradia",
                    budgetBucket = "NEEDS",
                    dueDayOfMonth = todayDayOfMonth,
                    reminderDaysBefore = 2,
                    autoPostEnabled = true,
                    lastPostedYearMonth = "",
                    active = true,
                    privacyScope = PrivacyScope.FAMILY,
                    responsibleMemberName = "Família",
                    responsibleMemberColorHex = 0xFF0E7C5B,
                    splitMode = SplitMode.EQUAL,
                    ringtoneCategory = "CONTAS"
                ),
                SubscriptionBill(
                    id = "sub_demo_2",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Plano Celular Pessoal",
                    amountOriginal = 59.90,
                    currency = "BRL",
                    exchangeRateToBrl = 1.0,
                    amountBrl = 59.90,
                    category = "Assinatura",
                    budgetBucket = "NEEDS",
                    dueDayOfMonth = todayDayOfMonth,
                    reminderDaysBefore = 1,
                    autoPostEnabled = true,
                    lastPostedYearMonth = "",
                    active = true,
                    privacyScope = PrivacyScope.PERSONAL,
                    responsibleMemberName = "Só Meu",
                    responsibleMemberColorHex = 0xFF7C4DFF,
                    responsibleMemberEmoji = "🔒",
                    splitMode = SplitMode.NONE,
                    ringtoneCategory = "CONTAS"
                )
            )
            saveSubscriptions(initial)
            return initial
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        SubscriptionBill(
                            id = obj.optString("id"),
                            userId = obj.optString("userId", LOCAL_USER_ID),
                            householdId = obj.optString("householdId", "FAM_OMNI_LOCAL"),
                            title = obj.optString("title"),
                            amountOriginal = obj.optDouble("amountOriginal", 0.0),
                            currency = obj.optString("currency", "BRL"),
                            exchangeRateToBrl = obj.optDouble("exchangeRateToBrl", 1.0),
                            amountBrl = obj.optDouble("amountBrl", 0.0),
                            category = obj.optString("category", "Assinatura"),
                            budgetBucket = obj.optString("budgetBucket", "NEEDS"),
                            dueDayOfMonth = obj.optLong("dueDayOfMonth", 10L),
                            reminderDaysBefore = obj.optLong("reminderDaysBefore", 2L),
                            autoPostEnabled = obj.optBoolean("autoPostEnabled", true),
                            lastPostedYearMonth = obj.optString("lastPostedYearMonth", ""),
                            active = obj.optBoolean("active", true),
                            privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                            visibleToMemberIds = deserializeStringList(obj.optJSONArray("visibleToMemberIds")),
                            responsibleMemberId = obj.optString("responsibleMemberId", ""),
                            responsibleMemberName = obj.optString("responsibleMemberName", "Família"),
                            responsibleMemberColorHex = obj.optLong("responsibleMemberColorHex", 0xFF0E7C5B),
                            responsibleMemberEmoji = obj.optString("responsibleMemberEmoji", "🌿"),
                            splitMode = obj.optString("splitMode", SplitMode.NONE),
                            splitWithMemberIds = deserializeStringList(obj.optJSONArray("splitWithMemberIds")),
                            splitSharesMap = deserializeDoubleMap(obj.optJSONObject("splitSharesMap")),
                            splitShares = deserializeSplitShares(obj.optJSONArray("splitShares")),
                            linkedTaskId = obj.optString("linkedTaskId", ""),
                            syncDueDayToCalendar = obj.optBoolean("syncDueDayToCalendar", true),
                            ringtoneCategory = obj.optString("ringtoneCategory", "CONTAS")
                        )
                    )
                }
            }.sortedBy { it.dueDayOfMonth }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveSubscriptions(list: List<SubscriptionBill>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("userId", s.userId)
                    put("householdId", s.householdId)
                    put("title", s.title)
                    put("amountOriginal", s.amountOriginal)
                    put("currency", s.currency)
                    put("exchangeRateToBrl", s.exchangeRateToBrl)
                    put("amountBrl", s.amountBrl)
                    put("category", s.category)
                    put("budgetBucket", s.budgetBucket)
                    put("dueDayOfMonth", s.dueDayOfMonth)
                    put("reminderDaysBefore", s.reminderDaysBefore)
                    put("autoPostEnabled", s.autoPostEnabled)
                    put("lastPostedYearMonth", s.lastPostedYearMonth)
                    put("active", s.active)
                    put("privacyScope", s.privacyScope)
                    put("visibleToMemberIds", serializeStringList(s.visibleToMemberIds))
                    put("responsibleMemberId", s.responsibleMemberId)
                    put("responsibleMemberName", s.responsibleMemberName)
                    put("responsibleMemberColorHex", s.responsibleMemberColorHex)
                    put("responsibleMemberEmoji", s.responsibleMemberEmoji)
                    put("splitMode", s.splitMode)
                    put("splitWithMemberIds", serializeStringList(s.splitWithMemberIds))
                    put("splitSharesMap", serializeDoubleMap(s.splitSharesMap))
                    put("splitShares", serializeSplitShares(s.splitShares))
                    put("linkedTaskId", s.linkedTaskId)
                    put("syncDueDayToCalendar", s.syncDueDayToCalendar)
                    put("ringtoneCategory", s.ringtoneCategory)
                }
            )
        }
        prefs.edit().putString(KEY_SUBS_JSON, arr.toString()).apply()
    }

    fun addSubscriptionBill(
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
        responsibleMemberId: String = LOCAL_USER_ID,
        responsibleMemberName: String = "Família",
        responsibleMemberColorHex: Long = 0xFF0E7C5B,
        responsibleMemberEmoji: String = "🌿",
        splitMode: String = SplitMode.NONE,
        splitWithMemberIds: List<String> = emptyList(),
        splitSharesMap: Map<String, Double> = emptyMap(),
        splitShares: List<ExpenseSplitShare> = emptyList(),
        linkedTaskId: String = "",
        syncDueDayToCalendar: Boolean = true,
        targetYearMonth: String = DateCalendarUtils.currentYearMonth(),
        ringtoneCategory: String = "CONTAS"
    ): String {
        val id = "sub_" + UUID.randomUUID().toString().take(8)
        val safeRate = if (currency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
        val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)

        var resolvedTaskId = linkedTaskId
        if (syncDueDayToCalendar && resolvedTaskId.isBlank()) {
            val parts = targetYearMonth.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
            val maxDay = DateCalendarUtils.daysInMonth(year, month)
            val dueEpochDay = DateCalendarUtils.toEpochDay(year, month, dueDayOfMonth.coerceIn(1, maxDay))
            val scopeLabel = if (privacyScope == PrivacyScope.PERSONAL) "Conta Individual" else "Custo Mensal Familiar"
            resolvedTaskId = addTask(
                householdId = householdId,
                title = "Pagar: ${title.trim()}",
                notes = "$scopeLabel • Vence todo dia $dueDayOfMonth (${formatCurrencyOriginal(amountOriginal, currency)})",
                category = "Finanças",
                priority = "HIGH",
                dateEpochDay = dueEpochDay,
                alarmHour = 9,
                alarmMinute = 0,
                alarmEnabled = true,
                privacyScope = privacyScope,
                visibleToMemberIds = visibleToMemberIds,
                assignedMemberId = responsibleMemberId,
                assignedMemberName = responsibleMemberName,
                assignedMemberColorHex = responsibleMemberColorHex,
                assignedMemberEmoji = if (privacyScope == PrivacyScope.PERSONAL) "🔒" else responsibleMemberEmoji,
                createdByName = responsibleMemberName,
                createdByColorHex = responsibleMemberColorHex,
                linkedSubscriptionId = id,
                estimatedCostBrl = amountBrl,
                autoCreateExpenseOnComplete = true,
                splitMode = splitMode,
                splitShares = splitShares,
                isEssential = true,
                iconEmoji = "💳",
                taskColorHex = responsibleMemberColorHex,
                recurrence = TaskRecurrence.MONTHLY,
                ringtoneCategory = ringtoneCategory
            )
        }

        val sub = SubscriptionBill(
            id = id,
            userId = LOCAL_USER_ID,
            householdId = householdId,
            title = title.trim(),
            amountOriginal = amountOriginal,
            currency = currency,
            exchangeRateToBrl = safeRate,
            amountBrl = amountBrl,
            category = category,
            budgetBucket = budgetBucket,
            dueDayOfMonth = dueDayOfMonth.toLong(),
            reminderDaysBefore = reminderDaysBefore.toLong(),
            autoPostEnabled = autoPostEnabled,
            lastPostedYearMonth = "",
            active = true,
            privacyScope = privacyScope,
            visibleToMemberIds = visibleToMemberIds,
            responsibleMemberId = responsibleMemberId,
            responsibleMemberName = responsibleMemberName,
            responsibleMemberColorHex = responsibleMemberColorHex,
            responsibleMemberEmoji = responsibleMemberEmoji,
            splitMode = splitMode,
            splitWithMemberIds = splitWithMemberIds,
            splitSharesMap = splitSharesMap,
            splitShares = splitShares,
            linkedTaskId = resolvedTaskId,
            syncDueDayToCalendar = syncDueDayToCalendar,
            ringtoneCategory = ringtoneCategory
        )
        val updated = (_subscriptionsFlow.value + sub).sortedBy { it.dueDayOfMonth }
        saveSubscriptions(updated)
        _subscriptionsFlow.value = updated
        return id
    }

    fun updateSubscriptionBill(
        subId: String,
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
    ) {
        val safeRate = if (currency == "BRL") 1.0 else exchangeRateToBrl.coerceIn(0.01, 10000.0)
        val amountBrl = (Math.round(amountOriginal * safeRate * 100.0) / 100.0).coerceAtLeast(0.01)
        val updated = _subscriptionsFlow.value.map { sub ->
            if (sub.id == subId) {
                sub.copy(
                    title = title.trim().ifBlank { sub.title },
                    amountOriginal = amountOriginal,
                    currency = currency,
                    exchangeRateToBrl = safeRate,
                    amountBrl = amountBrl,
                    category = category.ifBlank { "Assinatura" },
                    budgetBucket = budgetBucket.ifBlank { "NEEDS" },
                    dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31).toLong(),
                    reminderDaysBefore = reminderDaysBefore.coerceIn(0, 15).toLong(),
                    autoPostEnabled = autoPostEnabled,
                    privacyScope = privacyScope
                )
            } else {
                sub
            }
        }.sortedBy { it.dueDayOfMonth }
        saveSubscriptions(updated)
        _subscriptionsFlow.value = updated
    }

    fun updateSubscriptionRingtone(subId: String, ringtoneCategory: String) {
        val updated = _subscriptionsFlow.value.map {
            if (it.id == subId) it.copy(ringtoneCategory = ringtoneCategory) else it
        }
        saveSubscriptions(updated)
        _subscriptionsFlow.value = updated
    }

    fun markSubscriptionPosted(subId: String, yearMonth: String) {
        val updated = _subscriptionsFlow.value.map {
            if (it.id == subId) it.copy(lastPostedYearMonth = yearMonth) else it
        }
        saveSubscriptions(updated)
        _subscriptionsFlow.value = updated
    }

    fun deleteSubscription(subId: String) {
        val updated = _subscriptionsFlow.value.filterNot { it.id == subId }
        saveSubscriptions(updated)
        _subscriptionsFlow.value = updated
    }

    // Savings Goals
    private fun loadSavingsGoals(): List<SavingsGoal> {
        val raw = prefs.getString(KEY_GOALS_JSON, null)
        if (raw == null) {
            val initial = listOf(
                SavingsGoal(
                    id = "goal_demo_1",
                    userId = LOCAL_USER_ID,
                    householdId = "FAM_OMNI_LOCAL",
                    title = "Reserva de Emergência Familiar",
                    targetAmountBrl = 15000.0,
                    currentAmountBrl = 4500.0,
                    incomePercentage = 15,
                    category = "Reserva",
                    targetYearMonth = "2027-06",
                    privacyScope = PrivacyScope.FAMILY,
                    responsibleMemberName = "Família",
                    responsibleMemberColorHex = 0xFF0E7C5B,
                    splitMode = SplitMode.EQUAL
                )
            )
            saveSavingsGoals(initial)
            return initial
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        SavingsGoal(
                            id = obj.optString("id"),
                            userId = obj.optString("userId", LOCAL_USER_ID),
                            householdId = obj.optString("householdId", "FAM_OMNI_LOCAL"),
                            title = obj.optString("title"),
                            targetAmountBrl = obj.optDouble("targetAmountBrl", 1000.0),
                            currentAmountBrl = obj.optDouble("currentAmountBrl", 0.0),
                            incomePercentage = obj.optLong("incomePercentage", 10L),
                            category = obj.optString("category", "Reserva"),
                            targetYearMonth = obj.optString("targetYearMonth", "2027-12"),
                            privacyScope = obj.optString("privacyScope", PrivacyScope.FAMILY),
                            responsibleMemberId = obj.optString("responsibleMemberId", ""),
                            responsibleMemberName = obj.optString("responsibleMemberName", "Família"),
                            responsibleMemberColorHex = obj.optLong("responsibleMemberColorHex", 0xFF0E7C5B),
                            responsibleMemberEmoji = obj.optString("responsibleMemberEmoji", "🌿"),
                            splitMode = obj.optString("splitMode", SplitMode.EQUAL),
                            memberContributionsMap = deserializeDoubleMap(obj.optJSONObject("memberContributionsMap")),
                            linkedTaskId = obj.optString("linkedTaskId", "")
                        )
                    )
                }
            }.sortedByDescending { it.incomePercentage }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveSavingsGoals(list: List<SavingsGoal>) {
        val arr = JSONArray()
        list.forEach { g ->
            arr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("userId", g.userId)
                    put("householdId", g.householdId)
                    put("title", g.title)
                    put("targetAmountBrl", g.targetAmountBrl)
                    put("currentAmountBrl", g.currentAmountBrl)
                    put("incomePercentage", g.incomePercentage)
                    put("category", g.category)
                    put("targetYearMonth", g.targetYearMonth)
                    put("privacyScope", g.privacyScope)
                    put("responsibleMemberId", g.responsibleMemberId)
                    put("responsibleMemberName", g.responsibleMemberName)
                    put("responsibleMemberColorHex", g.responsibleMemberColorHex)
                    put("responsibleMemberEmoji", g.responsibleMemberEmoji)
                    put("splitMode", g.splitMode)
                    put("memberContributionsMap", serializeDoubleMap(g.memberContributionsMap))
                    put("linkedTaskId", g.linkedTaskId)
                }
            )
        }
        prefs.edit().putString(KEY_GOALS_JSON, arr.toString()).apply()
    }

    fun addSavingsGoal(
        householdId: String,
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String,
        privacyScope: String = PrivacyScope.FAMILY,
        responsibleMemberId: String = LOCAL_USER_ID,
        responsibleMemberName: String = "Família",
        responsibleMemberColorHex: Long = 0xFF0E7C5B,
        responsibleMemberEmoji: String = "🌿",
        splitMode: String = SplitMode.EQUAL,
        memberContributionsMap: Map<String, Double> = emptyMap(),
        linkedTaskId: String = ""
    ): String {
        val id = "goal_" + UUID.randomUUID().toString().take(8)
        val goal = SavingsGoal(
            id = id,
            userId = LOCAL_USER_ID,
            householdId = householdId,
            title = title.trim(),
            targetAmountBrl = targetAmountBrl,
            currentAmountBrl = currentAmountBrl,
            incomePercentage = incomePercentage.toLong(),
            category = category,
            targetYearMonth = targetYearMonth,
            privacyScope = privacyScope,
            responsibleMemberId = responsibleMemberId,
            responsibleMemberName = responsibleMemberName,
            responsibleMemberColorHex = responsibleMemberColorHex,
            responsibleMemberEmoji = responsibleMemberEmoji,
            splitMode = splitMode,
            memberContributionsMap = memberContributionsMap,
            linkedTaskId = linkedTaskId
        )
        val updated = (_savingsGoalsFlow.value + goal).sortedByDescending { it.incomePercentage }
        saveSavingsGoals(updated)
        _savingsGoalsFlow.value = updated
        return id
    }

    fun updateSavingsGoal(
        goalId: String,
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String
    ) {
        val updated = _savingsGoalsFlow.value.map { g ->
            if (g.id == goalId) {
                g.copy(
                    title = title.trim().ifBlank { g.title },
                    targetAmountBrl = targetAmountBrl.coerceAtLeast(1.0),
                    currentAmountBrl = currentAmountBrl.coerceAtLeast(0.0),
                    incomePercentage = incomePercentage.coerceIn(0, 100).toLong(),
                    category = category.ifBlank { "Reserva" },
                    targetYearMonth = targetYearMonth.ifBlank { g.targetYearMonth }
                )
            } else {
                g
            }
        }.sortedByDescending { it.incomePercentage }
        saveSavingsGoals(updated)
        _savingsGoalsFlow.value = updated
    }

    fun contributeToSavingsGoal(
        goalId: String,
        addedAmountBrl: Double,
        memberId: String = LOCAL_USER_ID
    ) {
        val updated = _savingsGoalsFlow.value.map {
            if (it.id == goalId) {
                val updatedMap = it.memberContributionsMap.toMutableMap()
                updatedMap[memberId] = (updatedMap[memberId] ?: 0.0) + addedAmountBrl
                it.copy(
                    currentAmountBrl = (it.currentAmountBrl + addedAmountBrl).coerceAtLeast(0.0),
                    memberContributionsMap = updatedMap
                )
            } else it
        }
        saveSavingsGoals(updated)
        _savingsGoalsFlow.value = updated
    }

    fun deleteSavingsGoal(goalId: String) {
        val updated = _savingsGoalsFlow.value.filterNot { it.id == goalId }
        saveSavingsGoals(updated)
        _savingsGoalsFlow.value = updated
    }

    companion object {
        const val LOCAL_USER_ID = "LOCAL_TEST_USER"
        private const val PREFS_NAME = "aura_local_offline_store"
        private const val KEY_HOUSEHOLD_ID = "household_id"
        private const val KEY_HOUSEHOLD_NAME = "household_name"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_MEMBER_NAME = "member_name"
        private const val KEY_MEMBER_COLOR_HEX = "member_color_hex"
        private const val KEY_MEMBER_EMOJI = "member_emoji"
        private const val KEY_MEMBER_INITIALS = "member_initials"
        private const val KEY_MEMBER_ROLE = "member_role"
        private const val KEY_MEMBER_SPLIT_PCT = "member_split_pct"
        private const val KEY_DEFAULT_SPLIT_MODE = "default_split_mode"
        private const val KEY_MONTHLY_INCOME = "monthly_income"
        private const val KEY_NEEDS_PCT = "needs_pct"
        private const val KEY_WANTS_PCT = "wants_pct"
        private const val KEY_SAVINGS_PCT = "savings_pct"
        private const val KEY_FAMILY_MEMBERS_JSON = "family_members_json"
        private const val KEY_BANK_ACCOUNTS_JSON = "bank_accounts_json"
        private const val KEY_TASKS_JSON = "tasks_json"
        private const val KEY_TX_JSON = "tx_json"
        private const val KEY_SUBS_JSON = "subs_json"
        private const val KEY_GOALS_JSON = "goals_json"
        private const val KEY_CATEGORY_BUDGETS_JSON = "category_budgets_json"
        private const val KEY_CUSTOM_CATEGORIES_JSON = "custom_categories_json"
    }
}
