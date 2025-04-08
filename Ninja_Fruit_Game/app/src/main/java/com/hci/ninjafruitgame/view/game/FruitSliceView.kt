package com.hci.ninjafruitgame.view.game

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.ArrayDeque
import java.util.Deque
import androidx.core.graphics.toColorInt

class FruitSliceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maxLen = 10 // độ dài tối đa của đường dao
    private var addWidth = 2f // độ rộng tăng thêm

    private val pointFSMap = mutableMapOf<Int, Deque<PointF>>()         // từng ngón
    private val pointFSCloseMap = mutableMapOf<Int, Deque<PointF>>()    // khép kín dao cho từng ngón

    private val mPaint = Paint()
    private var mShader: Shader? = null

    private var isDiff = false

    private val diff = object : Runnable {
        override fun run() {
            var hasPoint = false
            for (deque in pointFSMap.values) {
                if (deque.isNotEmpty()) {
                    deque.pollFirst()
                    hasPoint = true
                }
            }
            postInvalidate()
            if (hasPoint || isDiff) {
                postDelayed(this, 25)
            }
        }
    }

    private val clearP = Runnable {
        pointFSMap.clear()
        pointFSCloseMap.clear()
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

    private val handSlices = mutableMapOf<Int, Deque<PointF>>() // handId: 1001 (left), 1002 (right)
    private val maxHandPath = 10

    fun registerHandSlice(handId: Int, x: Float, y: Float) {
        val path = handSlices.getOrPut(handId) { ArrayDeque(maxLen) }
        if (path.size >= maxLen) path.pollFirst()
        path.addLast(PointF(x - outRect.left, y - outRect.top))

        pointFSCloseMap.getOrPut(handId) { ArrayDeque(maxLen) }

        postInvalidate()
    }



    fun onTouch(event: MotionEvent) {
        val actionMasked = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                isDiff = true
                removeCallbacks(diff)
                removeCallbacks(clearP)
                postDelayed(diff, 80)

                pointFSMap[pointerId] = ArrayDeque(maxLen)
                pointFSCloseMap[pointerId] = ArrayDeque(maxLen)
                pointFSMap[pointerId]?.addLast(
                    PointF(event.getX(pointerIndex) - outRect.left, event.getY(pointerIndex) - outRect.top)
                )
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i) - outRect.left
                    val y = event.getY(i) - outRect.top
                    if (!pointFSMap.containsKey(id)) continue

                    val deque = pointFSMap[id]!!
                    if (deque.size >= maxLen - 1) deque.pollFirst()
                    deque.addLast(PointF(x, y))
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pointFSMap.remove(pointerId)
                pointFSCloseMap.remove(pointerId)
            }
        }

        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        for ((pointerId, pointFS) in pointFSMap) {
            if (pointFS.isEmpty()) continue

            val pointFSClose = pointFSCloseMap[pointerId] ?: continue
            val path = createPath(pointFS, pointFSClose)

            // Viền dao
            mPaint.color = Color.BLACK
            mPaint.strokeWidth = 1f
            mPaint.style = Paint.Style.STROKE
            mPaint.shader = null
            canvas.drawPath(path, mPaint)

            // Tô dao
            mPaint.style = Paint.Style.FILL
            mPaint.shader = mShader
            canvas.drawPath(path, mPaint)
        }

        // Vẽ dao từ tay camera (hand)
        for ((handId, pointFS) in handSlices) {
            if (pointFS.isEmpty()) continue

            val pointFSClose = pointFSCloseMap.getOrPut(handId) { ArrayDeque(maxLen) }
            val path = createPath(pointFS, pointFSClose)

            // Viền dao
            mPaint.color = Color.BLACK
            mPaint.strokeWidth = 1f
            mPaint.style = Paint.Style.STROKE
            mPaint.shader = null
            canvas.drawPath(path, mPaint)

            // Tô dao
            mPaint.style = Paint.Style.FILL
            mPaint.shader = mShader
            canvas.drawPath(path, mPaint)
        }


    }

    private fun createPath(pointFS: Deque<PointF>, pointFSClose: Deque<PointF>): Path {
        val start = pointFS.peek() ?: return Path().apply { moveTo(0f, 0f) }

        pointFSClose.clear()

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

        while (pointFSClose.isNotEmpty()) {
            val pf = pointFSClose.pollFirst()
            if (pf != null) path.lineTo(pf.x, pf.y)
        }

        path.close()
        return path
    }
}
