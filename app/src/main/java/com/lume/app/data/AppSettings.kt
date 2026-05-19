package com.lume.app.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lume.app.ai.models.Layer2Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lume_settings")

class AppSettings(private val context: Context) {

    private val vaultUriKey = stringPreferencesKey("vault_uri")
    private val anthropicModelKey = stringPreferencesKey("anthropic_model")
    private val geminiModelKey = stringPreferencesKey("gemini_model")
    private val kimiModelKey = stringPreferencesKey("kimi_model")
    private val layer2ProviderKey = stringPreferencesKey("layer2_provider")
    private val experimentalRewriteKey = booleanPreferencesKey("experimental_prompt_rewrite")

    val vaultUri: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[vaultUriKey]?.let { Uri.parse(it) }
    }

    val anthropicModel: Flow<String> = context.dataStore.data.map {
        it[anthropicModelKey] ?: DEFAULT_ANTHROPIC_MODEL
    }

    val geminiModel: Flow<String> = context.dataStore.data.map {
        it[geminiModelKey] ?: DEFAULT_GEMINI_MODEL
    }

    val kimiModel: Flow<String> = context.dataStore.data.map {
        it[kimiModelKey] ?: DEFAULT_KIMI_MODEL
    }

    val layer2Provider: Flow<Layer2Provider> = context.dataStore.data.map {
        when (it[layer2ProviderKey]) {
            "KIMI" -> Layer2Provider.KIMI_K2_6
            else -> Layer2Provider.CLAUDE_SONNET_4_5
        }
    }

    val experimentalPromptRewrite: Flow<Boolean> = context.dataStore.data.map {
        it[experimentalRewriteKey] ?: false
    }

    suspend fun setVaultUri(uri: Uri) {
        context.dataStore.edit { it[vaultUriKey] = uri.toString() }
    }

    suspend fun getVaultUriOnce(): Uri? = vaultUri.first()

    suspend fun setAnthropicModel(model: String) =
        context.dataStore.edit { it[anthropicModelKey] = model }

    suspend fun setGeminiModel(model: String) =
        context.dataStore.edit { it[geminiModelKey] = model }

    suspend fun setKimiModel(model: String) =
        context.dataStore.edit { it[kimiModelKey] = model }

    suspend fun setLayer2Provider(provider: Layer2Provider) =
        context.dataStore.edit {
            it[layer2ProviderKey] = when (provider) {
                Layer2Provider.CLAUDE_SONNET_4_5 -> "CLAUDE"
                Layer2Provider.KIMI_K2_6 -> "KIMI"
            }
        }

    suspend fun setExperimentalRewrite(enabled: Boolean) =
        context.dataStore.edit { it[experimentalRewriteKey] = enabled }

    companion object {
        const val DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-5"
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_KIMI_MODEL = "kimi-k2.6"
    }
}
