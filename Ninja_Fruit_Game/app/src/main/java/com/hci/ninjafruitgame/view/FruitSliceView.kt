package com.hci.ninjafruitgame.view

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Nullable
import java.util.ArrayDeque
import java.util.Deque
import androidx.core.graphics.toColorInt

class FruitSliceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maxLen = 10 // độ dài tối đa của đường dao
    private var addWidth = 3f // độ rộng tăng thêm

    private val pointFS: Deque<PointF> = ArrayDeque(maxLen) // mép trên đường dao
    private val pointFSClose: Deque<PointF> = ArrayDeque(maxLen) // mép dưới đường dao

    private val mPaint = Paint()
    private var mShader: Shader? = null // hiệu ứng màu dao

    private var isDiff = false

    private val diff = object : Runnable {
        override fun run() {
            val pointF = pointFS.pollFirst()
            val delayMillis = 25
            if (pointF != null) {
                postInvalidate()
                postDelayed(this, delayMillis.toLong())
                return
            }

            if (isDiff) {
                postDelayed(this, delayMillis.toLong())
            }
        }
    }

    private val clearP = Runnable {
        pointFS.clear()
        postInvalidate()
    }

    private val outRect = Rect()

    init {
        setWillNotDraw(false)

        mPaint.isAntiAlias = true
        mPaint.pathEffect = CornerPathEffect(5f)

        mShader = LinearGradient(
            0f, 0f, 40f, 60f,
            intArrayOf(
                "#f8f8f8".toColorInt(),
                "#C0C0C0".toColorInt(),
                "#f8f8f8".toColorInt()
            ),
            null,
            Shader.TileMode.CLAMP
        )

        val widthPixels = resources.displayMetrics.widthPixels
        if (widthPixels > 1080) {
            addWidth *= 2
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        getGlobalVisibleRect(outRect)
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(diff)
        removeCallbacks(clearP)
        super.onDetachedFromWindow()
    }

    fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDiff = true
                removeCallbacks(diff)
                removeCallbacks(clearP)
                postDelayed(diff, 80)
                pointFS.clear()
                pointFS.addLast(PointF(event.x - outRect.left, event.y - outRect.top))
                postInvalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                onMove(event.x - outRect.left, event.y - outRect.top)
                postInvalidate()
            }

            MotionEvent.ACTION_UP -> {
                isDiff = false
                postDelayed(clearP, 400)
            }
        }
    }

    private fun onMove(x: Float, y: Float) {
        if (pointFS.size >= maxLen - 1) {
            pointFS.pollFirst()
        }
        pointFS.addLast(PointF(x, y))
    }

    override fun onDraw(canvas: Canvas) {
        val start = pointFS.peek() ?: return

        val path = createPath()

        // Viền
        mPaint.color = Color.BLACK
        mPaint.strokeWidth = 1f
        mPaint.style = Paint.Style.STROKE
        mPaint.shader = null
        canvas.drawPath(path, mPaint)

        // Tô màu dao
        mPaint.style = Paint.Style.FILL
        mPaint.shader = mShader
        canvas.drawPath(path, mPaint)
    }

    private fun createPath(): Path {
        val start = pointFS.peek() ?: return Path().apply { moveTo(0f, 0f) }

        val path = Path()
        path.moveTo(start.x, start.y)

        var width = 1
        var pre: PointF? = null
        var next: PointF?

        val iterator = pointFS.iterator()
        while (iterator.hasNext()) {
            next = iterator.next()
            if (iterator.hasNext()) {
                val v = width / 2f
                var k = 0f
                if (pre != null && next.x != pre.x) {
                    k = (next.y - pre.y) / (next.x - pre.x)
                }
                if (Math.abs(1 - k) < 0.9) {
                    path.lineTo(next.x, next.y - v)
                    pointFSClose.addFirst(PointF(next.x, next.y + v))
                } else {
                    path.lineTo(next.x - v, next.y - v)
                    pointFSClose.addFirst(PointF(next.x + v, next.y + v))
                }
                pre = next
            } else {
                path.lineTo(next.x, next.y)
            }
            width += addWidth.toInt()
        }

        while (pointFSClose.peekFirst() != null) {
            val pf = pointFSClose.pollFirst()
            if (pf == null) break
            path.lineTo(pf.x, pf.y)
        }

        path.close()
        return path
    }

    fun registerWristSlice(x: Float, y: Float) {
        // xử lý giống như cảm ứng thường
        onTouch(MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_MOVE,
            x,
            y,
            0
        ))
    }

}
