package com.lume.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lume.app.LumeApplication
import com.lume.app.MainActivity

/**
 * Foreground service que mantém a bolha do Lume sobre outros apps.
 *
 * Esta é a versão V1: stub que apenas mantém o foreground service rodando.
 * A bolha real, MediaProjection e captura de tela serão implementadas na iteração V2.
 *
 * Comandos suportados via Intent extras:
 * - ACTION_START: inicia o serviço
 * - ACTION_STOP: encerra o serviço
 */
class LumeOverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startInForeground()
                // TODO V2: criar bolha flutuante via WindowManager
                // TODO V2: inicializar MediaProjection
                // TODO V2: implementar lógica de captura ao tocar bolha
            }
        }
        return START_STICKY
    }

    private fun startInForeground() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LumeOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, LumeApplication.CHANNEL_OVERLAY)
            .setContentTitle("Lume ativo")
            .setContentText("Toque na bolha pra capturar a tela atual")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .addAction(0, "Encerrar", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO V2: limpar WindowManager view + MediaProjection
    }

    companion object {
        const val ACTION_START = "com.lume.app.action.START"
        const val ACTION_STOP = "com.lume.app.action.STOP"
        private const val NOTIFICATION_ID = 1001
    }
}
