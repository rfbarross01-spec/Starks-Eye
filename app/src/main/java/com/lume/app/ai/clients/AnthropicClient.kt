package com.lume.app.ai.clients

import android.util.Base64
import com.lume.app.ai.models.*
import com.lume.app.ai.prompts.ContextAdapter
import com.lume.app.ai.prompts.PromptStore
import com.lume.app.data.KeyStore
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Cliente Anthropic (Claude Sonnet 4.5) pra Camada 2 e Veredito.
 * Suporta web_search nativo.
 */
class AnthropicClient(
    private val keyStore: KeyStore,
    private val promptStore: PromptStore,
    private val model: String = "claude-sonnet-4-5",
    private val httpClient: HttpClient = HttpClientFactory.create(timeoutMs = 180_000)
) {

    suspend fun analyzeLayer2(
        ctx: CaptureContext,
        layer1: Layer1Result
    ): Result<Layer2Result> = runCatching {
        val systemPrompt = ContextAdapter.adaptLayer2Prompt(
            promptStore.getLayer2Prompt(),
            layer1,
            userQuestion = ctx.userQuestion
        )

        val rawResponse = callApi(
            systemPrompt = systemPrompt,
            ctx = ctx,
            maxTokens = 4000,
            enableWebSearch = true
        )

        val jsonText = JsonExtractor.extract(rawResponse)
            ?: error("Não consegui extrair JSON da Layer 2:\n${rawResponse.take(500)}")

        JsonExtractor.LENIENT_JSON.decodeFromString<Layer2Result>(jsonText)
    }

    suspend fun analyzeVerdict(
        ctx: CaptureContext,
        layer1: Layer1Result
    ): Result<VerdictResult> = runCatching {
        val systemPrompt = ContextAdapter.adaptVerdictPrompt(
            promptStore.getVerdictPrompt(),
            layer1,
            userQuestion = ctx.userQuestion
        )

        val rawResponse = callApi(
            systemPrompt = systemPrompt,
            ctx = ctx,
            maxTokens = 4500,
            enableWebSearch = true
        )

        val jsonText = JsonExtractor.extract(rawResponse)
            ?: error("Não consegui extrair JSON do Veredito:\n${rawResponse.take(500)}")

        JsonExtractor.LENIENT_JSON.decodeFromString<VerdictResult>(jsonText)
    }

    private suspend fun callApi(
        systemPrompt: String,
        ctx: CaptureContext,
        maxTokens: Int,
        enableWebSearch: Boolean
    ): String {
        val apiKey = keyStore.getAnthropicKey() ?: error("Chave Anthropic não configurada")
        val imageB64 = Base64.encodeToString(ctx.imageBytes, Base64.NO_WRAP)

        val response = httpClient.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(buildRequestBody(systemPrompt, ctx, imageB64, maxTokens, enableWebSearch).toString())
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            error("Anthropic ${response.status.value}: ${body.take(800)}")
        }

        val responseBody = response.bodyAsText()
        val root = HttpClientFactory.JSON.parseToJsonElement(responseBody).jsonObject

        // A resposta pode ter múltiplos content blocks (text + tool_use + tool_result)
        // Vamos pegar o texto agregado de todos os blocks "text"
        val contentArray = root["content"]?.jsonArray ?: error("Sem content na resposta")
        val textBlocks = contentArray.filter { block ->
            block.jsonObject["type"]?.jsonPrimitive?.content == "text"
        }
        val combined = textBlocks.joinToString("\n") { block ->
            block.jsonObject["text"]?.jsonPrimitive?.content ?: ""
        }

        if (combined.isBlank()) {
            error("Resposta vazia (sem blocos de texto). Body: ${responseBody.take(500)}")
        }
        return combined
    }

    private fun buildRequestBody(
        systemPrompt: String,
        ctx: CaptureContext,
        imageB64: String,
        maxTokens: Int,
        enableWebSearch: Boolean
    ): JsonObject = buildJsonObject {
        put("model", model)
        put("max_tokens", maxTokens)
        put("temperature", 0.7)
        put("system", systemPrompt)

        if (enableWebSearch) {
            putJsonArray("tools") {
                addJsonObject {
                    put("type", "web_search_20250305")
                    put("name", "web_search")
                    put("max_uses", 5)
                }
            }
        }

        putJsonArray("messages") {
            addJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "image")
                        putJsonObject("source") {
                            put("type", "base64")
                            put("media_type", ctx.mimeType)
                            put("data", imageB64)
                        }
                    }
                    addJsonObject {
                        put("type", "text")
                        put("text", buildUserText(ctx))
                    }
                }
            }
        }
    }

    private fun buildUserText(ctx: CaptureContext): String = buildString {
        appendLine("Esta é a captura. A Camada 1 já fez a primeira leitura.")
        if (ctx.ocrText.isNotBlank()) {
            appendLine()
            appendLine("OCR ON-DEVICE:")
            appendLine(ctx.ocrText.take(5000))
        }
        if (ctx.labels.isNotEmpty()) {
            appendLine()
            appendLine("LABELS: ${ctx.labels.joinToString(", ")}")
        }
        if (!ctx.userQuestion.isNullOrBlank()) {
            appendLine()
            appendLine("PERGUNTA EXPLÍCITA DO USUÁRIO:")
            appendLine("\"${ctx.userQuestion}\"")
        }
        appendLine()
        append("Faça a análise. Retorne APENAS JSON conforme schema do system prompt. Use web_search quando precisar de fato verificável.")
    }
}
