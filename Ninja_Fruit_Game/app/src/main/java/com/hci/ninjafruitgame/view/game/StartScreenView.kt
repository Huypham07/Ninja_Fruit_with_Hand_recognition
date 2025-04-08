package com.hci.ninjafruitgame.view.game

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toColorInt
import com.hci.ninjafruitgame.R
import kotlin.random.Random
import androidx.core.graphics.withTranslation
import com.hci.ninjafruitgame.utils.SoundManager


class StartScreenView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), SliceEffectReceiver {

    private val choreographer = Choreographer.getInstance()

    private var backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.bg1)
    private val titleBitmap = BitmapFactory.decodeResource(resources, R.drawable.gametitle)
    private val ringBitmap = BitmapFactory.decodeResource(resources, R.drawable.newgame)
    private val fruitBitmap = BitmapFactory.decodeResource(resources, R.drawable.peach)
    private val fruitColor = Color.YELLOW

    private val ringSettingBitmap = BitmapFactory.decodeResource(resources, R.drawable.settings)
    private val fruitSettingBitmap = BitmapFactory.decodeResource(resources, R.drawable.apple)
    private val fruitSettingColor = Color.GREEN

    private val ringQuitBitmap = BitmapFactory.decodeResource(resources, R.drawable.quit)
    private val fruitQuitBitmap = BitmapFactory.decodeResource(resources, R.drawable.bomb)
    private val fruitQuitColor = "#FBF1BC".toColorInt()

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

    private val shiftX = 150f
    private val shiftY = 100f

    private val largeShiftY = 250f

    private var isRemovedBackground = false

    fun removeBackground(remove: Boolean) {
        isRemovedBackground = remove
    }

    private var useHandDetection = false

    fun setHandDetection(enable: Boolean) {
        useHandDetection = enable
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 800
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val value = it.animatedValue as Float
            exitAlpha = 0.8f + value * 0.2f
            // Title đi lên
            titleY = height - value * height
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

    fun show() {
        visibility = VISIBLE
        isExiting = false
        animator.start()
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            ringRotation += 2f
            fruitRotation -= 3f
            val pIter = particles.iterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.update()
                if (!p.isAlive()) pIter.remove()
            }
            // remove all particles
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

    private val leftIndexFillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val leftIndexStrokePaint = Paint().apply {
        color = Color.WHITE
        alpha = (255 * 0.4f).toInt() // 30% opacity
        style = Paint.Style.STROKE
        strokeWidth = 40f
        isAntiAlias = true
    }

    private val rightIndexFillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val rightIndexStrokePaint = Paint().apply {
        color = Color.WHITE
        alpha = (255 * 0.4f).toInt() // 30% opacity
        style = Paint.Style.STROKE
        strokeWidth = 40f
        isAntiAlias = true
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isRemovedBackground) {
            canvas.drawBitmap(backgroundBitmap, null, Rect(0, 0, width, height), null)
        }

        val centerX = width / 2f
        val centerY = height / 2f

        paint.alpha = (255 * exitAlpha).toInt()

        // Vẽ tiêu đề (từ dưới lên)
        canvas.drawBitmap(titleBitmap, centerX - titleBitmap.width / 2f, titleY, paint)


        drawFruitWithRing(
            canvas,
            centerX + shiftX,
            centerY + shiftY,
            ringBitmap,
            fruitBitmap,
            ringScale,
            fruitRotation,
            ringRotation
        )
        drawFruitWithRing(
            canvas,
            centerX - shiftX - ringSettingBitmap.width / 2,
            centerY + largeShiftY,
            ringSettingBitmap,
            fruitSettingBitmap,
            ringScale * 0.8f,
            fruitRotation * 0.8f,
            ringRotation * 0.8f
        )
        drawFruitWithRing(
            canvas,
            centerX + shiftX + ringBitmap.width,
            centerY + largeShiftY,
            ringQuitBitmap,
            fruitQuitBitmap,
            ringScale * 0.8f,
            fruitRotation * 0.8f,
            ringRotation * 0.8f
        )


        splashMarks.forEach { it.draw(canvas, paint) }
        particles.forEach { it.draw(canvas, paint) }

        if (useHandDetection) {
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexFillPaint)
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexStrokePaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexFillPaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexStrokePaint)
        }
    }

    private fun drawFruitWithRing(
        canvas: Canvas,
        x: Float,
        y: Float,
        ring: Bitmap,
        fruit: Bitmap,
        scale: Float,
        fruitRot: Float,
        ringRot: Float
    ) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(ringRot)
        canvas.scale(scale, scale)
        canvas.drawBitmap(ring, -ring.width / 2f, -ring.height / 2f, paint)
        canvas.restore()

        canvas.withTranslation(x, y) {
            rotate(fruitRot)
            scale(scale, scale)
            drawBitmap(fruit, -fruit.width / 2f, -fruit.height / 2f, paint)
        }
    }


    override fun onSliceAt(x: Float, y: Float) {
        if (isExiting) return

        when {
            isSlicedAt(x, y, width / 2f + shiftX, height / 2f + shiftY, fruitBitmap) -> {
                emitStartParticles(x, y, fruitColor)
                addStartSplash(x, y, "peach")
                SoundManager.playSlice()
                playExitAnimation {
                    onStartGame?.invoke()
                }
            }

            isSlicedAt(
                x,
                y,
                width / 2f - shiftX - ringSettingBitmap.width / 2,
                height / 2f + largeShiftY,
                fruitSettingBitmap
            ) -> {
                emitStartParticles(x, y, fruitSettingColor)
                addStartSplash(x, y, "apple")
                SoundManager.playSlice()
                onOpenSettings?.invoke()

            }

            isSlicedAt(
                x,
                y,
                width / 2f + shiftX + ringBitmap.width,
                height / 2f + largeShiftY,
                fruitQuitBitmap
            ) -> {
                emitStartParticles(x, y, fruitQuitColor)
                addStartSplash(x, y, "bomb")
                SoundManager.playSlice()
                onQuit?.invoke()
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun addStartSplash(x: Float, y: Float, name: String) {
        val splashResId = resources.getIdentifier("${name}_s", "drawable", context.packageName)

        if (splashResId != 0) {
            val splashBitmap = BitmapFactory.decodeResource(resources, splashResId)
            splashMarks.add(SplashMark(bitmap = splashBitmap, position = PointF(x, y)))
        }
    }

    private fun emitStartParticles(x: Float, y: Float, color: Int) {
        repeat(25) {
            val velocity = PointF(Random.nextFloat() * 12f - 6f, Random.nextFloat() * -12f)
            val radius = Random.nextFloat() * 8f + 4f
            val shrink = Random.nextFloat() * 0.15f + 0.05f

            particles.add(
                Particle(
                    position = PointF(x, y),
                    velocity = velocity,
                    radius = radius,
                    color = color,
                    shrinkRate = shrink
                )
            )
        }
    }

    fun isSlicedAt(x: Float, y: Float, posX: Float, posY: Float, fruit: Bitmap): Boolean {
        val dx = x - posX
        val dy = y - posY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        val hitRadius = fruit.width * 0.6f
        return distance < hitRadius
    }

    fun setBackground(resId: Int) {
        backgroundBitmap = BitmapFactory.decodeResource(resources, resId)
    }

    private var leftHandX = 0f
    private var leftHandY = 0f
    private var rightHandX = 0f
    private var rightHandY = 0f

    fun updateLeftHandPosition(leftX: Float, leftY: Float) {
        leftHandX = leftX
        leftHandY = leftY
    }

    fun updateRightHandPosition(rightX: Float, rightY: Float) {
        rightHandX = rightX
        rightHandY = rightY
    }

    var onStartGame: (() -> Unit)? = null
    var onOpenSettings: (() -> Unit)? = null
    var onQuit: (() -> Unit)? = null
}
