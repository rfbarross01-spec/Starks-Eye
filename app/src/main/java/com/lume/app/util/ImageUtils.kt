package com.lume.app.util

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Redimensiona pra max dimension e exporta como JPEG bytes.
     * Mantém aspect ratio. Pra screenshots de celular (1080x2400) → ~768x1707.
     */
    fun bitmapToJpegBytes(
        bitmap: Bitmap,
        maxDimension: Int = 1568,
        quality: Int = 85
    ): ByteArray {
        val resized = resizeToMaxDimension(bitmap, maxDimension)
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
        if (resized !== bitmap) resized.recycle()
        return output.toByteArray()
    }

    fun resizeToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val max = maxOf(w, h)
        if (max <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / max
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }
}
