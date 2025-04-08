package com.hci.ninjafruitgame.view.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import com.hci.ninjafruitgame.model.GameObject
import com.hci.ninjafruitgame.R
import com.hci.ninjafruitgame.model.GameObjectType
import com.hci.ninjafruitgame.model.SlicedPiece
import kotlin.random.Random
import androidx.core.content.edit
import com.hci.ninjafruitgame.utils.SoundManager
import kotlin.math.sqrt

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), SliceEffectReceiver {

    private val choreographer = Choreographer.getInstance()

    private var backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.bg1)

    private val fruits = mutableListOf<GameObject>()
    private val particles = mutableListOf<Particle>()
    private val fruitBitmaps = listOf(
        BitmapFactory.decodeResource(resources, R.drawable.apple),
        BitmapFactory.decodeResource(resources, R.drawable.basaha),
        BitmapFactory.decodeResource(resources, R.drawable.peach),
        BitmapFactory.decodeResource(resources, R.drawable.sandia),
        BitmapFactory.decodeResource(resources, R.drawable.banana),
        BitmapFactory.decodeResource(resources, R.drawable.orange)
    )

    private val fruitBitmapResIds = listOf(
        R.drawable.apple,
        R.drawable.basaha,
        R.drawable.peach,
        R.drawable.sandia,
        R.drawable.banana,
        R.drawable.orange
    )

    private val fruitColorMap = mapOf(
        R.drawable.apple to Color.GREEN,
        R.drawable.basaha to Color.RED,
        R.drawable.peach to Color.YELLOW,
        R.drawable.sandia to Color.GREEN,
        R.drawable.banana to Color.YELLOW,
        R.drawable.orange to Color.rgb(255, 165, 0) // Màu cam
    )

    private val bombs = mutableListOf<GameObject>()
    private val bombBitmap = BitmapFactory.decodeResource(resources, R.drawable.bomb)
    private val bombBitmapResIds = R.drawable.bomb

    private val freezes = mutableListOf<GameObject>()
    private val freezeBitmap = BitmapFactory.decodeResource(resources, R.drawable.freeze_fruit)
    private val freezeBitmapResId = R.drawable.freeze_fruit

    private var isFreezeActive = false
    private var freezeStartTime = 0L
    private val freezeDuration = 10000L // 10 giây
    private val freezeOverlayBitmap = BitmapFactory.decodeResource(resources, R.drawable.freeze_bg)

    private var isPaused = false
    fun getIsPaused(): Boolean {
        return isPaused
    }

    private var lives = 3
    private val maxLives = 3
    private val lifeBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.life)
    private val lifeSize = 80
    private val lifeMargin = 30

    private var isGameOver = false
    private var gameOverScale = 2.5f
    private var scaleDirection = -1
    private var gameOverBitmap = BitmapFactory.decodeResource(resources, R.drawable.gameover)
    private var scorePaint = Paint().apply {
        color = Color.YELLOW
        textAlign = Paint.Align.CENTER
        textSize = 80f
        isAntiAlias = true
    }


    private var currentScore = 0
    private val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    private var bestScore = prefs.getInt("best_score", 0)
    private val scoreIcon = BitmapFactory.decodeResource(resources, R.drawable.score)

    private val scorePops = mutableListOf<ScorePop>()
    private val slicedPieces = mutableListOf<SlicedPiece>()
    private val splashMarks = mutableListOf<SplashMark>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gravity = 0.6f
    private var spawnCounter = 0

    private var isRemovedBackground = false
    private var useHandDetection = false

    fun setHandDetection(enable: Boolean) {
        useHandDetection = enable
    }

    fun removeBackground(remove: Boolean) {
        isRemovedBackground = remove
    }

    private val gameLoop = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            update()
            invalidate()
            choreographer.postFrameCallback(this) // gọi lại khung tiếp theo
        }
    }


    init {
        choreographer.postFrameCallback(gameLoop)
    }


    private fun update() {
        if (isPaused || isGameOver) return

        // Kiểm tra hiệu ứng freeze hết hạn
        if (isFreezeActive && System.currentTimeMillis() - freezeStartTime > freezeDuration) {
            isFreezeActive = false
        }

        // Tính toán tỉ lệ làm chậm
        val speedFactor = if (isFreezeActive) 0.25f else 1f


        // Spawn fruit mỗi 1.2s
        spawnCounter++
        if (spawnCounter >= 75) {
            if (spawnCounter % 75 == 0) {
                spawnFruit()
            }
            if (spawnCounter >= 300) {
                spawnFreeze()
                spawnBomb()
                spawnCounter = 0
            }
        }

        // Update Freeze
        val fIter = freezes.iterator()
        while (fIter.hasNext()) {
            val freeze = fIter.next()
            freeze.update(gravity, speedFactor)

            if (freeze.position.y > height + 200) {
                fIter.remove() // rơi xuống đáy
            }
        }

        // Cập nhật quả
        val iterator = fruits.iterator()
        while (iterator.hasNext()) {
            val fruit = iterator.next()
            fruit.update(gravity, speedFactor)

            if (fruit.position.y > height + 200) {
                iterator.remove() // rơi xuống đáy
            }
        }

        val bIter = bombs.iterator()
        while (bIter.hasNext()) {
            val bomb = bIter.next()
            bomb.update(gravity, speedFactor)

            if (bomb.position.y > height + 200) {
                bIter.remove() // rơi xuống đáy
            }
        }

        // Cập nhật particle
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.update()
            if (!p.isAlive()) pIter.remove()
        }

        // Cập nhật các mảnh đã cắt
        val iter = slicedPieces.iterator()
        while (iter.hasNext()) {
            val piece = iter.next()
            piece.update(gravity, speedFactor)
            if (piece.position.y > height + 200) {
                iter.remove()
            }
        }

        splashMarks.removeAll { !it.isAlive() }
        scorePops.forEach { it.update() }
        scorePops.removeAll { !it.isAlive() }
    }

    private fun spawnSingle(type: Int) {
        var bitmap = fruitBitmaps.random()
        val fruitResId = fruitBitmapResIds[fruitBitmaps.indexOf(bitmap)]
        if (type == GameObjectType.TYPE_BOMB.value) {
            bitmap = bombBitmap
        }

        val startX = Random.nextInt(100, maxOf(101, width - 200)).toFloat()
        val screenMid = width / 2f

        val xVelocity = when {
            startX < screenMid - 100 -> Random.nextFloat() * 5f + 1f      // bay sang phải
            startX > screenMid + 100 -> -Random.nextFloat() * 5f - 1f     // bay sang trái
            else -> Random.nextFloat() * 2f - 1f                          // bay thẳng đứng (gần như 0)
        }

        val minHeight = height / 2f + bitmap.height / 2f
        val maxHeight = height.toFloat() - bitmap.height / 2f

        val targetHeight = Random.nextFloat() * (maxHeight - minHeight) + minHeight
        val yVelocity = -sqrt(2 * gravity * targetHeight) * 1.0f

        if (type == GameObjectType.TYPE_FRUIT.value) {
            val fruit = GameObject(
                bitmap = bitmap,
                bitmapResId = fruitResId,
                position = PointF(startX, height.toFloat()),
                velocity = PointF(xVelocity, yVelocity),
                rotationSpeed = Random.nextFloat() * 8f - 4f,
                type = GameObjectType.TYPE_FRUIT.value
            )

            fruits.add(fruit)
        } else if (type == GameObjectType.TYPE_BOMB.value) {
            val bomb = GameObject(
                bitmap = bitmap,
                bitmapResId = bombBitmapResIds,
                position = PointF(startX, height.toFloat()),
                velocity = PointF(xVelocity, yVelocity),
                rotationSpeed = Random.nextFloat() * 8f - 4f,
                type = GameObjectType.TYPE_BOMB.value
            )

            bombs.add(bomb)

        }
    }

    private fun spawnFruit() {
        if (width < 300 || height < 300) return

        // random number of fruits
        val numFruits = Random.nextInt(1, 4)
        for (i in 0 until numFruits) {
            spawnSingle(GameObjectType.TYPE_FRUIT.value)
        }
    }

    private fun spawnBomb() {
        if (width < 300 || height < 300) return

        val numBombs = Random.nextInt(1, 2)
        for (i in 0 until numBombs) {
            spawnSingle(GameObjectType.TYPE_BOMB.value)
        }
    }

    private fun spawnFreeze() {
        if (Random.nextFloat() > 0.25f) return // Hiếm hơn

        val startX = Random.nextInt(100, width - 200).toFloat()
        val screenMid = width / 2f
        val xVelocity = when {
            startX < screenMid - 100 -> Random.nextFloat() * 5f + 1f
            startX > screenMid + 100 -> -Random.nextFloat() * 5f - 1f
            else -> Random.nextFloat() * 2f - 1f
        }

        val minHeight = height / 2f + freezeBitmap.height / 2f
        val maxHeight = height.toFloat() - freezeBitmap.height / 2f
        val targetHeight = Random.nextFloat() * (maxHeight - minHeight) + minHeight
        val yVelocity = -sqrt(2 * gravity * targetHeight)

        val freeze = GameObject(
            bitmap = freezeBitmap,
            bitmapResId = freezeBitmapResId,
            position = PointF(startX, height.toFloat()),
            velocity = PointF(xVelocity, yVelocity),
            rotationSpeed = Random.nextFloat() * 8f - 4f,
            type = GameObjectType.TYPE_FREEZE.value
        )
        freezes.add(freeze)
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

        freezes.forEach { it.draw(canvas) }

        splashMarks.forEach { it.draw(canvas, paint) }
        fruits.forEach { it.draw(canvas) }
        bombs.forEach { it.draw(canvas) }
        slicedPieces.forEach { it.draw(canvas) }
        particles.forEach { it.draw(canvas, paint) }
        scorePops.forEach { it.draw(canvas, scorePopTextPaint) }

        if (isFreezeActive) {
            canvas.drawBitmap(freezeOverlayBitmap, null, Rect(0, 0, width, height), null)
        }

        if (useHandDetection) {
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexFillPaint)
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexStrokePaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexFillPaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexStrokePaint)
        }

        drawScore(canvas)
        drawLives(canvas)

        if (isGameOver) {
            val overlayPaint = Paint().apply {
                color = Color.argb((0.6f * 255).toInt(), 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
            gameOverScale += scaleDirection * 0.01f
            if (gameOverScale <= 1f) {
                gameOverScale = 1f
                scaleDirection = 1
            } else if (gameOverScale >= 1.25f) {
                gameOverScale = 1.25f
                scaleDirection = -1
            }

            canvas.save()
            canvas.scale(gameOverScale, gameOverScale, width / 2f, height / 2f - 50f)
            canvas.drawBitmap(gameOverBitmap, width / 2f - gameOverBitmap.width / 2f, height / 2f - gameOverBitmap.height / 2f, null)
            canvas.restore()

            canvas.drawText("Score: $currentScore", width / 2f, height / 2f + gameOverBitmap.height, scorePaint)

            invalidate() // Để giữ cho animation chạy
        }
    }

    private fun drawLives(canvas: Canvas) {
        for (i in 0 until lives) {
            val left = width - (i + 1) * (lifeSize + lifeMargin)
            val top = lifeMargin
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(lifeBitmap, lifeSize, lifeSize, false),
                left.toFloat(),
                top.toFloat(),
                null
            )
        }
    }


    override fun onSliceAt(x: Float, y: Float) {
        sliceAt(x, y)
    }

    @SuppressLint("DiscouragedApi")
    fun sliceAt(x: Float, y: Float) {
        fruits.forEach { fruit ->
            if (!fruit.isSliced) {
                val centerX = fruit.position.x + fruit.bitmap.width / 2f
                val centerY = fruit.position.y + fruit.bitmap.height / 2f
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                val hitRadius = fruit.bitmap.width * 0.6f
                if (distance < hitRadius) {
                    fruit.isSliced = true

                    currentScore++
                    scorePops.add(
                        ScorePop(
                            position = PointF(centerX + 30f, centerY), // gần điểm chém
                            text = "+1"
                        )
                    )

                    if (currentScore > bestScore) {
                        bestScore = currentScore
                        prefs.edit { putInt("best_score", bestScore) }
                    }

                    val fruitColor = fruitColorMap[fruit.bitmapResId] ?: Color.WHITE
                    emitParticles(centerX, centerY, fruitColor)
                    addSplashMark(centerX, centerY, fruit.bitmapResId)

                    // lấy tên quả từ resource name
                    val resName = resources.getResourceEntryName(fruit.bitmapResId)
                    val leftResId =
                        resources.getIdentifier("${resName}_l", "drawable", context.packageName)
                    val rightResId =
                        resources.getIdentifier("${resName}_r", "drawable", context.packageName)

                    val leftBitmap = BitmapFactory.decodeResource(resources, leftResId)
                    val rightBitmap = BitmapFactory.decodeResource(resources, rightResId)

                    // tạo 2 mảnh rơi lệch
                    val leftPiece = SlicedPiece(
                        bitmap = leftBitmap,
                        position = PointF(fruit.position.x, fruit.position.y),
                        velocity = PointF(fruit.velocity.x - 3f, fruit.velocity.y * 1.2f),
                        rotationSpeed = -5f
                    )

                    val rightPiece = SlicedPiece(
                        bitmap = rightBitmap,
                        position = PointF(fruit.position.x + 20f, fruit.position.y),
                        velocity = PointF(fruit.velocity.x + 3f, fruit.velocity.y * 1.15f),
                        rotationSpeed = 6f
                    )

                    if (leftPiece.velocity.y < 0) {
                        leftPiece.velocity.y = 0f
                    }
                    if (rightPiece.velocity.y < 0) {
                        rightPiece.velocity.y = 0f
                    }


                    slicedPieces.add(leftPiece)
                    slicedPieces.add(rightPiece)
                    SoundManager.playSlice()
                }
            }
        }

        bombs.forEach { bomb ->
            if (!bomb.isSliced) {
                val centerX = bomb.position.x + bomb.bitmap.width / 2f
                val centerY = bomb.position.y + bomb.bitmap.height / 2f
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                val hitRadius = bomb.bitmap.width * 0.6f
                if (distance < hitRadius) {
                    SoundManager.playSliceBomb()
                    lives--
                    if (lives <= 0) {
                        triggerGameOver()
                    } else {
                        bomb.isSliced = true
                    }
                }
            }
        }

        freezes.forEach { freeze ->
            if (!freeze.isSliced) {
                val centerX = freeze.position.x + freeze.bitmap.width / 2f
                val centerY = freeze.position.y + freeze.bitmap.height / 2f
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                val hitRadius = freeze.bitmap.width * 0.6f
                if (distance < hitRadius) {
                    freeze.isSliced = true

                    isFreezeActive = true
                    freezeStartTime = System.currentTimeMillis()

                    emitParticles(centerX, centerY, Color.CYAN)
                    SoundManager.playSlice()
                }
            }
        }


    }

    private fun triggerGameOver() {
        isGameOver = true
        invalidate()

        onGameOver?.invoke()
    }

    @SuppressLint("DiscouragedApi")
    private fun addSplashMark(x: Float, y: Float, fruitResId: Int) {
        val resName = resources.getResourceEntryName(fruitResId)
        val splashResId = resources.getIdentifier("${resName}_s", "drawable", context.packageName)

        if (splashResId != 0) {
            val splashBitmap = BitmapFactory.decodeResource(resources, splashResId)
            splashMarks.add(SplashMark(bitmap = splashBitmap, position = PointF(x, y)))
        }
    }

    private fun emitParticles(x: Float, y: Float, fruitColor: Int) {
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

    private val scoreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 215, 0) // vàng đậm
        textSize = 64f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val scorePopTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 215, 0) // vàng đậm
        textSize = 64f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val scoreStrokePaint = Paint(scoreTextPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.BLACK
    }

    private val bestTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 215, 0) // vàng đậm
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val bestStrokePaint = Paint(bestTextPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLACK
    }

    private fun drawScore(canvas: Canvas) {
        val iconX = 40f
        val iconY = 60f

        // Vẽ icon điểm
        canvas.drawBitmap(scoreIcon, iconX, iconY, null)

        // Vị trí điểm hiện tại
        val scoreX = iconX + scoreIcon.width + 20f
        val scoreY = iconY + scoreIcon.height * 0.75f

        // Vẽ điểm hiện tại với viền
        canvas.drawText(currentScore.toString(), scoreX, scoreY, scoreStrokePaint)
        canvas.drawText(currentScore.toString(), scoreX, scoreY, scoreTextPaint)

        // Vị trí Best Score
        val bestY = scoreY + 70f
        canvas.drawText("Best: $bestScore", iconX, bestY, bestStrokePaint)
        canvas.drawText("Best: $bestScore", iconX, bestY, bestTextPaint)
    }

    fun pauseGame() {
        isPaused = true
    }

    fun resumeGame() {
        isPaused = false
    }

    fun resetGame() {
        isGameOver = false
        // Reset tất cả danh sách
        fruits.clear()
        slicedPieces.clear()
        particles.clear()
        splashMarks.clear()
        scorePops.clear()
        bombs.clear()

        // Reset điểm
        currentScore = 0
        lives = maxLives

        // Reset trạng thái
        isPaused = false
        spawnCounter = 0

        // Reset background nếu cần (giữ nguyên hoặc về mặc định)
        // backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg)
        invalidate()
    }


    fun setBackground(resId: Int) {
        backgroundBitmap = BitmapFactory.decodeResource(resources, resId)
    }

    private var pauseRequested: (() -> Unit)? = null

    fun setOnPauseRequestedListener(listener: () -> Unit) {
        pauseRequested = listener
    }

    private var onGameOver: (() -> Unit)? = null

    fun setOnGameOverListener(listener: () -> Unit) {
        onGameOver = listener
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