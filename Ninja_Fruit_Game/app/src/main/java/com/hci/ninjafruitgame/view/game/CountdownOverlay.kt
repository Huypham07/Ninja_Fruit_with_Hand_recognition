package com.hci.ninjafruitgame.view.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import androidx.core.graphics.withScale

class CountdownOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var count = 3
    private var onFinish: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isCountingDown = false
    private var countdownRunnable: Runnable? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 180f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Thêm Paint cho overlay nền tối
    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 128
    }

    private var scaleFactor = 1f
    private var startTime = 0L

    fun startCountdown(onDone: () -> Unit) {
        count = 3
        onFinish = onDone
        visibility = VISIBLE
        startTime = System.currentTimeMillis()
        isCountingDown = true
        next()
    }

    fun cancelCountdown() {
        if (isCountingDown) {
            isCountingDown = false
            countdownRunnable?.let { handler.removeCallbacks(it) }
            countdownRunnable = null
            visibility = GONE
        }
    }

    private fun next() {
        startTime = System.currentTimeMillis()
        invalidate()

        if (!isCountingDown) {
            return
        }

        if (count == 0) {
            countdownRunnable = Runnable {
                if (isCountingDown) {
                    visibility = GONE
                    isCountingDown = false
                    onFinish?.invoke()
                }
            }
            handler.postDelayed(countdownRunnable!!, 500)
        } else {
            countdownRunnable = Runnable {
                if (isCountingDown) {
                    count--
                    next()
                }
            }
            handler.postDelayed(countdownRunnable!!, 700)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isCountingDown) return

        // Vẽ overlay nền tối trước khi vẽ text
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Hiệu ứng scale dao động bằng sin theo thời gian
        val elapsed = System.currentTimeMillis() - startTime
        scaleFactor = 1f + 0.2f * sin((elapsed % 700) / 700f * Math.PI).toFloat()

        canvas.withScale(scaleFactor, scaleFactor, width / 2f, height / 2f) {
            val displayText = if (count == 0) "GO!" else count.toString()
            drawText(displayText, width / 2f, height / 2f, paint)
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelCountdown()
    }
}