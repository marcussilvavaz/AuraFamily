package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.AppCustomizationConfig
import com.example.data.CustomizationRepository
import com.example.data.DateCalendarUtils
import com.example.data.ExamMarkerItem
import com.example.data.ExamMarkerStatus
import com.example.data.FamilyMemberBadge
import com.example.data.GeminiMedicalExtractor
import com.example.data.HealthDocumentCategory
import com.example.data.HealthDocumentRecord
import com.example.data.MedicalDocumentImageProcessor
import com.example.data.MedicalDocumentsRepository
import com.example.data.MedicalOrderStatus
import com.example.data.PregnancyCalculator
import com.example.data.PrescriptionMedItem
import com.example.data.PrescriptionStatus
import com.example.data.RequestedExamCheckItem
import com.example.data.StagedDocumentPage
import com.example.data.UltrasoundReportData
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalDocumentsCenterCard(
    householdId: String,
    loggedMemberId: String,
    loggedMemberName: String,
    familyMembers: List<FamilyMemberBadge>,
    customizationConfig: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onSyncPregnancyDppAndMode: (dppDate: String, comparisonMode: String, babyName: String?, babyGender: String?) -> Unit,
    onSyncBabyIdentity: (babyName: String, babyGender: String) -> Unit,
    onScheduleOrderInAgenda: (
        docTitle: String,
        memberName: String,
        memberId: String,
        dateEpochDay: Long,
        alarmHour: Int,
        alarmMinute: Int,
        location: String,
        notes: String,
        existingLinkedTaskId: String,
        onTaskLinked: (String) -> Unit
    ) -> Unit,
    onMarkLinkedTaskCompleted: (linkedTaskId: String, completed: Boolean) -> Unit,
    onDeleteLinkedTask: (linkedTaskId: String) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val medDocsRepo = remember(context) { MedicalDocumentsRepository(context) }
    val allDocuments by medDocsRepo.documentsFlow.collectAsState()

    LaunchedEffect(householdId) {
        if (householdId.isNotBlank()) {
            medDocsRepo.attachHouseholdRealtimeSync(householdId)
        }
    }

    val memberFilterOptions = remember(familyMembers, loggedMemberName) {
        val base = mutableListOf("Todos", "Nathalia", "Marcus")
        familyMembers.forEach { m ->
            val n = m.displayName.trim()
            if (n.isNotBlank() && base.none { it.equals(n, ignoreCase = true) }) {
                base.add(n)
            }
        }
        if (loggedMemberName.isNotBlank() && base.none { it.equals(loggedMemberName, ignoreCase = true) }) {
            base.add(loggedMemberName)
        }
        base.distinct()
    }

    var selectedMemberFilter by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf(HealthDocumentCategory.EXAME) }

    // Modal states
    var showCaptureStagingDialog by remember { mutableStateOf(false) }
    var editingDocument by remember { mutableStateOf<HealthDocumentRecord?>(null) }
    var pendingExtractedPages by remember { mutableStateOf<List<StagedDocumentPage>>(emptyList()) }
    var schedulingOrderDoc by remember { mutableStateOf<HealthDocumentRecord?>(null) }
    var viewingOriginalDoc by remember { mutableStateOf<HealthDocumentRecord?>(null) }
    var showBabyIdentityDialogInUltrasound by remember { mutableStateOf(false) }

    val filteredByMemberAndSearch = remember(allDocuments, selectedMemberFilter, searchQuery) {
        allDocuments.filter { doc ->
            val matchesMember = selectedMemberFilter == "Todos" ||
                doc.memberName.equals(selectedMemberFilter, ignoreCase = true)
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            val matchesSearch = if (q.isEmpty()) {
                true
            } else {
                doc.title.lowercase(Locale.ROOT).contains(q) ||
                    doc.doctorName.lowercase(Locale.ROOT).contains(q) ||
                    doc.doctorSpecialty.lowercase(Locale.ROOT).contains(q) ||
                    doc.clinicOrLab.lowercase(Locale.ROOT).contains(q) ||
                    doc.clinicalSummaryPt.lowercase(Locale.ROOT).contains(q) ||
                    doc.notes.lowercase(Locale.ROOT).contains(q) ||
                    doc.examMarkers.any { it.name.lowercase(Locale.ROOT).contains(q) } ||
                    doc.requestedExams.any { it.name.lowercase(Locale.ROOT).contains(q) } ||
                    doc.prescribedMeds.any { it.name.lowercase(Locale.ROOT).contains(q) } ||
                    doc.ultrasoundData.conclusion.lowercase(Locale.ROOT).contains(q)
            }
            matchesMember && matchesSearch
        }
    }

    val countsByCategory = remember(filteredByMemberAndSearch) {
        HealthDocumentCategory.allCategories.associateWith { cat ->
            filteredByMemberAndSearch.count { it.category == cat }
        }
    }

    val activeTabDocuments = remember(filteredByMemberAndSearch, selectedCategoryTab) {
        filteredByMemberAndSearch.filter { it.category == selectedCategoryTab }
            .sortedByDescending { it.dateEpochDay }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medical_documents_center_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📋 Documentos & Exames Médicos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Exames, Ultrassons, Pedidos e Receitas com leitura IA sob demanda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCaptureStagingDialog = true },
                    modifier = Modifier.testTag("btn_scan_upload_medical_doc"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B6E4F),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Escanear ou Enviar Documento",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Escanear / Enviar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Member Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                memberFilterOptions.forEach { memberOpt ->
                    val selected = selectedMemberFilter.equals(memberOpt, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { selectedMemberFilter = memberOpt },
                        label = {
                            Text(
                                text = memberOpt,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Keyword Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_docs_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar exames ou medicamentos"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar busca"
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "Buscar exame, marcador (ex: Ferritina, Glicose), médico ou remédio...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // 4 Sub-tabs: Exames | Ultrassons | Pedidos | Receitas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthDocumentCategory.allCategories.forEach { cat ->
                    val selected = selectedCategoryTab == cat
                    val count = countsByCategory[cat] ?: 0
                    val labelText = "${HealthDocumentCategory.emojiFor(cat)} ${HealthDocumentCategory.labelFor(cat)} ($count)"
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategoryTab = cat }
                            .testTag("med_doc_tab_${cat.lowercase(Locale.ROOT)}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            Color(0xFF0B6E4F)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }
                    ) {
                        Text(
                            text = labelText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Special Header in "Ultrassons" Tab for Baby Name & Gender (Synced Across Family)
            if (selectedCategoryTab == HealthDocumentCategory.ULTRASSOM) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val babyLabel = PregnancyCalculator.formatBabyDisplayLabelPt(
                                babyName = customizationConfig.babyName,
                                babyGender = customizationConfig.babyGender
                            )
                            Text(
                                text = "👶 Identidade do Bebê: $babyLabel",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                            val preg = PregnancyCalculator.calculateFromDpp(customizationConfig.pregnancyDppDate)
                            Text(
                                text = PregnancyCalculator.formatBabyArrivalCountdownPt(
                                    daysRemaining = preg.daysRemainingToDpp,
                                    babyName = customizationConfig.babyName,
                                    babyGender = customizationConfig.babyGender
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { showBabyIdentityDialogInUltrasound = true },
                            modifier = Modifier.testTag("btn_edit_baby_in_ultrasound_tab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar nome e sexo do bebê",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF6D28D9)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nome / Sexo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                        }
                    }
                }
            }

            // Documents List for Selected Sub-tab
            if (activeTabDocuments.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Nenhum documento em ${HealthDocumentCategory.labelFor(selectedCategoryTab)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toque em '+ Escanear / Enviar' para fotografar uma ou várias folhas, escolher da galeria ou importar um PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeTabDocuments.forEach { doc ->
                        MedicalDocumentItemCard(
                            doc = doc,
                            medDocsRepo = medDocsRepo,
                            onViewOriginal = { viewingOriginalDoc = doc },
                            onEdit = {
                                pendingExtractedPages = emptyList()
                                editingDocument = doc
                            },
                            onDelete = {
                                if (doc.linkedTaskId.isNotBlank()) {
                                    onDeleteLinkedTask(doc.linkedTaskId)
                                }
                                medDocsRepo.deleteDocument(doc.id, householdId)
                                onStatusMessage("Documento \"${doc.title}\" removido.")
                            },
                            onScheduleOrder = { schedulingOrderDoc = doc },
                            onUpdateOrderStatus = { newStatus ->
                                val updated = doc.copy(
                                    orderStatus = newStatus,
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                                medDocsRepo.saveOrUpdateDocument(
                                    record = updated,
                                    stagedPages = emptyList(),
                                    explicitHouseholdId = householdId
                                )
                                if (doc.linkedTaskId.isNotBlank()) {
                                    onMarkLinkedTaskCompleted(
                                        doc.linkedTaskId,
                                        newStatus == MedicalOrderStatus.REALIZADO
                                    )
                                }
                                onStatusMessage("Status do pedido atualizado para $newStatus!")
                            },
                            onToggleRequestedExam = { examId ->
                                medDocsRepo.toggleRequestedExamChecked(doc.id, examId, householdId)
                            },
                            onUpdatePrescriptionStatus = { newStatus ->
                                val updated = doc.copy(
                                    prescriptionStatus = newStatus,
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                                medDocsRepo.saveOrUpdateDocument(
                                    record = updated,
                                    stagedPages = emptyList(),
                                    explicitHouseholdId = householdId
                                )
                                onStatusMessage("Status da receita atualizado para $newStatus!")
                            },
                            onApplyUltrasoundDpp = { newDppBr ->
                                customizationRepo.updatePregnancyModule(
                                    enabled = customizationConfig.pregnancyEnabled,
                                    dppDate = newDppBr,
                                    notes = customizationConfig.pregnancyNotes,
                                    comparisonMode = customizationConfig.pregnancyComparisonMode,
                                    babyName = customizationConfig.babyName,
                                    babyGender = customizationConfig.babyGender
                                )
                                onSyncPregnancyDppAndMode(
                                    newDppBr,
                                    customizationConfig.pregnancyComparisonMode,
                                    customizationConfig.babyName,
                                    customizationConfig.babyGender
                                )
                                onStatusMessage("DPP da gestação atualizada para $newDppBr a partir do ultrassom!")
                            },
                            onApplyUltrasoundBabyGender = { detectedSex ->
                                val normGender = when {
                                    detectedSex.contains("MASC", ignoreCase = true) ||
                                        detectedSex.contains("MENINO", ignoreCase = true) -> "MENINO"
                                    detectedSex.contains("FEM", ignoreCase = true) ||
                                        detectedSex.contains("MENINA", ignoreCase = true) -> "MENINA"
                                    else -> "SURPRESA"
                                }
                                customizationRepo.updateBabyIdentity(
                                    babyName = customizationConfig.babyName,
                                    babyGender = normGender
                                )
                                onSyncBabyIdentity(customizationConfig.babyName, normGender)
                                onStatusMessage("Sexo do bebê atualizado para ${ PregnancyCalculator.formatBabyDisplayLabelPt(customizationConfig.babyName, normGender) }!")
                            }
                        )
                    }
                }
            }
        }
    }

    // 1. Multi-Page Capture & Staging Modal
    if (showCaptureStagingDialog) {
        MultiPageCaptureAndAnalyzeDialog(
            defaultMemberName = loggedMemberName.ifBlank { "Nathalia" },
            defaultMemberId = loggedMemberId,
            familyMembers = familyMembers,
            geminiApiKey = customizationConfig.geminiApiKey,
            defaultCategory = selectedCategoryTab,
            onDismiss = { showCaptureStagingDialog = false },
            onProceedToConfirmation = { extractedDraft, stagedPages ->
                showCaptureStagingDialog = false
                pendingExtractedPages = stagedPages
                editingDocument = extractedDraft
            }
        )
    }

    // 2. Confirm / Edit Extracted Medical Document Dialog
    editingDocument?.let { draftDoc ->
        ConfirmOrEditMedicalDocumentDialog(
            initialRecord = draftDoc,
            stagedPages = pendingExtractedPages,
            familyMembers = familyMembers,
            onDismiss = {
                editingDocument = null
                pendingExtractedPages = emptyList()
            },
            onSave = { finalDoc ->
                val saved = medDocsRepo.saveOrUpdateDocument(
                    record = finalDoc.copy(householdId = householdId),
                    stagedPages = pendingExtractedPages,
                    explicitHouseholdId = householdId
                )
                selectedCategoryTab = saved.category
                editingDocument = null
                pendingExtractedPages = emptyList()
                onStatusMessage("Documento \"${saved.title}\" salvo em ${HealthDocumentCategory.labelFor(saved.category)}!")
            }
        )
    }

    // 3. Schedule Medical Order ("Pedidos") in Agenda + Google Maps Modal
    schedulingOrderDoc?.let { orderDoc ->
        ScheduleMedicalOrderDialog(
            orderDoc = orderDoc,
            onDismiss = { schedulingOrderDoc = null },
            onConfirmSchedule = { scheduledEpochDay, timeHHmm, locationText ->
                val parts = timeHHmm.split(":")
                val hour = (parts.getOrNull(0)?.toIntOrNull() ?: 8).coerceIn(0, 23)
                val minute = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
                val examNames = orderDoc.requestedExams.joinToString(", ") { it.name }
                val taskNotes = buildList {
                    if (examNames.isNotBlank()) add("Exames: $examNames")
                    if (orderDoc.clinicalIndication.isNotBlank()) add("Indicação: ${orderDoc.clinicalIndication}")
                    if (orderDoc.doctorName.isNotBlank()) add("Solicitante: Dr(a). ${orderDoc.doctorName}")
                }.joinToString(" • ")

                onScheduleOrderInAgenda(
                    orderDoc.title,
                    orderDoc.memberName,
                    orderDoc.memberId,
                    scheduledEpochDay,
                    hour,
                    minute,
                    locationText,
                    taskNotes,
                    orderDoc.linkedTaskId
                ) { linkedTaskId ->
                    val updatedDoc = orderDoc.copy(
                        orderStatus = MedicalOrderStatus.AGENDADO,
                        scheduledDateEpochDay = scheduledEpochDay,
                        scheduledTimeHHmm = DateCalendarUtils.formatTime(hour, minute),
                        scheduledLocation = locationText,
                        linkedTaskId = linkedTaskId,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    medDocsRepo.saveOrUpdateDocument(
                        record = updatedDoc,
                        stagedPages = emptyList(),
                        explicitHouseholdId = householdId
                    )
                }
                schedulingOrderDoc = null
            }
        )
    }

    // 4. Full-screen Zoomable & Swipeable Original Document Viewer
    viewingOriginalDoc?.let { docToView ->
        OriginalDocumentFullViewerDialog(
            doc = docToView,
            medDocsRepo = medDocsRepo,
            householdId = householdId,
            onDismiss = { viewingOriginalDoc = null }
        )
    }

    // 5. Quick Baby Name & Gender Dialog inside Ultrasounds Tab
    if (showBabyIdentityDialogInUltrasound) {
        var nameInput by remember(customizationConfig.babyName) { mutableStateOf(customizationConfig.babyName) }
        var genderInput by remember(customizationConfig.babyGender) {
            mutableStateOf(customizationConfig.babyGender.ifBlank { "SURPRESA" })
        }
        AlertDialog(
            onDismissRequest = { showBabyIdentityDialogInUltrasound = false },
            title = {
                Text(
                    text = "👶 Nome e Sexo do Bebê",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Essas informações são sincronizadas automaticamente entre os celulares da família e aplicadas nos textos da Gestação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Nome do Bebê (opcional)") },
                        placeholder = { Text("Ex: Aurora, Gael, Theo...") }
                    )
                    Text(
                        text = "Sexo do Bebê:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "MENINO" to "👦 Menino",
                            "MENINA" to "👧 Menina",
                            "SURPRESA" to "✨ Surpresa"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = genderInput == code,
                                onClick = { genderInput = code },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customizationRepo.updateBabyIdentity(nameInput, genderInput)
                        onSyncBabyIdentity(nameInput, genderInput)
                        showBabyIdentityDialogInUltrasound = false
                        onStatusMessage("Identidade do bebê atualizada e sincronizada!")
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBabyIdentityDialogInUltrasound = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicalDocumentItemCard(
    doc: HealthDocumentRecord,
    medDocsRepo: MedicalDocumentsRepository,
    onViewOriginal: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onScheduleOrder: () -> Unit,
    onUpdateOrderStatus: (String) -> Unit,
    onToggleRequestedExam: (String) -> Unit,
    onUpdatePrescriptionStatus: (String) -> Unit,
    onApplyUltrasoundDpp: (String) -> Unit,
    onApplyUltrasoundBabyGender: (String) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medical_doc_item_${doc.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Title, Member Badge, Date & AI tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${HealthDocumentCategory.emojiFor(doc.category)} ${doc.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (doc.extractedByAi) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0B6E4F).copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "✨ IA",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0B6E4F)
                                )
                            }
                        }
                    }
                    val metaLine = buildList {
                        add("📅 ${doc.dateBr}")
                        if (doc.memberName.isNotBlank()) add("👤 ${doc.memberName}")
                        if (doc.clinicOrLab.isNotBlank()) add("🏥 ${doc.clinicOrLab}")
                        if (doc.doctorName.isNotBlank()) {
                            val spec = if (doc.doctorSpecialty.isNotBlank()) " (${doc.doctorSpecialty})" else ""
                            add("👨‍⚕️ Dr(a). ${doc.doctorName}$spec")
                        }
                    }.joinToString(" • ")
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (doc.clinicalSummaryPt.isNotBlank()) {
                Text(
                    text = doc.clinicalSummaryPt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Category-specific content
            when (doc.category) {
                HealthDocumentCategory.EXAME -> {
                    if (doc.examMarkers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            doc.examMarkers.forEach { marker ->
                                val isOut = marker.status == ExamMarkerStatus.ABAIXO || marker.status == ExamMarkerStatus.ACIMA
                                val statusColor = when (marker.status) {
                                    ExamMarkerStatus.ABAIXO -> Color(0xFFD97706)
                                    ExamMarkerStatus.ACIMA -> Color(0xFFDC2626)
                                    else -> Color(0xFF0B6E4F)
                                }
                                val prevPair = remember(doc.id, marker.name) {
                                    medDocsRepo.findPreviousMarkerForMember(
                                        currentDocId = doc.id,
                                        memberName = doc.memberName,
                                        markerName = marker.name,
                                        currentDateEpochDay = doc.dateEpochDay
                                    )
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isOut) statusColor.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = marker.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${marker.value} ${marker.unit}".trim(),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = statusColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = statusColor.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = when (marker.status) {
                                                        ExamMarkerStatus.ABAIXO -> "⬇ Abaixo"
                                                        ExamMarkerStatus.ACIMA -> "⬆ Acima"
                                                        else -> "✓ Normal"
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                        val refAndPrev = buildList {
                                            if (marker.referenceRange.isNotBlank()) add("Ref: ${marker.referenceRange}")
                                            if (prevPair != null) {
                                                add("Anterior (${prevPair.first.dateBr}): ${prevPair.second.value} ${prevPair.second.unit}".trim())
                                            }
                                        }.joinToString(" • ")
                                        if (refAndPrev.isNotBlank()) {
                                            Text(
                                                text = refAndPrev,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HealthDocumentCategory.ULTRASSOM -> {
                    val us = doc.ultrasoundData
                    val prevUsDoc = remember(doc.id, doc.dateEpochDay) {
                        medDocsRepo.findPreviousUltrasound(doc.id, doc.dateEpochDay)
                    }
                    val calculatedDpp = remember(doc.dateBr, us.gestationalWeeks, us.gestationalDays, us.estimatedDppBr) {
                        us.estimatedDppBr.ifBlank {
                            if (us.gestationalWeeks > 0) {
                                PregnancyCalculator.calculateDppFromUltrasound(
                                    examDateBr = doc.dateBr,
                                    weeksAtExam = us.gestationalWeeks,
                                    daysAtExam = us.gestationalDays
                                ).orEmpty()
                            } else ""
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val metrics = buildList {
                                if (us.gestationalWeeks > 0 || us.gestationalDays > 0) {
                                    add("IG: ${us.gestationalWeeks} sem e ${us.gestationalDays} d")
                                }
                                if (calculatedDpp.isNotBlank()) add("DPP: $calculatedDpp")
                                if (us.estimatedFetalWeightGrams > 0) add("Peso: ${us.estimatedFetalWeightGrams} g")
                                if (us.fetalLengthCm > 0.0) add("Comp: ${us.fetalLengthCm} cm")
                                if (us.fetalHeartRateBpm > 0) add("BCF: ${us.fetalHeartRateBpm} bpm")
                                if (us.fetalSex.isNotBlank()) add("Sexo fetal: ${us.fetalSex}")
                            }.joinToString(" • ")
                            if (metrics.isNotBlank()) {
                                Text(
                                    text = metrics,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                            if (prevUsDoc != null && us.estimatedFetalWeightGrams > 0 && prevUsDoc.ultrasoundData.estimatedFetalWeightGrams > 0) {
                                val deltaWeight = us.estimatedFetalWeightGrams - prevUsDoc.ultrasoundData.estimatedFetalWeightGrams
                                val sign = if (deltaWeight >= 0) "+$deltaWeight" else "$deltaWeight"
                                Text(
                                    text = "📈 Evolução: $sign g desde o ultrassom de ${prevUsDoc.dateBr} (${prevUsDoc.ultrasoundData.estimatedFetalWeightGrams} g)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0B6E4F)
                                )
                            }
                            val biometry = listOfNotNull(
                                us.biometryDbp.takeIf { it.isNotBlank() }?.let { "DBP: $it" },
                                us.biometryCc.takeIf { it.isNotBlank() }?.let { "CC: $it" },
                                us.biometryCa.takeIf { it.isNotBlank() }?.let { "CA: $it" },
                                us.biometryCf.takeIf { it.isNotBlank() }?.let { "CF: $it" },
                                us.placenta.takeIf { it.isNotBlank() }?.let { "Placenta: $it" },
                                us.amnioticFluidIla.takeIf { it.isNotBlank() }?.let { "Líquido/ILA: $it" }
                            ).joinToString(" • ")
                            if (biometry.isNotBlank()) {
                                Text(
                                    text = biometry,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (us.conclusion.isNotBlank()) {
                                Text(
                                    text = "Conclusão: ${us.conclusion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (calculatedDpp.isNotBlank()) {
                                    TextButton(onClick = { onApplyUltrasoundDpp(calculatedDpp) }) {
                                        Text(
                                            text = "📅 Atualizar DPP ($calculatedDpp)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (us.fetalSex.isNotBlank()) {
                                    TextButton(onClick = { onApplyUltrasoundBabyGender(us.fetalSex) }) {
                                        Text(
                                            text = "👶 Aplicar Sexo (${us.fetalSex})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HealthDocumentCategory.PEDIDO -> {
                    val statusColor = when (doc.orderStatus) {
                        MedicalOrderStatus.AGENDADO -> Color(0xFF0284C7)
                        MedicalOrderStatus.REALIZADO -> Color(0xFF0B6E4F)
                        else -> Color(0xFFD97706)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                MedicalOrderStatus.PENDENTE to "Pendente",
                                MedicalOrderStatus.AGENDADO to "Agendado",
                                MedicalOrderStatus.REALIZADO to "Já Realizado"
                            ).forEach { (statusCode, statusLabel) ->
                                val selected = doc.orderStatus.equals(statusCode, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateOrderStatus(statusCode) },
                                    label = {
                                        Text(
                                            text = statusLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (doc.clinicalIndication.isNotBlank()) {
                        Text(
                            text = "Indicação clínica: ${doc.clinicalIndication}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (doc.scheduledDateEpochDay > 0L) {
                        val schedDatePt = DateCalendarUtils.formatEpochDayPt(doc.scheduledDateEpochDay)
                        val locText = doc.scheduledLocation.ifBlank { doc.clinicOrLab }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📅 Agendado: $schedDatePt às ${doc.scheduledTimeHHmm.ifBlank { "08:00" }}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    if (locText.isNotBlank()) {
                                        Text(
                                            text = "📍 $locText",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (locText.isNotBlank()) {
                                    TextButton(
                                        onClick = { openMedicalLocationInGoogleMaps(context, locText) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "Abrir no Google Maps",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Maps", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (doc.requestedExams.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            doc.requestedExams.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleRequestedExam(item.id) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.checked,
                                        onCheckedChange = { onToggleRequestedExam(item.id) }
                                    )
                                    Text(
                                        text = item.name + if (item.notes.isNotBlank()) " (${item.notes})" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onScheduleOrder,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Agendar na Agenda",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (doc.scheduledDateEpochDay > 0L) "Alterar Data / Local na Agenda" else "Agendar Exame na Agenda + Maps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HealthDocumentCategory.RECEITA -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            PrescriptionStatus.EM_USO to "💊 Em Uso",
                            PrescriptionStatus.FINALIZADA to "✓ Finalizada"
                        ).forEach { (code, label) ->
                            val selected = doc.prescriptionStatus.equals(code, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdatePrescriptionStatus(code) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                    if (doc.prescribedMeds.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            doc.prescribedMeds.forEach { med ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "💊 ${med.name} ${med.dosage}".trim(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val posLine = listOfNotNull(
                                            med.posology.takeIf { it.isNotBlank() },
                                            med.duration.takeIf { it.isNotBlank() }?.let { "Duração: $it" }
                                        ).joinToString(" • ")
                                        if (posLine.isNotBlank()) {
                                            Text(
                                                text = posLine,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions Row: View Original Pages, Edit, Delete
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (doc.pageCount > 0) {
                    TextButton(
                        onClick = onViewOriginal,
                        modifier = Modifier.testTag("btn_view_original_doc_${doc.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Ver documento original",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ver Original (${doc.pageCount} pág.)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar documento",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir documento",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun openMedicalLocationInGoogleMaps(context: Context, locationQuery: String) {
    val clean = locationQuery.trim()
    if (clean.isBlank()) return
    runCatching {
        val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(clean)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapIntent)
    }.onFailure {
        runCatching {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(clean)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

@Composable
private fun MultiPageCaptureAndAnalyzeDialog(
    defaultMemberName: String,
    defaultMemberId: String,
    familyMembers: List<FamilyMemberBadge>,
    geminiApiKey: String,
    defaultCategory: String,
    onDismiss: () -> Unit,
    onProceedToConfirmation: (HealthDocumentRecord, List<StagedDocumentPage>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val memberChoices = remember(familyMembers, defaultMemberName) {
        val list = mutableListOf(
            "Nathalia" to "mem_nathalia",
            "Marcus" to "mem_marcus"
        )
        familyMembers.forEach { m ->
            val n = m.displayName.trim()
            if (n.isNotBlank()) {
                val idx = list.indexOfFirst { it.first.equals(n, ignoreCase = true) }
                if (idx >= 0) {
                    list[idx] = n to m.userId.ifBlank { list[idx].second }
                } else {
                    list.add(n to m.userId)
                }
            }
        }
        list
    }

    var selectedMemberName by remember {
        mutableStateOf(
            memberChoices.firstOrNull { it.first.equals(defaultMemberName, ignoreCase = true) }?.first
                ?: memberChoices.first().first
        )
    }
    var selectedMemberId by remember {
        mutableStateOf(
            memberChoices.firstOrNull { it.first.equals(selectedMemberName, ignoreCase = true) }?.second
                ?: defaultMemberId
        )
    }

    val stagedPages = remember { mutableStateListOf<StagedDocumentPage>() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingImages by remember { mutableStateOf(false) }
    var isAnalyzingWithAi by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 1. Camera Launcher (TakePicture + FileProvider)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = pendingCameraUri
        if (success && capturedUri != null) {
            scope.launch {
                isProcessingImages = true
                errorMessage = null
                val compressed = MedicalDocumentImageProcessor.compressImageUriToPage(
                    context = context,
                    uri = capturedUri,
                    pageIndex = stagedPages.size
                )
                isProcessingImages = false
                if (compressed != null) {
                    stagedPages.add(compressed)
                } else {
                    errorMessage = "Não foi possível processar a foto capturada (limite máximo 650 KB por página)."
                }
            }
        }
    }

    // 2. Gallery Multi-Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessingImages = true
                errorMessage = null
                uris.forEach { uri ->
                    val page = MedicalDocumentImageProcessor.compressImageUriToPage(
                        context = context,
                        uri = uri,
                        pageIndex = stagedPages.size
                    )
                    if (page != null) {
                        stagedPages.add(page)
                    }
                }
                isProcessingImages = false
                if (stagedPages.isEmpty()) {
                    errorMessage = "Nenhuma imagem pôde ser comprimida dentro do limite do plano gratuito."
                }
            }
        }
    }

    // 3. PDF Document Picker
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingImages = true
                errorMessage = null
                val rendered = MedicalDocumentImageProcessor.renderPdfUriToPages(
                    context = context,
                    pdfUri = uri,
                    startIndex = stagedPages.size,
                    maxPages = 10
                )
                isProcessingImages = false
                if (rendered.isNotEmpty()) {
                    stagedPages.addAll(rendered)
                } else {
                    errorMessage = "Não foi possível renderizar as páginas do arquivo PDF selecionado."
                }
            }
        }
    }

    fun launchCameraCapture() {
        runCatching {
            val dir = File(context.cacheDir, "camera_captures").apply { if (!exists()) mkdirs() }
            val tempFile = File(dir, "doc_page_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { e ->
            errorMessage = "Não foi possível abrir a câmera: ${e.localizedMessage ?: "erro"}"
        }
    }

    fun buildManualFallbackRecord(): HealthDocumentRecord {
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        return HealthDocumentRecord(
            id = "hdoc_" + UUID.randomUUID().toString().take(10),
            category = defaultCategory,
            title = "Novo ${HealthDocumentCategory.labelFor(defaultCategory).dropLast(1)}",
            memberId = selectedMemberId,
            memberName = selectedMemberName,
            dateBr = DateCalendarUtils.formatEpochDayPt(todayEpoch),
            dateEpochDay = todayEpoch,
            pageCount = stagedPages.size,
            extractedByAi = false
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isAnalyzingWithAi) onDismiss() },
        title = {
            Text(
                text = "📸 Escanear / Enviar Documento Médico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. De quem é este exame, ultrassom, pedido ou receita?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    memberChoices.forEach { (mName, mId) ->
                        val selected = selectedMemberName.equals(mName, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedMemberName = mName
                                selectedMemberId = mId
                            },
                            label = {
                                Text(
                                    text = "👤 $mName",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Text(
                    text = "2. Adicione uma ou mais páginas (Câmera, Galeria ou PDF):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchCameraCapture() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Câmera", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galeria", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (isProcessingImages) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Otimizando e comprimindo páginas (<250 KB)...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Multi-Page Staging Preview
                if (stagedPages.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📑 ${stagedPages.size} folha(s) pronta(s) para análise",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { launchCameraCapture() }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Outra folha", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(stagedPages) { idx, page ->
                                    val thumbBitmap = remember(page.base64Jpeg) {
                                        runCatching {
                                            val bytes = Base64.decode(page.base64Jpeg, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                        }.getOrNull()
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(116.dp)
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (thumbBitmap != null) {
                                                Image(
                                                    bitmap = thumbBitmap,
                                                    contentDescription = "Folha ${idx + 1}",
                                                    modifier = Modifier
                                                        .size(96.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Text(
                                                text = "Folha ${idx + 1} (${page.sizeBytes / 1024} KB)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (idx > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            val item = stagedPages.removeAt(idx)
                                                            stagedPages.add(idx - 1, item)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mover para esquerda", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (idx < stagedPages.lastIndex) {
                                                    IconButton(
                                                        onClick = {
                                                            val item = stagedPages.removeAt(idx)
                                                            stagedPages.add(idx + 1, item)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mover para direita", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { stagedPages.removeAt(idx) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remover folha",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Suas fotos estão preservadas! Você pode tentar analisar novamente ou preencher manualmente.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (isAnalyzingWithAi) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analisando ${stagedPages.size} folha(s) com Gemini 2.5 Flash...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        if (stagedPages.isEmpty()) {
                            errorMessage = "Adicione pelo menos 1 foto ou PDF antes de analisar."
                            return@Button
                        }
                        val reindexed = stagedPages.mapIndexed { i, p -> p.copy(pageIndex = i) }
                        if (geminiApiKey.isBlank()) {
                            errorMessage = "Chave API Gemini não configurada em Perfil -> Chave API Gemini. Você pode configurar a chave ou salvar manualmente agora."
                            return@Button
                        }
                        scope.launch {
                            isAnalyzingWithAi = true
                            errorMessage = null
                            val result = GeminiMedicalExtractor.analyzeDocumentPages(
                                apiKey = geminiApiKey,
                                pages = reindexed,
                                memberName = selectedMemberName
                            )
                            isAnalyzingWithAi = false
                            result.onSuccess { extracted ->
                                onProceedToConfirmation(
                                    extracted.copy(
                                        memberId = selectedMemberId,
                                        memberName = selectedMemberName,
                                        pageCount = reindexed.size
                                    ),
                                    reindexed
                                )
                            }.onFailure { err ->
                                errorMessage = err.localizedMessage ?: "Falha ao analisar com IA."
                            }
                        }
                    },
                    enabled = !isAnalyzingWithAi && !isProcessingImages && stagedPages.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B6E4F),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (errorMessage != null) {
                            "Tentar Novamente (${stagedPages.size} folha(s))"
                        } else {
                            "Concluir e Analisar (${stagedPages.size} folha(s))"
                        }
                    )
                }

                TextButton(
                    onClick = {
                        val reindexed = stagedPages.mapIndexed { i, p -> p.copy(pageIndex = i) }
                        onProceedToConfirmation(buildManualFallbackRecord(), reindexed)
                    },
                    enabled = !isAnalyzingWithAi && !isProcessingImages
                ) {
                    Text("Preencher / Salvar Manualmente")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAnalyzingWithAi
            ) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmOrEditMedicalDocumentDialog(
    initialRecord: HealthDocumentRecord,
    stagedPages: List<StagedDocumentPage>,
    familyMembers: List<FamilyMemberBadge>,
    onDismiss: () -> Unit,
    onSave: (HealthDocumentRecord) -> Unit
) {
    var category by remember(initialRecord) { mutableStateOf(initialRecord.category) }
    var title by remember(initialRecord) { mutableStateOf(initialRecord.title) }
    var memberName by remember(initialRecord) { mutableStateOf(initialRecord.memberName.ifBlank { "Nathalia" }) }
    var dateBr by remember(initialRecord) { mutableStateOf(initialRecord.dateBr) }
    var doctorName by remember(initialRecord) { mutableStateOf(initialRecord.doctorName) }
    var doctorSpecialty by remember(initialRecord) { mutableStateOf(initialRecord.doctorSpecialty) }
    var clinicOrLab by remember(initialRecord) { mutableStateOf(initialRecord.clinicOrLab) }
    var clinicalSummaryPt by remember(initialRecord) { mutableStateOf(initialRecord.clinicalSummaryPt) }
    var notes by remember(initialRecord) { mutableStateOf(initialRecord.notes) }

    // Exam markers state
    val markersList = remember(initialRecord) {
        mutableStateListOf<ExamMarkerItem>().apply { addAll(initialRecord.examMarkers) }
    }
    var newMarkerName by remember { mutableStateOf("") }
    var newMarkerValue by remember { mutableStateOf("") }
    var newMarkerUnit by remember { mutableStateOf("") }
    var newMarkerRef by remember { mutableStateOf("") }
    var newMarkerStatus by remember { mutableStateOf(ExamMarkerStatus.NORMAL) }

    // Ultrasound state
    val initUs = initialRecord.ultrasoundData
    var usWeeks by remember(initialRecord) { mutableStateOf(if (initUs.gestationalWeeks > 0) initUs.gestationalWeeks.toString() else "") }
    var usDays by remember(initialRecord) { mutableStateOf(if (initUs.gestationalDays > 0) initUs.gestationalDays.toString() else "") }
    var usDppBr by remember(initialRecord) { mutableStateOf(initUs.estimatedDppBr) }
    var usSex by remember(initialRecord) { mutableStateOf(initUs.fetalSex) }
    var usWeightG by remember(initialRecord) { mutableStateOf(if (initUs.estimatedFetalWeightGrams > 0) initUs.estimatedFetalWeightGrams.toString() else "") }
    var usBcfBpm by remember(initialRecord) { mutableStateOf(if (initUs.fetalHeartRateBpm > 0) initUs.fetalHeartRateBpm.toString() else "") }
    var usPlacenta by remember(initialRecord) { mutableStateOf(initUs.placenta) }
    var usIla by remember(initialRecord) { mutableStateOf(initUs.amnioticFluidIla) }
    var usConclusion by remember(initialRecord) { mutableStateOf(initUs.conclusion) }

    // Requested exams (Pedidos)
    val requestedExamsList = remember(initialRecord) {
        mutableStateListOf<RequestedExamCheckItem>().apply { addAll(initialRecord.requestedExams) }
    }
    var clinicalIndication by remember(initialRecord) { mutableStateOf(initialRecord.clinicalIndication) }
    var newRequestedExamName by remember { mutableStateOf("") }

    // Prescription medications (Receitas)
    val medsList = remember(initialRecord) {
        mutableStateListOf<PrescriptionMedItem>().apply { addAll(initialRecord.prescribedMeds) }
    }
    var newMedName by remember { mutableStateOf("") }
    var newMedDosage by remember { mutableStateOf("") }
    var newMedPosology by remember { mutableStateOf("") }
    var newMedDuration by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmar / Editar Documento Médico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Categoria do Documento:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HealthDocumentCategory.allCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = {
                                Text(
                                    text = "${HealthDocumentCategory.emojiFor(cat)} ${HealthDocumentCategory.labelFor(cat)}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                // Member choice
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Nathalia", "Marcus").forEach { mOpt ->
                        FilterChip(
                            selected = memberName.equals(mOpt, ignoreCase = true),
                            onClick = { memberName = mOpt },
                            label = { Text("👤 $mOpt", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Título do Documento") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateBr,
                        onValueChange = { dateBr = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Data (DD/MM/AAAA)") }
                    )
                    OutlinedTextField(
                        value = clinicOrLab,
                        onValueChange = { clinicOrLab = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Laboratório / Clínica") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Médico(a)") }
                    )
                    OutlinedTextField(
                        value = doctorSpecialty,
                        onValueChange = { doctorSpecialty = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Especialidade") }
                    )
                }

                OutlinedTextField(
                    value = clinicalSummaryPt,
                    onValueChange = { clinicalSummaryPt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Resumo Clínico em Português") }
                )

                // Category-Specific Fields
                when (category) {
                    HealthDocumentCategory.EXAME -> {
                        Text(
                            text = "Marcadores Extraídos (${markersList.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        markersList.forEachIndexed { idx, m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${m.name}: ${m.value} ${m.unit} [${m.status}]",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { markersList.removeAt(idx) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newMarkerName,
                                onValueChange = { newMarkerName = it },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                label = { Text("Exame/Marcador") }
                            )
                            OutlinedTextField(
                                value = newMarkerValue,
                                onValueChange = { newMarkerValue = it },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true,
                                label = { Text("Valor") }
                            )
                            OutlinedTextField(
                                value = newMarkerUnit,
                                onValueChange = { newMarkerUnit = it },
                                modifier = Modifier.weight(0.7f),
                                singleLine = true,
                                label = { Text("Unid.") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                ExamMarkerStatus.NORMAL to "Normal",
                                ExamMarkerStatus.ABAIXO to "Abaixo",
                                ExamMarkerStatus.ACIMA to "Acima"
                            ).forEach { (st, lbl) ->
                                FilterChip(
                                    selected = newMarkerStatus == st,
                                    onClick = { newMarkerStatus = st },
                                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (newMarkerName.isNotBlank() && newMarkerValue.isNotBlank()) {
                                        markersList.add(
                                            ExamMarkerItem(
                                                name = newMarkerName.trim(),
                                                value = newMarkerValue.trim(),
                                                unit = newMarkerUnit.trim(),
                                                referenceRange = newMarkerRef.trim(),
                                                status = newMarkerStatus
                                            )
                                        )
                                        newMarkerName = ""
                                        newMarkerValue = ""
                                        newMarkerUnit = ""
                                        newMarkerRef = ""
                                    }
                                }
                            ) {
                                Text("+ Adicionar")
                            }
                        }
                    }

                    HealthDocumentCategory.ULTRASSOM -> {
                        Text(
                            text = "Dados do Ultrassom:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = usWeeks,
                                onValueChange = { usWeeks = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("IG Semanas") }
                            )
                            OutlinedTextField(
                                value = usDays,
                                onValueChange = { usDays = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("IG Dias") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = usWeightG,
                                onValueChange = { usWeightG = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Peso Fetal (g)") }
                            )
                            OutlinedTextField(
                                value = usBcfBpm,
                                onValueChange = { usBcfBpm = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("BCF (bpm)") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = usDppBr,
                                onValueChange = { usDppBr = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("DPP (DD/MM/AAAA)") }
                            )
                            OutlinedTextField(
                                value = usSex,
                                onValueChange = { usSex = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Sexo Fetal") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = usPlacenta,
                                onValueChange = { usPlacenta = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Placenta") }
                            )
                            OutlinedTextField(
                                value = usIla,
                                onValueChange = { usIla = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Líquido / ILA") }
                            )
                        }
                        OutlinedTextField(
                            value = usConclusion,
                            onValueChange = { usConclusion = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Conclusão do Laudo") }
                        )
                    }

                    HealthDocumentCategory.PEDIDO -> {
                        OutlinedTextField(
                            value = clinicalIndication,
                            onValueChange = { clinicalIndication = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Indicação Clínica") }
                        )
                        Text(
                            text = "Exames Solicitados (${requestedExamsList.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        requestedExamsList.forEachIndexed { idx, req ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${req.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { requestedExamsList.removeAt(idx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newRequestedExamName,
                                onValueChange = { newRequestedExamName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Adicionar exame solicitado") }
                            )
                            TextButton(
                                onClick = {
                                    if (newRequestedExamName.isNotBlank()) {
                                        requestedExamsList.add(RequestedExamCheckItem(name = newRequestedExamName.trim()))
                                        newRequestedExamName = ""
                                    }
                                }
                            ) {
                                Text("+ Add")
                            }
                        }
                    }

                    HealthDocumentCategory.RECEITA -> {
                        Text(
                            text = "Medicamentos / Vitaminas (${medsList.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        medsList.forEachIndexed { idx, med ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${med.name} ${med.dosage} (${med.posology})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { medsList.removeAt(idx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newMedName,
                                onValueChange = { newMedName = it },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                label = { Text("Medicamento") }
                            )
                            OutlinedTextField(
                                value = newMedDosage,
                                onValueChange = { newMedDosage = it },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true,
                                label = { Text("Dosagem") }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newMedPosology,
                                onValueChange = { newMedPosology = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Posologia (ex: 1 cp 12/12h)") }
                            )
                            TextButton(
                                onClick = {
                                    if (newMedName.isNotBlank()) {
                                        medsList.add(
                                            PrescriptionMedItem(
                                                name = newMedName.trim(),
                                                dosage = newMedDosage.trim(),
                                                posology = newMedPosology.trim(),
                                                duration = newMedDuration.trim()
                                            )
                                        )
                                        newMedName = ""
                                        newMedDosage = ""
                                        newMedPosology = ""
                                        newMedDuration = ""
                                    }
                                }
                            ) {
                                Text("+ Add")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedEpoch = GeminiMedicalExtractor.parseDateBrToEpochDay(dateBr)
                    val matchedMember = familyMembers.firstOrNull {
                        it.displayName.equals(memberName, ignoreCase = true)
                    }
                    val finalRecord = initialRecord.copy(
                        category = category,
                        title = title.trim().ifBlank { "Documento Médico" },
                        memberId = matchedMember?.userId ?: initialRecord.memberId,
                        memberName = memberName.trim().ifBlank { "Nathalia" },
                        dateBr = DateCalendarUtils.formatEpochDayPt(parsedEpoch),
                        dateEpochDay = parsedEpoch,
                        doctorName = doctorName.trim(),
                        doctorSpecialty = doctorSpecialty.trim(),
                        clinicOrLab = clinicOrLab.trim(),
                        clinicalSummaryPt = clinicalSummaryPt.trim(),
                        notes = notes.trim(),
                        pageCount = if (stagedPages.isNotEmpty()) stagedPages.size else initialRecord.pageCount,
                        examMarkers = markersList.toList(),
                        ultrasoundData = initUs.copy(
                            gestationalWeeks = usWeeks.toIntOrNull() ?: 0,
                            gestationalDays = usDays.toIntOrNull() ?: 0,
                            estimatedDppBr = usDppBr.trim(),
                            fetalSex = usSex.trim(),
                            estimatedFetalWeightGrams = usWeightG.toIntOrNull() ?: 0,
                            fetalHeartRateBpm = usBcfBpm.toIntOrNull() ?: 0,
                            placenta = usPlacenta.trim(),
                            amnioticFluidIla = usIla.trim(),
                            conclusion = usConclusion.trim()
                        ),
                        clinicalIndication = clinicalIndication.trim(),
                        requestedExams = requestedExamsList.toList(),
                        prescribedMeds = medsList.toList(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    onSave(finalRecord)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B6E4F),
                    contentColor = Color.White
                )
            ) {
                Text("Salvar Documento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ScheduleMedicalOrderDialog(
    orderDoc: HealthDocumentRecord,
    onDismiss: () -> Unit,
    onConfirmSchedule: (scheduledEpochDay: Long, timeHHmm: String, location: String) -> Unit
) {
    val context = LocalContext.current
    val todayEpoch = DateCalendarUtils.todayEpochDay()
    val initialEpoch = if (orderDoc.scheduledDateEpochDay > 0L) orderDoc.scheduledDateEpochDay else todayEpoch + 1L
    var dateInputBr by remember(orderDoc) {
        mutableStateOf(DateCalendarUtils.formatEpochDayPt(initialEpoch))
    }
    var timeInputHHmm by remember(orderDoc) {
        mutableStateOf(orderDoc.scheduledTimeHHmm.ifBlank { "08:00" })
    }
    var locationInput by remember(orderDoc) {
        mutableStateOf(orderDoc.scheduledLocation.ifBlank { orderDoc.clinicOrLab })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📅 Agendar Pedido Médico na Agenda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${orderDoc.title} (${orderDoc.memberName})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B6E4F)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Hoje" to todayEpoch,
                        "Amanhã" to (todayEpoch + 1L),
                        "+7 dias" to (todayEpoch + 7L)
                    ).forEach { (lbl, ep) ->
                        FilterChip(
                            selected = dateInputBr == DateCalendarUtils.formatEpochDayPt(ep),
                            onClick = { dateInputBr = DateCalendarUtils.formatEpochDayPt(ep) },
                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateInputBr,
                        onValueChange = { dateInputBr = it },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        label = { Text("Data (DD/MM/AAAA)") }
                    )
                    OutlinedTextField(
                        value = timeInputHHmm,
                        onValueChange = { timeInputHHmm = it },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        label = { Text("Hora (HH:mm)") }
                    )
                }

                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Clínica / Laboratório / Endereço") },
                    placeholder = { Text("Ex: Fleury, Sabin, Dasa, Hospital...") }
                )

                OutlinedButton(
                    onClick = { openMedicalLocationInGoogleMaps(context, locationInput.ifBlank { "Laboratório de exames" }) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Abrir no Google Maps",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📍 Abrir / Verificar no Google Maps",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedEpoch = GeminiMedicalExtractor.parseDateBrToEpochDay(dateInputBr)
                    onConfirmSchedule(
                        parsedEpoch,
                        timeInputHHmm.trim().ifBlank { "08:00" },
                        locationInput.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B6E4F),
                    contentColor = Color.White
                )
            ) {
                Text("Salvar e Agendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun OriginalDocumentFullViewerDialog(
    doc: HealthDocumentRecord,
    medDocsRepo: MedicalDocumentsRepository,
    householdId: String,
    onDismiss: () -> Unit
) {
    var pages by remember { mutableStateOf<List<StagedDocumentPage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(doc.id, householdId) {
        isLoading = true
        pages = medDocsRepo.loadPagesForDocument(doc.id, householdId)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0E1412)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${doc.dateBr} • ${doc.memberName} • Pinça ou toque duplo para zoom",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar visualizador",
                            tint = Color.White
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (pages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma imagem original encontrada para este documento.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { pages.size })
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIdx ->
                            val page = pages[pageIdx]
                            ZoomableDocumentPageView(page = page)
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "Página ${pagerState.currentPage + 1} de ${pages.size}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableDocumentPageView(page: StagedDocumentPage) {
    val imageBitmap = remember(page.base64Jpeg) {
        runCatching {
            val bytes = Base64.decode(page.base64Jpeg, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.4f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4.5f)
                    scale = nextScale
                    if (nextScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Página ${page.pageIndex + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}
