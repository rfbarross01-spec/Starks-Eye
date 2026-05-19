package com.lume.app

import android.app.Application
import com.lume.app.ai.AnalysisOrchestrator
import com.lume.app.ai.clients.AnthropicClient
import com.lume.app.ai.clients.GeminiClient
import com.lume.app.ai.clients.KimiClient
import com.lume.app.ai.prompts.PromptStore
import com.lume.app.data.AppSettings
import com.lume.app.data.KeyStore
import com.lume.app.data.database.LumeDatabase
import com.lume.app.util.ObsidianExporter
import com.lume.app.util.SessionExporter
import kotlinx.coroutines.flow.first

/**
 * Application class — container de dependências (DI manual sem Hilt/Koin).
 * Acesso via (context.applicationContext as LumeApplication).<thing>
 */
class LumeApplication : Application() {

    val keyStore: KeyStore by lazy { KeyStore(this) }
    val settings: AppSettings by lazy { AppSettings(this) }
    val database: LumeDatabase by lazy { LumeDatabase.get(this) }

    val promptStore: PromptStore by lazy {
        PromptStore(this) { settings.vaultUri.first() }
    }

    val geminiClient: GeminiClient by lazy {
        GeminiClient(keyStore, promptStore)
    }

    val anthropicClient: AnthropicClient by lazy {
        AnthropicClient(keyStore, promptStore)
    }

    val kimiClient: KimiClient by lazy {
        KimiClient(keyStore, promptStore)
    }

    val orchestrator: AnalysisOrchestrator by lazy {
        AnalysisOrchestrator(geminiClient, anthropicClient, kimiClient)
    }

    val obsidianExporter: ObsidianExporter by lazy {
        ObsidianExporter(this, settings)
    }

    val sessionExporter: SessionExporter by lazy {
        SessionExporter(this, database, promptStore)
    }
}
