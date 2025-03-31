package com.hci.ninjafruitgame.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.hci.ninjafruitgame.R
import kotlin.random.Random


class StartScreenView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), SliceEffectReceiver {

    private val choreographer = Choreographer.getInstance()

    private val backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.bg)
    private val titleBitmap = BitmapFactory.decodeResource(resources, R.drawable.gametitle)
    private val ringBitmap = BitmapFactory.decodeResource(resources, R.drawable.newgame)
    private val fruitBitmap = BitmapFactory.decodeResource(resources, R.drawable.peach)
    private val fruitColor = Color.YELLOW
    private val particles = mutableListOf<Particle>()
    private val splashMarks = mutableListOf<SplashMark>()

    private var isExiting = false
    private var exitAnimator: ValueAnimator? = null

    private var exitAlpha = 1f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var titleY = 0f
    private var ringScale = 2.5f

    private var ringRotation = 0f
    private var fruitRotation = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 800
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val value = it.animatedValue as Float
            // Title đi lên
            titleY = height - value * (height / 2f + titleBitmap.height)
            // Ring thu nhỏ dần
            ringScale = 2.5f - value * 1.5f
            invalidate()
        }
        start()
    }

    fun playExitAnimation(onFinished: () -> Unit) {
        isExiting = true

        val startTitleY = titleY
        val endTitleY = -titleBitmap.height.toFloat() - 100f // Trượt ra khỏi màn

        exitAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()

            addUpdateListener { anim ->
                val value = anim.animatedValue as Float

                // Trượt title lên
                titleY = startTitleY + (endTitleY - startTitleY) * value

                // Scale và mờ vòng + quả
                ringScale = 1f + value * 1.5f
                exitAlpha = (1f - value)

                invalidate()
            }

            doOnEnd {
                onFinished()
            }

            start()
        }
    }


    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            ringRotation += 2f
            fruitRotation -= 3f
            particles.removeAll { !it.isAlive() }
            splashMarks.removeAll { !it.isAlive() }
            invalidate()
            choreographer.postFrameCallback(this)
        }
    }

    init {
        choreographer.postFrameCallback(frameCallback)

        // animation intro vẫn giữ nguyên
        animator.start()
    }



    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(backgroundBitmap, null, Rect(0, 0, width, height), null)

        val centerX = width / 2f
        val centerY = height / 2f + 100f

        val shiftX = 200f

        paint.alpha = (255 * exitAlpha).toInt()

        // Vẽ tiêu đề (từ dưới lên)
        canvas.drawBitmap(
            titleBitmap,
            centerX - titleBitmap.width / 2f,
            titleY,
            paint
        )


        // Vẽ vòng xoay
        canvas.save()
        canvas.translate(centerX + shiftX, centerY)
        canvas.rotate(ringRotation)
        canvas.scale(ringScale, ringScale)
        canvas.drawBitmap(
            ringBitmap,
            -ringBitmap.width / 2f,
            -ringBitmap.height / 2f,
            paint
        )
        canvas.restore()

        // Vẽ trái cây xoay ngược bên trong
        canvas.save()
        canvas.translate(centerX + shiftX, centerY)
        canvas.rotate(fruitRotation)
        canvas.scale(ringScale, ringScale)
        canvas.drawBitmap(
            fruitBitmap,
            -fruitBitmap.width / 2f,
            -fruitBitmap.height / 2f,
            paint
        )
        canvas.restore()

        splashMarks.forEach { it.draw(canvas, paint) }
        particles.forEach { it.draw(canvas, paint) }
    }

    override fun onSliceAt(x: Float, y: Float) {
        if (isSlicedAt(x, y)) {
            emitStartParticles(x, y)
            addStartSplash(x, y)
            playExitAnimation {
                onStartGame?.invoke()
            }
        }
    }

    private fun addStartSplash(x: Float, y: Float) {
        val splashResId = resources.getIdentifier("peach_s", "drawable", context.packageName)

        if (splashResId != 0) {
            val splashBitmap = BitmapFactory.decodeResource(resources, splashResId)
            splashMarks.add(SplashMark(bitmap = splashBitmap, position = PointF(x, y)))
        }
    }

    private fun emitStartParticles(x: Float, y: Float) {
        repeat(25) {
            val velocity = PointF(Random.nextFloat() * 12f - 6f, Random.nextFloat() * -12f)
            val radius = Random.nextFloat() * 8f + 4f
            val shrink = Random.nextFloat() * 0.15f + 0.05f

            particles.add(
                Particle(
                    position = PointF(x, y),
                    velocity = velocity,
                    radius = radius,
                    color = fruitColor,
                    shrinkRate = shrink
                )
            )
        }
    }

    fun isSlicedAt(x: Float, y: Float): Boolean {
        val centerX = width / 2f + 200f
        val centerY = height / 2f + 100f
        val dx = x - centerX
        val dy = y - centerY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        val hitRadius = fruitBitmap.width * 0.6f
        return distance < hitRadius
    }

    var onStartGame: (() -> Unit)? = null
}
