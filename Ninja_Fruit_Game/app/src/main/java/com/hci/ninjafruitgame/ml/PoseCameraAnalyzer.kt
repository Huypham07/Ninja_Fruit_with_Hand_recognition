package com.hci.ninjafruitgame.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
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
    private var lastAnalyzedTime = 0L
    private val resizedWidth = 256
    private val resizedHeight = 256
    private var reusableBitmap: Bitmap? = null

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalyzedTime < 33) {
            image.close()
            return
        }
        lastAnalyzedTime = currentTime

        try {
            // Chuyển image sang bitmap
            val bitmap = image.toBitmap() ?: run {
                image.close()
                return
            }

            // Tạo bitmap dùng lại để resize nếu chưa có
            if (reusableBitmap == null) {
                reusableBitmap = Bitmap.createBitmap(resizedWidth, resizedHeight, Bitmap.Config.ARGB_8888)
            }

            // Vẽ bitmap gốc lên bitmap resized bằng Canvas (tối ưu hơn createScaledBitmap)
            val canvas = Canvas(reusableBitmap!!)
            val dstRect = Rect(0, 0, resizedWidth, resizedHeight)
            canvas.drawBitmap(bitmap, null, dstRect, null)

            // Dự đoán pose
            val result = moveNet.estimatePoses(reusableBitmap!!)

            if (result.isNotEmpty()) {
                val person = result[0]

                val scaleX = previewView.width / resizedWidth.toFloat()
                val scaleY = previewView.height / resizedHeight.toFloat()

                // Mirror toàn bộ keypoint cho camera trước
                person.keyPoints.forEach { keyPoint ->
                    keyPoint.coordinate.x *= scaleX
                    keyPoint.coordinate.y *= scaleY
                    keyPoint.coordinate.x = previewView.width - keyPoint.coordinate.x
                }

                // Gửi vị trí cổ tay (nếu đủ độ tin cậy)
                val wrist = person.keyPoints.find { it.bodyPart == BodyPart.RIGHT_WRIST }
                if (wrist != null && wrist.score > 0.5f) {
                    onWristDetected(wrist.coordinate)
                }

                // Gửi toàn bộ keypoints nếu có
                onKeypointsDetected?.invoke(person.keyPoints)
            }
        } catch (e: Exception) {
            Log.e("PoseAnalyzer", "Pose estimation failed: ${e.message}")
        } finally {
            image.close()
        }
    }



}

