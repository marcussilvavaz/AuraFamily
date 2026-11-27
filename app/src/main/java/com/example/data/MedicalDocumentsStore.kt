package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

object HealthDocumentCategory {
    const val EXAME = "EXAME"
    const val ULTRASSOM = "ULTRASSOM"
    const val PEDIDO = "PEDIDO"
    const val RECEITA = "RECEITA"

    val allCategories: List<String> = listOf(EXAME, ULTRASSOM, PEDIDO, RECEITA)

    fun normalize(raw: String): String {
        return when (raw.trim().uppercase(Locale.ROOT)) {
            "EXAME", "EXAMES", "LABORATORIO", "LABORATÓRIO" -> EXAME
            "ULTRASSOM", "ULTRASSONOGRAFIA", "USG", "ECOGRAFIA" -> ULTRASSOM
            "PEDIDO", "PEDIDOS", "SOLICITACAO", "SOLICITAÇÃO", "GUIA" -> PEDIDO
            "RECEITA", "RECEITAS", "PRESCRICAO", "PRESCRIÇÃO" -> RECEITA
            else -> EXAME
        }
    }

    fun tabLabel(category: String): String {
        return when (normalize(category)) {
            EXAME -> "Exames"
            ULTRASSOM -> "Ultrassons"
            PEDIDO -> "Pedidos"
            RECEITA -> "Receitas"
            else -> "Exames"
        }
    }

    fun labelFor(category: String): String = tabLabel(category)

    fun emojiFor(category: String): String {
        return when (normalize(category)) {
            EXAME -> "🧪"
            ULTRASSOM -> "👶"
            PEDIDO -> "📋"
            RECEITA -> "💊"
            else -> "📄"
        }
    }
}

object ExamMarkerStatus {
    const val NORMAL = "NORMAL"
    const val ABAIXO = "ABAIXO"
    const val ACIMA = "ACIMA"

    fun normalize(raw: String): String {
        return when (raw.trim().uppercase(Locale.ROOT)) {
            "ABAIXO", "BAIXO", "LOW" -> ABAIXO
            "ACIMA", "ALTO", "ALTERADO", "HIGH" -> ACIMA
            else -> NORMAL
        }
    }
}

object MedicalOrderStatus {
    const val PENDENTE = "PENDENTE"
    const val AGENDADO = "AGENDADO"
    const val JA_REALIZADO = "JÁ REALIZADO"
    const val REALIZADO = JA_REALIZADO

    fun normalize(raw: String): String {
        return when (raw.trim().uppercase(Locale.ROOT)) {
            "AGENDADO" -> AGENDADO
            "JÁ REALIZADO", "JA REALIZADO", "REALIZADO", "CONCLUIDO", "CONCLUÍDO" -> JA_REALIZADO
            else -> PENDENTE
        }
    }
}

object PrescriptionStatus {
    const val EM_USO = "EM USO"
    const val FINALIZADA = "FINALIZADA"

    fun normalize(raw: String): String {
        return when (raw.trim().uppercase(Locale.ROOT)) {
            "FINALIZADA", "FINALIZADO", "CONCLUIDA", "CONCLUÍDA" -> FINALIZADA
            else -> EM_USO
        }
    }
}

data class ExamMarkerItem(
    val name: String = "",
    val value: String = "",
    val unit: String = "",
    val referenceRange: String = "",
    val status: String = ExamMarkerStatus.NORMAL
) {
    val isOutOfRange: Boolean
        get() = ExamMarkerStatus.normalize(status) != ExamMarkerStatus.NORMAL
}

data class UltrasoundReportData(
    val gestationalAgeText: String = "",
    val gestationalWeeks: Int = 0,
    val gestationalDays: Int = 0,
    val estimatedDppBr: String = "",
    val fetalSex: String = "", // "MENINO", "MENINA", "SURPRESA" or ""
    val estimatedFetalWeightGrams: Int = 0,
    val fetalLengthCm: Double = 0.0,
    val fetalHeartRateBpm: Int = 0,
    val biometryDbp: String = "",
    val biometryCc: String = "",
    val biometryCa: String = "",
    val biometryCf: String = "",
    val placenta: String = "",
    val amnioticFluidIla: String = "",
    val conclusion: String = ""
) {
    val weeksAtExam: Int get() = gestationalWeeks
    val daysAtExam: Int get() = gestationalDays
    val dppDateBr: String get() = estimatedDppBr
    val fetalWeightGrams: Int get() = estimatedFetalWeightGrams
    val bcfBpm: Int get() = fetalHeartRateBpm
    val dbpMm: String get() = biometryDbp
    val ccMm: String get() = biometryCc
    val caMm: String get() = biometryCa
    val cfMm: String get() = biometryCf
    val ila: String get() = amnioticFluidIla
}

data class RequestedExamCheckItem(
    val id: String = "req_" + UUID.randomUUID().toString().take(6),
    val name: String = "",
    val notes: String = "",
    val checked: Boolean = false
) {
    val examName: String get() = name
    val done: Boolean get() = checked
}

data class PrescriptionMedItem(
    val id: String = "med_" + UUID.randomUUID().toString().take(6),
    val name: String = "",
    val dosage: String = "",
    val posology: String = "",
    val duration: String = ""
)

data class StagedDocumentPage(
    val id: String = "pg_" + UUID.randomUUID().toString().take(6),
    val pageIndex: Int = 0,
    val base64Jpeg: String = "",
    val sizeBytes: Int = 0,
    val widthPx: Int = 0,
    val heightPx: Int = 0
)

data class HealthDocumentRecord(
    val id: String = "hdoc_" + UUID.randomUUID().toString().take(10),
    val householdId: String = "",
    val category: String = HealthDocumentCategory.EXAME,
    val memberId: String = "",
    val memberName: String = "Nathalia",
    val title: String = "",
    val dateBr: String = "",
    val dateEpochDay: Long = DateCalendarUtils.todayEpochDay(),
    val clinicOrLab: String = "",
    val doctorName: String = "",
    val doctorSpecialty: String = "",
    val clinicalSummaryPt: String = "",
    val clinicalIndication: String = "",
    val notes: String = "",
    val examMarkers: List<ExamMarkerItem> = emptyList(),
    val ultrasoundData: UltrasoundReportData = UltrasoundReportData(),
    val requestedExams: List<RequestedExamCheckItem> = emptyList(),
    val orderStatus: String = MedicalOrderStatus.PENDENTE,
    val scheduledDateBr: String = "",
    val scheduledDateEpochDay: Long = 0L,
    val scheduledTimeHHmm: String = "",
    val scheduledLocation: String = "",
    val linkedTaskId: String = "",
    val prescribedMeds: List<PrescriptionMedItem> = emptyList(),
    val prescriptionStatus: String = PrescriptionStatus.EM_USO,
    val pageCount: Int = 0,
    val extractedByAi: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val doctorOrSpecialty: String
        get() = listOf(doctorName, doctorSpecialty).filter { it.isNotBlank() }.joinToString(" • ")
    val summaryOrIndication: String
        get() = clinicalSummaryPt.ifBlank { clinicalIndication }
    val scheduledEpochDay: Long
        get() = scheduledDateEpochDay
    val medications: List<PrescriptionMedItem>
        get() = prescribedMeds
    val aiExtracted: Boolean
        get() = extractedByAi
}

