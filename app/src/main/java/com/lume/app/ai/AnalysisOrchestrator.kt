package com.lume.app.ai

import com.lume.app.ai.clients.AnthropicClient
import com.lume.app.ai.clients.AnthropicLayer2Adapter
import com.lume.app.ai.clients.GeminiClient
import com.lume.app.ai.clients.KimiClient
import com.lume.app.ai.clients.KimiLayer2Adapter
import com.lume.app.ai.clients.Layer2ProviderClient
import com.lume.app.ai.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Orquestra o pipeline completo de análise.
 *
 * Fluxo:
 * 1. Layer 1 (Gemini Flash) — identificação
 * 2. Decisão: vale aprofundar? é tech? force_verdict?
 * 3. Layer 2 (Claude/Kimi) — análise editorial OU veredito
 * 4. Emite eventos em Flow pra UI mostrar progresso
 */
class AnalysisOrchestrator(
    private val gemini: GeminiClient,
    private val anthropic: AnthropicClient,
    private val kimi: KimiClient
) {
    fun analyze(ctx: CaptureContext, layer2Provider: Layer2Provider): Flow<AnalysisEvent> = flow {
        emit(AnalysisEvent.Started)

        // ===== CAMADA 1 =====
        emit(AnalysisEvent.Layer1Started)
        val layer1Result = gemini.analyzeLayer1(ctx)
        val layer1 = layer1Result.getOrElse {
            emit(AnalysisEvent.Error("Camada 1 (Gemini): ${it.message}", it))
            return@flow
        }
        emit(AnalysisEvent.Layer1Done(layer1))

        // ===== DECISÃO =====
        val shouldDeepen = layer1.valeAprofundar || ctx.forceVerdict || !ctx.userQuestion.isNullOrBlank()
        val shouldVerdict = (layer1.ehTechHype || ctx.forceVerdict) && shouldDeepen

        if (!shouldDeepen) {
            emit(AnalysisEvent.SkippedDeepening(layer1.razaoNaoAprofundar ?: "Conteúdo trivial"))
            emit(AnalysisEvent.Completed)
            return@flow
        }

        // ===== CAMADA 2 (normal ou veredito) =====
        val provider: Layer2ProviderClient = when (layer2Provider) {
            Layer2Provider.CLAUDE_SONNET_4_5 -> AnthropicLayer2Adapter(anthropic)
            Layer2Provider.KIMI_K2_6 -> KimiLayer2Adapter(kimi)
        }

        if (shouldVerdict) {
            emit(AnalysisEvent.VerdictStarted(provider.displayName))
            val r = provider.analyzeVerdict(ctx, layer1)
            r.getOrElse {
                emit(AnalysisEvent.Error("Veredito (${provider.displayName}): ${it.message}", it))
                return@flow
            }.let { emit(AnalysisEvent.VerdictDone(it)) }
        } else {
            emit(AnalysisEvent.Layer2Started(provider.displayName))
            val r = provider.analyzeLayer2(ctx, layer1)
            r.getOrElse {
                emit(AnalysisEvent.Error("Camada 2 (${provider.displayName}): ${it.message}", it))
                return@flow
            }.let { emit(AnalysisEvent.Layer2Done(it)) }
        }

        emit(AnalysisEvent.Completed)
    }
}

sealed class AnalysisEvent {
    object Started : AnalysisEvent()
    object Layer1Started : AnalysisEvent()
    data class Layer1Done(val result: Layer1Result) : AnalysisEvent()
    data class SkippedDeepening(val reason: String) : AnalysisEvent()
    data class Layer2Started(val providerName: String) : AnalysisEvent()
    data class Layer2Done(val result: Layer2Result) : AnalysisEvent()
    data class VerdictStarted(val providerName: String) : AnalysisEvent()
    data class VerdictDone(val result: VerdictResult) : AnalysisEvent()
    data class Error(val message: String, val cause: Throwable?) : AnalysisEvent()
    object Completed : AnalysisEvent()
}
