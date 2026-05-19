package com.lume.app.ui.result

import com.lume.app.ai.models.CaptureContext

object PendingAnalysisHolder {
    @Volatile private var context: CaptureContext? = null

    fun set(ctx: CaptureContext) { context = ctx }

    fun consume(): CaptureContext? {
        val c = context
        context = null
        return c
    }

    fun peek(): CaptureContext? = context
}