typealias MedicalDocumentMeta = HealthDocumentRecord

/**
 * Utilitário de compressão de imagens e renderização de PDFs para Base64 leve
 * (< 250 KB por página, teto rígido < 650 KB), 100% compatível com o plano gratuito Spark do Firestore.
 */
object MedicalDocumentImageProcessor {
    private const val TARGET_MAX_DIMENSION_PX = 1100
    private const val TARGET_SOFT_MAX_BYTES = 240 * 1024 // < 250 KB ideal
    private const val HARD_CAP_MAX_BYTES = 460 * 1024 // ~615 KB em Base64 (< 650 KB teto rígido)

    suspend fun compressImageUriToPage(
        context: Context,
        uri: Uri,
        pageIndex: Int
    ): StagedDocumentPage? {
        return processImageUri(context, uri, pageIndex).getOrNull()
    }

    suspend fun processImageUri(
        context: Context,
        uri: Uri,
        pageIndex: Int
    ): Result<StagedDocumentPage> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File(context.cacheDir, "med_doc_scan_${System.currentTimeMillis()}_$pageIndex.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw IllegalArgumentException("Não foi possível ler a imagem selecionada.")
            }
            try {
                processImageFile(tempFile, pageIndex)
            } finally {
                runCatching { tempFile.delete() }
            }
        }
    }

    fun processImageFile(
        imageFile: File,
        pageIndex: Int
    ): StagedDocumentPage {
        val rotationDegrees = try {
            val exif = ExifInterface(imageFile.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, boundsOptions)
        var sampleSize = 1
        while ((boundsOptions.outWidth / sampleSize) > TARGET_MAX_DIMENSION_PX * 2 ||
            (boundsOptions.outHeight / sampleSize) > TARGET_MAX_DIMENSION_PX * 2
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Falha ao decodificar foto do documento.")

        val oriented = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }

        return compressBitmapToStagedPage(oriented, pageIndex)
    }

    suspend fun renderPdfUriToPages(
        context: Context,
        pdfUri: Uri,
        startIndex: Int = 0,
        maxPages: Int = 10
    ): List<StagedDocumentPage> = withContext(Dispatchers.IO) {
        runCatching {
            val tempPdf = File(context.cacheDir, "med_doc_pdf_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(tempPdf).use { output ->
                    input.copyTo(output)
                }
            }
            if (!tempPdf.exists() || tempPdf.length() == 0L) {
                return@runCatching emptyList<StagedDocumentPage>()
            }
            try {
                val pages = mutableListOf<StagedDocumentPage>()
                ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val count = renderer.pageCount.coerceAtMost(maxPages)
                        for (i in 0 until count) {
                            renderer.openPage(i).use { pdfPage ->
                                val srcW = pdfPage.width.coerceAtLeast(1)
                                val srcH = pdfPage.height.coerceAtLeast(1)
                                val scale = minOf(
                                    TARGET_MAX_DIMENSION_PX.toFloat() / srcW.toFloat(),
                                    TARGET_MAX_DIMENSION_PX.toFloat() / srcH.toFloat()
                                ).coerceAtLeast(0.5f)
                                val targetW = (srcW * scale).toInt().coerceIn(300, TARGET_MAX_DIMENSION_PX)
                                val targetH = (srcH * scale).toInt().coerceIn(300, TARGET_MAX_DIMENSION_PX)

                                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(AndroidColor.WHITE)
                                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                pages.add(compressBitmapToStagedPage(bitmap, startIndex + i))
                            }
                        }
                    }
                }
                pages
            } finally {
                runCatching { tempPdf.delete() }
            }
        }.getOrElse { emptyList() }
    }

    fun compressBitmapToStagedPage(
        source: Bitmap,
        pageIndex: Int
    ): StagedDocumentPage {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val scale = minOf(
            TARGET_MAX_DIMENSION_PX.toFloat() / w.toFloat(),
            TARGET_MAX_DIMENSION_PX.toFloat() / h.toFloat(),
            1f
        )
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        var workingBitmap = if (targetW != w || targetH != h) {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } else {
            source
        }

        var quality = 70
        var jpegBytes = compressJpeg(workingBitmap, quality)

        while (jpegBytes.size > TARGET_SOFT_MAX_BYTES && quality >= 42) {
            quality -= 10
            jpegBytes = compressJpeg(workingBitmap, quality)
        }

        if (jpegBytes.size > HARD_CAP_MAX_BYTES) {
            val downW = (workingBitmap.width * 0.78f).toInt().coerceAtLeast(320)
            val downH = (workingBitmap.height * 0.78f).toInt().coerceAtLeast(320)
            workingBitmap = Bitmap.createScaledBitmap(workingBitmap, downW, downH, true)
            jpegBytes = compressJpeg(workingBitmap, 48)
        }

        val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        if (base64.length > 650 * 1024) {
            throw IllegalStateException("Página excedeu o limite máximo de 650 KB para o Firestore. Tente fotografar com menos zoom.")
        }

        return StagedDocumentPage(
            pageIndex = pageIndex,
            base64Jpeg = base64,
            sizeBytes = jpegBytes.size,
            widthPx = workingBitmap.width,
            heightPx = workingBitmap.height
        )
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 90), baos)
        return baos.toByteArray()
    }

    fun decodeBase64ToBitmap(base64Jpeg: String): Bitmap? {
        if (base64Jpeg.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64Jpeg, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}

/**
 * Cliente REST sob demanda para o modelo oficial `gemini-2.5-flash`.
 * NUNCA executa em segundo plano e NUNCA contém chave hardcoded.
 */
object GeminiMedicalExtractor {
    private const val GEMINI_MODEL = "gemini-2.5-flash"

    fun resolveConfiguredGeminiApiKey(context: Context): String {
        val fromProfile = runCatching {
            CustomizationRepository(context).configFlow.value.geminiApiKey.trim()
        }.getOrDefault("")
        if (fromProfile.isNotBlank()) return fromProfile

        val fromBuildConfig = runCatching {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.declaredFields.firstOrNull {
                it.name.equals("GEMINI_API_KEY", ignoreCase = true)
            }
            field?.isAccessible = true
            (field?.get(null) as? String)?.trim().orEmpty()
        }.getOrDefault("")
        return fromBuildConfig
    }

    suspend fun analyzeDocumentPages(
        context: Context,
        pages: List<StagedDocumentPage>,
        memberId: String,
        memberName: String,
        preferredCategoryHint: String? = null
    ): Result<HealthDocumentRecord> {
        val apiKey = resolveConfiguredGeminiApiKey(context)
        return analyzeDocumentPages(
            apiKey = apiKey,
            pages = pages,
            memberName = memberName,
            memberId = memberId,
            preferredCategoryHint = preferredCategoryHint
        )
    }

    suspend fun analyzeDocumentPages(
        apiKey: String,
        pages: List<StagedDocumentPage>,
        memberName: String,
        memberId: String = "",
        preferredCategoryHint: String? = null
    ): Result<HealthDocumentRecord> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                throw IllegalStateException(
                    "Chave da API Gemini não configurada. Adicione sua chave em Perfil -> Chave API Gemini ou preencha os dados manualmente."
                )
            }
            if (pages.isEmpty()) {
                throw IllegalArgumentException("Nenhuma página capturada para análise.")
            }

            val prompt = """
                Você é um assistente médico especialista em estruturação de documentos clínicos em português do Brasil (pt-BR).
                Analise TODAS as ${pages.size} página(s) deste documento médico do(a) paciente "$memberName" como um único documento contínuo.
                ${if (!preferredCategoryHint.isNullOrBlank()) "Dica de categoria selecionada pelo usuário: $preferredCategoryHint." else ""}
                
                Classifique o documento em EXATAMENTE uma destas categorias:
                - "EXAME" (exames laboratoriais de sangue, urina, curva glicêmica, hemograma, etc.)
                - "ULTRASSOM" (ultrassonografia obstétrica, morfológica, transvaginal, doppler, ecografia)
                - "PEDIDO" (pedido/solicitação médica de exames ou guia para agendamento)
                - "RECEITA" (receituário médico de medicamentos, vitaminas ou suplementos)
                
                Retorne EXCLUSIVAMENTE um objeto JSON válido (sem markdown ```json) com a seguinte estrutura exata:
                {
                  "category": "EXAME | ULTRASSOM | PEDIDO | RECEITA",
                  "title": "Título claro do documento (ex: Hemograma e Glicemia, Ultrassom Morfológico 2º Trimestre, Pedido de Exames Pré-Natal, Receita Obstétrica)",
                  "dateBr": "DD/MM/AAAA (data da coleta/exame/emissão se visível, ou vazio)",
                  "clinicOrLab": "Nome do laboratório ou clínica",
                  "doctorName": "Nome do médico(a) e CRM",
                  "doctorSpecialty": "Especialidade médica",
                  "summaryOrIndication": "Resumo clínico em linguagem simples e acolhedora (ou indicação clínica / conclusão)",
                  "examMarkers": [
                    {
                      "name": "Nome do marcador (ex: Glicose em Jejum, Hemoglobina, Ferritina, TSH)",
                      "value": "Valor numérico ou textual (ex: 86, 12.4)",
                      "unit": "Unidade (ex: mg/dL, g/dL, ng/mL)",
                      "referenceRange": "Valor de referência do laudo (ex: 70 a 99 mg/dL)",
                      "status": "NORMAL | ABAIXO | ACIMA"
                    }
                  ],
                  "ultrasoundData": {
                    "gestationalAgeText": "Ex: 19 semanas e 2 dias",
                    "weeksAtExam": 19,
                    "daysAtExam": 2,
                    "dppDateBr": "DD/MM/AAAA (Data Prevista do Parto calculada/impressa no laudo, ou vazio)",
                    "fetalSex": "MENINO | MENINA | SURPRESA",
                    "fetalWeightGrams": 285,
                    "fetalLengthCm": 24.5,
                    "bcfBpm": 148,
                    "dbpMm": "Ex: 44 mm",
                    "ccMm": "Ex: 165 mm",
                    "caMm": "Ex: 142 mm",
                    "cfMm": "Ex: 30 mm",
                    "placenta": "Ex: Anterior, Grau 0",
                    "ila": "Ex: Normal (13,5 cm)",
                    "conclusion": "Conclusão principal do laudo de ultrassom"
                  },
                  "requestedExams": [
                    "Nome de cada exame solicitado no pedido médico"
                  ],
                  "medications": [
                    {
                      "name": "Nome do medicamento ou vitamina",
                      "dosage": "Dosagem / concentração (ex: 500mg, 400mcg)",
                      "posology": "Posologia / horários (ex: 1 comprimido após o almoço)",
                      "duration": "Duração do tratamento (ex: Uso contínuo, 30 dias)"
                    }
                  ]
                }
            """.trimIndent()

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            pages.sortedBy { it.pageIndex }.forEach { page ->
                val inlineData = JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", page.base64Jpeg)
                partsArray.put(JSONObject().put("inline_data", inlineData))
            }

            val contentsArray = JSONArray().put(
                JSONObject().put("parts", partsArray)
            )
            val generationConfig = JSONObject()
                .put("temperature", 0.15)
                .put("responseMimeType", "application/json")

            val requestBody = JSONObject()
                .put("contents", contentsArray)
                .put("generationConfig", generationConfig)
                .toString()

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$cleanKey"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            conn.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            val responseText = if (statusCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errBody = runCatching {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                throw IllegalStateException("Erro Gemini ($statusCode): ${extractGeminiErrorMessage(errBody, statusCode)}")
            }

            parseGeminiResponseToRecord(
                responseText = responseText,
                memberId = memberId,
                memberName = memberName,
                pageCount = pages.size,
                fallbackCategory = preferredCategoryHint
            )
        }
    }

    private fun extractGeminiErrorMessage(errBody: String, statusCode: Int): String {
        return runCatching {
            val msg = JSONObject(errBody).optJSONObject("error")?.optString("message").orEmpty()
            if (msg.isNotBlank()) msg else "Falha na comunicação com a API Gemini ($statusCode)"
        }.getOrDefault("Falha na comunicação com a API Gemini ($statusCode)")
    }

    private fun parseGeminiResponseToRecord(
        responseText: String,
        memberId: String,
        memberName: String,
        pageCount: Int,
        fallbackCategory: String?
    ): HealthDocumentRecord {
        val root = JSONObject(responseText)
        val rawCandidateText = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text", "")
            ?.trim()
            .orEmpty()

        val cleanedJson = rawCandidateText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .let { txt ->
                val firstBrace = txt.indexOf('{')
                val lastBrace = txt.lastIndexOf('}')
                if (firstBrace >= 0 && lastBrace > firstBrace) txt.substring(firstBrace, lastBrace + 1) else txt
            }

        val obj = JSONObject(cleanedJson)
        val category = HealthDocumentCategory.normalize(
            obj.optString("category", fallbackCategory ?: HealthDocumentCategory.EXAME)
        )
        val todayEpoch = DateCalendarUtils.todayEpochDay()
        val rawDateBr = obj.optString("dateBr", "").trim()
        val parsedEpoch = parseBrDateOrNull(rawDateBr) ?: todayEpoch
        val effectiveDateBr = if (parseBrDateOrNull(rawDateBr) != null) {
            rawDateBr
        } else {
            DateCalendarUtils.formatEpochDayPt(todayEpoch)
        }

        val markersList = mutableListOf<ExamMarkerItem>()
        obj.optJSONArray("examMarkers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                val name = m.optString("name", "").trim()
                if (name.isNotEmpty()) {
                    markersList.add(
                        ExamMarkerItem(
                            name = name,
                            value = m.optString("value", "").trim(),
                            unit = m.optString("unit", "").trim(),
                            referenceRange = m.optString("referenceRange", "").trim(),
                            status = ExamMarkerStatus.normalize(m.optString("status", "NORMAL"))
                        )
                    )
                }
            }
        }

        val usgObj = obj.optJSONObject("ultrasoundData")
        val ultrasoundData = if (usgObj != null) {
            val weeks = usgObj.optInt("weeksAtExam", usgObj.optInt("gestationalWeeks", 0)).coerceIn(0, 42)
            val days = usgObj.optInt("daysAtExam", usgObj.optInt("gestationalDays", 0)).coerceIn(0, 6)
            val rawDpp = usgObj.optString("dppDateBr", usgObj.optString("estimatedDppBr", "")).trim()
            val resolvedDpp = if (parseBrDateOrNull(rawDpp) != null) {
                rawDpp
            } else if (weeks in 4..42) {
                PregnancyCalculator.calculateDppFromUltrasound(effectiveDateBr, weeks, days).orEmpty()
            } else ""

            UltrasoundReportData(
                gestationalAgeText = usgObj.optString("gestationalAgeText", "").trim().ifBlank {
                    if (weeks > 0) "$weeks semanas e $days dias" else ""
                },
                gestationalWeeks = weeks,
                gestationalDays = days,
                estimatedDppBr = resolvedDpp,
                fetalSex = usgObj.optString("fetalSex", "").trim().uppercase(Locale.ROOT),
                estimatedFetalWeightGrams = usgObj.optInt("fetalWeightGrams", usgObj.optInt("estimatedFetalWeightGrams", 0)).coerceAtLeast(0),
                fetalLengthCm = usgObj.optDouble("fetalLengthCm", 0.0).let { if (it.isNaN()) 0.0 else it },
                fetalHeartRateBpm = usgObj.optInt("bcfBpm", usgObj.optInt("fetalHeartRateBpm", 0)).coerceAtLeast(0),
                biometryDbp = usgObj.optString("dbpMm", usgObj.optString("biometryDbp", "")).trim(),
                biometryCc = usgObj.optString("ccMm", usgObj.optString("biometryCc", "")).trim(),
                biometryCa = usgObj.optString("caMm", usgObj.optString("biometryCa", "")).trim(),
                biometryCf = usgObj.optString("cfMm", usgObj.optString("biometryCf", "")).trim(),
                placenta = usgObj.optString("placenta", "").trim(),
                amnioticFluidIla = usgObj.optString("ila", usgObj.optString("amnioticFluidIla", "")).trim(),
                conclusion = usgObj.optString("conclusion", "").trim()
            )
        } else {
            UltrasoundReportData()
        }

        val requestedList = mutableListOf<RequestedExamCheckItem>()
        obj.optJSONArray("requestedExams")?.let { arr ->
            for (i in 0 until arr.length()) {
                val itemVal = arr.opt(i)
                val examName = when (itemVal) {
                    is JSONObject -> itemVal.optString("examName", itemVal.optString("name", "")).trim()
                    is String -> itemVal.trim()
                    else -> itemVal?.toString()?.trim().orEmpty()
                }
                if (examName.isNotEmpty()) {
                    requestedList.add(RequestedExamCheckItem(name = examName, checked = false))
                }
            }
        }

        val medsList = mutableListOf<PrescriptionMedItem>()
        obj.optJSONArray("medications")?.let { arr ->
            for (i in 0 until arr.length()) {
                val med = arr.optJSONObject(i) ?: continue
                val name = med.optString("name", "").trim()
                if (name.isNotEmpty()) {
                    medsList.add(
                        PrescriptionMedItem(
                            name = name,
                            dosage = med.optString("dosage", "").trim(),
                            posology = med.optString("posology", "").trim(),
                            duration = med.optString("duration", "").trim()
                        )
                    )
                }
            }
        }

        val defaultTitle = when (category) {
            HealthDocumentCategory.ULTRASSOM -> "Ultrassom Obstétrico"
            HealthDocumentCategory.PEDIDO -> "Pedido de Exames"
            HealthDocumentCategory.RECEITA -> "Receita Médica"
            else -> "Exame Laboratorial"
        }

        val summary = obj.optString("summaryOrIndication", obj.optString("clinicalSummaryPt", "")).trim().ifBlank {
            ultrasoundData.conclusion
        }
        val docName = obj.optString("doctorName", obj.optString("doctorOrSpecialty", "")).trim()
        val docSpec = obj.optString("doctorSpecialty", "").trim()

        return HealthDocumentRecord(
            category = category,
            memberId = memberId,
            memberName = memberName,
            title = obj.optString("title", defaultTitle).trim().ifBlank { defaultTitle },
            dateBr = effectiveDateBr,
            dateEpochDay = parsedEpoch,
            clinicOrLab = obj.optString("clinicOrLab", "").trim(),
            doctorName = docName,
            doctorSpecialty = docSpec,
            clinicalSummaryPt = summary,
            clinicalIndication = if (category == HealthDocumentCategory.PEDIDO) summary else "",
            examMarkers = markersList,
            ultrasoundData = ultrasoundData,
            requestedExams = requestedList,
            orderStatus = MedicalOrderStatus.PENDENTE,
            prescribedMeds = medsList,
            prescriptionStatus = PrescriptionStatus.EM_USO,
            pageCount = pageCount,
            extractedByAi = true
        )
    }

    fun parseBrDateOrNull(raw: String): Long? {
        val parts = raw.trim().split("/", "-", ".")
        if (parts.size < 3) return null
        val d = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val y = parts[2].toIntOrNull() ?: return null
        if (y !in 2000..2100 || m !in 1..12 || d !in 1..31) return null
        return DateCalendarUtils.toEpochDay(y, m, d)
    }

    fun parseDateBrToEpochDay(raw: String): Long {
        return parseBrDateOrNull(raw) ?: DateCalendarUtils.todayEpochDay()
    }
}

