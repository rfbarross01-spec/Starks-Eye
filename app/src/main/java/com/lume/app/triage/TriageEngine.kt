package com.lume.app.triage

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Triagem on-device: extrai OCR + labels do screenshot ANTES de mandar pra IA.
 *
 * Benefícios:
 * - Texto extraído ajuda Camada 1 a entender melhor (mesmo Gemini com vision se beneficia)
 * - Labels permitem heurística rápida (ex: detectar "FLAG_SECURE" telas sensíveis)
 * - 100% on-device, zero custo, gratuito, sem rede
 */
class TriageEngine {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
    )

    suspend fun triage(bitmap: Bitmap): TriageResult {
        val ocrText = extractOcr(bitmap)
        val labels = extractLabels(bitmap)

        val isSensitive = detectSensitive(labels, ocrText)
        val isTrivial = detectTrivial(labels, ocrText)

        return TriageResult(
            ocrText = ocrText,
            labels = labels,
            isSensitive = isSensitive,
            isTrivial = isTrivial
        )
    }

    private suspend fun extractOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result.text)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume("")
            }
    }

    private suspend fun extractLabels(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(image)
            .addOnSuccessListener { labels ->
                if (cont.isActive) {
                    cont.resume(labels.take(8).map { it.text })
                }
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(emptyList())
            }
    }

    private fun detectSensitive(labels: List<String>, ocr: String): Boolean {
        val ocrLower = ocr.lowercase()
        val sensitiveKeywords = listOf(
            "senha", "password", "cartão de crédito", "cvv", "code:",
            "verificação em duas etapas", "two-factor", "transferência",
            "saldo:", "balance:", "cpf:", "rg:"
        )
        return sensitiveKeywords.any { ocrLower.contains(it) }
    }

    private fun detectTrivial(labels: List<String>, ocr: String): Boolean {
        // Screenshots quase sem texto E sem objetos identificáveis = trivial
        return ocr.trim().length < 20 && labels.isEmpty()
    }

    fun close() {
        textRecognizer.close()
        imageLabeler.close()
    }
}

data class TriageResult(
    val ocrText: String,
    val labels: List<String>,
    val isSensitive: Boolean,
    val isTrivial: Boolean
)
