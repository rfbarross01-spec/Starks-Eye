package com.lume.app.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.lume.app.R
import kotlin.math.abs

/**
 * Gerencia a bolha flutuante do Lume.
 *
 * Comportamentos:
 * - tap simples → onTap (captura screenshot)
 * - long-press (500ms) → onLongPress (V3: vídeo/áudio; V2: força veredito)
 * - drag → reposiciona, com snap-to-edge ao soltar
 * - escondida durante captura (pra não aparecer no screenshot)
 */
class BubbleManager(
    private val context: Context,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var bubbleView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var longPressFired = false
    private val longPressRunnable = Runnable {
        longPressFired = true
        bubbleView?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        onLongPress()
    }

    fun show() {
        if (bubbleView != null) return

        val size = (52 * context.resources.displayMetrics.density).toInt()
        bubbleView = ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_lume_bubble))
            scaleType = ImageView.ScaleType.FIT_CENTER
            elevation = 8f
        }

        params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = context.resources.displayMetrics.widthPixels - size - 32
            y = context.resources.displayMetrics.heightPixels / 3
        }

        bubbleView!!.setOnTouchListener(touchListener)
        windowManager.addView(bubbleView, params)
    }

    fun hide() {
        bubbleView?.visibility = View.GONE
    }

    fun reveal() {
        bubbleView?.visibility = View.VISIBLE
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        try { bubbleView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        bubbleView = null
    }

    private val touchListener = object : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var moved = false
        private val slopPx = 48 * context.resources.displayMetrics.density

        override fun onTouch(v: View?, ev: MotionEvent): Boolean {
            val p = params ?: return false
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    touchX = ev.rawX
                    touchY = ev.rawY
                    moved = false
                    longPressFired = false
                    handler.postDelayed(longPressRunnable, 500)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - touchX
                    val dy = ev.rawY - touchY
                    if (!moved && (abs(dx) > slopPx || abs(dy) > slopPx)) {
                        moved = true
                        handler.removeCallbacks(longPressRunnable)
                    }
                    if (moved) {
                        p.x = (initialX + dx).toInt()
                        p.y = (initialY + dy).toInt()
                        try { windowManager.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (longPressFired) {
                        longPressFired = false
                        snapToEdge()
                        return true
                    }
                    if (!moved) {
                        v?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        onTap()
                    }
                    snapToEdge()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    longPressFired = false
                    return true
                }
            }
            return false
        }
    }

    private fun snapToEdge() {
        val p = params ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val bubbleSize = p.width
        val toX = if (p.x + bubbleSize / 2 < screenWidth / 2) 16
        else screenWidth - bubbleSize - 16

        ValueAnimator.ofInt(p.x, toX).apply {
            duration = 200
            addUpdateListener { anim ->
                p.x = anim.animatedValue as Int
                try { windowManager.updateViewLayout(bubbleView, p) } catch (_: Exception) {}
            }
            start()
        }
    }
}
