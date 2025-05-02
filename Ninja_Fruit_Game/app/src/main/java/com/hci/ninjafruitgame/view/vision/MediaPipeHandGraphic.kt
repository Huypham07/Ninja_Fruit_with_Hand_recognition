package com.hci.ninjafruitgame.view.vision

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.hci.ninjafruitgame.view.vision.GraphicOverlay
import com.hci.ninjafruitgame.view.vision.MediapipeHandProcessor.ResultBundle

class MediaPipeHandGraphic(
    val overlay: GraphicOverlay,
    private var results: HandLandmarkerResult,
) : GraphicOverlay.Graphic(overlay) {
    private var linePaint = Paint()
    private var pointPaint = Paint()

    init {
        initPaints()
    }

    fun clear() {
        linePaint.reset()
        pointPaint.reset()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = Color.GREEN
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val width = overlay.width
        val height = overlay.height
        results.let { handLandMarkerResult ->
            for (landmark in handLandMarkerResult.landmarks()!!) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * width,
                        normalizedLandmark.y() * height,
                        pointPaint
                    )
                }

                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        landmark.get(it!!.start())
                            .x() * width,
                        landmark.get(it.start())
                            .y() * height,
                        landmark.get(it.end())
                            .x() * width,
                        landmark.get(it.end())
                            .y() * height,
                        linePaint
                    )
                }
            }
        }
    }


    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}
