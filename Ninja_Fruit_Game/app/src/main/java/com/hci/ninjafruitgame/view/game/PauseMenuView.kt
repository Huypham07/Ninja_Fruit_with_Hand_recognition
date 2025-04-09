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
import com.hci.ninjafruitgame.R
import kotlin.random.Random
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.hci.ninjafruitgame.utils.SoundManager
import com.hci.ninjafruitgame.model.GameState as GS

class PauseMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SliceEffectReceiver {

    private val choreographer = Choreographer.getInstance()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = mutableListOf<Particle>()
    private val splashMarks = mutableListOf<SplashMark>()

    private val bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_settings)
    private val pauseTitleBitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_title)
    private val ringBitmap = BitmapFactory.decodeResource(resources, R.drawable.back)
    private val fruitBitmap = BitmapFactory.decodeResource(resources, R.drawable.bomb)
    private val fruitColor = "#FBF1BC".toColorInt()

    private val btnHome = BitmapFactory.decodeResource(resources, R.drawable.back_to_home)
    private val btnRestart = BitmapFactory.decodeResource(resources, R.drawable.restart)
    private val btnResume = BitmapFactory.decodeResource(resources, R.drawable.resume)

    private val prevBgBitmap = BitmapFactory.decodeResource(resources, R.drawable.prev)
    private val nextBgBitmap = BitmapFactory.decodeResource(resources, R.drawable.next)

    private val cameraBitmap = BitmapFactory.decodeResource(resources, R.drawable.camera_bg)
    private val cameraLockBitmap = BitmapFactory.decodeResource(resources, R.drawable.camera_bg_lock)
    private val handBitmap = BitmapFactory.decodeResource(resources, R.drawable.hand_detect)
    private val handLockBitmap = BitmapFactory.decodeResource(resources, R.drawable.hand_detect_lock)

    private val muteBitmap = BitmapFactory.decodeResource(resources, R.drawable.mute)
    private val unmuteBitmap = BitmapFactory.decodeResource(resources, R.drawable.unmute)

    private var exitAnim: ValueAnimator? = null

    private var exitAlpha = 1f
    private var ringRotation = 0f
    private var fruitRotation = 0f
    private var ringScale = 1.2f

    private var titleY = -200f
    private var bgY = -300f
    private var btnY = 1700f

    private var homeRect = RectF()
    private var restartRect = RectF()
    private var resumeRect = RectF()
    private var prevBgRect = RectF()
    private var nextBgRect = RectF()

    private var cameraRect = RectF()
    private var handRect = RectF()

    private var muteRect = RectF()

    private var currentBgIndex = 1

    private var isExiting = false

    var onBackgroundChange: ((Int) -> Unit)? = null
    var onBackToStart: (() -> Unit)? = null
    var onResume: (() -> Unit)? = null
    var onRestart: (() -> Unit)? = null
    var onToggleMusicEnabled: ((Boolean) -> Unit)? = null
    var onToggleCameraBackground: ((Boolean) -> Unit)? = null
    var onToggleHandDetection: ((Boolean) -> Unit)? = null

    private val introAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 600
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val value = it.animatedValue as Float
            titleY = value * 40f
            bgY = value * 150f

            btnY = 970f - value * 100f
            ringScale = 1.2f - value * 0.5f
            exitAlpha = 0.5f + value * 0.5f
            invalidate()
        }
        start()
    }

    fun show() {
        visibility = VISIBLE
        introAnim.start()
        isExiting = false
    }

    fun playExitAnimation(onFinished: () -> Unit) {

        exitAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                titleY = 40f - value * 150f
                bgY = 150f - value * 600f

                btnY = 870f + value * 300f

                ringScale = 0.7f + value * 0.5f
                exitAlpha = 1f - value
                invalidate()
            }
            doOnEnd {
                visibility = GONE
                onFinished()
            }
            start()
        }
    }

    init {
        choreographer.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                ringRotation += 2f
                fruitRotation -= 3f

                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.update()
                    if (!p.isAlive()) pIter.remove()
                }
                splashMarks.removeAll { !it.isAlive() }
                invalidate()
                choreographer.postFrameCallback(this)
            }
        })
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
        canvas.drawColor("#88000000".toColorInt())

        paint.alpha = (255 * exitAlpha).toInt()

        val centerX = width / 2f

        // scaled bg_settings bitmap
        val maxHeight = height * 0.65f
        val scaledHeight = maxHeight.toInt()
        val aspectRatio = bgBitmap.width.toFloat() / bgBitmap.height.toFloat()
        val scaledWidth = (scaledHeight * aspectRatio).toInt()

        if (GS.isGameStarted()) {
            val titleRect = RectF(
                centerX - scaledWidth * 0.2f,
                titleY,
                centerX + scaledWidth * 0.2f,
                bgY - 10
            )
            canvas.drawBitmap(pauseTitleBitmap, null, titleRect, paint)
        }


        val left = (width - scaledWidth) / 2
        val top = bgY.toInt()
        val right = left + scaledWidth
        val bottom = top + scaledHeight

        val destRect = Rect(left, top, right, bottom)
        canvas.drawBitmap(bgBitmap, null, destRect, paint)

        val iconSize = 100f
        val prevX = centerX + 120f - (200f + iconSize)
        val nextX = centerX + 120f + 200f
        val icon1Y = (bgY + scaledHeight / 2f - 40f - iconSize).toInt()

        prevBgRect.set(prevX, icon1Y.toFloat(), prevX + iconSize, icon1Y + iconSize)
        nextBgRect.set(nextX, icon1Y.toFloat(), nextX + iconSize, icon1Y + iconSize)

        canvas.drawBitmap(prevBgBitmap, null, prevBgRect, paint)
        canvas.drawBitmap(nextBgBitmap, null, nextBgRect, paint)

        val muteX = centerX - 120f - 200f - iconSize
        muteRect.set(muteX, icon1Y.toFloat(), muteX + iconSize, icon1Y + iconSize)

        canvas.drawBitmap(if (GS.isMusicEnabled()) unmuteBitmap else muteBitmap, null, muteRect, paint)

        val icon2XLeft = right - 140f - iconSize
        val icon2XRight = right - 140f
        cameraRect.set(icon2XLeft, bgY + scaledHeight / 2 + 140f - iconSize, icon2XRight, bgY + scaledHeight / 2 + 140f)
        handRect.set(icon2XLeft, bgY + scaledHeight / 2 + 260f - iconSize, icon2XRight, bgY + scaledHeight / 2 + 260f)

        canvas.drawBitmap(if (GS.isUseCamera()) cameraBitmap else cameraLockBitmap, null, cameraRect, paint)
        canvas.drawBitmap(if (GS.isUseHandTracker()) handBitmap else handLockBitmap, null, handRect, paint)

        if (GS.isGameStarted()) {
            // Tính chiều rộng vùng bg_settings đã vẽ
            val totalButtonArea = (scaledWidth * 0.6).toInt()

            // Các nút có cùng kích thước
            val buttonWidth = 160
            val spacing = (totalButtonArea - buttonWidth * 3) / 2f

            val homeLeft = (left + scaledWidth * 0.2f).toInt()
            val homeTop = btnY.toInt()
            val homeRight = homeLeft + buttonWidth
            val homeBottom = homeTop + buttonWidth

            val restartLeft = homeLeft + buttonWidth + spacing.toInt()
            val restartTop = btnY.toInt()
            val restartRight = restartLeft + buttonWidth
            val restartBottom = restartTop + buttonWidth

            val resumeLeft = restartLeft + buttonWidth + spacing.toInt()
            val resumeTop = btnY.toInt()
            val resumeRight = resumeLeft + buttonWidth
            val resumeBottom = resumeTop + buttonWidth

            homeRect.set(
                homeLeft.toFloat(),
                homeTop.toFloat(),
                homeRight.toFloat(),
                homeBottom.toFloat()
            )
            restartRect.set(
                restartLeft.toFloat(),
                restartTop.toFloat(),
                restartRight.toFloat(),
                restartBottom.toFloat()
            )
            resumeRect.set(
                resumeLeft.toFloat(),
                resumeTop.toFloat(),
                resumeRight.toFloat(),
                resumeBottom.toFloat()
            )

            canvas.drawBitmap(btnHome, null, homeRect, paint)
            canvas.drawBitmap(btnRestart, null, restartRect, paint)
            canvas.drawBitmap(btnResume, null, resumeRect, paint)
        } else {
            drawBackFruit(canvas, 260f, 800f)
        }

        splashMarks.forEach { it.draw(canvas, paint) }
        particles.forEach { it.draw(canvas, paint) }

        // Draw background index control row
        paint.color = "#68351a".toColorInt()
        paint.textSize = 52f
        paint.textAlign = Paint.Align.CENTER
        val text = "Background: $currentBgIndex"
        canvas.drawText(text, centerX + 120f, bgY + scaledHeight / 2 - 60f, paint)

        paint.textSize = 48f
        paint.textAlign = Paint.Align.LEFT
        val leftAlignX = left + 140f
        val useCameraBgText = if (GS.isUseCamera()) "Use Camera as background" else "Use Camera as background (locked)"
        canvas.drawText(useCameraBgText, leftAlignX, bgY + scaledHeight / 2 + 120, paint)

        val useHandText = if (GS.isUseHandTracker()) "Use Hand Detection" else "Use Hand Detection (locked)"
        canvas.drawText(useHandText, leftAlignX, bgY + scaledHeight / 2 + 240, paint)

        if (GS.isUseHandTracker()) {
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexFillPaint)
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexStrokePaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexFillPaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexStrokePaint)
        }
    }

    private fun drawBackFruit(canvas: Canvas, x: Float, y: Float) {
        canvas.withTranslation(x, y) {
            rotate(ringRotation)
            scale(ringScale, ringScale)
            drawBitmap(ringBitmap, -ringBitmap.width / 2f, -ringBitmap.height / 2f, paint)
        }

        canvas.withTranslation(x, y) {
            rotate(fruitRotation)
            scale(ringScale, ringScale)
            drawBitmap(fruitBitmap, -fruitBitmap.width / 2f, -fruitBitmap.height / 2f, paint)
        }
    }

    private var canChangeBg = true
    private fun debounceBgChange() {
        canChangeBg = false
        postDelayed({ canChangeBg = true }, 400) // 0.4s delay trước khi cho phép đổi tiếp
    }

    override fun onSliceAt(x: Float, y: Float) {
        if (isExiting) return
        when {
            prevBgRect.contains(x, y) && canChangeBg -> {
                if (currentBgIndex > 1) currentBgIndex--
                onBackgroundChange?.invoke(currentBgIndex)
                debounceBgChange()
            }
            nextBgRect.contains(x, y) && canChangeBg -> {
                if (currentBgIndex < 6) currentBgIndex++
                onBackgroundChange?.invoke(currentBgIndex)
                debounceBgChange()
            }
            muteRect.contains(x, y) -> {
                val enable = !GS.isMusicEnabled()
                onToggleMusicEnabled?.invoke(enable)
            }
            cameraRect.contains(x, y) -> {
                val isUsedCamera = !GS.isUseCamera()
                onToggleCameraBackground?.invoke(isUsedCamera)
            }
            handRect.contains(x, y) -> {
                val isUsedHand = !GS.isUseHandTracker()
                onToggleHandDetection?.invoke(isUsedHand)
            }
        }
        if (!GS.isGameStarted()) {
            val centerX = 260f
            val centerY = 700f
            val dx = x - centerX
            val dy = y - centerY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < fruitBitmap.width * 0.6f) {
                emitParticles(centerX, centerY)
                addSplash(centerX, centerY)
                SoundManager.playSlice()
                isExiting = true
                playExitAnimation {
                    onBackToStart?.invoke()
                }

            }
        } else {
            when {
                homeRect.contains(x, y) -> {
                    isExiting = true
                    playExitAnimation {
                        onBackToStart?.invoke()
                    }
                }

                restartRect.contains(x, y) -> {
                    isExiting = true
                    playExitAnimation {
                        onRestart?.invoke()
                    }
                }

                resumeRect.contains(x, y) -> {
                    isExiting = true
                    playExitAnimation {
                        onResume?.invoke()
                    }
                }
            }
        }
    }

    private fun emitParticles(x: Float, y: Float) {
        repeat(25) {
            val velocity = PointF(Random.nextFloat() * 12f - 6f, Random.nextFloat() * -12f)
            val radius = Random.nextFloat() * 8f + 4f
            val shrink = Random.nextFloat() * 0.15f + 0.05f
            particles.add(
                Particle(
                    PointF(x, y),
                    velocity,
                    radius = radius,
                    color = fruitColor,
                    shrinkRate = shrink
                )
            )
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun addSplash(x: Float, y: Float) {
        val splashResId = resources.getIdentifier("bomb_s", "drawable", context.packageName)
        if (splashResId != 0) {
            val splashBitmap = BitmapFactory.decodeResource(resources, splashResId)
            splashMarks.add(SplashMark(bitmap = splashBitmap, position = PointF(x, y)))
        }
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
}