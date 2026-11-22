package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `verify app_name resource and Structured color catalog`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Finanças", appName)
        assertTrue(CustomizationCatalog.structuredAppColors.size >= 8)
    }

    @Test
    fun `verify Structured layout modes Completo Simplificado Minimo and Daily State persistence`() {
        val repo = CustomizationRepository(context)

        // Test Layout Modes: Completo -> Simplificado -> Mínimo
        repo.updateAgendaLayoutMode(AgendaLayoutMode.SIMPLIFICADO)
        assertEquals(AgendaLayoutMode.SIMPLIFICADO, repo.configFlow.value.agendaLayoutMode)

        repo.updateAgendaLayoutMode(AgendaLayoutMode.MINIMO)
        assertEquals(AgendaLayoutMode.MINIMO, repo.configFlow.value.agendaLayoutMode)

        repo.updateAgendaLayoutMode(AgendaLayoutMode.COMPLETO)
        assertEquals(AgendaLayoutMode.COMPLETO, repo.configFlow.value.agendaLayoutMode)

        // Test Structured App Color change
        val royalBlueHex = 0xFF0077E6
        repo.updateStructuredAppColor(royalBlueHex)
        assertEquals(royalBlueHex, repo.configFlow.value.profileColorHex)

        // Test Daily State ("Acordei" & "Modo Dia Difícil" freezing streak)
        val dateIso = "2026-11-20"
        repo.recordWokeUp(dateIso, "06:30")
        assertEquals("06:30", repo.configFlow.value.dailyStateFor(dateIso).wokeUpAt)

        repo.toggleHardDayMode(dateIso)
        val hardDayState = repo.configFlow.value.dailyStateFor(dateIso)
        assertTrue(hardDayState.hardDayModeActive)
        assertTrue(hardDayState.streakFrozen)
    }

    @Test
    fun `verify Health Module trackers clinical records pregnancy and financial suggestions`() {
        val healthRepo = HealthRepository(context)
        val epochDay = DateCalendarUtils.todayEpochDay()
        val dateIso = DateCalendarUtils.formatIsoDate(epochDay)

        // 1. Daily Trackers progress
        val firstTracker = healthRepo.stateFlow.value.trackers.first()
        val beforeVal = healthRepo.stateFlow.value.trackerProgressByDate[dateIso]?.get(firstTracker.id) ?: 0
        healthRepo.incrementTrackerProgress(dateIso, firstTracker.id, 1)
        val afterVal = healthRepo.stateFlow.value.trackerProgressByDate[dateIso]?.get(firstTracker.id) ?: 0
        assertTrue(afterVal > beforeVal)

        // 2. Glucose & Blood Pressure records
        healthRepo.addGlucoseRecord(
            dateEpochDay = epochDay,
            timeHHmm = "08:30",
            slot = DiabetesSlotCatalog.JEJUM,
            valueMgDl = 94,
            notes = "Medição em jejum normal",
            privacyScope = PrivacyScope.PERSONAL
        )
        val addedGlucose = healthRepo.stateFlow.value.glucoseRecords.first { it.valueMgDl == 94 }
        assertEquals(94, addedGlucose.valueMgDl)
        assertEquals("Normal em Jejum", addedGlucose.statusLabel())

        healthRepo.addBloodPressureRecord(
            dateEpochDay = epochDay,
            timeHHmm = "21:00",
            systolic = 116,
            diastolic = 74,
            pulseBpm = 70,
            notes = "Antes de dormir",
            privacyScope = PrivacyScope.PERSONAL
        )
        val addedBp = healthRepo.stateFlow.value.bloodPressureRecords.first { it.systolic == 116 && it.diastolic == 74 }
        assertEquals(116, addedBp.systolic)
        assertEquals(74, addedBp.diastolic)
        assertEquals("Ótima / Normal", addedBp.statusLabel())

        // 3. Pregnancy Calculator & Financial Suggestions Integration
        val preg = PregnancyCalculator.calculateFromDpp("2027-03-15")
        assertTrue(preg.currentWeek in 1..42)
        assertTrue(HealthCatalog.financeSuggestions.any { it.category.equals("Farmácia", ignoreCase = true) })
        assertTrue(HealthCatalog.financeSuggestions.any { it.category.equals("Exames", ignoreCase = true) })
    }

    @Test
    fun `verify Private Diary hourly entries 3-shift quotes and AI Support Letter fallback`() = runBlocking {
        val diaryRepo = PrivateDiaryRepository(context)
        val epochDay = DateCalendarUtils.todayEpochDay()

        // 1. Shift classification (Manhã 05-11, Tarde 12-17, Noite 18-04)
        assertEquals(DayShift.MORNING, DayShift.fromHour(7))
        assertEquals(DayShift.AFTERNOON, DayShift.fromHour(14))
        assertEquals(DayShift.NIGHT, DayShift.fromHour(21))
        assertEquals(DayShift.NIGHT, DayShift.fromHour(2))

        // 2. Non-repeating consecutive shift quote rotation
        val firstQuote = diaryRepo.stateFlow.value.currentShiftQuote
        diaryRepo.refreshShiftQuote(forceNewQuote = true)
        val secondQuote = diaryRepo.stateFlow.value.currentShiftQuote
        assertNotEquals(firstQuote, secondQuote)

        // 3. Multiple hourly entries on the same day
        diaryRepo.addDiaryEntry(
            epochDay = epochDay,
            hour = 9,
            minute = 15,
            moodId = "CALM",
            symptomTags = listOf("Boa disposição", "Paz interior"),
            freeText = "Manhã produtiva e organizada."
        )
        diaryRepo.addDiaryEntry(
            epochDay = epochDay,
            hour = 18,
            minute = 45,
            moodId = "TIRED",
            symptomTags = listOf("Fadiga muscular"),
            freeText = "Fim de tarde mais cansativo."
        )
        val dayEntries = diaryRepo.stateFlow.value.entries.filter { it.epochDay == epochDay }
        assertTrue(dayEntries.size >= 2)

        // 4. Support Letter generation & saving
        val letter = diaryRepo.generatePersonalizedSupportLetter("Camila", epochDay)
        assertTrue(letter.content.isNotBlank())
        diaryRepo.saveSupportLetterToHistory(letter)
        assertTrue(diaryRepo.stateFlow.value.savedSupportLetters.any { it.id == letter.id })
    }

    @Test
    fun `verify Global Voice Command Interpreter parses Expense Task and Diary commands`() {
        // 1. Financial Expense Voice Command -> Finance Integration
        val expenseDraft = VoiceCommandInterpreter.parseTranscript("Gastei 85,50 reais na farmácia hoje")
        assertEquals(VoiceIntentType.EXPENSE, expenseDraft.detectedIntent)
        assertEquals(85.50, expenseDraft.expenseAmountBrl, 0.01)
        assertEquals("Farmácia", expenseDraft.expenseCategory)

        // 2. Agenda Task Voice Command -> Agenda Integration
        val taskDraft = VoiceCommandInterpreter.parseTranscript("Agendar tarefa reunião de planejamento às 14:30")
        assertEquals(VoiceIntentType.TASK, taskDraft.detectedIntent)
        assertEquals(14, taskDraft.taskHour)
        assertEquals(30, taskDraft.taskMinute)

        // 3. Private Diary Voice Command -> Diary Integration
        val diaryDraft = VoiceCommandInterpreter.parseTranscript("Registrar no diário que hoje estou me sentindo calmo e tranquilo")
        assertEquals(VoiceIntentType.DIARY, diaryDraft.detectedIntent)
        assertEquals("CALM", diaryDraft.diaryMoodId)
    }

    @Test
    fun `verify independent colors for 5 modules theme suggestions and organic visual styles`() {
        val repo = CustomizationRepository(context)

        // 1. Apply a curated theme suggestion and verify all 5 modules receive their preset palette
        val lavenderTheme = CustomizationCatalog.presets.first { it.id == "LAVENDER_CALM" }
        repo.applyThemePreset(lavenderTheme)
        assertEquals(lavenderTheme.hubColorHex, repo.configFlow.value.colorForModule(AppModuleId.HUB))
        assertEquals(lavenderTheme.agendaColorHex, repo.configFlow.value.colorForModule(AppModuleId.AGENDA))
        assertEquals(lavenderTheme.healthColorHex, repo.configFlow.value.colorForModule(AppModuleId.HEALTH))
        assertEquals(lavenderTheme.diaryColorHex, repo.configFlow.value.colorForModule(AppModuleId.DIARY))
        assertEquals(lavenderTheme.financeColorHex, repo.configFlow.value.colorForModule(AppModuleId.FINANCE))

        // 2. Customize each of the 5 modules independently
        repo.updateModuleColor(AppModuleId.HUB, 0xFF118D5F)
        repo.updateModuleColor(AppModuleId.AGENDA, 0xFFE88378)
        repo.updateModuleColor(AppModuleId.HEALTH, 0xFF00A896)
        repo.updateModuleColor(AppModuleId.DIARY, 0xFF8B5CF6)
        repo.updateModuleColor(AppModuleId.FINANCE, 0xFFE09F3E)

        assertEquals(0xFF118D5F, repo.configFlow.value.hubColorHex)
        assertEquals(0xFFE88378, repo.configFlow.value.agendaColorHex)
        assertEquals(0xFFE88378, repo.configFlow.value.profileColorHex)
        assertEquals(0xFF00A896, repo.configFlow.value.healthColorHex)
        assertEquals(0xFF8B5CF6, repo.configFlow.value.diaryColorHex)
        assertEquals(0xFFE09F3E, repo.configFlow.value.financeColorHex)

        // 3. Switch between the 3 selectable Organic Visual Styles
        repo.updateOrganicVisualStyle(OrganicVisualStyle.TRANSLUCIDO_GLASS)
        assertEquals(OrganicVisualStyle.TRANSLUCIDO_GLASS, repo.configFlow.value.organicVisualStyle)
        assertEquals(30, repo.configFlow.value.cardCornerRadiusDp)

        repo.updateOrganicVisualStyle(OrganicVisualStyle.MINIMALISTA_ACOLHEDOR)
        assertEquals(OrganicVisualStyle.MINIMALISTA_ACOLHEDOR, repo.configFlow.value.organicVisualStyle)
        assertEquals(28, repo.configFlow.value.cardCornerRadiusDp)

        repo.updateOrganicVisualStyle(OrganicVisualStyle.ORGANICO_FLUIDO)
        assertEquals(OrganicVisualStyle.ORGANICO_FLUIDO, repo.configFlow.value.organicVisualStyle)
        assertEquals(34, repo.configFlow.value.cardCornerRadiusDp)
    }

    @Test
    fun `verify specific questionnaire modules only appear when marked by the user`() {
        val defaultConfig = AppCustomizationConfig()
        assertFalse(defaultConfig.pregnancyEnabled)
        assertFalse(defaultConfig.diabetesEnabled)
        assertFalse(defaultConfig.bloodPressureEnabled)
        assertFalse(defaultConfig.chronicPainFatigueEnabled)

        // When pregnancyEnabled is false, Pré-Natal suggestion must NOT be shown
        val suggestionsWithoutPregnancy = HealthCatalog.financeSuggestionsFor(defaultConfig)
        assertFalse(suggestionsWithoutPregnancy.any { it.id == "hf_prenatal" })

        // When pregnancyEnabled is true, Pré-Natal suggestion MUST be shown
        val configWithPregnancy = defaultConfig.copy(pregnancyEnabled = true, pregnancyDppDate = "15/03/2027")
        val suggestionsWithPregnancy = HealthCatalog.financeSuggestionsFor(configWithPregnancy)
        assertTrue(suggestionsWithPregnancy.any { it.id == "hf_prenatal" })
    }

    @Test
    fun `verify Achievements and Medals system evaluates Health Finance and Calendar goals and unlocks medals`() {
        val achievementsRepo = AchievementsRepository(context)
        val healthRepo = HealthRepository(context)
        val config = AppCustomizationConfig()

        // Create a custom objective tied to Financeiro (automatically evaluated by active funded savings goals)
        val customGoal = achievementsRepo.addCustomAchievementGoal(
            title = "Duas Caixinhas Ativas",
            description = "Manter 2 caixinhas de reserva com saldo positivo",
            module = AchievementModule.FINANCE,
            medalTier = MedalTier.GOLD,
            targetValue = 2,
            unitLabel = "caixinhas"
        )

        val baseTransactions = listOf(
            FinanceTransaction(id = "tx1", householdId = "FAM", title = "Salário", amountOriginal = 6000.0, amountBrl = 6000.0, type = "INCOME", budgetBucket = "INCOME", category = "Renda", yearMonth = "2026-11"),
            FinanceTransaction(id = "tx2", householdId = "FAM", title = "Mercado", amountOriginal = 450.0, amountBrl = 450.0, type = "EXPENSE", budgetBucket = "NEEDS", category = "Alimentação", yearMonth = "2026-11"),
            FinanceTransaction(id = "tx3", householdId = "FAM", title = "Farmácia", amountOriginal = 120.0, amountBrl = 120.0, type = "EXPENSE", budgetBucket = "NEEDS", category = "Saúde", yearMonth = "2026-11")
        )
        val baseTasks = listOf(
            TaskItem(
                id = "t1",
                householdId = "FAM",
                title = "Revisar orçamento",
                notes = "",
                category = "Finanças",
                priority = "HIGH",
                dateEpochDay = DateCalendarUtils.todayEpochDay(),
                yearMonth = "2026-11",
                completed = true,
                isEssential = true
            )
        )

        // With only 1 funded savings goal, the custom goal (target = 2 caixinhas) is NOT unlocked yet
        val summaryWithOneGoal = achievementsRepo.evaluateAchievements(
            healthState = healthRepo.stateFlow.value,
            customizationConfig = config,
            tasks = baseTasks,
            transactions = baseTransactions,
            subscriptions = emptyList(),
            savingsGoals = listOf(
                SavingsGoal(id = "sg1", householdId = "FAM", title = "Reserva", targetAmountBrl = 5000.0, currentAmountBrl = 1500.0, incomePercentage = 10L, category = "Emergência", targetYearMonth = "2027-06")
            ),
            household = Household(householdId = "FAM", name = "Família", monthlyIncomeBrl = 6000.0),
            selectedYearMonth = "2026-11"
        )
        assertFalse(summaryWithOneGoal.goals.first { it.id == customGoal.id }.isUnlocked)

        // When the user genuinely adds a 2nd funded savings goal, the custom Gold Medal unlocks automatically!
        val summary = achievementsRepo.evaluateAchievements(
            healthState = healthRepo.stateFlow.value,
            customizationConfig = config,
            tasks = baseTasks,
            transactions = baseTransactions,
            subscriptions = emptyList(),
            savingsGoals = listOf(
                SavingsGoal(id = "sg1", householdId = "FAM", title = "Reserva", targetAmountBrl = 5000.0, currentAmountBrl = 1500.0, incomePercentage = 10L, category = "Emergência", targetYearMonth = "2027-06"),
                SavingsGoal(id = "sg2", householdId = "FAM", title = "Viagem", targetAmountBrl = 3000.0, currentAmountBrl = 600.0, incomePercentage = 5L, category = "Lazer", targetYearMonth = "2027-12")
            ),
            household = Household(householdId = "FAM", name = "Família", monthlyIncomeBrl = 6000.0),
            selectedYearMonth = "2026-11"
        )

        // Verify expanded automatic goals exist across all pillars (7 Saúde + 7 Financeiro + 7 Calendário + 7 Tríade + 1 custom = 29)
        assertTrue(summary.healthTotal >= 7)
        assertTrue(summary.financeTotal >= 7)
        assertTrue(summary.calendarTotal >= 7)
        assertTrue(summary.totalCount >= 28)
        assertTrue(summary.unlockedCount >= 5)
        assertTrue(summary.totalXp > 0)
        assertTrue(summary.goals.any { it.id == customGoal.id && it.isUnlocked && it.medalTier == MedalTier.GOLD })
    }

    @Test
    fun `verify AppUpdateManager triggers update prompt on installed device when remote build changes`() {
        val updateManager = AppUpdateManager(context)
        val localState = updateManager.uiState.value

        // Simulate a newer remote release published by AI Studio in Firestore (/app_releases/latest)
        val newerRemoteRelease = AppReleaseMetadata(
            buildRevision = localState.localKnownRevision + 3L,
            versionCode = localState.localVersionCode,
            versionName = localState.localVersionName,
            dexSignatureChain = "${localState.localBuildSignature},A9F8E7D6",
            releaseNotes = "Sistema de Conquistas & Medalhas e correção de atualização automática",
            downloadUrl = "https://ais-pre-4si6mi2cx6qcf46zmdgzwc-153058406669.us-east1.run.app",
            mandatory = false,
            publishedBy = "Aura Studio Build #A9F8E7D6",
            updatedAtEpochMs = System.currentTimeMillis() + 50_000L,
            buildTimestampMs = localState.localBuildTimestampMs + 50_000L
        )

        // When running on an installed phone (isStudioPreviewOverride = false), it must NOT overwrite remote
        // and MUST immediately flag isUpdateAvailable = true!
        updateManager.handleRemoteReleaseForTesting(
            remote = newerRemoteRelease,
            isStudioPreviewOverride = false
        )

        val afterRemoteSnapshot = updateManager.uiState.value
        assertTrue(afterRemoteSnapshot.isUpdateAvailable)
        assertEquals(newerRemoteRelease.buildRevision, afterRemoteSnapshot.remoteRelease?.buildRevision)
        assertEquals("A9F8E7D6", afterRemoteSnapshot.remoteRelease?.latestSignature)

        // Verify snooze ("Lembrar Depois") hides the modal for the session while keeping top banner active
        updateManager.dismissModalForSession()
        assertTrue(updateManager.uiState.value.isModalDismissedForSession)
        assertTrue(updateManager.uiState.value.isUpdateAvailable)

        // Verify reopenUpdateModal brings back the dialog
        updateManager.reopenUpdateModal()
        assertFalse(updateManager.uiState.value.isModalDismissedForSession)

        // Verify "Já Atualizei" acknowledges the revision on this device
        updateManager.acknowledgeCurrentRevisionOnThisDevice()
        assertFalse(updateManager.uiState.value.isUpdateAvailable)
        assertEquals(newerRemoteRelease.buildRevision, updateManager.uiState.value.localKnownRevision)

        // Verify broken .run.app placeholder URLs are scrubbed and Google Drive links are converted to direct download
        assertEquals(
            "",
            AppUpdateManager.sanitizeAndNormalizeUrl("https://ais-pre-4si6mi2cx6qcf46zmdgzwc-153058406669.us-east1.run.app")
        )
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
            AppUpdateManager.sanitizeAndNormalizeUrl("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view?usp=sharing")
        )
        assertTrue(updateManager.suggestedApkFileName().endsWith(".apk"))
    }
}

