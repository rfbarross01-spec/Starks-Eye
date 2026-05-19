package com.lume.app.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class Layer1Result(
    val tipoConteudo: String,
    val tituloTipo: String,
    val tituloEvocativo: String,
    val observacaoAguda: String,
    val valeAprofundar: Boolean,
    val razaoNaoAprofundar: String? = null,
    val ehTechHype: Boolean = false,
    val confianca: String = "media"
)
