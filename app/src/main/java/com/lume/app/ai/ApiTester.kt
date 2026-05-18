package com.lume.app.ai

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * Testa se as chaves de API funcionam fazendo chamadas mínimas reais.
 * Sem mock — se retornar OK, é porque a API respondeu de verdade.
 */
class ApiTester {

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
        }
        expectSuccess = false
    }

    /**
     * Testa a chave Gemini com uma chamada mínima (modelo gemini-2.5-flash).
     */
    suspend fun testGemini(apiKey: String, model: String = "gemini-2.5-flash"): TestResult {
        if (apiKey.isBlank()) return TestResult.Failure("Chave vazia")
        return try {
            val response = httpClient.post(
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            ) {
                url { parameters.append("key", apiKey) }
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        putJsonArray("contents") {
                            addJsonObject {
                                putJsonArray("parts") {
                                    addJsonObject { put("text", "Diga apenas 'ok'.") }
                                }
                            }
                        }
                        putJsonObject("generationConfig") {
                            put("maxOutputTokens", 10)
                            put("temperature", 0)
                        }
                    }.toString()
                )
            }
            val body = response.bodyAsText()
            when (response.status.value) {
                200 -> TestResult.Success("Gemini OK")
                400 -> TestResult.Failure("Chave inválida (400)")
                401, 403 -> TestResult.Failure("Acesso negado — verifique a chave")
                429 -> TestResult.Failure("Rate limit — tente em alguns segundos")
                else -> TestResult.Failure("Erro ${response.status.value}: ${body.take(200)}")
            }
        } catch (e: Exception) {
            TestResult.Failure("Falha de rede: ${e.message?.take(150)}")
        }
    }

    /**
     * Testa a chave Anthropic com uma chamada mínima.
     */
    suspend fun testAnthropic(apiKey: String, model: String = "claude-sonnet-4-5"): TestResult {
        if (apiKey.isBlank()) return TestResult.Failure("Chave vazia")
        return try {
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("model", model)
                        put("max_tokens", 10)
                        putJsonArray("messages") {
                            addJsonObject {
                                put("role", "user")
                                put("content", "Diga apenas 'ok'.")
                            }
                        }
                    }.toString()
                )
            }
            val body = response.bodyAsText()
            when (response.status.value) {
                200 -> TestResult.Success("Anthropic OK")
                401 -> TestResult.Failure("Chave inválida (401)")
                403 -> TestResult.Failure("Acesso negado (403)")
                429 -> TestResult.Failure("Rate limit — tente em alguns segundos")
                400 -> {
                    // Tentar identificar se é modelo não disponível
                    if (body.contains("model", ignoreCase = true)) {
                        TestResult.Failure("Modelo '$model' não disponível na sua conta")
                    } else {
                        TestResult.Failure("Erro 400: ${body.take(200)}")
                    }
                }
                else -> TestResult.Failure("Erro ${response.status.value}: ${body.take(200)}")
            }
        } catch (e: Exception) {
            TestResult.Failure("Falha de rede: ${e.message?.take(150)}")
        }
    }

    fun close() {
        httpClient.close()
    }
}

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Failure(val reason: String) : TestResult()
}
