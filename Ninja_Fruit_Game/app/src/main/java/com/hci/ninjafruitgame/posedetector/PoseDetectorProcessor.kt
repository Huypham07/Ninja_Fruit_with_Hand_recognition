/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hci.ninjafruitgame.posedetector

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.odml.image.MlImage
import com.google.mlkit.vision.common.InputImage
import com.hci.ninjafruitgame.view.vision.GraphicOverlay
import com.hci.ninjafruitgame.view.vision.VisionProcessorBase
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import com.google.mlkit.vision.pose.PoseLandmark

/** A processor to run pose detector. */
class PoseDetectorProcessor(
  private val context: Context,
  options: PoseDetectorOptionsBase,
  private val showInFrameLikelihood: Boolean,
  private val visualizeZ: Boolean,
  private val rescaleZForVisualization: Boolean,
  var multiPlayerMode: Boolean = false
) : VisionProcessorBase<PoseDetectorResult>(context) {

  private val detector: PoseDetector

  private var handLandmarkListener: HandLandmarkListener? = null
  fun setHandLandmarkListener(listener: HandLandmarkListener) {
    handLandmarkListener = listener
  }

  init {
    detector = PoseDetection.getClient(options)
  }

  override fun stop() {
    super.stop()
    detector.close()
  }

  override fun detectInImage(image: InputImage): Task<PoseDetectorResult> {
    if (!multiPlayerMode) {
      return detector.process(image).continueWith { task ->
        PoseDetectorResult(player1Pose = task.result, player2Pose = null)
      }
    } else {
      // Trong chế độ đa người chơi, chúng ta cần chia hình ảnh
      val bitmap = getBitmapFromInputImage(image)
      if (bitmap != null) {
        return splitAndProcessImage(bitmap)
      } else {
        Log.e(TAG, "Cannot get bitmap from InputImage for multiplayer mode")
        // Nếu không thể chia hình ảnh, xử lý toàn bộ hình ảnh như trong trường hợp MlImage
        return detector.process(image).continueWith { task ->
          val fullPose = task.result
          val estimatedWidth = image.width
          val halfWidth = estimatedWidth / 2

          PoseDetectorResult(player1Pose = fullPose, player2Pose = fullPose, halfWidth = halfWidth, isImageSplit = false)
        }
      }
    }
  }

  private fun getBitmapFromInputImage(image: InputImage): Bitmap? {
    // InputImage có thể trực tiếp lấy bitmap trong một số trường hợp
    try {
      // Thử lấy bitmap nếu InputImage được tạo từ bitmap
      return image.bitmapInternal
    } catch (e: Exception) {
      // Nếu không thể truy cập được bitmap, ghi log và trả về null
      Log.e(TAG, "Cannot access bitmap from InputImage", e)
      return null
    }
  }

    override fun detectInImage(
    image: MlImage,
    originalCameraImage: Bitmap?
  ): Task<PoseDetectorResult> {
    if (!multiPlayerMode) {
      return detector.process(image).continueWith { task ->
        PoseDetectorResult(player1Pose = task.result, player2Pose = null)
      }
    } else {
      if (originalCameraImage != null) {
        return splitAndProcessImage(originalCameraImage)
      } else {
        Log.e(TAG, "Cannot get bitmap from InputImage for multiplayer mode")
        // Nếu không thể chia hình ảnh, xử lý toàn bộ hình ảnh như trong trường hợp MlImage
        return detector.process(image).continueWith { task ->
          val fullPose = task.result
          val estimatedWidth = image.width
          val halfWidth = estimatedWidth / 2

          PoseDetectorResult(player1Pose = fullPose, player2Pose = fullPose, halfWidth = halfWidth, isImageSplit = false)
        }
      }
    }

  }

  private fun splitAndProcessImage(bitmap: Bitmap): Task<PoseDetectorResult> {
    val width = bitmap.width
    val halfWidth = width / 2

    // Tạo bitmap cho nửa trái và nửa phải
    val leftBitmap = Bitmap.createBitmap(bitmap, 0, 0, halfWidth, bitmap.height)
    val rightBitmap = Bitmap.createBitmap(bitmap, halfWidth, 0, width - halfWidth, bitmap.height)

    // Tạo InputImage từ các bitmap
    val leftInputImage = InputImage.fromBitmap(leftBitmap, 0)
    val rightInputImage = InputImage.fromBitmap(rightBitmap, 0)

    // Detect pose trên cả hai nửa
    val player1Task = detector.process(leftInputImage)
    val player2Task = detector.process(rightInputImage)

    // Chờ cả hai task hoàn thành
    return Tasks.whenAllSuccess<Any>(player1Task, player2Task).continueWith { task ->
      val player1Pose = player1Task.result
      val player2Pose = player2Task.result

      // Trả về kết quả từ cả hai pose
      PoseDetectorResult(player1Pose = player1Pose, player2Pose = player2Pose, halfWidth = halfWidth)
    }
  }

  override fun onSuccess(
    result: PoseDetectorResult,
    graphicOverlay: GraphicOverlay
  ) {
    val MIN_CONFIDENCE = 0.7f // Ngưỡng độ tin cậy

    if (multiPlayerMode && result.player2Pose != null) {
      if (result.isImageSplit) {
        // Xử lý cho trường hợp hình ảnh được tách
        // Xử lý cho player 1
        processPlayerPose(result.player1Pose, 0, graphicOverlay, MIN_CONFIDENCE, isPlayer1 = true)

        // Xử lý cho player 2
        processPlayerPose(result.player2Pose, result.halfWidth ?: 0, graphicOverlay, MIN_CONFIDENCE, isPlayer1 = false)
      } else {
        // Xử lý cho trường hợp hình ảnh KHÔNG được tách (với MlImage)
        // Phân tích tư thế đầy đủ và phân chia landmarks dựa trên tọa độ X
        val halfWidth = result.halfWidth ?: (graphicOverlay.width / 2)

        // Phân tách điểm mốc cho player 1 và player 2 dựa trên vị trí X
        val allLandmarks = result.player1Pose.allPoseLandmarks

        var leftIndexP1: PoseLandmark? = null
        var rightIndexP1: PoseLandmark? = null
        var leftIndexP2: PoseLandmark? = null
        var rightIndexP2: PoseLandmark? = null

        // Tìm các điểm ngón trỏ
        for (landmark in allLandmarks) {
          if (landmark.position.x < halfWidth) {
            // Thuộc về player 1
            when (landmark.landmarkType) {
              PoseLandmark.LEFT_INDEX -> leftIndexP1 = landmark
              PoseLandmark.RIGHT_INDEX -> rightIndexP1 = landmark
            }
          } else {
            // Thuộc về player 2
            when (landmark.landmarkType) {
              PoseLandmark.LEFT_INDEX -> leftIndexP2 = landmark
              PoseLandmark.RIGHT_INDEX -> rightIndexP2 = landmark
            }
          }
        }

        // Xử lý điểm mốc cho player 1
        processHandLandmarks(leftIndexP1, rightIndexP1, 0, graphicOverlay, MIN_CONFIDENCE, isPlayer1 = true)

        // Xử lý điểm mốc cho player 2
        processHandLandmarks(leftIndexP2, rightIndexP2, 0, graphicOverlay, MIN_CONFIDENCE, isPlayer1 = false)

        // Vẽ đồ họa cho cả hai người chơi
//        graphicOverlay.add(
//          PoseGraphic(
//            graphicOverlay,
//            result.player1Pose,
//            showInFrameLikelihood,
//            visualizeZ,
//            rescaleZForVisualization,
//            xOffset = 0f
//          )
//        )
      }
    } else {
      // Chế độ một người chơi
      processPlayerPose(result.player1Pose, 0, graphicOverlay, MIN_CONFIDENCE, isPlayer1 = true)
    }
  }

  private fun processHandLandmarks(
    leftIndex: PoseLandmark?,
    rightIndex: PoseLandmark?,
    xOffset: Int,
    graphicOverlay: GraphicOverlay,
    minConfidence: Float,
    isPlayer1: Boolean
  ) {
    var leftX: Float? = null
    var leftY: Float? = null
    var rightX: Float? = null
    var rightY: Float? = null

    // Xử lý ngón trỏ trái
    if (leftIndex != null && leftIndex.inFrameLikelihood >= minConfidence) {
      leftX = graphicOverlay.translateXRaw(leftIndex.position.x + xOffset)
      leftY = graphicOverlay.translateYRaw(leftIndex.position.y)
    }

    // Xử lý ngón trỏ phải
    if (rightIndex != null && rightIndex.inFrameLikelihood >= minConfidence) {
      rightX = graphicOverlay.translateXRaw(rightIndex.position.x + xOffset)
      rightY = graphicOverlay.translateYRaw(rightIndex.position.y)
    }

    // Gửi dữ liệu đến listener
    if (multiPlayerMode) {
      if (isPlayer1) {
        handLandmarkListener?.onPlayer1HandLandmarksReceived(leftX, leftY, rightX, rightY)
      } else {
        handLandmarkListener?.onPlayer2HandLandmarksReceived(leftX, leftY, rightX, rightY)
      }
    } else {
      handLandmarkListener?.onPlayer1HandLandmarksReceived(leftX, leftY, rightX, rightY)
    }
  }

  private fun processPlayerPose(
    pose: Pose,
    xOffset: Int,
    graphicOverlay: GraphicOverlay,
    minConfidence: Float,
    isPlayer1: Boolean
  ) {
    val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
    val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)

    processHandLandmarks(leftIndex, rightIndex, xOffset, graphicOverlay, minConfidence, isPlayer1)

    // Thêm đồ họa cho người chơi
//    graphicOverlay.add(
//      PoseGraphic(
//        graphicOverlay,
//        pose,
//        showInFrameLikelihood,
//        visualizeZ,
//        rescaleZForVisualization,
//        xOffset = xOffset.toFloat()
//      )
//    )
  }

  override fun onFailure(e: Exception) {
    Log.e(TAG, "Pose detection failed!", e)
  }

  override fun isMlImageEnabled(context: Context?): Boolean {
    // Use MlImage in Pose Detection by default, change it to OFF to switch to InputImage.
    return true
  }

  companion object {
    private val TAG = "PoseDetectorProcessor"
  }
}

// Lớp để chứa kết quả pose detection cho cả hai người chơi
data class PoseDetectorResult(
  val player1Pose: Pose,
  val player2Pose: Pose?,
  val halfWidth: Int? = null,
  val isImageSplit: Boolean = true // Flag để biết image đã được tách hay chưa
)