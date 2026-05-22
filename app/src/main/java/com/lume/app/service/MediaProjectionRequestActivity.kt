package com.lume.app.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Activity transparente — pede permissão MediaProjection.
 *
 * Lifecycle:
 *   Service detecta "preciso de captura mas não tenho grant"
 *   → startActivity(MediaProjectionRequestActivity)
 *   → user vê dialog "Permitir captura?" do sistema
 *   → result vai pra MediaProjectionHolder
 *   → Activity envia broadcast pro service continuar
 */
class MediaProjectionRequestActivity : ComponentActivity() {

    companion object {
        const val ACTION_GRANTED = "com.lume.app.PROJECTION_GRANTED"
        const val ACTION_DENIED = "com.lume.app.PROJECTION_DENIED"
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            MediaProjectionHolder.setResult(result.resultCode, result.data!!)
            // Com o consent recém-concedido, o app-op project_media está ativo.
            // Agora podemos iniciar o foreground service com tipo mediaProjection
            // sem cair em SecurityException (Android 14+).
            startForegroundService(LumeOverlayService.startIntent(this))
            sendBroadcast(Intent(ACTION_GRANTED).setPackage(packageName))
        } else {
            sendBroadcast(Intent(ACTION_DENIED).setPackage(packageName))
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }
}
