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
}
