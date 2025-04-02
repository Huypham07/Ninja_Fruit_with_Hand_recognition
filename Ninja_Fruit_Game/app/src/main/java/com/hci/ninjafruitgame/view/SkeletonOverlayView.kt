package com.hci.ninjafruitgame.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.hci.ninjafruitgame.data.KeyPoint
import com.hci.ninjafruitgame.data.BodyPart

class SkeletonOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
        isDither = false  // Disable dithering for performance
    }

    private var keypoints: List<KeyPoint> = emptyList()
    private val drawPath = Path()  // Reuse path object

    // Cache visible points and connections to avoid recalculating
    private val visibleConnections = mutableListOf<Pair<PointF, PointF>>()

    fun setKeypoints(newPoints: List<KeyPoint>) {
        keypoints = newPoints
        updateVisibleConnections()  // Pre-calculate visible connections
        invalidate()
    }

    private fun updateVisibleConnections() {
        visibleConnections.clear()
        val visible = keypoints.filter { it.score > 0.3f }

        for ((p1, p2) in connections) {
            val kp1 = visible.find { it.bodyPart == p1 }
            val kp2 = visible.find { it.bodyPart == p2 }

            if (kp1 != null && kp2 != null) {
                visibleConnections.add(Pair(kp1.coordinate, kp2.coordinate))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the pre-calculated connections
        for ((p1, p2) in visibleConnections) {
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
        }
    }

    companion object {
        val connections = listOf(
            BodyPart.LEFT_SHOULDER to BodyPart.RIGHT_SHOULDER,
            BodyPart.LEFT_SHOULDER to BodyPart.LEFT_ELBOW,
            BodyPart.LEFT_ELBOW to BodyPart.LEFT_WRIST,
            BodyPart.RIGHT_SHOULDER to BodyPart.RIGHT_ELBOW,
            BodyPart.RIGHT_ELBOW to BodyPart.RIGHT_WRIST,
            BodyPart.LEFT_HIP to BodyPart.RIGHT_HIP,
            BodyPart.LEFT_SHOULDER to BodyPart.LEFT_HIP,
            BodyPart.RIGHT_SHOULDER to BodyPart.RIGHT_HIP
        )
    }
}