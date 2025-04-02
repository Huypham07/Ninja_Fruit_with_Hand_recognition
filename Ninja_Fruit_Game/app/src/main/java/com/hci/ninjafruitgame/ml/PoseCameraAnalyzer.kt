package com.hci.ninjafruitgame.ml

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.hci.ninjafruitgame.data.*

class PoseCameraAnalyzer(
    private val moveNet: MoveNet,
    private val previewView: PreviewView,
    private val onWristDetected: (PointF) -> Unit,
    private val onKeypointsDetected: ((List<KeyPoint>) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap() ?: return
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val result = moveNet.estimatePoses(resizedBitmap)

        if (result.isNotEmpty()) {
            val person = result[0]

            val scaleX = previewView.width / 256f
            val scaleY = previewView.height / 256f

            // Mirror toàn bộ keypoint vì dùng camera trước
            person.keyPoints.forEach { keyPoint ->
                keyPoint.coordinate.x *= scaleX
                keyPoint.coordinate.y *= scaleY

                // MIRROR X
                keyPoint.coordinate.x = previewView.width - keyPoint.coordinate.x
            }

            val wrist = person.keyPoints.find { it.bodyPart == BodyPart.RIGHT_WRIST }
            if (wrist != null && wrist.score > 0.5f) {
                onWristDetected(wrist.coordinate)
            }

            onKeypointsDetected?.invoke(person.keyPoints)
        }

        image.close()
    }


}

