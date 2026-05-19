package com.lume.app.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class VerdictResult(
    val veredito: String,
    val vereditoUmaLinha: String,
    val maturidade: Maturidade,
    val lindyAnalysis: LindyAnalysis,
    val ehRebrand: Rebrand? = null,
    val ehInfraOuWrapper: InfraOuWrapper,
    val sinaisDeSubstancia: List<String> = emptyList(),
    val sinaisDeHype: List<String> = emptyList(),
    val alternativasMaisSerias: List<Alternativa> = emptyList(),
    val custoOportunidade: String,
    val recomendacaoConcreta: String,
    val quandoReavaliar: String,
    val confianca: String,
    val porQueConfianca: String,
    val fontesWeb: List<FonteWeb> = emptyList()
)

@Serializable data class Maturidade(val hypeCycle: String, val wardley: String, val crossingChasm: String, val justificativa: String)
@Serializable data class LindyAnalysis(val expectativaDuracaoMeses: String, val justificativa: String)
@Serializable data class Rebrand(val nomeOriginal: String, val anoOriginal: String? = null, val oQueMudou: String)
@Serializable data class InfraOuWrapper(val classificacao: String, val justificativa: String)
@Serializable data class Alternativa(val nome: String, val porQueMaisSeria: String)

object Veredito {
    const val GO = "GO"
    const val NO_GO = "NO_GO"
    const val WATCH = "WATCH"
    const val OBSOLETO = "OBSOLETO"
    const val DEPENDE = "DEPENDE"
}
