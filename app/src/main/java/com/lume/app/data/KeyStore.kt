package com.lume.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setGeminiKey(key: String) = prefs.edit().putString(KEY_GEMINI, key.trim()).apply()
    fun getGeminiKey(): String? = prefs.getString(KEY_GEMINI, null)?.takeIf { it.isNotBlank() }

    fun setAnthropicKey(key: String) = prefs.edit().putString(KEY_ANTHROPIC, key.trim()).apply()
    fun getAnthropicKey(): String? = prefs.getString(KEY_ANTHROPIC, null)?.takeIf { it.isNotBlank() }

    fun setKimiKey(key: String) = prefs.edit().putString(KEY_KIMI, key.trim()).apply()
    fun getKimiKey(): String? = prefs.getString(KEY_KIMI, null)?.takeIf { it.isNotBlank() }

    fun hasMinimumKeys(): Boolean = getGeminiKey() != null && getAnthropicKey() != null

    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        private const val PREFS_NAME = "lume_secure_prefs"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_ANTHROPIC = "anthropic_api_key"
        private const val KEY_KIMI = "kimi_api_key"
    }
}
