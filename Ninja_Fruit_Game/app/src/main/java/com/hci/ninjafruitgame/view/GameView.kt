package com.hci.ninjafruitgame.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View
import com.hci.ninjafruitgame.model.Fruit
import com.hci.ninjafruitgame.R
import com.hci.ninjafruitgame.model.Bomb
import com.hci.ninjafruitgame.model.GameObjectType
import com.hci.ninjafruitgame.model.SlicedPiece
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), SliceEffectReceiver {

    private val choreographer = Choreographer.getInstance()

    private var backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.bg1)

    private val fruits = mutableListOf<Fruit>()
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

    private val bombs = mutableListOf<Bomb>()
    private val bombBitmap = BitmapFactory.decodeResource(resources, R.drawable.bomb)

    private var isPaused = false
    fun getIsPaused(): Boolean {
        return isPaused
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
        if (isPaused) return
        // Spawn fruit mỗi 1.2s
        spawnCounter++
        if (spawnCounter >= 75) {
            if (spawnCounter % 75 == 0) {
                spawnFruit()
            }
            if (spawnCounter >= 300) {
                spawnBomb()
                spawnCounter = 0
            }
        }

        // Cập nhật quả
        val iterator = fruits.iterator()
        while (iterator.hasNext()) {
            val fruit = iterator.next()
            fruit.update(gravity)

            if (fruit.position.y > height + 200) {
                iterator.remove() // rơi xuống đáy
            }
        }

        val bIter = bombs.iterator()
        while (bIter.hasNext()) {
            val bomb = bIter.next()
            bomb.update(gravity)

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
            piece.update(gravity)
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

        val gravityAdjusted = gravity // dùng đúng gravity đang dùng ở game
        val targetHeight = Random.nextFloat() * (maxHeight - minHeight) + minHeight
        val yVelocity = -kotlin.math.sqrt(2 * gravityAdjusted * targetHeight) * 1.0f

        if (type == GameObjectType.TYPE_FRUIT.value) {
            val fruit = Fruit(
                bitmap = bitmap,
                bitmapResId = fruitResId,
                position = PointF(startX, height.toFloat()),
                velocity = PointF(xVelocity, yVelocity),
                rotationSpeed = Random.nextFloat() * 8f - 4f
            )

            fruits.add(fruit)
            Log.d("test", "spawn fruit")
        } else if (type == GameObjectType.TYPE_BOMB.value) {
            val bomb = Bomb(
                bitmap = bitmap, position = PointF(startX, height.toFloat()),
                velocity = PointF(xVelocity, yVelocity),
                rotationSpeed = Random.nextFloat() * 8f - 4f
            )

            bombs.add(bomb)
            Log.d("test", "spawn bomb")

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

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(backgroundBitmap, null, Rect(0, 0, width, height), null)

        splashMarks.forEach { it.draw(canvas, paint) }
        fruits.forEach { it.draw(canvas) }
        bombs.forEach { it.draw(canvas) }
        slicedPieces.forEach { it.draw(canvas) }
        particles.forEach { it.draw(canvas, paint) }
        scorePops.forEach { it.draw(canvas, scorePopTextPaint) }

        drawScore(canvas)
    }

    override fun onSliceAt(x: Float, y: Float) {
        sliceAt(x, y)
    }

    fun sliceAt(x: Float, y: Float) {
        fruits.forEach { fruit ->
            if (!fruit.isSliced) {
                val centerX = fruit.position.x + fruit.bitmap.width / 2f
                val centerY = fruit.position.y + fruit.bitmap.height / 2f
                val dx = x - centerX
                val dy = y - centerY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

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
                        prefs.edit().putInt("best_score", bestScore).apply()
                    }

                    var fruitColor = fruitColorMap[fruit.bitmapResId] ?: Color.WHITE
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
                }
            }
        }
    }

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
        // Reset tất cả danh sách
        fruits.clear()
        slicedPieces.clear()
        particles.clear()
        splashMarks.clear()
        scorePops.clear()

        // Reset điểm
        currentScore = 0

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
}