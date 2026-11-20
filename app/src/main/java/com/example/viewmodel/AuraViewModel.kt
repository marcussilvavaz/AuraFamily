package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.AuraFirestoreRepository
import com.example.data.CurrencyExchangeService
import com.example.data.CurrencyRatesState
import com.example.data.DateCalendarUtils
import com.example.data.FinanceTransaction
import com.example.data.Household
import com.example.data.PrivacyScope
import com.example.data.SavingsGoal
import com.example.data.SubscriptionBill
import com.example.data.TaskItem
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

    // Gate household listeners until ensureUserProfileAndHousehold has committed the server document
    private val _isProfileServerReady = MutableStateFlow(false)

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
            repository.ensureUserProfileAndHousehold(initialDisplayName)
            _isProfileServerReady.value = true
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

    fun updateUserProfileIdentity(displayName: String, memberColorHex: Long) {
        viewModelScope.launch {
            val cleanName = displayName.trim().ifBlank { currentUserName() }
            val currentEmoji = (userProfileState.value as? UiState.Success)?.data?.avatarEmoji?.ifBlank { "🌿" } ?: "🌿"
            val result = repository.updateUserMemberIdentity(
                displayName = cleanName,
                memberColorHex = memberColorHex,
                avatarEmoji = currentEmoji
            )
            result.onSuccess {
                _statusBannerMessage.value = "Perfil e cor da agenda familiar atualizados!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao atualizar perfil."
            }
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode)
        }
    }

    fun joinOrCreateHousehold(householdCode: String, familyName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _isProfileServerReady.value = false
            val result = repository.switchOrJoinHousehold(householdCode, familyName)
            _isProfileServerReady.value = true
            _isBusy.value = false
            result.onSuccess { code ->
                _statusBannerMessage.value = "Sincronizado com o orçamento familiar: $code"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Erro ao alterar código familiar."
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
        category: String,
        priority: String,
        dateEpochDay: Long,
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
        recurrence: String = com.example.data.TaskRecurrence.ONCE,
        subtasks: List<com.example.data.SubtaskItem> = emptyList(),
        ringtoneCategory: String = "SUAVE"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val scopeMemberName = if (privacyScope == PrivacyScope.PERSONAL) "Só Meu (${currentUserName()})" else currentUserName()
            val scopeEmoji = if (privacyScope == PrivacyScope.PERSONAL) "🔒" else "🌿"
            val memberColor = overrideMemberColorHex ?: currentMemberColorHex()
            val resolvedTaskColor = taskColorHex ?: memberColor
            val effectiveAlarmEnabled = if (isInbox) false else alarmEnabled
            val result = repository.addTask(
                householdId = currentHouseholdId(),
                title = title,
                notes = notes,
                category = category,
                priority = priority,
                dateEpochDay = dateEpochDay,
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
                        minute = alarmMinute
                    )
                }
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
                    minute = alarmMinute
                )
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

    fun toggleTaskCompletion(context: Context, task: TaskItem) {
        viewModelScope.launch {
            val newStatus = !task.completed
            val houseId = task.householdId.ifBlank { currentHouseholdId() }
            val result = repository.toggleTaskCompleted(
                householdId = houseId,
                taskId = task.id,
                completed = newStatus,
                completedByUserId = currentUserId,
                completedByName = currentUserName(),
                taskForAutoExpense = task
            )
            result.onSuccess {
                if (newStatus) {
                    AlarmScheduler.cancelTaskAlarm(context, task.id)
                    if (task.autoCreateExpenseOnComplete && task.estimatedCostBrl > 0.0 && task.linkedTransactionId.isBlank()) {
                        _statusBannerMessage.value = "Check ✓ em \"${task.title}\"! Pagamento de ${formatCurrencyBrl(task.estimatedCostBrl)} lançado nas Finanças!"
                    } else {
                        _statusBannerMessage.value = "Tarefa \"${task.title}\" concluída!"
                    }
                } else if (task.alarmEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = task.id,
                        title = task.title,
                        category = task.category,
                        dateEpochDay = task.dateEpochDay,
                        hour = task.alarmHour.toInt(),
                        minute = task.alarmMinute.toInt()
                    )
                }
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
        overrideMemberColorHex: Long? = null
    ) {
        viewModelScope.launch {
            val rate = _currencyRates.value.rateFor(currency)
            val scopeMemberName = if (privacyScope == PrivacyScope.PERSONAL) "Só Meu (${currentUserName()})" else currentUserName()
            val scopeEmoji = if (privacyScope == PrivacyScope.PERSONAL) "🔒" else "🌿"
            val memberColor = overrideMemberColorHex ?: currentMemberColorHex()
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
                generateCalendarTask = generateCalendarTask
            )
            _isBusy.value = false
            result.onSuccess {
                val scopeBadge = if (privacyScope == PrivacyScope.PERSONAL) "Individual 🔒" else "Família 👨‍👩‍👧‍👦"
                _statusBannerMessage.value = if (generateCalendarTask) {
                    "Conta ($scopeBadge) registrada e criada na Agenda com sua cor do perfil (${DateCalendarUtils.formatEpochDayPt(dateEpochDay)})!"
                } else {
                    "Lançamento ($scopeBadge) registrado com sucesso!"
                }
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao registrar lançamento."
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(currentHouseholdId(), transactionId).onSuccess {
                _statusBannerMessage.value = "Lançamento removido."
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
                _statusBannerMessage.value = "Meta familiar \"$title\" criada!"
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Falha ao criar meta."
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
