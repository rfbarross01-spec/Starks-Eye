package com.lume.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lume.app.ui.home.HomeScreen
import com.lume.app.ui.journal.EntryDetailScreen
import com.lume.app.ui.journal.JournalScreen
import com.lume.app.ui.onboarding.OnboardingScreen
import com.lume.app.ui.prompts.PromptsEditorScreen
import com.lume.app.ui.settings.SettingsScreen
import com.lume.app.ui.theme.LumeTheme

class MainActivity : ComponentActivity() {

    private val app get() = applicationContext as LumeApplication

    private val vaultPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            lifecycleScope.launch {
                app.settings.setVaultUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LumeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LumeNav()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LumeNav() {
        val navController = rememberNavController()
        val keyStore = app.keyStore

        val startRoute = if (keyStore.hasMinimumKeys()) "home" else "onboarding"

        NavHost(navController = navController, startDestination = startRoute) {
            composable("onboarding") {
                OnboardingScreen(
                    keyStore = keyStore,
                    onDone = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onActivateBubble = { tryStartOverlay() },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenJournal = { navController.navigate("journal") },
                    onOpenPrompts = { navController.navigate("prompts") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    application = app,
                    onPickVault = { vaultPickerLauncher.launch(null) },
                    onReconfigureKeys = {
                        navController.navigate("onboarding") {
                            popUpTo("home")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("journal") {
                JournalScreen(
                    application = app,
                    onOpenEntry = { id -> navController.navigate("entry/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("entry/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                EntryDetailScreen(
                    application = app,
                    captureId = id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("prompts") {
                PromptsEditorScreen(
                    application = app,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    private fun tryStartOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        if (!com.lume.app.service.MediaProjectionHolder.hasGrant()) {
            // Android 14+: precisa ter o consent ANTES de startForeground com tipo mediaProjection.
            // A activity de consent inicia o service quando o grant for concedido.
            startActivity(
                Intent(this, com.lume.app.service.MediaProjectionRequestActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        startForegroundService(com.lume.app.service.LumeOverlayService.startIntent(this))
    }
}
