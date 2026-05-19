package com.lume.app.ai.clients

import com.lume.app.ai.models.CaptureContext
import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.models.Layer2Result
import com.lume.app.ai.models.VerdictResult

/**
 * Interface unificada pros providers de Camada 2 (Claude Sonnet 4.5 ou Kimi K2.6).
 * Permite trocar nas configurações sem mudar código de orquestração.
 */
interface Layer2ProviderClient {
    val displayName: String
    suspend fun analyzeLayer2(ctx: CaptureContext, layer1: Layer1Result): Result<Layer2Result>
    suspend fun analyzeVerdict(ctx: CaptureContext, layer1: Layer1Result): Result<VerdictResult>
}

class AnthropicLayer2Adapter(private val client: AnthropicClient) : Layer2ProviderClient {
    override val displayName = "Claude Sonnet 4.5"
    override suspend fun analyzeLayer2(ctx: CaptureContext, layer1: Layer1Result) =
        client.analyzeLayer2(ctx, layer1)
    override suspend fun analyzeVerdict(ctx: CaptureContext, layer1: Layer1Result) =
        client.analyzeVerdict(ctx, layer1)
}

class KimiLayer2Adapter(private val client: KimiClient) : Layer2ProviderClient {
    override val displayName = "Kimi K2.6"
    override suspend fun analyzeLayer2(ctx: CaptureContext, layer1: Layer1Result) =
        client.analyzeLayer2(ctx, layer1)
    override suspend fun analyzeVerdict(ctx: CaptureContext, layer1: Layer1Result) =
        client.analyzeVerdict(ctx, layer1)
}
