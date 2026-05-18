package com.lume.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.lume.app.ai.ApiTester
import com.lume.app.data.KeyStore
import com.lume.app.service.LumeOverlayService
import com.lume.app.ui.home.HomeScreen
import com.lume.app.ui.onboarding.OnboardingScreen
import com.lume.app.ui.settings.SettingsScreen
import com.lume.app.ui.theme.LumeTheme

class MainActivity : ComponentActivity() {

    private lateinit var keyStore: KeyStore
    private lateinit var apiTester: ApiTester

    // Permissão de notificação (Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        keyStore = KeyStore(applicationContext)
        apiTester = ApiTester()

        // Pede permissão de notificação no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LumeTheme {
                val initialScreen = if (keyStore.hasBothKeys()) Screen.Home else Screen.Onboarding
                var currentScreen by remember { mutableStateOf(initialScreen) }
                var bubbleActive by remember { mutableStateOf(false) }

                when (currentScreen) {
                    Screen.Onboarding -> OnboardingScreen(
                        keyStore = keyStore,
                        apiTester = apiTester,
                        onComplete = { currentScreen = Screen.Home }
                    )
                    Screen.Home -> HomeScreen(
                        onOpenSettings = { currentScreen = Screen.Settings },
                        onActivateBubble = {
                            if (bubbleActive) {
                                stopBubbleService()
                                bubbleActive = false
                            } else {
                                if (canDrawOverlays()) {
                                    startBubbleService()
                                    bubbleActive = true
                                } else {
                                    requestOverlayPermission()
                                }
                            }
                        },
                        bubbleActive = bubbleActive
                    )
                    Screen.Settings -> SettingsScreen(
                        keyStore = keyStore,
                        onBack = { currentScreen = Screen.Home },
                        onResetKeys = {
                            keyStore.clearAll()
                            currentScreen = Screen.Onboarding
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        apiTester.close()
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startBubbleService() {
        val intent = Intent(this, LumeOverlayService::class.java).apply {
            action = LumeOverlayService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopBubbleService() {
        val intent = Intent(this, LumeOverlayService::class.java).apply {
            action = LumeOverlayService.ACTION_STOP
        }
        startService(intent)
    }

    private enum class Screen {
        Onboarding, Home, Settings
    }
}
