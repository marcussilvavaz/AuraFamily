package com.example.alarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class TaskRingtoneOption(
    val id: String,
    val categoryLabel: String,
    val title: String,
    val subtitle: String
)

object TaskRingtoneCatalog {
    const val SUAVE = "SUAVE"
    const val FOCO = "FOCO"
    const val URGENTE = "URGENTE"
    const val CONTAS = "CONTAS"
    const val DISCRETO = "DISCRETO"

    val allOptions: List<TaskRingtoneOption> = listOf(
        TaskRingtoneOption(
            id = SUAVE,
            categoryLabel = "Suave",
            title = "Harpa da Manhã",
            subtitle = "Acorde harmônico ascendente e acolhedor"
        ),
        TaskRingtoneOption(
            id = FOCO,
            categoryLabel = "Foco",
            title = "Pulso Cristal",
            subtitle = "Toque claro e ritmado para início de bloco de foco"
        ),
        TaskRingtoneOption(
            id = URGENTE,
            categoryLabel = "Urgente / Alarme",
            title = "Despertar Intenso",
            subtitle = "Sequência vibrante de alta prioridade"
        ),
        TaskRingtoneOption(
            id = CONTAS,
            categoryLabel = "Contas",
            title = "Sino Financeiro",
            subtitle = "Duplo toque metálico para vencimento de contas"
        ),
        TaskRingtoneOption(
            id = DISCRETO,
            categoryLabel = "Discreto",
            title = "Gota Minimalista",
            subtitle = "Bipe curto e silencioso para ambientes compartilhados"
        )
    )

    fun findById(id: String): TaskRingtoneOption {
        return allOptions.firstOrNull { it.id == id } ?: allOptions.first()
    }
}

/**
 * Real-time PCM audio synthesizer (Android equivalent of Web Audio API oscillator preview)
 * that plays distinct melodic envelopes for Suave, Foco, Urgente/Alarme, Contas, and Discreto.
 */
object TaskTonePreviewPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var activeJob: Job? = null

    private data class NoteSpec(
        val freqHz: Double,
        val durationMs: Int,
        val pauseAfterMs: Int = 25,
        val decayFactor: Double = 3.5
    )

    fun playPreview(ringtoneId: String) {
        activeJob?.cancel()
        activeJob = scope.launch {
            runCatching {
                val notes = when (ringtoneId) {
                    TaskRingtoneCatalog.SUAVE -> listOf(
                        NoteSpec(523.25, 190, 20, 2.8), // C5
                        NoteSpec(659.25, 190, 20, 2.8), // E5
                        NoteSpec(783.99, 280, 0, 2.2)   // G5
                    )
                    TaskRingtoneCatalog.FOCO -> listOf(
                        NoteSpec(587.33, 130, 35, 4.0), // D5
                        NoteSpec(880.00, 240, 0, 3.0)   // A5
                    )
                    TaskRingtoneCatalog.URGENTE -> listOf(
                        NoteSpec(880.00, 110, 25, 1.8), // A5
                        NoteSpec(987.77, 110, 25, 1.8), // B5
                        NoteSpec(880.00, 110, 25, 1.8), // A5
                        NoteSpec(1174.66, 220, 0, 1.5)  // D6
                    )
                    TaskRingtoneCatalog.CONTAS -> listOf(
                        NoteSpec(659.25, 120, 30, 3.5), // E5
                        NoteSpec(783.99, 120, 30, 3.5), // G5
                        NoteSpec(1046.50, 260, 0, 2.5)  // C6
                    )
                    TaskRingtoneCatalog.DISCRETO -> listOf(
                        NoteSpec(440.00, 140, 0, 5.5)   // A4 soft drop
                    )
                    else -> listOf(
                        NoteSpec(523.25, 180, 20, 3.0),
                        NoteSpec(659.25, 220, 0, 3.0)
                    )
                }
                synthesizeAndPlay(notes)
            }
        }
    }

    private fun synthesizeAndPlay(notes: List<NoteSpec>) {
        val sampleRate = 22050
        val totalMs = notes.sumOf { it.durationMs + it.pauseAfterMs }.coerceAtLeast(100)
        val totalSamples = (sampleRate * totalMs) / 1000
        val buffer = ShortArray(totalSamples)

        var cursor = 0
        for (note in notes) {
            val noteSamples = (sampleRate * note.durationMs) / 1000
            for (i in 0 until noteSamples) {
                if (cursor >= totalSamples) break
                val t = i.toDouble() / sampleRate.toDouble()
                val progress = i.toDouble() / noteSamples.toDouble()
                // Smooth attack + exponential decay envelope
                val attack = (i.toDouble() / (sampleRate * 0.012)).coerceAtMost(1.0)
                val envelope = attack * exp(-note.decayFactor * progress)
                val fundamental = sin(2.0 * PI * note.freqHz * t)
                val harmonic = 0.25 * sin(2.0 * PI * (note.freqHz * 2.0) * t)
                val sampleVal = ((fundamental + harmonic) * 0.55 * envelope * Short.MAX_VALUE).toInt()
                buffer[cursor++] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            val pauseSamples = (sampleRate * note.pauseAfterMs) / 1000
            cursor = (cursor + pauseSamples).coerceAtMost(totalSamples)
        }

        val byteCount = buffer.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(byteCount.coerceAtLeast(AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep((totalMs + 60).toLong())
        track.stop()
        track.release()
    }
}
