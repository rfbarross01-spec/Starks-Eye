package com.lume.app.ai.clients

import android.util.Base64
import com.lume.app.ai.models.CaptureContext
import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.prompts.ContextAdapter
import com.lume.app.ai.prompts.PromptStore
import com.lume.app.data.KeyStore
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Cliente Gemini 2.5 Flash pra Camada 1.
 * Recebe imagem + OCR + prompt SOTA, retorna Layer1Result em JSON.
 */
class GeminiClient(
    private val keyStore: KeyStore,
    private val promptStore: PromptStore,
    private val model: String = "gemini-2.5-flash",
    private val httpClient: HttpClient = HttpClientFactory.create(timeoutMs = 30_000)
) {

    suspend fun analyzeLayer1(ctx: CaptureContext): Result<Layer1Result> = runCatching {
        val apiKey = keyStore.getGeminiKey() ?: error("Chave Gemini não configurada")
        val prompt = ContextAdapter.adaptLayer1Prompt(
            promptStore.getLayer1Prompt(),
            userQuestion = ctx.userQuestion
        )
        val imageB64 = Base64.encodeToString(ctx.imageBytes, Base64.NO_WRAP)

        val response = httpClient.post(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        ) {
            url { parameters.append("key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(buildRequestBody(prompt, ctx, imageB64).toString())
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            error("Gemini ${response.status.value}: ${body.take(500)}")
        }

        val responseBody = response.bodyAsText()
        val root = HttpClientFactory.JSON.parseToJsonElement(responseBody).jsonObject

        val candidates = root["candidates"]?.jsonArray ?: error("Sem candidates na resposta")
        if (candidates.isEmpty()) error("Resposta vazia do Gemini")

        val content = candidates[0].jsonObject["content"]?.jsonObject
            ?: error("Sem content na resposta")
        val parts = content["parts"]?.jsonArray ?: error("Sem parts na resposta")
        val rawText = parts[0].jsonObject["text"]?.jsonPrimitive?.content
            ?: error("Sem texto na resposta")

        val jsonText = JsonExtractor.extract(rawText)
            ?: error("Não consegui extrair JSON da resposta:\n${rawText.take(300)}")

        JsonExtractor.LENIENT_JSON.decodeFromString<Layer1Result>(jsonText)
    }

    private fun buildRequestBody(
        prompt: String,
        ctx: CaptureContext,
        imageB64: String
    ): JsonObject = buildJsonObject {

        putJsonObject("systemInstruction") {
            putJsonArray("parts") {
                addJsonObject { put("text", prompt) }
            }
        }

        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    addJsonObject {
                        putJsonObject("inline_data") {
                            put("mime_type", ctx.mimeType)
                            put("data", imageB64)
                        }
                    }
                    addJsonObject {
                        put("text", buildUserText(ctx))
                    }
                }
            }
        }

        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            put("temperature", 0.5)
            put("maxOutputTokens", 800)
            put("topP", 0.95)
        }
    }

    private fun buildUserText(ctx: CaptureContext): String = buildString {
        appendLine("Esta é a captura.")
        if (ctx.ocrText.isNotBlank()) {
            appendLine()
            appendLine("OCR EXTRAÍDO ON-DEVICE:")
            appendLine(ctx.ocrText.take(3000))
        }
        if (ctx.labels.isNotEmpty()) {
            appendLine()
            appendLine("LABELS ML KIT: ${ctx.labels.joinToString(", ")}")
        }
        appendLine()
        append("Analise. Retorne APENAS JSON conforme schema do system prompt.")
    }
}
