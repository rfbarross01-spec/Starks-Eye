package com.lume.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Captura screenshot via MediaProjection.
 * Mantém VirtualDisplay durante a sessão pra capturas rápidas em sequência.
 */
class ScreenCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    fun isReady(): Boolean = mediaProjection != null && virtualDisplay != null

    @SuppressLint("WrongConstant")
    fun setup(resultCode: Int, resultData: Intent) {
        teardown()

        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, resultData)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { teardown() }
            }, null)
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        // Limita resolução pra reduzir memória / tempo
        val maxDim = 1568
        val scale = if (maxOf(metrics.widthPixels, metrics.heightPixels) > maxDim) {
            maxDim.toFloat() / maxOf(metrics.widthPixels, metrics.heightPixels)
        } else 1f
        screenWidth = (metrics.widthPixels * scale).toInt()
        screenHeight = (metrics.heightPixels * scale).toInt()
        screenDensity = metrics.densityDpi

        handlerThread = HandlerThread("LumeScreenCapture").apply { start() }
        handler = Handler(handlerThread!!.looper)

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "LumeVirtualDisplay",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, handler
        )
    }

    suspend fun captureSingle(timeoutMs: Long = 3000): Bitmap? = suspendCancellableCoroutine { cont ->
        val reader = imageReader
        if (reader == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val timeoutRunnable = Runnable {
            if (cont.isActive) {
                try {
                    reader.setOnImageAvailableListener(null, null)
                } catch (_: Exception) {}
                cont.resume(null)
            }
        }
        handler?.postDelayed(timeoutRunnable, timeoutMs)

        reader.setOnImageAvailableListener({ r ->
            handler?.removeCallbacks(timeoutRunnable)
            var image: Image? = null
            try {
                image = r.acquireLatestImage()
                if (image != null && cont.isActive) {
                    val bitmap = imageToBitmap(image)
                    cont.resume(bitmap)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            } finally {
                image?.close()
                try { r.setOnImageAvailableListener(null, null) } catch (_: Exception) {}
            }
        }, handler)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmapWidth = image.width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(bitmapWidth, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return if (rowPadding != 0) {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            bitmap.recycle()
            cropped
        } else bitmap
    }

    fun teardown() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        try { handlerThread?.quitSafely() } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        handlerThread = null
        handler = null
    }
}