/**
 * Repositório da Central de Documentos Médicos com persistência local + sincronização em tempo real
 * na subcoleção `households/{familyId}/health_documents` e páginas Base64 em `pages/{pageIndex}`.
 */
class MedicalDocumentsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _documentsFlow = MutableStateFlow(loadDocumentsFromPrefs())
    val documentsFlow: StateFlow<List<HealthDocumentRecord>> = _documentsFlow.asStateFlow()

    private fun getNamedFirestore(): FirebaseFirestore? {
        return runCatching {
            val dbId = appContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }.getOrNull()
    }

    private fun resolveActiveHouseholdId(explicitHouseholdId: String = ""): String? {
        if (explicitHouseholdId.isNotBlank()) return explicitHouseholdId
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
        if (uid.isNullOrBlank()) return null
        val localHouseId = appContext.getSharedPreferences("aura_local_offline_store", Context.MODE_PRIVATE)
            .getString("household_id", null)
            .orEmpty()
        return localHouseId.takeIf { it.isNotBlank() } ?: OFFICIAL_HOUSEHOLD_ID
    }

    init {
        startRealtimeSyncIfAuthenticated()
    }

    fun attachHouseholdRealtimeSync(householdId: String) {
        startRealtimeSyncIfAuthenticated(householdId)
    }

    private fun startRealtimeSyncIfAuthenticated(explicitHouseholdId: String = "") {
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
        val db = getNamedFirestore()
        if (uid.isNullOrBlank() || db == null) return
        val houseId = resolveActiveHouseholdId(explicitHouseholdId) ?: return
        if (activeSyncedHouseholdId == houseId && docsListener != null) return

        docsListener?.remove()
        activeSyncedHouseholdId = houseId

        docsListener = db.collection("households")
            .document(houseId)
            .collection("health_documents")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val remoteDocs = snap.documents.mapNotNull { doc ->
                    parseFirestoreDocToRecord(doc.id, houseId, doc.data ?: return@mapNotNull null)
                }
                if (remoteDocs.isNotEmpty() || snap.metadata.isFromCache.not()) {
                    mergeRemoteDocumentsIntoLocal(remoteDocs)
                }
            }
    }

    fun refreshRealtimeBinding() {
        startRealtimeSyncIfAuthenticated()
    }

    private fun mergeRemoteDocumentsIntoLocal(remoteDocs: List<HealthDocumentRecord>) {
        val currentById = _documentsFlow.value.associateBy { it.id }.toMutableMap()
        var changed = false
        for (remote in remoteDocs) {
            val existing = currentById[remote.id]
            if (existing == null || existing != remote) {
                currentById[remote.id] = remote
                changed = true
            }
        }
        if (changed) {
            val sorted = currentById.values.sortedWith(
                compareByDescending<HealthDocumentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )
            saveDocumentsToPrefs(sorted)
        }
    }

    fun saveOrUpdateDocument(
        record: HealthDocumentRecord,
        stagedPages: List<StagedDocumentPage> = emptyList(),
        explicitHouseholdId: String = ""
    ): HealthDocumentRecord {
        return saveDocumentWithPages(record, stagedPages, explicitHouseholdId)
    }

    fun saveDocumentWithPages(
        record: HealthDocumentRecord,
        pages: List<StagedDocumentPage> = emptyList(),
        explicitHouseholdId: String = ""
    ): HealthDocumentRecord {
        startRealtimeSyncIfAuthenticated(explicitHouseholdId)
        val houseId = resolveActiveHouseholdId(explicitHouseholdId).orEmpty()
        val effectivePageCount = if (pages.isNotEmpty()) pages.size else record.pageCount
        val cleanRecord = record.copy(
            householdId = houseId.ifBlank { record.householdId },
            category = HealthDocumentCategory.normalize(record.category),
            pageCount = effectivePageCount,
            updatedAtMillis = System.currentTimeMillis()
        )

        if (pages.isNotEmpty()) {
            savePagesToLocalDisk(cleanRecord.id, pages)
        }

        val updatedList = (_documentsFlow.value.filterNot { it.id == cleanRecord.id } + cleanRecord)
            .sortedWith(
                compareByDescending<HealthDocumentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )
        saveDocumentsToPrefs(updatedList)
        pushDocumentToFirestore(cleanRecord, pages, houseId)
        return cleanRecord
    }

    fun toggleRequestedExamChecked(
        docId: String,
        examId: String,
        explicitHouseholdId: String = ""
    ) {
        val existing = _documentsFlow.value.firstOrNull { it.id == docId } ?: return
        val updatedExams = existing.requestedExams.map { req ->
            if (req.id == examId) req.copy(checked = !req.checked) else req
        }
        val allChecked = updatedExams.isNotEmpty() && updatedExams.all { it.checked }
        val nextStatus = if (allChecked) MedicalOrderStatus.REALIZADO else existing.orderStatus
        val updatedDoc = existing.copy(
            requestedExams = updatedExams,
            orderStatus = nextStatus,
            updatedAtMillis = System.currentTimeMillis()
        )
        saveDocumentWithPages(updatedDoc, emptyList(), explicitHouseholdId)
    }

    fun deleteDocument(docId: String, explicitHouseholdId: String = "") {
        val updated = _documentsFlow.value.filterNot { it.id == docId }
        saveDocumentsToPrefs(updated)
        deletePagesFromLocalDisk(docId)

        val db = getNamedFirestore() ?: return
        val houseId = resolveActiveHouseholdId(explicitHouseholdId) ?: return
        ioScope.launch {
            runCatching {
                val docRef = db.collection("households")
                    .document(houseId)
                    .collection("health_documents")
                    .document(docId)
                val pagesSnap = docRef.collection("pages").get().await()
                pagesSnap.documents.forEach { pg ->
                    runCatching { pg.reference.delete().await() }
                }
                docRef.delete().await()
            }
        }
    }

    suspend fun loadPagesForDocument(
        docId: String,
        explicitHouseholdId: String = ""
    ): List<StagedDocumentPage> {
        val expectedCount = _documentsFlow.value.firstOrNull { it.id == docId }?.pageCount ?: 1
        return loadOriginalPagesForDocument(docId, expectedCount, explicitHouseholdId)
    }

    suspend fun loadOriginalPagesForDocument(
        docId: String,
        expectedPageCount: Int,
        explicitHouseholdId: String = ""
    ): List<StagedDocumentPage> = withContext(Dispatchers.IO) {
        val localPages = loadPagesFromLocalDisk(docId)
        if (localPages.isNotEmpty() && localPages.size >= expectedPageCount.coerceAtLeast(1)) {
            return@withContext localPages
        }
        val db = getNamedFirestore() ?: return@withContext localPages
        val houseId = resolveActiveHouseholdId(explicitHouseholdId) ?: return@withContext localPages
        val fetched = runCatching {
            val snap = db.collection("households")
                .document(houseId)
                .collection("health_documents")
                .document(docId)
                .collection("pages")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val base64 = doc.getString("base64Jpeg").orEmpty()
                if (base64.isBlank()) null
                else StagedDocumentPage(
                    id = doc.id,
                    pageIndex = (doc.getLong("pageIndex") ?: 0L).toInt(),
                    base64Jpeg = base64,
                    sizeBytes = (doc.getLong("sizeBytes") ?: 0L).toInt(),
                    widthPx = (doc.getLong("widthPx") ?: 0L).toInt(),
                    heightPx = (doc.getLong("heightPx") ?: 0L).toInt()
                )
            }.sortedBy { it.pageIndex }
        }.getOrElse { emptyList() }

        if (fetched.isNotEmpty()) {
            savePagesToLocalDisk(docId, fetched)
            fetched
        } else {
            localPages
        }
    }

    /**
     * Busca o marcador correspondente no exame anterior da mesma pessoa para exibir
     * comparação evolutiva automática (ex: "Anterior (10/10/2026): 94 mg/dL").
     */
    fun findPreviousMarkerForMember(
        currentDocId: String,
        memberName: String,
        markerName: String,
        currentDateEpochDay: Long
    ): Pair<HealthDocumentRecord, ExamMarkerItem>? {
        val cleanTarget = markerName.trim().lowercase(Locale.ROOT)
        if (cleanTarget.isBlank()) return null

        val previousExams = _documentsFlow.value
            .filter { doc ->
                doc.id != currentDocId &&
                    doc.category == HealthDocumentCategory.EXAME &&
                    doc.memberName.equals(memberName, ignoreCase = true) &&
                    doc.dateEpochDay <= currentDateEpochDay
            }
            .sortedWith(
                compareByDescending<HealthDocumentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )

        for (exam in previousExams) {
            val match = exam.examMarkers.firstOrNull { m ->
                val candidate = m.name.trim().lowercase(Locale.ROOT)
                candidate == cleanTarget ||
                    (cleanTarget.length >= 4 && (candidate.contains(cleanTarget) || cleanTarget.contains(candidate)))
            }
            if (match != null) {
                return exam to match
            }
        }
        return null
    }

    /**
     * Busca o ultrassom imediatamente anterior para comparação de evolução fetal
     * (peso em gramas, BCF e biometria).
     */
    fun findPreviousUltrasound(
        currentDocId: String,
        currentDateEpochDay: Long
    ): HealthDocumentRecord? {
        return _documentsFlow.value
            .filter { doc ->
                doc.id != currentDocId &&
                    doc.category == HealthDocumentCategory.ULTRASSOM &&
                    doc.dateEpochDay <= currentDateEpochDay
            }
            .sortedWith(
                compareByDescending<HealthDocumentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )
            .firstOrNull()
    }

    private fun pushDocumentToFirestore(
        record: HealthDocumentRecord,
        pages: List<StagedDocumentPage>,
        explicitHouseholdId: String = ""
    ) {
        val db = getNamedFirestore() ?: return
        val houseId = resolveActiveHouseholdId(explicitHouseholdId) ?: return
        ioScope.launch {
            runCatching {
                val docRef = db.collection("households")
                    .document(houseId)
                    .collection("health_documents")
                    .document(record.id)

                docRef.set(recordToFirestoreMap(record, houseId), SetOptions.merge()).await()

                if (pages.isNotEmpty()) {
                    pages.forEachIndexed { idx, pg ->
                        if (pg.base64Jpeg.length <= 650 * 1024) {
                            val pageDocId = "page_${pg.pageIndex.coerceAtLeast(idx)}"
                            val pageMap = mapOf(
                                "pageIndex" to pg.pageIndex.coerceAtLeast(idx),
                                "base64Jpeg" to pg.base64Jpeg,
                                "sizeBytes" to pg.sizeBytes,
                                "widthPx" to pg.widthPx,
                                "heightPx" to pg.heightPx,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                            docRef.collection("pages")
                                .document(pageDocId)
                                .set(pageMap, SetOptions.merge())
                                .await()
                        }
                    }
                }
            }
        }
    }

    private fun savePagesToLocalDisk(docId: String, pages: List<StagedDocumentPage>) {
        runCatching {
            val dir = File(appContext.filesDir, "medical_doc_pages/$docId").apply {
                if (!exists()) mkdirs()
            }
            pages.forEachIndexed { idx, pg ->
                val pageFile = File(dir, "page_${pg.pageIndex.coerceAtLeast(idx)}.json")
                val obj = JSONObject().apply {
                    put("pageIndex", pg.pageIndex.coerceAtLeast(idx))
                    put("base64Jpeg", pg.base64Jpeg)
                    put("sizeBytes", pg.sizeBytes)
                    put("widthPx", pg.widthPx)
                    put("heightPx", pg.heightPx)
                }
                pageFile.writeText(obj.toString(), Charsets.UTF_8)
            }
        }
    }

    private fun loadPagesFromLocalDisk(docId: String): List<StagedDocumentPage> {
        return runCatching {
            val dir = File(appContext.filesDir, "medical_doc_pages/$docId")
            if (!dir.exists()) return emptyList()
            dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.mapNotNull { file ->
                    runCatching {
                        val obj = JSONObject(file.readText(Charsets.UTF_8))
                        val b64 = obj.optString("base64Jpeg", "")
                        if (b64.isBlank()) null
                        else StagedDocumentPage(
                            id = file.nameWithoutExtension,
                            pageIndex = obj.optInt("pageIndex", 0),
                            base64Jpeg = b64,
                            sizeBytes = obj.optInt("sizeBytes", 0),
                            widthPx = obj.optInt("widthPx", 0),
                            heightPx = obj.optInt("heightPx", 0)
                        )
                    }.getOrNull()
                }
                ?.sortedBy { it.pageIndex }
                .orEmpty()
        }.getOrElse { emptyList() }
    }

    private fun deletePagesFromLocalDisk(docId: String) {
        runCatching {
            val dir = File(appContext.filesDir, "medical_doc_pages/$docId")
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    private fun saveDocumentsToPrefs(list: List<HealthDocumentRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(recordToJson(it)) }
        prefs.edit().putString(KEY_DOCUMENTS_JSON, arr.toString()).apply()
        _documentsFlow.value = list
    }

    private fun loadDocumentsFromPrefs(): List<HealthDocumentRecord> {
        val raw = prefs.getString(KEY_DOCUMENTS_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(jsonToRecord(obj))
                }
            }.sortedWith(
                compareByDescending<HealthDocumentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )
        }.getOrElse { emptyList() }
    }

    fun mergeImportedDocuments(imported: List<HealthDocumentRecord>): Int =
        mergeImportedDocumentsMetadata(imported)

    fun mergeImportedDocumentsMetadata(imported: List<HealthDocumentRecord>): Int {
        if (imported.isEmpty()) return 0
        val currentById = _documentsFlow.value.associateBy { it.id }.toMutableMap()
        var added = 0
        for (doc in imported) {
            if (doc.id !in currentById) {
                currentById[doc.id] = doc
                added++
                pushDocumentToFirestore(doc, emptyList())
            }
        }
        if (added > 0) {
            val sorted = currentById.values.sortedWith(
                compareByDescending<HealthDocumentRecord> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )
            saveDocumentsToPrefs(sorted)
        }
        return added
    }

    companion object {
        private const val PREFS_NAME = "omni_medical_documents_prefs"
        private const val KEY_DOCUMENTS_JSON = "medical_documents_list_json"
        private const val OFFICIAL_HOUSEHOLD_ID = "FAM_OMNI2448"

        @Volatile
        private var instance: MedicalDocumentsRepository? = null

        @Volatile
        private var docsListener: ListenerRegistration? = null

        @Volatile
        private var activeSyncedHouseholdId: String? = null

        fun getInstance(context: Context): MedicalDocumentsRepository {
            return instance ?: synchronized(this) {
                instance ?: MedicalDocumentsRepository(context.applicationContext).also { instance = it }
            }
        }

        fun parseRecordFromJson(obj: JSONObject): HealthDocumentRecord = jsonToRecord(obj)

        fun recordToJson(r: HealthDocumentRecord): JSONObject {
            return JSONObject().apply {
                put("id", r.id)
                put("householdId", r.householdId)
                put("category", r.category)
                put("memberId", r.memberId)
                put("memberName", r.memberName)
                put("title", r.title)
                put("dateBr", r.dateBr)
                put("dateEpochDay", r.dateEpochDay)
                put("clinicOrLab", r.clinicOrLab)
                put("doctorName", r.doctorName)
                put("doctorSpecialty", r.doctorSpecialty)
                put("doctorOrSpecialty", r.doctorOrSpecialty)
                put("clinicalSummaryPt", r.clinicalSummaryPt)
                put("clinicalIndication", r.clinicalIndication)
                put("summaryOrIndication", r.summaryOrIndication)
                put("notes", r.notes)
                put("orderStatus", r.orderStatus)
                put("scheduledDateBr", r.scheduledDateBr)
                put("scheduledDateEpochDay", r.scheduledDateEpochDay)
                put("scheduledEpochDay", r.scheduledDateEpochDay)
                put("scheduledTimeHHmm", r.scheduledTimeHHmm)
                put("scheduledLocation", r.scheduledLocation)
                put("linkedTaskId", r.linkedTaskId)
                put("prescriptionStatus", r.prescriptionStatus)
                put("pageCount", r.pageCount)
                put("extractedByAi", r.extractedByAi)
                put("aiExtracted", r.extractedByAi)
                put("createdAtMillis", r.createdAtMillis)
                put("updatedAtMillis", r.updatedAtMillis)

                val markersArr = JSONArray()
                r.examMarkers.forEach { m ->
                    markersArr.put(
                        JSONObject().apply {
                            put("name", m.name)
                            put("value", m.value)
                            put("unit", m.unit)
                            put("referenceRange", m.referenceRange)
                            put("status", m.status)
                        }
                    )
                }
                put("examMarkers", markersArr)

                val usg = r.ultrasoundData
                put(
                    "ultrasoundData",
                    JSONObject().apply {
                        put("gestationalAgeText", usg.gestationalAgeText)
                        put("gestationalWeeks", usg.gestationalWeeks)
                        put("weeksAtExam", usg.gestationalWeeks)
                        put("gestationalDays", usg.gestationalDays)
                        put("daysAtExam", usg.gestationalDays)
                        put("estimatedDppBr", usg.estimatedDppBr)
                        put("dppDateBr", usg.estimatedDppBr)
                        put("fetalSex", usg.fetalSex)
                        put("estimatedFetalWeightGrams", usg.estimatedFetalWeightGrams)
                        put("fetalWeightGrams", usg.estimatedFetalWeightGrams)
                        put("fetalLengthCm", usg.fetalLengthCm)
                        put("fetalHeartRateBpm", usg.fetalHeartRateBpm)
                        put("bcfBpm", usg.fetalHeartRateBpm)
                        put("biometryDbp", usg.biometryDbp)
                        put("dbpMm", usg.biometryDbp)
                        put("biometryCc", usg.biometryCc)
                        put("ccMm", usg.biometryCc)
                        put("biometryCa", usg.biometryCa)
                        put("caMm", usg.biometryCa)
                        put("biometryCf", usg.biometryCf)
                        put("cfMm", usg.biometryCf)
                        put("placenta", usg.placenta)
                        put("amnioticFluidIla", usg.amnioticFluidIla)
                        put("ila", usg.amnioticFluidIla)
                        put("conclusion", usg.conclusion)
                    }
                )

                val reqArr = JSONArray()
                r.requestedExams.forEach { req ->
                    reqArr.put(
                        JSONObject().apply {
                            put("id", req.id)
                            put("name", req.name)
                            put("examName", req.name)
                            put("notes", req.notes)
                            put("checked", req.checked)
                            put("done", req.checked)
                        }
                    )
                }
                put("requestedExams", reqArr)

                val medArr = JSONArray()
                r.prescribedMeds.forEach { med ->
                    medArr.put(
                        JSONObject().apply {
                            put("id", med.id)
                            put("name", med.name)
                            put("dosage", med.dosage)
                            put("posology", med.posology)
                            put("duration", med.duration)
                        }
                    )
                }
                put("medications", medArr)
            }
        }

        fun jsonToRecord(obj: JSONObject): HealthDocumentRecord {
            val markers = mutableListOf<ExamMarkerItem>()
            obj.optJSONArray("examMarkers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    markers.add(
                        ExamMarkerItem(
                            name = m.optString("name", ""),
                            value = m.optString("value", ""),
                            unit = m.optString("unit", ""),
                            referenceRange = m.optString("referenceRange", ""),
                            status = ExamMarkerStatus.normalize(m.optString("status", "NORMAL"))
                        )
                    )
                }
            }
            val usgObj = obj.optJSONObject("ultrasoundData")
            val usg = if (usgObj != null) {
                UltrasoundReportData(
                    gestationalAgeText = usgObj.optString("gestationalAgeText", ""),
                    gestationalWeeks = usgObj.optInt("gestationalWeeks", usgObj.optInt("weeksAtExam", 0)),
                    gestationalDays = usgObj.optInt("gestationalDays", usgObj.optInt("daysAtExam", 0)),
                    estimatedDppBr = usgObj.optString("estimatedDppBr", usgObj.optString("dppDateBr", "")),
                    fetalSex = usgObj.optString("fetalSex", ""),
                    estimatedFetalWeightGrams = usgObj.optInt("estimatedFetalWeightGrams", usgObj.optInt("fetalWeightGrams", 0)),
                    fetalLengthCm = usgObj.optDouble("fetalLengthCm", 0.0).let { if (it.isNaN()) 0.0 else it },
                    fetalHeartRateBpm = usgObj.optInt("fetalHeartRateBpm", usgObj.optInt("bcfBpm", 0)),
                    biometryDbp = usgObj.optString("biometryDbp", usgObj.optString("dbpMm", "")),
                    biometryCc = usgObj.optString("biometryCc", usgObj.optString("ccMm", "")),
                    biometryCa = usgObj.optString("biometryCa", usgObj.optString("caMm", "")),
                    biometryCf = usgObj.optString("biometryCf", usgObj.optString("cfMm", "")),
                    placenta = usgObj.optString("placenta", ""),
                    amnioticFluidIla = usgObj.optString("amnioticFluidIla", usgObj.optString("ila", "")),
                    conclusion = usgObj.optString("conclusion", "")
                )
            } else UltrasoundReportData()

            val reqs = mutableListOf<RequestedExamCheckItem>()
            obj.optJSONArray("requestedExams")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i) ?: continue
                    reqs.add(
                        RequestedExamCheckItem(
                            id = r.optString("id", "req_$i"),
                            name = r.optString("name", r.optString("examName", "")),
                            notes = r.optString("notes", ""),
                            checked = r.optBoolean("checked", r.optBoolean("done", false))
                        )
                    )
                }
            }

            val meds = mutableListOf<PrescriptionMedItem>()
            obj.optJSONArray("medications")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    meds.add(
                        PrescriptionMedItem(
                            id = m.optString("id", "med_$i"),
                            name = m.optString("name", ""),
                            dosage = m.optString("dosage", ""),
                            posology = m.optString("posology", ""),
                            duration = m.optString("duration", "")
                        )
                    )
                }
            }

            val summaryVal = obj.optString("clinicalSummaryPt", obj.optString("summaryOrIndication", ""))
            val indicationVal = obj.optString("clinicalIndication", obj.optString("summaryOrIndication", ""))
            val docNameVal = obj.optString("doctorName", obj.optString("doctorOrSpecialty", ""))

            return HealthDocumentRecord(
                id = obj.optString("id", "hdoc_" + UUID.randomUUID().toString().take(8)),
                householdId = obj.optString("householdId", ""),
                category = HealthDocumentCategory.normalize(obj.optString("category", HealthDocumentCategory.EXAME)),
                memberId = obj.optString("memberId", ""),
                memberName = obj.optString("memberName", "Nathalia"),
                title = obj.optString("title", ""),
                dateBr = obj.optString("dateBr", ""),
                dateEpochDay = obj.optLong("dateEpochDay", DateCalendarUtils.todayEpochDay()),
                clinicOrLab = obj.optString("clinicOrLab", ""),
                doctorName = docNameVal,
                doctorSpecialty = obj.optString("doctorSpecialty", ""),
                clinicalSummaryPt = summaryVal,
                clinicalIndication = indicationVal,
                notes = obj.optString("notes", ""),
                examMarkers = markers,
                ultrasoundData = usg,
                requestedExams = reqs,
                orderStatus = MedicalOrderStatus.normalize(obj.optString("orderStatus", MedicalOrderStatus.PENDENTE)),
                scheduledDateBr = obj.optString("scheduledDateBr", ""),
                scheduledDateEpochDay = obj.optLong("scheduledDateEpochDay", obj.optLong("scheduledEpochDay", 0L)),
                scheduledTimeHHmm = obj.optString("scheduledTimeHHmm", ""),
                scheduledLocation = obj.optString("scheduledLocation", ""),
                linkedTaskId = obj.optString("linkedTaskId", ""),
                prescribedMeds = meds,
                prescriptionStatus = PrescriptionStatus.normalize(obj.optString("prescriptionStatus", PrescriptionStatus.EM_USO)),
                pageCount = obj.optInt("pageCount", 0),
                extractedByAi = obj.optBoolean("extractedByAi", obj.optBoolean("aiExtracted", false)),
                createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
            )
        }

        fun recordToFirestoreMap(r: HealthDocumentRecord, householdId: String): Map<String, Any> {
            return mapOf(
                "id" to r.id,
                "householdId" to householdId,
                "category" to r.category,
                "memberId" to r.memberId,
                "memberName" to r.memberName,
                "title" to r.title,
                "dateBr" to r.dateBr,
                "dateEpochDay" to r.dateEpochDay,
                "clinicOrLab" to r.clinicOrLab,
                "doctorOrSpecialty" to r.doctorOrSpecialty,
                "summaryOrIndication" to r.summaryOrIndication,
                "orderStatus" to r.orderStatus,
                "scheduledDateBr" to r.scheduledDateBr,
                "scheduledEpochDay" to r.scheduledDateEpochDay,
                "scheduledTimeHHmm" to r.scheduledTimeHHmm,
                "scheduledLocation" to r.scheduledLocation,
                "linkedTaskId" to r.linkedTaskId,
                "prescriptionStatus" to r.prescriptionStatus,
                "pageCount" to r.pageCount,
                "aiExtracted" to r.extractedByAi,
                "createdAtMillis" to r.createdAtMillis,
                "payloadJson" to recordToJson(r).toString(),
                "serializedPayloadJson" to recordToJson(r).toString(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }

        fun parseFirestoreDocToRecord(docId: String, householdId: String, data: Map<String, Any?>): HealthDocumentRecord? {
            val serialized = (data["serializedPayloadJson"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: (data["payloadJson"] as? String)
            if (!serialized.isNullOrBlank()) {
                return runCatching {
                    jsonToRecord(JSONObject(serialized)).copy(id = docId, householdId = householdId)
                }.getOrNull()
            }
            val title = (data["title"] as? String)?.trim().orEmpty()
            if (title.isBlank()) return null
            return HealthDocumentRecord(
                id = docId,
                householdId = householdId,
                category = HealthDocumentCategory.normalize((data["category"] as? String).orEmpty()),
                memberId = (data["memberId"] as? String).orEmpty(),
                memberName = (data["memberName"] as? String).orEmpty().ifBlank { "Nathalia" },
                title = title,
                dateBr = (data["dateBr"] as? String).orEmpty(),
                dateEpochDay = (data["dateEpochDay"] as? Number)?.toLong() ?: DateCalendarUtils.todayEpochDay(),
                clinicOrLab = (data["clinicOrLab"] as? String).orEmpty(),
                doctorName = (data["doctorOrSpecialty"] as? String).orEmpty(),
                clinicalSummaryPt = (data["summaryOrIndication"] as? String).orEmpty(),
                orderStatus = MedicalOrderStatus.normalize((data["orderStatus"] as? String).orEmpty()),
                scheduledDateBr = (data["scheduledDateBr"] as? String).orEmpty(),
                scheduledDateEpochDay = (data["scheduledEpochDay"] as? Number)?.toLong() ?: 0L,
                scheduledTimeHHmm = (data["scheduledTimeHHmm"] as? String).orEmpty(),
                scheduledLocation = (data["scheduledLocation"] as? String).orEmpty(),
                linkedTaskId = (data["linkedTaskId"] as? String).orEmpty(),
                prescriptionStatus = PrescriptionStatus.normalize((data["prescriptionStatus"] as? String).orEmpty()),
                pageCount = (data["pageCount"] as? Number)?.toInt() ?: 0,
                extractedByAi = (data["aiExtracted"] as? Boolean) ?: false,
                createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
