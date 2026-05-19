package com.lume.app.ai.models

data class CaptureContext(
    val imageBytes: ByteArray,
    val mimeType: String,
    val ocrText: String,
    val labels: List<String>,
    val sourceApp: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val userQuestion: String? = null,
    val forceVerdict: Boolean = false
) {
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

enum class Layer2Provider {
    CLAUDE_SONNET_4_5,
    KIMI_K2_6
}
