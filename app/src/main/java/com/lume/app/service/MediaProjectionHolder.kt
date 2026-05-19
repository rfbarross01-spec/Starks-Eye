package com.lume.app.service

import android.content.Intent

/**
 * Singleton que carrega o resultado de MediaProjection (Intent) entre
 * Activity de permissão e o Service que precisa usar.
 *
 * Necessário porque Intent com MediaProjection data não pode ser
 * passado via startForegroundService de forma confiável.
 */
object MediaProjectionHolder {
    @Volatile private var resultCode: Int? = null
    @Volatile private var resultData: Intent? = null

    fun setResult(code: Int, data: Intent) {
        resultCode = code
        resultData = data
    }

    fun consume(): Pair<Int, Intent>? {
        val code = resultCode
        val data = resultData
        return if (code != null && data != null) {
            // Não limpa imediatamente — pode ser reusado durante a sessão
            Pair(code, data)
        } else null
    }

    fun clear() {
        resultCode = null
        resultData = null
    }

    fun hasGrant(): Boolean = resultCode != null && resultData != null
}
