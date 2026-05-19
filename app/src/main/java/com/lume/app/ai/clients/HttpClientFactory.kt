package com.lume.app.ai.clients

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientFactory {
    val JSON = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun create(timeoutMs: Long = 120_000L): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(JSON) }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = timeoutMs
        }
        expectSuccess = false
    }
}
