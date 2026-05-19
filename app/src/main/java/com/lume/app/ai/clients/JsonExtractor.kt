package com.lume.app.ai.clients

import kotlinx.serialization.json.Json

/**
 * Extrai JSON de respostas de LLM que podem vir com markdown, texto antes/depois, etc.
 * Tolerante mas rigoroso: se não acha JSON válido, retorna null.
 */
object JsonExtractor {

    val LENIENT_JSON = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun extract(raw: String): String? {
        val trimmed = raw.trim()

        // Caso 1: já é JSON limpo
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed
        }

        // Caso 2: dentro de ```json ... ```
        val codeBlockRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?\}|\[[\s\S]*?\])\s*```""")
        codeBlockRegex.find(trimmed)?.let {
            return it.groupValues[1]
        }

        // Caso 3: procurar primeiro { ... } balanceado
        val firstBrace = trimmed.indexOf('{')
        if (firstBrace >= 0) {
            val balanced = findBalanced(trimmed, firstBrace, '{', '}')
            if (balanced != null) return balanced
        }

        // Caso 4: procurar [ ... ]
        val firstBracket = trimmed.indexOf('[')
        if (firstBracket >= 0) {
            val balanced = findBalanced(trimmed, firstBracket, '[', ']')
            if (balanced != null) return balanced
        }

        return null
    }

    private fun findBalanced(text: String, start: Int, open: Char, close: Char): String? {
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until text.length) {
            val c = text[i]
            if (escape) { escape = false; continue }
            if (c == '\\' && inString) { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
