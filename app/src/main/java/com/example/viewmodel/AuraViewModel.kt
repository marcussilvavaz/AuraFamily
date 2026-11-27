package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.AuraFirestoreRepository
import com.example.data.BankAccount
import com.example.data.CurrencyExchangeService
import com.example.data.CurrencyRatesState
import com.example.data.DateCalendarUtils
import com.example.data.FamilyMemberBadge
import com.example.data.FinanceTransaction
import com.example.data.Household
import com.example.data.PrivacyScope
import com.example.data.SavingsGoal
import com.example.data.SubscriptionBill
import com.example.data.SubtaskItem
import com.example.data.TaskItem
import com.example.data.TaskRecurrence
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.formatCurrencyBrl
import com.example.data.formatCurrencyOriginal
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AuraViewModel(
    private val repository: AuraFirestoreRepository,
    private val currentUserId: String,
    private val initialDisplayName: String?,
    private val currencyService: CurrencyExchangeService = CurrencyExchangeService()
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(DateCalendarUtils.currentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private val _selectedEpochDay = MutableStateFlow(DateCalendarUtils.todayEpochDay())
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay.asStateFlow()

    private val _currencyRates = MutableStateFlow(CurrencyRatesState())
    val currencyRates: StateFlow<CurrencyRatesState> = _currencyRates.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isProfileServerReady = MutableStateFlow(false)
    val isProfileServerReady: StateFlow<Boolean> = _isProfileServerReady.asStateFlow()

    private val _isRestoringFromCloud = MutableStateFlow(true)
    val isRestoringFromCloud: StateFlow<Boolean> = _isRestoringFromCloud.asStateFlow()

    // 1. User Profile StateFlow
    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing userProfile", rootError)
            emit(UiState.Error(rootError.message ?: "Erro ao carregar perfil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val activeHouseholdIdFlow: Flow<String> = combine(
        userProfileState,
        _isProfileServerReady
    ) { state, isReady ->
        if (!isReady) {
            null
        } else {
            (state as? UiState.Success)?.data?.householdId?.takeIf { it.isNotBlank() }
        }
    }
        .filterNotNull()
        .distinctUntilChanged()

    // 2. Household StateFlow
    val householdState: StateFlow<UiState<Household?>> = activeHouseholdIdFlow
        .flatMapLatest { householdId ->
            repository.observeHousehold(householdId)
                .map<Household?, UiState<Household?>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing household", rootError)
                    emit(UiState.Error(rootError.message ?: "Erro ao carregar orçamento familiar"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 3. Tasks StateFlow
    val tasksState: StateFlow<UiState<List<TaskItem>>> = activeHouseholdIdFlow
        .flatMapLatest { householdId ->
            repository.observeHouseholdTasks(householdId)
                .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing tasks", rootError)
                    emit(UiState.Error(rootError.message ?: "Erro ao carregar tarefas"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 4. Transactions StateFlow
    val transactionsState: StateFlow<UiState<List<FinanceTransaction>>> = activeHouseholdIdFlow
        .flatMapLatest { householdId ->
            repository.observeHouseholdTransactions(householdId)
                .map<List<FinanceTransaction>, UiState<List<FinanceTransaction>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing transactions", rootError)
                    emit(UiState.Error(rootError.message ?: "Erro ao carregar transações"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 5. Subscriptions & Fixed Bills StateFlow
    val subscriptionsState: StateFlow<UiState<List<SubscriptionBill>>> = activeHouseholdIdFlow
        .flatMapLatest { householdId ->
            repository.observeHouseholdSubscriptions(householdId)
                .map<List<SubscriptionBill>, UiState<List<SubscriptionBill>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing subscriptions", rootError)
                    emit(UiState.Error(rootError.message ?: "Erro ao carregar assinaturas"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 6. Savings Goals StateFlow
    val savingsGoalsState: StateFlow<UiState<List<SavingsGoal>>> = activeHouseholdIdFlow
        .flatMapLatest { householdId ->
            repository.observeHouseholdSavingsGoals(householdId)
                .map<List<SavingsGoal>, UiState<List<SavingsGoal>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing savings goals", rootError)
                    emit(UiState.Error(rootError.message ?: "Erro ao carregar metas"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            _isRestoringFromCloud.value = true
            repository.ensureUserProfileAndHousehold(initialDisplayName)
            _isProfileServerReady.value = true
            _isRestoringFromCloud.value = false
        }
        refreshExchangeRates()
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            val updated = currencyService.fetchRatesToBrl(_currencyRates.value)
            _currencyRates.value = updated
        }
    }

    fun updateManualRate(currency: String, rateToBrl: Double) {
        val safeRate = rateToBrl.coerceIn(0.01, 10000.0)
        val current = _currencyRates.value
        _currencyRates.value = when (currency) {
            "USD" -> current.copy(usdToBrl = safeRate, lastUpdatedLabel = "Cotação ajustada manualmente")
            "EUR" -> current.copy(eurToBrl = safeRate, lastUpdatedLabel = "Cotação ajustada manualmente")
            "GBP" -> current.copy(gbpToBrl = safeRate, lastUpdatedLabel = "Cotação ajustada manualmente")
            else -> current
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun selectYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
        val parts = yearMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
        val currentSelectedYm = DateCalendarUtils.yearMonthFromEpochDay(_selectedEpochDay.value)
        if (currentSelectedYm != yearMonth) {
            _selectedEpochDay.value = DateCalendarUtils.toEpochDay(y, m, 1)
        }
    }

    fun shiftMonth(delta: Int) {
        val nextYm = DateCalendarUtils.shiftYearMonth(_selectedYearMonth.value, delta)
        selectYearMonth(nextYm)
    }

    fun selectEpochDay(epochDay: Long) {
        _selectedEpochDay.value = epochDay
        val ym = DateCalendarUtils.yearMonthFromEpochDay(epochDay)
        if (ym != _selectedYearMonth.value) {
            _selectedYearMonth.value = ym
        }
    }

    private fun currentHouseholdId(): String {
        val profile = (userProfileState.value as? UiState.Success)?.data
        return profile?.householdId?.takeIf { it.isNotBlank() }
            ?: repository.defaultHouseholdIdForUser(currentUserId)
    }

    private fun currentHousehold(): Household? {
        return (householdState.value as? UiState.Success)?.data
    }

    private fun currentUserName(): String {
        val profile = (userProfileState.value as? UiState.Success)?.data
        return profile?.displayName?.takeIf { it.isNotBlank() }
            ?: initialDisplayName?.takeIf { it.isNotBlank() }
            ?: "Família"
    }

    private fun currentMemberColorHex(): Long {
        val profile = (userProfileState.value as? UiState.Success)?.data
        return profile?.memberColorHex?.takeIf { it != 0L } ?: 0xFF0B6E4F
    }

    fun updateUserProfileIdentity(
        displayName: String,
        memberColorHex: Long,
        avatarEmoji: String? = null,
        avatarBase64: String = ""
    ) {
        viewModelScope.launch {
            val cleanName = displayName.trim().let { raw ->
                if (raw.equals("Você", ignoreCase = true) ||
                    raw.equals("Voce", ignoreCase = true) ||
                    raw.equals("VO", ignoreCase = true)
                ) "" else raw
            }.ifBlank { currentUserName() }
            val currentEmoji = avatarEmoji?.takeIf { it.isNotBlank() }
                ?: (userProfileState.value as? UiState.Success)?.data?.avatarEmoji?.ifBlank { "🌿" }
                ?: "🌿"
            val result = repository.updateUserMemberIdentity(
                displayName = cleanName,
                memberColorHex = memberColorHex,
                avatarEmoji = currentEmoji,
                avatarBase64 = avatarBase64
            )
            result.onSuccess {
                _statusBannerMessage.value = "Perfil e cor na agenda atualizados na nuvem!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao atualizar perfil."
            }
        }
    }

    fun updateUserAvatarBase64(avatarBase64: String) {
        viewModelScope.launch {
            val result = repository.updateUserAvatarBase64(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                avatarBase64 = avatarBase64
            )
            result.onSuccess {
                _statusBannerMessage.value = if (avatarBase64.isNotBlank()) {
                    "Foto de perfil sincronizada na nuvem!"
                } else {
                    "Foto de perfil removida."
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao sincronizar foto na nuvem."
            }
        }
    }

    fun forceFullCloudSyncNow(
        context: Context,
        displayNameHint: String = "",
        avatarBase64Hint: String = ""
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.forceFullCloudSyncNow(
                displayNameHint = displayNameHint.ifBlank { currentUserName() },
                avatarBase64Hint = avatarBase64Hint
            )
            _isBusy.value = false
            result.onSuccess { msg ->
                syncAlarmsWithCurrentTasksAndSubscriptions(context)
                _statusBannerMessage.value = msg
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao sincronizar com o Firestore."
            }
        }
    }

    fun syncAlarmsWithCurrentTasksAndSubscriptions(context: Context) {
        val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val subs = (subscriptionsState.value as? UiState.Success)?.data.orEmpty()
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        for (task in tasks) {
            if (!task.isInbox && task.alarmEnabled && !task.completed &&
                (task.dateEpochDay >= todayEpoch || task.isRecurringOrMultiDay)
            ) {
                val targetDate = if (task.dateEpochDay >= todayEpoch) task.dateEpochDay else todayEpoch
                AlarmScheduler.scheduleTaskAlarm(
                    context = context,
                    taskId = task.id,
                    title = task.title,
                    category = task.category,
                    dateEpochDay = targetDate,
                    hour = task.alarmHour.toInt(),
                    minute = task.alarmMinute.toInt(),
                    ringtoneCategory = task.ringtoneCategory
                )
            } else {
                AlarmScheduler.cancelTaskAlarm(context, task.id)
            }
        }
        for (sub in subs) {
            if (sub.active) {
                AlarmScheduler.scheduleSubscriptionReminder(
                    context = context,
                    subscriptionId = sub.id,
                    title = sub.title,
                    amountFormatted = formatCurrencyOriginal(sub.amountOriginal, sub.currency),
                    dueDayOfMonth = sub.dueDayOfMonth.toInt(),
                    reminderDaysBefore = sub.reminderDaysBefore.toInt(),
                    ringtoneCategory = sub.ringtoneCategory
                )
            } else {
                AlarmScheduler.cancelSubscriptionReminder(context, sub.id)
            }
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode)
        }
    }

    fun setHouseholdInviteOpen(inviteOpen: Boolean) {
        viewModelScope.launch {
            val houseId = currentHouseholdId()
            val result = repository.updateHouseholdInviteOpen(houseId, inviteOpen)
            result.onSuccess {
                _statusBannerMessage.value = if (inviteOpen) {
                    "Convite aberto! Novos membros podem entrar usando o código $houseId."
                } else {
                    "Convite fechado. Apenas membros atuais podem acessar a família."
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.message ?: err.localizedMessage ?: "Não foi possível alterar o status do convite."
            }
        }
    }

    fun joinOrCreateHousehold(
        householdCode: String,
        familyName: String,
        isCreatingNewFamily: Boolean = false
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.switchOrJoinHousehold(
                newHouseholdCodeRaw = householdCode,
                familyNameRaw = familyName,
                isCreatingNewFamily = isCreatingNewFamily
            )
            _isBusy.value = false
            result.onSuccess { code ->
                _statusBannerMessage.value = "Família ativa: ${familyName.ifBlank { "Família Omni" }} • Código: $code"
            }.onFailure { err ->
                _statusBannerMessage.value = err.message
                    ?: err.localizedMessage
                    ?: "Convite fechado ou código inválido. Peça para um membro da família ativar 'Aceitar novos membros' no Perfil."
            }
        }
    }

    fun createNewFamily(familyName: String, customInviteCode: String = "") {
        val generatedCode = customInviteCode.trim().uppercase().takeIf { it.isNotBlank() }
            ?: ("FAM_" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase())
        val cleanName = familyName.trim().ifBlank { "Família Omni" }
        joinOrCreateHousehold(generatedCode, cleanName, isCreatingNewFamily = true)
    }

    fun saveFamilyMember(member: FamilyMemberBadge) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveFamilyMember(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                member = member
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Membro \"${member.displayName}\" salvo na família!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao salvar membro da família."
            }
        }
    }

    fun removeFamilyMember(memberUserId: String, memberName: String = "Membro") {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.removeFamilyMember(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                memberUserId = memberUserId
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "\"$memberName\" removido da família."
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao remover membro."
            }
        }
    }

    fun saveBankAccount(account: BankAccount) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveBankAccount(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                account = account
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Conta \"${account.name}\" salva com sucesso!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao salvar conta bancária."
            }
        }
    }

    fun deleteBankAccount(accountId: String, accountName: String = "Conta") {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteBankAccount(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                accountId = accountId
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Conta \"$accountName\" removida."
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao remover conta."
            }
        }
    }

    fun saveHouseholdBudgetPlan(
        name: String,
        monthlyIncomeBrl: Double,
        needsPct: Int,
        wantsPct: Int,
        savingsPct: Int
    ) {
        viewModelScope.launch {
            if (needsPct + wantsPct + savingsPct != 100) {
                _statusBannerMessage.value = "A soma das porcentagens deve ser exatamente 100%."
                return@launch
            }
            _isBusy.value = true
            val result = repository.updateHouseholdBudgetPlan(
                householdId = currentHouseholdId(),
                name = name,
                monthlyIncomeBrl = monthlyIncomeBrl,
                needsPct = needsPct,
                wantsPct = wantsPct,
                savingsPct = savingsPct
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Plano de economia familiar atualizado!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao atualizar orçamento."
            }
        }
    }

    fun createTask(
        context: Context,
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
        estimatedCostBrl: Double = 0.0,
        autoCreateExpenseOnComplete: Boolean = false,
        overrideMemberColorHex: Long? = null,
        isEssential: Boolean = (priority == "HIGH"),
        isInbox: Boolean = false,
        durationMinutes: Int = 30,
        iconEmoji: String = "📋",
        taskColorHex: Long? = null,
        recurrence: String = TaskRecurrence.ONCE,
        recurrenceWeekdays: List<Int> = emptyList(),
        subtasks: List<SubtaskItem> = emptyList(),
        ringtoneCategory: String = "APP_DEFAULT"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val scopeMemberName = if (privacyScope == PrivacyScope.PERSONAL) "Só Meu (${currentUserName()})" else currentUserName()
            val scopeEmoji = if (privacyScope == PrivacyScope.PERSONAL) "🔒" else "🌿"
            val memberColor = overrideMemberColorHex ?: currentMemberColorHex()
            val resolvedTaskColor = taskColorHex ?: memberColor
            val effectiveAlarmEnabled = if (isInbox) false else alarmEnabled
            val safeEndEpoch = if (endDateEpochDay >= dateEpochDay) endDateEpochDay else 0L
            val result = repository.addTask(
                householdId = currentHouseholdId(),
                title = title,
                notes = notes,
                location = location,
                category = category,
                priority = priority,
                dateEpochDay = dateEpochDay,
                endDateEpochDay = safeEndEpoch,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute,
                alarmEnabled = effectiveAlarmEnabled,
                privacyScope = privacyScope,
                assignedMemberName = scopeMemberName,
                assignedMemberColorHex = memberColor,
                assignedMemberEmoji = scopeEmoji,
                createdByName = scopeMemberName,
                createdByColorHex = memberColor,
                estimatedCostBrl = estimatedCostBrl,
                autoCreateExpenseOnComplete = autoCreateExpenseOnComplete,
                isEssential = isEssential,
                isInbox = isInbox,
                durationMinutes = durationMinutes,
                iconEmoji = iconEmoji,
                taskColorHex = resolvedTaskColor,
                recurrence = recurrence,
                recurrenceWeekdays = recurrenceWeekdays,
                subtasks = subtasks,
                ringtoneCategory = ringtoneCategory
            )
            _isBusy.value = false
            result.onSuccess { taskId ->
                if (effectiveAlarmEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = taskId,
                        title = title,
                        category = category,
                        dateEpochDay = dateEpochDay,
                        hour = alarmHour,
                        minute = alarmMinute,
                        ringtoneCategory = ringtoneCategory
                    )
                }
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
                val scopeBadge = if (privacyScope == PrivacyScope.PERSONAL) "Individual 🔒" else "Família 👨‍👩‍👧‍👦"
                _statusBannerMessage.value = when {
                    isInbox -> "Tarefa ($scopeBadge) salva na Inbox! Toque em '+ Agendar' quando quiser encaixá-la na Linha do Tempo."
                    estimatedCostBrl > 0.0 && autoCreateExpenseOnComplete ->
                        "Tarefa ($scopeBadge) agendada (${DateCalendarUtils.formatTime(alarmHour, alarmMinute)} • ${durationMinutes}m)! Ao concluir, ${formatCurrencyBrl(estimatedCostBrl)} será lançado nas Finanças."
                    effectiveAlarmEnabled ->
                        "Tarefa ($scopeBadge) agendada na Linha do Tempo para ${DateCalendarUtils.formatTime(alarmHour, alarmMinute)} (${durationMinutes}m)!"
                    else ->
                        "Tarefa ($scopeBadge) adicionada à Linha do Tempo!"
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao salvar tarefa."
            }
        }
    }

    fun updateTask(
        context: Context,
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
        viewModelScope.launch {
            _isBusy.value = true
            val effectiveAlarmEnabled = if (isInbox) false else alarmEnabled
            val safeEndEpoch = if (endDateEpochDay >= dateEpochDay) endDateEpochDay else 0L
            val result = repository.updateTask(
                householdId = currentHouseholdId(),
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
                alarmEnabled = effectiveAlarmEnabled,
                privacyScope = privacyScope,
                estimatedCostBrl = estimatedCostBrl,
                autoCreateExpenseOnComplete = autoCreateExpenseOnComplete,
                isEssential = isEssential,
                isInbox = isInbox,
                durationMinutes = durationMinutes,
                iconEmoji = iconEmoji,
                taskColorHex = taskColorHex,
                recurrence = recurrence,
                recurrenceWeekdays = recurrenceWeekdays,
                subtasks = subtasks,
                ringtoneCategory = ringtoneCategory
            )
            _isBusy.value = false
            result.onSuccess {
                if (effectiveAlarmEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = taskId,
                        title = title,
                        category = category,
                        dateEpochDay = dateEpochDay,
                        hour = alarmHour,
                        minute = alarmMinute,
                        ringtoneCategory = ringtoneCategory
                    )
                } else {
                    AlarmScheduler.cancelTaskAlarm(context, taskId)
                }
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
                _statusBannerMessage.value = "Tarefa \"$title\" atualizada com sucesso!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao atualizar tarefa."
            }
        }
    }

    fun toggleSubtaskDone(task: TaskItem, subtaskId: String) {
        viewModelScope.launch {
            val houseId = task.householdId.ifBlank { currentHouseholdId() }
            repository.toggleSubtaskDone(houseId, task, subtaskId)
        }
    }

    fun scheduleInboxTask(
        context: Context,
        task: TaskItem,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        durationMinutes: Int = task.durationMinutes
    ) {
        viewModelScope.launch {
            val houseId = task.householdId.ifBlank { currentHouseholdId() }
            repository.scheduleInboxTask(
                householdId = houseId,
                taskId = task.id,
                dateEpochDay = dateEpochDay,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute,
                durationMinutes = durationMinutes,
                alarmEnabled = true
            ).onSuccess {
                AlarmScheduler.scheduleTaskAlarm(
                    context = context,
                    taskId = task.id,
                    title = task.title,
                    category = task.category,
                    dateEpochDay = dateEpochDay,
                    hour = alarmHour,
                    minute = alarmMinute,
                    ringtoneCategory = task.ringtoneCategory
                )
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
                _statusBannerMessage.value = "\"${task.title}\" movida da Inbox para a Linha do Tempo às ${DateCalendarUtils.formatTime(alarmHour, alarmMinute)}!"
            }
        }
    }

    fun updateSubscriptionRingtone(subscription: SubscriptionBill, ringtoneCategory: String) {
        viewModelScope.launch {
            val houseId = subscription.householdId.ifBlank { currentHouseholdId() }
            repository.updateSubscriptionRingtone(houseId, subscription.id, ringtoneCategory).onSuccess {
                _statusBannerMessage.value = "Som de alerta da conta \"${subscription.title}\" atualizado!"
            }
        }
    }

    fun toggleTaskEssential(task: TaskItem) {
        viewModelScope.launch {
            val houseId = task.householdId.ifBlank { currentHouseholdId() }
            val newStatus = !task.isEssential
            repository.toggleTaskEssential(houseId, task.id, newStatus).onSuccess {
                _statusBannerMessage.value = if (newStatus) {
                    "\"${task.title}\" marcada como Essencial / Inegociável ⭐ (visível no Modo Dia Difícil)."
                } else {
                    "\"${task.title}\" marcada como tarefa flexível."
                }
            }
        }
    }

    fun toggleTaskCompletion(
        context: Context,
        task: TaskItem,
        targetEpochDay: Long = _selectedEpochDay.value
    ) {
        viewModelScope.launch {
            val isCurrentlyDoneOnTargetDate = task.isCompletedOnDate(targetEpochDay)
            val newStatus = !isCurrentlyDoneOnTargetDate
            val targetDateIso = DateCalendarUtils.formatIsoDate(targetEpochDay)
            val houseId = task.householdId.ifBlank { currentHouseholdId() }
            val result = repository.toggleTaskCompleted(
                householdId = houseId,
                taskId = task.id,
                completed = newStatus,
                targetDateIso = targetDateIso,
                completedByUserId = currentUserId,
                completedByName = currentUserName(),
                taskForAutoExpense = task
            )
            result.onSuccess {
                if (newStatus) {
                    if (!task.isRecurringOrMultiDay) {
                        AlarmScheduler.cancelTaskAlarm(context, task.id)
                    }
                    if (task.autoCreateExpenseOnComplete && task.estimatedCostBrl > 0.0 && task.linkedTransactionId.isBlank()) {
                        _statusBannerMessage.value = "Check ✓ em \"${task.title}\"! Pagamento de ${formatCurrencyBrl(task.estimatedCostBrl)} lançado nas Finanças!"
                    } else {
                        val dateSuffix = if (task.isRecurringOrMultiDay) " (${DateCalendarUtils.formatEpochDayPt(targetEpochDay)})" else ""
                        _statusBannerMessage.value = "Tarefa \"${task.title}\" concluída$dateSuffix!"
                    }
                } else if (task.alarmEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = task.id,
                        title = task.title,
                        category = task.category,
                        dateEpochDay = targetEpochDay,
                        hour = task.alarmHour.toInt(),
                        minute = task.alarmMinute.toInt(),
                        ringtoneCategory = task.ringtoneCategory
                    )
                }
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Não foi possível atualizar a tarefa."
            }
        }
    }

    fun deleteTask(context: Context, task: TaskItem) {
        viewModelScope.launch {
            AlarmScheduler.cancelTaskAlarm(context, task.id)
            val houseId = task.householdId.ifBlank { currentHouseholdId() }
            repository.deleteTask(houseId, task.id).onSuccess {
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
                _statusBannerMessage.value = "Tarefa removida."
            }
        }
    }

    fun createTransaction(
        title: String,
        amountOriginal: Double,
        currency: String,
        type: String,
        budgetBucket: String,
        category: String,
        dateEpochDay: Long,
        privacyScope: String = PrivacyScope.FAMILY,
        generateCalendarTask: Boolean = false,
        overrideMemberColorHex: Long? = null,
        accountId: String = "acc_nubank",
        accountName: String = "Nubank",
        destinationAccountId: String = "",
        destinationAccountName: String = "",
        isRecurring: Boolean = false,
        isCreditCard: Boolean = false,
        installmentCount: Int = 1,
        isPaid: Boolean = !isCreditCard
    ) {
        viewModelScope.launch {
            val rate = _currencyRates.value.rateFor(currency)
            val scopeMemberName = if (privacyScope == PrivacyScope.PERSONAL) "Só Meu (${currentUserName()})" else currentUserName()
            val scopeEmoji = if (privacyScope == PrivacyScope.PERSONAL) "🔒" else "🌿"
            val memberColor = overrideMemberColorHex ?: currentMemberColorHex()
            val matchedAcc = currentHousehold()?.bankAccounts?.firstOrNull { it.id == accountId }
            val closingDay = (matchedAcc?.closingDay ?: 25).coerceIn(1, 31)
            val (_, _, purchaseDay) = DateCalendarUtils.fromEpochDay(dateEpochDay)
            val shiftedToNextInvoice = (type == "EXPENSE" && isCreditCard && purchaseDay > closingDay)
            _isBusy.value = true
            val result = repository.addTransaction(
                householdId = currentHouseholdId(),
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = rate,
                type = type,
                budgetBucket = budgetBucket,
                category = category,
                dateEpochDay = dateEpochDay,
                createdByName = scopeMemberName,
                createdByColorHex = memberColor,
                createdByEmoji = scopeEmoji,
                privacyScope = privacyScope,
                responsibleMemberName = scopeMemberName,
                responsibleMemberColorHex = memberColor,
                responsibleMemberEmoji = scopeEmoji,
                generateCalendarTask = generateCalendarTask,
                accountId = accountId,
                accountName = accountName,
                destinationAccountId = destinationAccountId,
                destinationAccountName = destinationAccountName,
                isRecurring = isRecurring,
                isCreditCard = isCreditCard,
                installmentCount = installmentCount,
                isPaid = isPaid,
                accountClosingDay = closingDay
            )
            _isBusy.value = false
            result.onSuccess {
                val scopeBadge = if (privacyScope == PrivacyScope.PERSONAL) "Individual 🔒" else "Família 👨‍👩‍👧‍👦"
                _statusBannerMessage.value = when {
                    type == "TRANSFER" ->
                        "Transferência de $accountName para $destinationAccountName registrada!"
                    type == "INCOME" ->
                        "Receita ($scopeBadge) creditada na conta $accountName!"
                    isCreditCard && shiftedToNextInvoice ->
                        "Compra após o fechamento (dia $closingDay): lançada na próxima fatura de $accountName (${installmentCount}x)!"
                    isCreditCard ->
                        "Compra no Crédito (${installmentCount}x) lançada na fatura de $accountName e abatida do limite disponível!"
                    generateCalendarTask ->
                        "Despesa ($scopeBadge) debitada de $accountName e criada na Agenda!"
                    else ->
                        "Despesa ($scopeBadge) registrada na conta $accountName!"
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao registrar lançamento."
            }
        }
    }

    fun updateTransaction(
        transactionId: String,
        title: String,
        amountOriginal: Double,
        currency: String,
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
        viewModelScope.launch {
            val rate = _currencyRates.value.rateFor(currency)
            _isBusy.value = true
            val result = repository.updateTransaction(
                householdId = currentHouseholdId(),
                transactionId = transactionId,
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = rate,
                type = type,
                budgetBucket = budgetBucket,
                category = category,
                dateEpochDay = dateEpochDay,
                privacyScope = privacyScope,
                accountId = accountId,
                accountName = accountName,
                destinationAccountId = destinationAccountId,
                destinationAccountName = destinationAccountName,
                isRecurring = isRecurring,
                isCreditCard = isCreditCard,
                isPaid = isPaid
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Lançamento \"$title\" atualizado e saldos recalculados!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao atualizar lançamento."
            }
        }
    }

    fun setCreditTransactionPaid(
        transaction: FinanceTransaction,
        isPaid: Boolean,
        overrideAccountId: String = transaction.accountId,
        overrideAccountName: String = transaction.accountName
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.setCreditTransactionPaid(
                householdId = currentHouseholdId(),
                transactionId = transaction.id,
                isPaid = isPaid,
                accountId = overrideAccountId,
                accountName = overrideAccountName
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = if (isPaid) {
                    "Parcela \"${transaction.title}\" marcada como Paga (descontada de $overrideAccountName)!"
                } else {
                    "Parcela \"${transaction.title}\" marcada como Pendente na Fatura."
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao atualizar status da parcela."
            }
        }
    }

    fun payAllCreditForMonth(
        yearMonth: String,
        accountId: String,
        accountName: String
    ) {
        viewModelScope.launch {
            val allTx = (transactionsState.value as? UiState.Success)?.data.orEmpty()
            val pending = allTx.filter {
                it.yearMonth == yearMonth &&
                    it.type == "EXPENSE" &&
                    it.isCreditCard &&
                    !it.isPaid &&
                    (accountId.isBlank() || it.accountId == accountId)
            }
            if (pending.isEmpty()) {
                _statusBannerMessage.value = "Nenhuma parcela de crédito pendente para pagar neste mês."
                return@launch
            }
            _isBusy.value = true
            var paidCount = 0
            for (tx in pending) {
                val res = repository.setCreditTransactionPaid(
                    householdId = currentHouseholdId(),
                    transactionId = tx.id,
                    isPaid = true,
                    accountId = accountId.ifBlank { tx.accountId },
                    accountName = accountName.ifBlank { tx.accountName }
                )
                if (res.isSuccess) paidCount++
            }
            _isBusy.value = false
            _statusBannerMessage.value = "$paidCount parcela(s) da fatura pagas e descontadas da conta $accountName!"
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(currentHouseholdId(), transactionId).onSuccess {
                _statusBannerMessage.value = "Lançamento removido e saldo recalculado."
            }
        }
    }

    fun createSubscriptionBill(
        context: Context,
        title: String,
        amountOriginal: Double,
        currency: String,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String = PrivacyScope.FAMILY,
        syncDueDayToCalendar: Boolean = true,
        overrideMemberColorHex: Long? = null
    ) {
        viewModelScope.launch {
            val rate = _currencyRates.value.rateFor(currency)
            val scopeMemberName = if (privacyScope == PrivacyScope.PERSONAL) "Só Meu (${currentUserName()})" else currentUserName()
            val scopeEmoji = if (privacyScope == PrivacyScope.PERSONAL) "🔒" else "🌿"
            val memberColor = overrideMemberColorHex ?: currentMemberColorHex()
            _isBusy.value = true
            val result = repository.addSubscriptionBill(
                householdId = currentHouseholdId(),
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = rate,
                category = category,
                budgetBucket = bucketOrDefault(budgetBucket),
                dueDayOfMonth = dueDayOfMonth,
                reminderDaysBefore = reminderDaysBefore,
                autoPostEnabled = autoPostEnabled,
                privacyScope = privacyScope,
                responsibleMemberName = scopeMemberName,
                responsibleMemberColorHex = memberColor,
                responsibleMemberEmoji = scopeEmoji,
                syncDueDayToCalendar = syncDueDayToCalendar,
                targetYearMonth = _selectedYearMonth.value
            )
            _isBusy.value = false
            result.onSuccess { subId ->
                AlarmScheduler.scheduleSubscriptionReminder(
                    context = context,
                    subscriptionId = subId,
                    title = title,
                    amountFormatted = formatCurrencyOriginal(amountOriginal, currency),
                    dueDayOfMonth = dueDayOfMonth,
                    reminderDaysBefore = reminderDaysBefore
                )
                val scopeBadge = if (privacyScope == PrivacyScope.PERSONAL) "Individual 🔒" else "Família 👨‍👩‍👧‍👦"
                _statusBannerMessage.value = if (syncDueDayToCalendar) {
                    "Custo mensal ($scopeBadge) cadastrado e tarefa criada na Agenda com sua cor para o dia $dueDayOfMonth!"
                } else {
                    "Assinatura/Conta fixa ($scopeBadge) cadastrada com lembrete recorrente!"
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao salvar conta recorrente."
            }
        }
    }

    fun updateSubscriptionBill(
        context: Context,
        subscriptionId: String,
        title: String,
        amountOriginal: Double,
        currency: String,
        category: String,
        budgetBucket: String,
        dueDayOfMonth: Int,
        reminderDaysBefore: Int,
        autoPostEnabled: Boolean,
        privacyScope: String
    ) {
        viewModelScope.launch {
            val rate = _currencyRates.value.rateFor(currency)
            _isBusy.value = true
            val result = repository.updateSubscriptionBill(
                householdId = currentHouseholdId(),
                subscriptionId = subscriptionId,
                title = title,
                amountOriginal = amountOriginal,
                currency = currency,
                exchangeRateToBrl = rate,
                category = category,
                budgetBucket = bucketOrDefault(budgetBucket),
                dueDayOfMonth = dueDayOfMonth,
                reminderDaysBefore = reminderDaysBefore,
                autoPostEnabled = autoPostEnabled,
                privacyScope = privacyScope
            )
            _isBusy.value = false
            result.onSuccess {
                AlarmScheduler.scheduleSubscriptionReminder(
                    context = context,
                    subscriptionId = subscriptionId,
                    title = title,
                    amountFormatted = formatCurrencyOriginal(amountOriginal, currency),
                    dueDayOfMonth = dueDayOfMonth,
                    reminderDaysBefore = reminderDaysBefore
                )
                _statusBannerMessage.value = "Conta fixa \"$title\" atualizada!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao atualizar assinatura."
            }
        }
    }

    private fun bucketOrDefault(bucket: String): String = bucket.ifBlank { "NEEDS" }

    fun postSingleSubscriptionNow(subscription: SubscriptionBill) {
        viewModelScope.launch {
            _isBusy.value = true
            val ym = _selectedYearMonth.value
            val result = repository.postSubscriptionForMonth(
                subscription = subscription,
                yearMonth = ym,
                createdByName = currentUserName()
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "\"${subscription.title}\" lançada nas despesas de ${DateCalendarUtils.formatYearMonthDisplay(ym)}!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao lançar assinatura."
            }
        }
    }

    fun autoPostDueSubscriptionsForSelectedMonth() {
        viewModelScope.launch {
            val subs = (subscriptionsState.value as? UiState.Success)?.data.orEmpty()
            val ym = _selectedYearMonth.value
            val pending = subs.filter { it.active && it.autoPostEnabled && it.lastPostedYearMonth != ym }
            if (pending.isEmpty()) {
                _statusBannerMessage.value = "Todas as assinaturas automáticas já foram lançadas em ${DateCalendarUtils.formatYearMonthDisplay(ym)}."
                return@launch
            }
            _isBusy.value = true
            var count = 0
            for (sub in pending) {
                val res = repository.postSubscriptionForMonth(sub, ym, currentUserName())
                if (res.isSuccess) count++
            }
            _isBusy.value = false
            _statusBannerMessage.value = "$count conta(s) fixa(s) lançada(s) automaticamente em ${DateCalendarUtils.formatYearMonthDisplay(ym)}!"
        }
    }

    fun deleteSubscription(subscriptionId: String) {
        viewModelScope.launch {
            repository.deleteSubscriptionBill(currentHouseholdId(), subscriptionId).onSuccess {
                _statusBannerMessage.value = "Assinatura removida."
            }
        }
    }

    fun createSavingsGoal(
        title: String,
        targetAmountBrl: Double,
        currentAmountBrl: Double,
        incomePercentage: Int,
        category: String,
        targetYearMonth: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addSavingsGoal(
                householdId = currentHouseholdId(),
                title = title,
                targetAmountBrl = targetAmountBrl,
                currentAmountBrl = currentAmountBrl,
                incomePercentage = incomePercentage,
                category = category,
                targetYearMonth = targetYearMonth
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Meta \"$title\" criada!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao criar meta."
            }
        }
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
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateSavingsGoal(
                householdId = currentHouseholdId(),
                goalId = goalId,
                title = title,
                targetAmountBrl = targetAmountBrl,
                currentAmountBrl = currentAmountBrl,
                incomePercentage = incomePercentage,
                category = category,
                targetYearMonth = targetYearMonth
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Meta \"$title\" atualizada!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao atualizar meta."
            }
        }
    }

    fun contributeToGoal(goal: SavingsGoal, amountBrl: Double, recordInMonthlyExpenses: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.contributeToSavingsGoal(
                goal = goal,
                addedAmountBrl = amountBrl,
                recordAsExpenseInMonth = recordInMonthlyExpenses,
                createdByName = currentUserName()
            )
            _isBusy.value = false
            result.onSuccess {
                _statusBannerMessage.value = "Aporte registrado na meta \"${goal.title}\"!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao registrar aporte."
            }
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(currentHouseholdId(), goalId).onSuccess {
                _statusBannerMessage.value = "Meta removida."
            }
        }
    }

    fun updateCategoryBudget(category: String, limitBrl: Double) {
        viewModelScope.launch {
            val cleanCat = category.trim()
            if (cleanCat.isEmpty()) return@launch
            val result = repository.updateCategoryBudget(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                category = cleanCat,
                limitBrl = limitBrl
            )
            result.onSuccess {
                _statusBannerMessage.value = if (limitBrl > 0.0) {
                    "Teto mensal de \"$cleanCat\" definido em ${formatCurrencyBrl(limitBrl)}!"
                } else {
                    "Teto de gastos de \"$cleanCat\" removido."
                }
            }
        }
    }

    fun addCustomCategory(newCategory: String) {
        viewModelScope.launch {
            val clean = newCategory.trim().take(36)
            if (clean.isEmpty()) return@launch
            val currentList = currentHousehold()?.customCategories.orEmpty()
            val updated = (currentList + clean).distinct()
            repository.saveCustomCategories(currentHouseholdId(), updated).onSuccess {
                _statusBannerMessage.value = "Categoria \"$clean\" sincronizada com toda a família!"
            }
        }
    }

    fun addMuralItem(
        context: Context,
        title: String,
        category: String = "Compras",
        notes: String = "",
        pinned: Boolean = false,
        isNote: Boolean = false,
        addedBy: String = "",
        addedByColorHex: Long = 0L
    ) {
        viewModelScope.launch {
            val cleanTitle = title.trim()
            if (cleanTitle.isEmpty()) return@launch
            val authorName = addedBy.ifBlank { currentUserName() }
            val authorColor = if (addedByColorHex != 0L) addedByColorHex else currentMemberColorHex()
            com.example.data.OmniBackupAndMigrationManager.addFamilyMuralItem(
                context = context,
                title = cleanTitle,
                category = category,
                addedBy = authorName,
                addedByColorHex = authorColor,
                notes = notes,
                pinned = pinned,
                isNote = isNote
            )
            val allItems = com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context)
            repository.syncHouseholdMuralItems(currentHouseholdId(), allItems)
            _statusBannerMessage.value = if (isNote) {
                "Nota fixada no Mural da Família!"
            } else {
                "\"$cleanTitle\" adicionado ao Mural da Família ($category)!"
            }
        }
    }

    fun toggleMuralItem(
        context: Context,
        itemId: String,
        checkedBy: String = "",
        checkedByColorHex: Long = 0L
    ) {
        viewModelScope.launch {
            val updated = com.example.data.OmniBackupAndMigrationManager.toggleFamilyMuralItem(
                context = context,
                itemId = itemId,
                checkedByName = checkedBy.ifBlank { currentUserName() },
                checkedByColorHex = if (checkedByColorHex != 0L) checkedByColorHex else currentMemberColorHex()
            )
            repository.syncHouseholdMuralItems(currentHouseholdId(), updated)
        }
    }

    fun togglePinMuralItem(context: Context, itemId: String) {
        viewModelScope.launch {
            val updated = com.example.data.OmniBackupAndMigrationManager.togglePinMuralItem(context, itemId)
            repository.syncHouseholdMuralItems(currentHouseholdId(), updated)
        }
    }

    fun clearCompletedMuralItems(context: Context, categoryFilter: String? = null) {
        viewModelScope.launch {
            val updated = com.example.data.OmniBackupAndMigrationManager.clearCompletedMuralItems(context, categoryFilter)
            repository.syncHouseholdMuralItems(currentHouseholdId(), updated)
            _statusBannerMessage.value = "Itens marcados limpos da lista!"
        }
    }

    fun deleteMuralItem(context: Context, itemId: String) {
        viewModelScope.launch {
            val updated = com.example.data.OmniBackupAndMigrationManager.deleteFamilyMuralItem(context, itemId)
            repository.syncHouseholdMuralItems(currentHouseholdId(), updated)
        }
    }

    fun showStatusMessage(message: String) {
        if (message.isNotBlank()) {
            _statusBannerMessage.value = message
        }
    }

    fun updateHouseholdPregnancyConfig(
        dppDate: String,
        comparisonMode: String,
        babyName: String? = null,
        babyGender: String? = null,
        showConfirmationBanner: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.updateHouseholdPregnancyConfig(
                householdId = currentHouseholdId(),
                dppDate = dppDate,
                comparisonMode = comparisonMode,
                babyName = babyName,
                babyGender = babyGender
            )
            if (showConfirmationBanner) {
                result.onSuccess {
                    _statusBannerMessage.value = "DPP e dados da gestação sincronizados com a família!"
                }
            }
        }
    }

    fun updateHouseholdBabyIdentity(
        babyName: String,
        babyGender: String,
        showConfirmationBanner: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.updateHouseholdBabyIdentity(
                householdId = currentHouseholdId(),
                babyName = babyName,
                babyGender = babyGender
            )
            if (showConfirmationBanner) {
                result.onSuccess {
                    _statusBannerMessage.value = "Nome e sexo do bebê sincronizados com toda a família!"
                }
            }
        }
    }

    fun updateHouseholdGeminiApiKey(
        geminiApiKey: String,
        showConfirmationBanner: Boolean = true
    ) {
        viewModelScope.launch {
            val result = repository.updateHouseholdGeminiApiKey(
                householdId = currentHouseholdId(),
                geminiApiKey = geminiApiKey
            )
            if (showConfirmationBanner) {
                result.onSuccess {
                    _statusBannerMessage.value = if (geminiApiKey.isNotBlank()) {
                        "Chave API Gemini salva e sincronizada com a família!"
                    } else {
                        "Chave API Gemini removida."
                    }
                }
            }
        }
    }

    fun syncMemberHealthSettingsToCloud(settings: Map<String, Boolean>) {
        viewModelScope.launch {
            repository.updateMemberHealthSettings(
                householdId = currentHouseholdId(),
                currentHousehold = currentHousehold(),
                memberUid = currentUserId,
                settings = settings
            )
        }
    }

    fun scheduleOrUpdateMedicalOrderTask(
        context: Context,
        docTitle: String,
        memberName: String,
        memberId: String,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        location: String,
        notes: String,
        existingLinkedTaskId: String = "",
        onTaskLinked: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val houseId = currentHouseholdId()
            val house = currentHousehold()
            val matchedMember = house?.members?.firstOrNull {
                (memberId.isNotBlank() && it.userId == memberId) ||
                    it.displayName.equals(memberName, ignoreCase = true)
            }
            val memberColor = matchedMember?.colorHex?.takeIf { it != 0L } ?: currentMemberColorHex()
            val memberEmoji = matchedMember?.avatarEmoji?.ifBlank { "🩺" } ?: "🩺"
            val taskTitle = "🩺 Exame: ${docTitle.trim()} (${memberName.ifBlank { currentUserName() }})"
            val allTasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
            val existingTask = if (existingLinkedTaskId.isNotBlank()) {
                allTasks.firstOrNull { it.id == existingLinkedTaskId }
            } else null

            if (existingTask != null) {
                val updateRes = repository.updateTask(
                    householdId = houseId,
                    taskId = existingTask.id,
                    title = taskTitle,
                    notes = notes,
                    location = location,
                    category = "Saúde",
                    priority = "HIGH",
                    dateEpochDay = dateEpochDay,
                    endDateEpochDay = 0L,
                    alarmHour = alarmHour,
                    alarmMinute = alarmMinute,
                    alarmEnabled = true,
                    privacyScope = PrivacyScope.FAMILY,
                    estimatedCostBrl = existingTask.estimatedCostBrl,
                    autoCreateExpenseOnComplete = existingTask.autoCreateExpenseOnComplete,
                    isEssential = true,
                    isInbox = false,
                    durationMinutes = 45,
                    iconEmoji = "🩺",
                    taskColorHex = memberColor,
                    recurrence = TaskRecurrence.ONCE,
                    recurrenceWeekdays = emptyList(),
                    subtasks = existingTask.subtasks,
                    ringtoneCategory = "SAUDE"
                )
                updateRes.onSuccess {
                    AlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = existingTask.id,
                        title = taskTitle,
                        category = "Saúde",
                        dateEpochDay = dateEpochDay,
                        hour = alarmHour,
                        minute = alarmMinute,
                        ringtoneCategory = "SAUDE"
                    )
                    com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
                    onTaskLinked(existingTask.id)
                    _statusBannerMessage.value = "Agendamento atualizado na Agenda da Família para ${DateCalendarUtils.formatEpochDayPt(dateEpochDay)} às ${DateCalendarUtils.formatTime(alarmHour, alarmMinute)}!"
                }
            } else {
                val addRes = repository.addTask(
                    householdId = houseId,
                    title = taskTitle,
                    notes = notes,
                    location = location,
                    category = "Saúde",
                    priority = "HIGH",
                    dateEpochDay = dateEpochDay,
                    endDateEpochDay = 0L,
                    alarmHour = alarmHour,
                    alarmMinute = alarmMinute,
                    alarmEnabled = true,
                    privacyScope = PrivacyScope.FAMILY,
                    assignedMemberId = matchedMember?.userId ?: memberId.ifBlank { currentUserId },
                    assignedMemberName = memberName.ifBlank { currentUserName() },
                    assignedMemberColorHex = memberColor,
                    assignedMemberEmoji = memberEmoji,
                    createdByName = currentUserName(),
                    createdByColorHex = currentMemberColorHex(),
                    isEssential = true,
                    isInbox = false,
                    durationMinutes = 45,
                    iconEmoji = "🩺",
                    taskColorHex = memberColor,
                    recurrence = TaskRecurrence.ONCE,
                    ringtoneCategory = "SAUDE"
                )
                addRes.onSuccess { newTaskId ->
                    AlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = newTaskId,
                        title = taskTitle,
                        category = "Saúde",
                        dateEpochDay = dateEpochDay,
                        hour = alarmHour,
                        minute = alarmMinute,
                        ringtoneCategory = "SAUDE"
                    )
                    com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
                    onTaskLinked(newTaskId)
                    _statusBannerMessage.value = "Exame agendado na Agenda da Família para ${DateCalendarUtils.formatEpochDayPt(dateEpochDay)} às ${DateCalendarUtils.formatTime(alarmHour, alarmMinute)}!"
                }
            }
        }
    }

    fun setLinkedMedicalOrderTaskCompleted(
        context: Context,
        linkedTaskId: String,
        completed: Boolean
    ) {
        if (linkedTaskId.isBlank()) return
        viewModelScope.launch {
            val allTasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
            val task = allTasks.firstOrNull { it.id == linkedTaskId }
            val houseId = task?.householdId?.ifBlank { currentHouseholdId() } ?: currentHouseholdId()
            val targetEpoch = task?.dateEpochDay ?: DateCalendarUtils.todayEpochDay()
            repository.toggleTaskCompleted(
                householdId = houseId,
                taskId = linkedTaskId,
                completed = completed,
                targetDateIso = DateCalendarUtils.formatIsoDate(targetEpoch),
                completedByUserId = currentUserId,
                completedByName = currentUserName(),
                taskForAutoExpense = null
            ).onSuccess {
                if (completed) {
                    AlarmScheduler.cancelTaskAlarm(context, linkedTaskId)
                }
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
            }
        }
    }

    fun deleteLinkedMedicalOrderTask(
        context: Context,
        linkedTaskId: String
    ) {
        if (linkedTaskId.isBlank()) return
        viewModelScope.launch {
            AlarmScheduler.cancelTaskAlarm(context, linkedTaskId)
            repository.deleteTask(currentHouseholdId(), linkedTaskId).onSuccess {
                com.example.widget.OmniHomeWidgetProvider.requestUpdate(context)
            }
        }
    }

    fun refreshMorningSummarySchedule(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        if (!enabled) {
            AlarmScheduler.cancelMorningSummary(context)
            return
        }
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val (_, _, todayDayOfMonth) = DateCalendarUtils.fromEpochDay(todayEpoch)
        val tasksToday = (tasksState.value as? UiState.Success)?.data.orEmpty()
            .count { !it.isInbox && it.occursOnEpochDay(todayEpoch) && !it.isCompletedOnDate(todayEpoch) }
        val subsToday = (subscriptionsState.value as? UiState.Success)?.data.orEmpty()
            .count { it.active && it.dueDayOfMonth.toInt() == todayDayOfMonth }
        val cardsToday = currentHousehold()?.bankAccounts.orEmpty()
            .count { it.creditLimitBrl > 0.0 && it.dueDay == todayDayOfMonth }
        val shoppingPending = com.example.data.OmniBackupAndMigrationManager.getFamilyMuralItems(context)
            .count { item -> !item.completed && !item.isNote }

        AlarmScheduler.scheduleMorningSummary(
            context = context,
            hour = hour,
            minute = minute,
            tasksCountToday = tasksToday,
            billsDueTodayCount = subsToday + cardsToday,
            shoppingItemsPendingCount = shoppingPending
        )
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    companion object {
        private const val TAG = "AuraViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
