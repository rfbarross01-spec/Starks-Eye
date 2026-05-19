package com.lume.app.util

import java.text.Normalizer

object SlugGenerator {
    fun slug(input: String, maxLength: Int = 60): String {
        if (input.isBlank()) return "captura"
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
        return normalized.take(maxLength).ifBlank { "captura" }
    }
}
