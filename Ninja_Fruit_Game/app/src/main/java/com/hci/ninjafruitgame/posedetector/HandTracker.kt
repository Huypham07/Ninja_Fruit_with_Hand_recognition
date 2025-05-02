package com.hci.ninjafruitgame.posedetector

class HandTracker(
    private val movementThreshold: Float = 3f,
    private val listener: HandLandmarkListener
) {

    private val kalmanLeft1 = KalmanFilter2D()
    private val kalmanRight1 = KalmanFilter2D()
    private val kalmanLeft2 = KalmanFilter2D()
    private val kalmanRight2 = KalmanFilter2D()

    private var lastLeftX1: Float? = null
    private var lastLeftY1: Float? = null
    private var lastRightX1: Float? = null
    private var lastRightY1: Float? = null

    private var lastLeftX2: Float? = null
    private var lastLeftY2: Float? = null
    private var lastRightX2: Float? = null
    private var lastRightY2: Float? = null

    // Hàm cập nhật cả 2 người cùng lúc
    fun update(
        leftX1: Float?, leftY1: Float?, rightX1: Float?, rightY1: Float?,
        leftX2: Float?, leftY2: Float?, rightX2: Float?, rightY2: Float?
    ) {
        val updated1 = updatePlayer1(leftX1, leftY1, rightX1, rightY1, false)
        val updated2 = updatePlayer2(leftX2, leftY2, rightX2, rightY2, false)

        if (updated1) listener.onPlayer1HandLandmarksReceived(lastLeftX1, lastLeftY1, lastRightX1, lastRightY1)
        if (updated2) listener.onPlayer2HandLandmarksReceived(lastLeftX2, lastLeftY2, lastRightX2, lastRightY2)
    }

    // Hàm cập nhật riêng cho người 1
    fun updatePlayer1(
        leftX: Float?, leftY: Float?, rightX: Float?, rightY: Float?,
        notify: Boolean = true
    ): Boolean {
        var sendUpdate = false

        // LEFT
        if (leftX != null && leftY != null) {
            val (filteredLeftX, filteredLeftY) = kalmanLeft1.update(leftX, leftY)
            val dx = if (lastLeftX1 != null) kotlin.math.abs(filteredLeftX - lastLeftX1!!) else Float.MAX_VALUE
            val dy = if (lastLeftY1 != null) kotlin.math.abs(filteredLeftY - lastLeftY1!!) else Float.MAX_VALUE

            if (dx > movementThreshold || dy > movementThreshold) {
                lastLeftX1 = filteredLeftX
                lastLeftY1 = filteredLeftY
                sendUpdate = true
            }
        }

        // RIGHT
        if (rightX != null && rightY != null) {
            val (filteredRightX, filteredRightY) = kalmanRight1.update(rightX, rightY)
            val dx = if (lastRightX1 != null) kotlin.math.abs(filteredRightX - lastRightX1!!) else Float.MAX_VALUE
            val dy = if (lastRightY1 != null) kotlin.math.abs(filteredRightY - lastRightY1!!) else Float.MAX_VALUE

            if (dx > movementThreshold || dy > movementThreshold) {
                lastRightX1 = filteredRightX
                lastRightY1 = filteredRightY
                sendUpdate = true
            }
        }

        if (sendUpdate && notify) {
            listener.onPlayer1HandLandmarksReceived(
                lastLeftX1, lastLeftY1,
                lastRightX1, lastRightY1
            )
        }

        return sendUpdate
    }

    // Hàm cập nhật riêng cho người 2
    fun updatePlayer2(
        leftX: Float?, leftY: Float?, rightX: Float?, rightY: Float?,
        notify: Boolean = true
    ): Boolean {
        var sendUpdate = false

        // LEFT
        if (leftX != null && leftY != null) {
            val (filteredLeftX, filteredLeftY) = kalmanLeft2.update(leftX, leftY)
            val dx = if (lastLeftX2 != null) kotlin.math.abs(filteredLeftX - lastLeftX2!!) else Float.MAX_VALUE
            val dy = if (lastLeftY2 != null) kotlin.math.abs(filteredLeftY - lastLeftY2!!) else Float.MAX_VALUE

            if (dx > movementThreshold || dy > movementThreshold) {
                lastLeftX2 = filteredLeftX
                lastLeftY2 = filteredLeftY
                sendUpdate = true
            }
        }

        // RIGHT
        if (rightX != null && rightY != null) {
            val (filteredRightX, filteredRightY) = kalmanRight2.update(rightX, rightY)
            val dx = if (lastRightX2 != null) kotlin.math.abs(filteredRightX - lastRightX2!!) else Float.MAX_VALUE
            val dy = if (lastRightY2 != null) kotlin.math.abs(filteredRightY - lastRightY2!!) else Float.MAX_VALUE

            if (dx > movementThreshold || dy > movementThreshold) {
                lastRightX2 = filteredRightX
                lastRightY2 = filteredRightY
                sendUpdate = true
            }
        }

        if (sendUpdate && notify) {
            listener.onPlayer2HandLandmarksReceived(
                lastLeftX2, lastLeftY2,
                lastRightX2, lastRightY2
            )
        }

        return sendUpdate
    }
}

class KalmanFilter2D {
    private var isInitialized = false

    private var x = 0f
    private var y = 0f
    private var vx = 0f
    private var vy = 0f

    private val processNoise = 1f  // Q: process noise
    private val measurementNoise = 10f  // R: measurement noise
    private val errorCovariance = floatArrayOf(100f, 100f) // Initial estimation uncertainty

    fun update(measuredX: Float, measuredY: Float): Pair<Float, Float> {
        if (!isInitialized) {
            x = measuredX
            y = measuredY
            isInitialized = true
            return Pair(x, y)
        }

        // Predict step
        x += vx
        y += vy
        errorCovariance[0] += processNoise
        errorCovariance[1] += processNoise

        // Update step (X)
        val kx = errorCovariance[0] / (errorCovariance[0] + measurementNoise)
        x += kx * (measuredX - x)
        errorCovariance[0] *= (1 - kx)

        // Update step (Y)
        val ky = errorCovariance[1] / (errorCovariance[1] + measurementNoise)
        y += ky * (measuredY - y)
        errorCovariance[1] *= (1 - ky)

        // Velocity estimation (optional for future prediction)
        vx = kx * (measuredX - x)
        vy = ky * (measuredY - y)

        return Pair(x, y)
    }
}