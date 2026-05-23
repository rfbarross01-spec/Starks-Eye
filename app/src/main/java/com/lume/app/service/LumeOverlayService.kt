package com.lume.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lume.app.LumeApplication
import com.lume.app.MainActivity
import com.lume.app.R
import com.lume.app.ai.models.CaptureContext
import com.lume.app.triage.TriageEngine
import com.lume.app.ui.result.PendingAnalysisHolder
import com.lume.app.ui.result.ResultOverlayActivity
import com.lume.app.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.launch

/**
 * Foreground service que mantém a bolha + screen capture vivos.
 *
 * Lifecycle:
 *   start → mostra bolha (se já tem grant projection, prepara capture)
 *   se não tem grant → dispara MediaProjectionRequestActivity
 *   user toca bolha → esconde bolha → captura → triagem → ResultOverlayActivity
 *
 * Stop via notificação.
 */
class LumeOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.lume.app.START_OVERLAY"
        const val ACTION_STOP = "com.lume.app.STOP_OVERLAY"

        private const val CHANNEL_ID = "lume_overlay"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context) = Intent(context, LumeOverlayService::class.java)
            .setAction(ACTION_START)

        fun stopIntent(context: Context) = Intent(context, LumeOverlayService::class.java)
            .setAction(ACTION_STOP)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bubbleManager: BubbleManager? = null
    private var captureManager: ScreenCaptureManager? = null
    private val triage = TriageEngine()
    private var currentCaptureJob: Job? = null

    private val projectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                MediaProjectionRequestActivity.ACTION_GRANTED -> setupCapture()
                MediaProjectionRequestActivity.ACTION_DENIED -> {
                    showToast("Permissão de captura negada. Lume não pode operar sem isso.")
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(MediaProjectionRequestActivity.ACTION_GRANTED)
            addAction(MediaProjectionRequestActivity.ACTION_DENIED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(projectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(projectionReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Android 14+: startForeground com tipo mediaProjection só funciona
                // se o app-op project_media já foi concedido. O fluxo agora garante
                // que MediaProjectionRequestActivity inicia este service só após o grant.
                if (!MediaProjectionHolder.hasGrant()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startInForeground()
            }
        }

        setupCapture()

        bubbleManager?.show() ?: run {
            bubbleManager = BubbleManager(
                context = this,
                onTap = { onBubbleTap(forceVerdict = false) },
                onLongPress = { onBubbleTap(forceVerdict = true) }
            ).also { it.show() }
        }

        return START_STICKY
    }

    private fun startInForeground() {
        val stopIntent = stopIntent(this).let { intent ->
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        val openIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lume ativo")
            .setContentText("Toque na bolha pra capturar e analisar")
            .setSmallIcon(R.drawable.ic_lume_bubble)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, "Pausar", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun requestMediaProjection() {
        val intent = Intent(this, MediaProjectionRequestActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun setupCapture() {
        val grant = MediaProjectionHolder.consume()
        if (grant == null) {
            Log.w("LumeOverlay", "setupCapture: grant null — MediaProjectionHolder vazio")
            return
        }
        Log.d("LumeOverlay", "setupCapture: grant obtido, configurando captureManager")
        if (captureManager == null) captureManager = ScreenCaptureManager(this)
        captureManager?.setup(grant.first, grant.second)
        Log.d("LumeOverlay", "setupCapture: isReady=${captureManager?.isReady()}")
    }

    private fun onBubbleTap(forceVerdict: Boolean) {
        Log.d("LumeOverlay", "onBubbleTap forceVerdict=$forceVerdict")
        if (currentCaptureJob?.isActive == true) {
            Log.d("LumeOverlay", "onBubbleTap: job já ativo, ignorando")
            return
        }

        currentCaptureJob = scope.launch {
            try {
                val ready = captureManager?.isReady()
                Log.d("LumeOverlay", "onBubbleTap: captureManager.isReady=$ready")
                if (ready != true) {
                    showToast("Preparando captura — toque de novo em 2s")
                    requestMediaProjection()
                    return@launch
                }

                bubbleManager?.hide()
                delay(200)

                Log.d("LumeOverlay", "onBubbleTap: capturando screenshot...")
                val bitmap: Bitmap? = captureManager?.captureSingle(timeoutMs = 3000)
                Log.d("LumeOverlay", "onBubbleTap: bitmap=${bitmap != null}")

                bubbleManager?.reveal()

                if (bitmap == null) {
                    showToast("Não consegui capturar a tela")
                    return@launch
                }

                Log.d("LumeOverlay", "onBubbleTap: triagem...")
                val triageResult = triage.triage(bitmap)
                Log.d("LumeOverlay", "onBubbleTap: triage isSensitive=${triageResult.isSensitive}")
                if (triageResult.isSensitive) {
                    showToast("Conteúdo sensível detectado — captura cancelada")
                    bitmap.recycle()
                    return@launch
                }

                Log.d("LumeOverlay", "onBubbleTap: convertendo para JPEG...")
                val jpegBytes = ImageUtils.bitmapToJpegBytes(bitmap, maxDimension = 1568, quality = 85)
                bitmap.recycle()
                Log.d("LumeOverlay", "onBubbleTap: JPEG ${jpegBytes.size} bytes")

                val ctx = CaptureContext(
                    imageBytes = jpegBytes,
                    mimeType = "image/jpeg",
                    ocrText = triageResult.ocrText,
                    labels = triageResult.labels,
                    forceVerdict = forceVerdict
                )

                PendingAnalysisHolder.set(ctx)
                Log.d("LumeOverlay", "onBubbleTap: iniciando ResultOverlayActivity")
                val intent = Intent(this@LumeOverlayService, ResultOverlayActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("LumeOverlay", "onBubbleTap: erro", e)
                showToast("Erro: ${e.message}")
                bubbleManager?.reveal()
            }
        }
    }

    private fun showToast(msg: String) {
        scope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(this@LumeOverlayService, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(projectionReceiver) } catch (_: Exception) {}
        bubbleManager?.destroy()
        captureManager?.teardown()
        triage.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lume",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Bolha flutuante do Lume"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
