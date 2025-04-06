package com.hci.ninjafruitgame.posedetector

class HandTracker(
    private val movementThreshold: Float = 3f,
    private val listener: HandLandmarkListener
) {

    private val kalmanLeft = KalmanFilter2D()
    private val kalmanRight = KalmanFilter2D()

    private var lastLeftX: Float? = null
    private var lastLeftY: Float? = null
    private var lastRightX: Float? = null
    private var lastRightY: Float? = null

    fun update(leftX: Float?, leftY: Float?, rightX: Float?, rightY: Float?) {
        var sendUpdate = false

        // LEFT
        if (leftX != null && leftY != null) {
            val (filteredLeftX, filteredLeftY) = kalmanLeft.update(leftX, leftY)

            val dx = if (lastLeftX != null) kotlin.math.abs(filteredLeftX - lastLeftX!!) else Float.MAX_VALUE
            val dy = if (lastLeftY != null) kotlin.math.abs(filteredLeftY - lastLeftY!!) else Float.MAX_VALUE

            if (dx > movementThreshold || dy > movementThreshold) {
                lastLeftX = filteredLeftX
                lastLeftY = filteredLeftY
                sendUpdate = true
            }
        }

        // RIGHT
        if (rightX != null && rightY != null) {
            val (filteredRightX, filteredRightY) = kalmanRight.update(rightX, rightY)

            val dx = if (lastRightX != null) kotlin.math.abs(filteredRightX - lastRightX!!) else Float.MAX_VALUE
            val dy = if (lastRightY != null) kotlin.math.abs(filteredRightY - lastRightY!!) else Float.MAX_VALUE

            if (dx > movementThreshold || dy > movementThreshold) {
                lastRightX = filteredRightX
                lastRightY = filteredRightY
                sendUpdate = true
            }
        }

        if (sendUpdate) {
            listener.onHandLandmarksReceived(
                lastLeftX, lastLeftY,
                lastRightX, lastRightY
            )
        }
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

//package com.hci.ninjafruitgame.posedetector
//
//class HandTracker(
//    private val minCutoff: Float = 1.0f,
//    private val beta: Float = 0.0f,
//    private val movementThreshold: Float = 3f,
//    private val listener: HandLandmarkListener
//) {
//
//    private val leftFilterX = OneEuroFilter()
//    private val leftFilterY = OneEuroFilter()
//    private val rightFilterX = OneEuroFilter()
//    private val rightFilterY = OneEuroFilter()
//
//    private var lastLeftX: Float? = null
//    private var lastLeftY: Float? = null
//    private var lastRightX: Float? = null
//    private var lastRightY: Float? = null
//
//    fun update(leftX: Float?, leftY: Float?, rightX: Float?, rightY: Float?) {
//        var sendUpdate = false
//
//        val timestamp = System.nanoTime() / 1_000_000_000.0 // Convert to seconds
//
//        // LEFT
//        if (leftX != null && leftY != null) {
//            val filteredLeftX = leftFilterX.filter(leftX, timestamp, minCutoff, beta)
//            val filteredLeftY = leftFilterY.filter(leftY, timestamp, minCutoff, beta)
//
//            val dx = if (lastLeftX != null) kotlin.math.abs(filteredLeftX - lastLeftX!!) else Float.MAX_VALUE
//            val dy = if (lastLeftY != null) kotlin.math.abs(filteredLeftY - lastLeftY!!) else Float.MAX_VALUE
//
//            if (dx > movementThreshold || dy > movementThreshold) {
//                lastLeftX = filteredLeftX
//                lastLeftY = filteredLeftY
//                sendUpdate = true
//            }
//        }
//
//        // RIGHT
//        if (rightX != null && rightY != null) {
//            val filteredRightX = rightFilterX.filter(rightX, timestamp, minCutoff, beta)
//            val filteredRightY = rightFilterY.filter(rightY, timestamp, minCutoff, beta)
//
//            val dx = if (lastRightX != null) kotlin.math.abs(filteredRightX - lastRightX!!) else Float.MAX_VALUE
//            val dy = if (lastRightY != null) kotlin.math.abs(filteredRightY - lastRightY!!) else Float.MAX_VALUE
//
//            if (dx > movementThreshold || dy > movementThreshold) {
//                lastRightX = filteredRightX
//                lastRightY = filteredRightY
//                sendUpdate = true
//            }
//        }
//
//        if (sendUpdate) {
//            listener.onHandLandmarksReceived(
//                lastLeftX, lastLeftY,
//                lastRightX, lastRightY
//            )
//        }
//    }
//}
//
//class OneEuroFilter {
//    private var lastValue: Float? = null
//    private var lastDerivate: Float = 0f
//    private var lastTime: Double? = null
//
//    fun filter(x: Float, t: Double, minCutoff: Float, beta: Float, dCutoff: Float = 1.0f): Float {
//        if (lastValue == null) {
//            lastValue = x
//            lastTime = t
//            return x
//        }
//
//        val dt = (t - lastTime!!).coerceAtLeast(1e-6)
//        lastTime = t
//
//        // Derivative
//        val dx = (x - lastValue!!) / dt
//        val alphaD = smoothingFactor(dt, dCutoff)
//        lastDerivate += alphaD * (dx.toFloat() - lastDerivate)
//
//        val cutoff = minCutoff + beta * kotlin.math.abs(lastDerivate)
//        val alpha = smoothingFactor(dt, cutoff)
//        lastValue = lastValue!! + alpha * (x - lastValue!!)
//
//        return lastValue!!
//    }
//
//    private fun smoothingFactor(dt: Double, cutoff: Float): Float {
//        val r = 2 * Math.PI * cutoff * dt
//        return (r / (r + 1)).toFloat()
//    }
//}
//
