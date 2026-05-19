package com.lume.app.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class Layer2Result(
    val oQueE: String,
    val contexto: String,
    val camadasMaisProfundas: String,
    val tensoes: String,
    val conexoes: List<Conexao> = emptyList(),
    val paraRefletir: String,
    val paraIrAlem: List<String> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val fontesWeb: List<FonteWeb> = emptyList(),
    val tagsObsidian: List<String> = emptyList()
)

@Serializable data class Conexao(val wikilink: String, val porQue: String)
@Serializable data class Flashcard(val frente: String, val verso: String)
@Serializable data class FonteWeb(val titulo: String, val url: String, val trecho: String? = null)
