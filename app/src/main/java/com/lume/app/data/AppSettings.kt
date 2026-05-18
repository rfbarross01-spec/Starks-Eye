package com.lume.app.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lume_settings")

/**
 * Configurações não-secretas: pasta do vault, modelo escolhido, etc.
 */
class AppSettings(private val context: Context) {

    private val vaultUriKey = stringPreferencesKey("vault_uri")
    private val anthropicModelKey = stringPreferencesKey("anthropic_model")
    private val geminiModelKey = stringPreferencesKey("gemini_model")

    val vaultUri: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[vaultUriKey]?.let { Uri.parse(it) }
    }

    val anthropicModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[anthropicModelKey] ?: DEFAULT_ANTHROPIC_MODEL
    }

    val geminiModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[geminiModelKey] ?: DEFAULT_GEMINI_MODEL
    }

    suspend fun setVaultUri(uri: Uri) {
        context.dataStore.edit { it[vaultUriKey] = uri.toString() }
    }

    suspend fun getVaultUriOnce(): Uri? {
        return vaultUri.first()
    }

    suspend fun setAnthropicModel(model: String) {
        context.dataStore.edit { it[anthropicModelKey] = model }
    }

    suspend fun setGeminiModel(model: String) {
        context.dataStore.edit { it[geminiModelKey] = model }
    }

    companion object {
        const val DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-5"
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
    }
}
