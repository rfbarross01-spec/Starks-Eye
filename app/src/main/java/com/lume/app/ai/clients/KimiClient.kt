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
 * Cliente Kimi K2.6 (Moonshot AI) pra Camada 2 e Veredito.
 *
 * API compatível com OpenAI em api.moonshot.ai/v1.
 * Web search nativo via builtin_function $web_search.
 * IMPORTANTE: $web_search é incompatível com thinking mode em K2.6 (limitação atual).
 */
class KimiClient(
    private val keyStore: KeyStore,
    private val promptStore: PromptStore,
    private val model: String = "kimi-k2.6",
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

        val rawResponse = callApiWithLoop(
            systemPrompt = systemPrompt,
            ctx = ctx,
            maxTokens = 8000
        )

        val jsonText = JsonExtractor.extract(rawResponse)
            ?: error("Não consegui extrair JSON da Layer 2 (Kimi):\n${rawResponse.take(500)}")

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

        val rawResponse = callApiWithLoop(
            systemPrompt = systemPrompt,
            ctx = ctx,
            maxTokens = 9000
        )

        val jsonText = JsonExtractor.extract(rawResponse)
            ?: error("Não consegui extrair JSON do Veredito (Kimi):\n${rawResponse.take(500)}")

        JsonExtractor.LENIENT_JSON.decodeFromString<VerdictResult>(jsonText)
    }

    /**
     * Loop tool_calls do Kimi: chamamos a API, se ela pedir $web_search,
     * devolvemos o argumento como resultado (Kimi executa search internamente
     * mesmo — o protocolo é peculiar mas é assim que está documentado).
     */
    private suspend fun callApiWithLoop(
        systemPrompt: String,
        ctx: CaptureContext,
        maxTokens: Int,
        maxIterations: Int = 6
    ): String {
        val apiKey = keyStore.getKimiKey() ?: error("Chave Kimi não configurada")
        val imageB64 = Base64.encodeToString(ctx.imageBytes, Base64.NO_WRAP)

        val messages = mutableListOf<JsonElement>()
        messages.add(buildJsonObject {
            put("role", "system")
            put("content", systemPrompt)
        })
        messages.add(buildJsonObject {
            put("role", "user")
            putJsonArray("content") {
                addJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") {
                        put("url", "data:${ctx.mimeType};base64,$imageB64")
                    }
                }
                addJsonObject {
                    put("type", "text")
                    put("text", buildUserText(ctx))
                }
            }
        })

        var iterations = 0
        while (iterations < maxIterations) {
            iterations++

            val response = httpClient.post("https://api.moonshot.ai/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("model", model)
                    put("max_tokens", maxTokens)
                    put("temperature", 0.7)
                    putJsonArray("messages") { messages.forEach { add(it) } }
                    putJsonArray("tools") {
                        addJsonObject {
                            put("type", "builtin_function")
                            putJsonObject("function") {
                                put("name", "\$web_search")
                            }
                        }
                    }
                    // Thinking incompatível com $web_search no K2.6 atual
                    putJsonObject("thinking") {
                        put("type", "disabled")
                    }
                }.toString())
            }

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                error("Kimi ${response.status.value}: ${body.take(800)}")
            }

            val responseBody = response.bodyAsText()
            val root = HttpClientFactory.JSON.parseToJsonElement(responseBody).jsonObject
            val choices = root["choices"]?.jsonArray ?: error("Sem choices na resposta Kimi")
            if (choices.isEmpty()) error("choices vazio")

            val choice = choices[0].jsonObject
            val finishReason = choice["finish_reason"]?.jsonPrimitive?.content
            val message = choice["message"]?.jsonObject ?: error("Sem message")

            messages.add(message)

            if (finishReason == "tool_calls") {
                // Kimi quer chamar $web_search — devolvemos o argumento (Kimi executa internamente)
                val toolCalls = message["tool_calls"]?.jsonArray ?: break
                toolCalls.forEach { call ->
                    val callObj = call.jsonObject
                    val callId = callObj["id"]?.jsonPrimitive?.content ?: ""
                    val function = callObj["function"]?.jsonObject
                    val arguments = function?.get("arguments")?.jsonPrimitive?.content ?: "{}"
                    messages.add(buildJsonObject {
                        put("role", "tool")
                        put("tool_call_id", callId)
                        put("name", "\$web_search")
                        put("content", arguments)
                    })
                }
                continue
            }

            if (finishReason == "stop" || finishReason == "length") {
                val content = message["content"]?.jsonPrimitive?.content ?: ""
                return content
            }

            // outro finish_reason — sai
            break
        }

        error("Loop Kimi excedeu $maxIterations iterações")
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
        append("Faça a análise. Retorne APENAS JSON conforme schema do system prompt. Use \$web_search se precisar de fato verificável.")
    }
}
