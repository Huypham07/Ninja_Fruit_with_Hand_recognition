package com.hci.ninjafruitgame.view.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View
import com.hci.ninjafruitgame.model.GameObject
import com.hci.ninjafruitgame.R
import com.hci.ninjafruitgame.model.GameObjectType
import com.hci.ninjafruitgame.model.PlayerState
import com.hci.ninjafruitgame.model.SlicedPiece
import kotlin.random.Random
import com.hci.ninjafruitgame.model.GameState as GS
import com.hci.ninjafruitgame.utils.SoundManager
import kotlin.math.sqrt

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), SliceEffectReceiver, HandPositionUpdatable {

    private val playerState = PlayerState()

    private val choreographer = Choreographer.getInstance()

    private var backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.bg1)

    private val gameObjects = mutableListOf<GameObject>()
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
        R.drawable.orange to Color.rgb(255, 165, 0)
    )

    private val bombBitmap = BitmapFactory.decodeResource(resources, R.drawable.bomb)
    private val bombBitmapResIds = R.drawable.bomb

    private val freezeBitmap = BitmapFactory.decodeResource(resources, R.drawable.freeze_fruit)
    private val freezeBitmapResId = R.drawable.freeze_fruit
    private val freezeOverlayBitmap = BitmapFactory.decodeResource(resources, R.drawable.freeze_bg)


    private val explodeBitmap = BitmapFactory.decodeResource(resources, R.drawable.explode)
    private val explodeBitmapResId = R.drawable.explode

    private val guardBitmap = BitmapFactory.decodeResource(resources, R.drawable.guard)
    private val guardBitmapResId = R.drawable.guard

    private val lifeBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.life)
    private val lifeSize = 80
    private val lifeMargin = 30

    private val guardSize = 80

    private var gameOverScale = 2.5f
    private var scaleDirection = -1
    private var gameOverBitmap = BitmapFactory.decodeResource(resources, R.drawable.gameover)
    private var scorePaint = Paint().apply {
        color = Color.YELLOW
        textAlign = Paint.Align.CENTER
        textSize = 80f
        isAntiAlias = true
    }

    private val scoreIcon = BitmapFactory.decodeResource(resources, R.drawable.score)
    private val scorePops = mutableListOf<ScorePop>()
    private val slicedPieces = mutableListOf<SlicedPiece>()
    private val splashMarks = mutableListOf<SplashMark>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gravity = 0.6f
    private var spawnCounter = 0

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
        if (GS.isPaused || playerState.isGameOver) return

        // Kiểm tra hiệu ứng freeze hết hạn
        if (playerState.isFreeze && System.currentTimeMillis() - playerState.freezeStartTime > playerState.freezeDuration) {
            playerState.isFreeze = false
        }

        // Tính toán tỉ lệ làm chậm
        val speedFactor = if (playerState.isFreeze) 0.25f else 1f


        // Spawn fruit mỗi 1.2s

        spawnCounter++
        if (spawnCounter >= 75) {
            if (spawnCounter % 75 == 0) {
                spawnFruit()
            }
            if (spawnCounter % 300 == 0) {
                spawnBomb()
            }
            if (spawnCounter >= 600) {
                spawnFreeze()
                spawnExplode()
                spawnGuard()
                spawnCounter = 0
            }
        }

        val gIter = gameObjects.iterator()
        while(gIter.hasNext()) {
            val gameObject = gIter.next()
            gameObject.update(gravity, speedFactor)

            if (gameObject.position.y > height + 200) {
                gIter.remove()
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

            gameObjects.add(fruit)
        } else if (type == GameObjectType.TYPE_BOMB.value) {
            val bomb = GameObject(
                bitmap = bitmap,
                bitmapResId = bombBitmapResIds,
                position = PointF(startX, height.toFloat()),
                velocity = PointF(xVelocity, yVelocity),
                rotationSpeed = Random.nextFloat() * 8f - 4f,
                type = GameObjectType.TYPE_BOMB.value
            )

            gameObjects.add(bomb)

        }
    }

    private fun spawnFruit() {
        if (width < 300 || height < 300) return

        // random number of fruits
        val numFruits = Random.nextInt(1, 5)
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
        if (Random.nextFloat() > 0.8f) return // Hiếm hơn
        val startX = Random.nextInt(100, maxOf(101, width - 200)).toFloat()
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
        gameObjects.add(freeze)
    }

    private fun spawnExplode() {
        if (Random.nextFloat() > 0.8f) return // Hiếm hơn
        val startX = Random.nextInt(100, maxOf(101, width - 200)).toFloat()
        val screenMid = width / 2f
        val xVelocity = when {
            startX < screenMid - 100 -> Random.nextFloat() * 5f + 1f
            startX > screenMid + 100 -> -Random.nextFloat() * 5f - 1f
            else -> Random.nextFloat() * 2f - 1f
        }

        val minHeight = height / 2f + explodeBitmap.height / 2f
        val maxHeight = height.toFloat() - explodeBitmap.height / 2f
        val targetHeight = Random.nextFloat() * (maxHeight - minHeight) + minHeight
        val yVelocity = -sqrt(2 * gravity * targetHeight)

        val explode = GameObject(
            bitmap = explodeBitmap,
            bitmapResId = explodeBitmapResId,
            position = PointF(startX, height.toFloat()),
            velocity = PointF(xVelocity, yVelocity),
            rotationSpeed = Random.nextFloat() * 8f - 4f,
            type = GameObjectType.TYPE_EXPLODE.value
        )
        gameObjects.add(explode)
    }

    private fun spawnGuard() {
        if (Random.nextFloat() > 0.9f) return // Hiếm hơn
        val startX = Random.nextInt(100, maxOf(101, width - 200)).toFloat()
        val screenMid = width / 2f
        val xVelocity = when {
            startX < screenMid - 100 -> Random.nextFloat() * 5f + 1f
            startX > screenMid + 100 -> -Random.nextFloat() * 5f - 1f
            else -> Random.nextFloat() * 2f - 1f
        }

        val minHeight = height / 2f + explodeBitmap.height / 2f
        val maxHeight = height.toFloat() - explodeBitmap.height / 2f
        val targetHeight = Random.nextFloat() * (maxHeight - minHeight) + minHeight
        val yVelocity = -sqrt(2 * gravity * targetHeight)

        val guard = GameObject(
            bitmap = guardBitmap,
            bitmapResId = guardBitmapResId,
            position = PointF(startX, height.toFloat()),
            velocity = PointF(xVelocity, yVelocity),
            rotationSpeed = Random.nextFloat() * 8f - 4f,
            type = GameObjectType.TYPE_GUARD.value
        )
        gameObjects.add(guard)
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

        if (!GS.isUseCamera) {
            canvas.drawBitmap(backgroundBitmap, null, Rect(0, 0, width, height), null)
        }

        gameObjects.forEach { it.draw(canvas) }

        splashMarks.forEach { it.draw(canvas, paint) }
        slicedPieces.forEach { it.draw(canvas) }
        if (!playerState.isFreeze) {
            particles.forEach { it.draw(canvas, paint) }
        }
        scorePops.forEach { it.draw(canvas, scorePopTextPaint) }

        if (playerState.isFreeze) {
            canvas.drawBitmap(freezeOverlayBitmap, null, Rect(0, 0, width, height), null)
        }


        if (GS.isUseHandTracker) {
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexFillPaint)
            canvas.drawCircle(leftHandX, leftHandY, 40f, leftIndexStrokePaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexFillPaint)
            canvas.drawCircle(rightHandX, rightHandY, 40f, rightIndexStrokePaint)
        }

        drawScore(canvas)
        drawLives(canvas)
        drawGuard(canvas)

        if (playerState.isGameOver) {
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
            val ratio = gameOverBitmap.width / gameOverBitmap.height
            val w = width * 4 / 6
            val h =  w / ratio
            val gameOverRect = Rect(
                width / 2 - w / 2,
                height / 2 - h / 2,
                width / 2 + w / 2,
                height / 2 + h / 2
            )
            canvas.drawBitmap(gameOverBitmap, null, gameOverRect, null)
            canvas.restore()

            canvas.drawText("Score: ${playerState.score}", width / 2f, height / 2f + gameOverBitmap.height, scorePaint)

            invalidate() // Để giữ cho animation chạy
        }
    }

    private fun drawLives(canvas: Canvas) {
        for (i in 0 until playerState.lives) {
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

    private fun drawGuard(canvas: Canvas) {
        if (playerState.isGuard) {
            val left = width - 330
            val top = 140
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(guardBitmap, guardSize, guardSize, false),
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
        gameObjects.forEach { gameObject ->
            if (!gameObject.isSliced) {
                val centerX = gameObject.position.x + gameObject.bitmap.width / 2f
                val centerY = gameObject.position.y + gameObject.bitmap.height / 2f

                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                val hitRadius = gameObject.bitmap.width * 0.6f

                if (distance < hitRadius) {
                    gameObject.isSliced = true
                    when (gameObject.type) {
                        GameObjectType.TYPE_BOMB.value -> {
                            SoundManager.playSliceBomb()
                            if (playerState.isGuard) {
                                playerState.isGuard = false
                            } else {
                                val updateLives = playerState.lives - 1
                                playerState.lives = updateLives
                                if (updateLives <= 0) {
                                    triggerGameOver()
                                } else {
                                    gameObject.isSliced = true
                                }
                            }
                        }
                        else -> {
                            SoundManager.playSlice()
                            val resName = resources.getResourceEntryName(gameObject.bitmapResId)
                            val leftResId =
                                resources.getIdentifier("${resName}_l", "drawable", context.packageName)
                            val rightResId =
                                resources.getIdentifier("${resName}_r", "drawable", context.packageName)

                            val leftBitmap = BitmapFactory.decodeResource(resources, leftResId)
                            val rightBitmap = BitmapFactory.decodeResource(resources, rightResId)

                            // tạo 2 mảnh rơi lệch
                            val leftPiece = SlicedPiece(
                                bitmap = leftBitmap,
                                position = PointF(gameObject.position.x, gameObject.position.y),
                                velocity = PointF(gameObject.velocity.x - 3f, gameObject.velocity.y * 1.2f),
                                rotationSpeed = -5f
                            )

                            val rightPiece = SlicedPiece(
                                bitmap = rightBitmap,
                                position = PointF(gameObject.position.x + 20f, gameObject.position.y),
                                velocity = PointF(gameObject.velocity.x + 3f, gameObject.velocity.y * 1.15f),
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
                        }
                    }

                    when (gameObject.type) {
                        GameObjectType.TYPE_FRUIT.value -> {
                            val currentScore = playerState.score + 1
                            playerState.score = currentScore
                            scorePops.add(
                                ScorePop(
                                    position = PointF(centerX + 30f, centerY), // gần điểm chém
                                    text = "+1"
                                )
                            )

                            if (currentScore > GS.bestScore && !GS.isVersusMode) {
                                GS.bestScore = currentScore
                            }

                            val fruitColor = fruitColorMap[gameObject.bitmapResId] ?: Color.WHITE
                            emitParticles(centerX, centerY, fruitColor)
                            addSplashMark(centerX, centerY, gameObject.bitmapResId)
                        }
                        GameObjectType.TYPE_FREEZE.value -> {
                            playerState.isFreeze = true
                            playerState.freezeStartTime = System.currentTimeMillis()

                            emitParticles(centerX, centerY, Color.CYAN)
                        }
                        GameObjectType.TYPE_EXPLODE.value -> {
                            emitParticles(centerX, centerY, Color.YELLOW)
                            addSplashMark(centerX, centerY, explodeBitmapResId)

                            explodeAll()
                        }
                        GameObjectType.TYPE_GUARD.value -> {
                            playerState.isGuard = true
                        }
                    }
                }
            }
        }
    }

    private fun explodeAll() {
        gameObjects.forEach { gameObject ->
            if (!gameObject.isSliced) {
                gameObject.isSliced = true
                val centerX = gameObject.position.x + gameObject.bitmap.width / 2f
                val centerY = gameObject.position.y + gameObject.bitmap.height / 2f
                when (gameObject.type) {
                    GameObjectType.TYPE_BOMB.value -> {
                        SoundManager.playSliceBomb()
                    }
                    else -> {
                        SoundManager.playSlice()
                        val resName = resources.getResourceEntryName(gameObject.bitmapResId)
                        val leftResId =
                            resources.getIdentifier("${resName}_l", "drawable", context.packageName)
                        val rightResId =
                            resources.getIdentifier("${resName}_r", "drawable", context.packageName)

                        val leftBitmap = BitmapFactory.decodeResource(resources, leftResId)
                        val rightBitmap = BitmapFactory.decodeResource(resources, rightResId)

                        // tạo 2 mảnh rơi lệch
                        val leftPiece = SlicedPiece(
                            bitmap = leftBitmap,
                            position = PointF(gameObject.position.x, gameObject.position.y),
                            velocity = PointF(gameObject.velocity.x - 3f, gameObject.velocity.y * 1.2f),
                            rotationSpeed = -5f
                        )

                        val rightPiece = SlicedPiece(
                            bitmap = rightBitmap,
                            position = PointF(gameObject.position.x + 20f, gameObject.position.y),
                            velocity = PointF(gameObject.velocity.x + 3f, gameObject.velocity.y * 1.15f),
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
                    }
                }

                when (gameObject.type) {
                    GameObjectType.TYPE_FRUIT.value -> {
                        val currentScore = playerState.score + 1
                        playerState.score = currentScore
                        scorePops.add(
                            ScorePop(
                                position = PointF(centerX + 30f, centerY), // gần điểm chém
                                text = "+1"
                            )
                        )

                        if (currentScore > GS.bestScore && !GS.isVersusMode) {
                            GS.bestScore = currentScore
                        }

                        val fruitColor = fruitColorMap[gameObject.bitmapResId] ?: Color.WHITE
                        emitParticles(centerX, centerY, fruitColor)
                        addSplashMark(centerX, centerY, gameObject.bitmapResId)
                    }
                    GameObjectType.TYPE_FREEZE.value -> {
                        playerState.isFreeze = true
                        playerState.freezeStartTime = System.currentTimeMillis()

                        emitParticles(centerX, centerY, Color.CYAN)
                    }
                    GameObjectType.TYPE_EXPLODE.value -> {
                        emitParticles(centerX, centerY, Color.YELLOW)
                        addSplashMark(centerX, centerY, explodeBitmapResId)

                        explodeAll()
                    }
                    GameObjectType.TYPE_GUARD.value -> {
                        playerState.isGuard = true
                    }
                }
            }
        }
    }

    private fun triggerGameOver() {
        playerState.isGameOver = true
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
        canvas.drawText(playerState.score.toString(), scoreX, scoreY, scoreStrokePaint)
        canvas.drawText(playerState.score.toString(), scoreX, scoreY, scoreTextPaint)

        // Vị trí Best Score
        if (!GS.isVersusMode) {
            val bestY = scoreY + 70f
            canvas.drawText("Best: ${GS.bestScore}", iconX, bestY, bestStrokePaint)
            canvas.drawText("Best: ${GS.bestScore}", iconX, bestY, bestTextPaint)
        }
    }

    fun resetGame() {
        // Reset tất cả danh sách
        gameObjects.clear()
        slicedPieces.clear()
        particles.clear()
        splashMarks.clear()
        scorePops.clear()

        spawnCounter = 0

        playerState.reset()
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

    private var leftHandX = -200f
    private var leftHandY = -200f
    private var rightHandX = -200f
    private var rightHandY = -200f

    override fun updateLeftHandPosition(leftX: Float, leftY: Float) {
        leftHandX = leftX
        leftHandY = leftY
    }

    override fun updateRightHandPosition(rightX: Float, rightY: Float) {
        rightHandX = rightX
        rightHandY = rightY
    }
}