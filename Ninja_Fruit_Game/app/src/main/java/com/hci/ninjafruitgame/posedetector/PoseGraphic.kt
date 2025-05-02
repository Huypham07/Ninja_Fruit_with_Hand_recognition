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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.hci.ninjafruitgame.view.vision.GraphicOverlay
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.Locale

/**
 * Draw the detected pose in preview.
 */
class PoseGraphic(
  overlay: GraphicOverlay,
  private val pose: Pose,
  private val showInFrameLikelihood: Boolean,
  private val visualizeZ: Boolean,
  private val rescaleZForVisualization: Boolean,
  private val xOffset: Float = 0f // Thêm offset để hỗ trợ người chơi thứ hai
) : GraphicOverlay.Graphic(overlay) {

  private val leftPaint = Paint()
  private val rightPaint = Paint()
  private val whitePaint = Paint()

  init {
    whitePaint.strokeWidth = STROKE_WIDTH
    whitePaint.color = Color.WHITE
    whitePaint.textSize = IN_FRAME_LIKELIHOOD_TEXT_SIZE

    leftPaint.strokeWidth = STROKE_WIDTH
    leftPaint.color = Color.GREEN
    rightPaint.strokeWidth = STROKE_WIDTH
    rightPaint.color = Color.YELLOW
  }

  override fun draw(canvas: Canvas) {
    val landmarks = pose.allPoseLandmarks
    if (landmarks.isEmpty()) {
      return
    }

    // Vẽ các điểm mốc
    for (landmark in landmarks) {
      drawPoint(canvas, landmark, whitePaint)
      if (showInFrameLikelihood) {
        canvas.drawText(
          String.format(Locale.US, "%.2f", landmark.inFrameLikelihood),
          translateX(landmark.position.x + xOffset),
          translateY(landmark.position.y),
          whitePaint
        )
      }
    }

    // Vẽ các đường kết nối giữa các điểm mốc
    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
    val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
    val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
    val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
    val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
    val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
    val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
    val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
    val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
    val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
    val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

    val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
    val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
    val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
    val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
    val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
    val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
    val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
    val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
    val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
    val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

    // Face
    // drawLine(canvas, nose, lefyEyeInner, whitePaint)
    // ... bạn có thể thêm các đường kết nối khuôn mặt ở đây

    // Torso
    drawLine(canvas, leftShoulder, rightShoulder, whitePaint)
    drawLine(canvas, leftHip, rightHip, whitePaint)
    drawLine(canvas, leftShoulder, leftHip, leftPaint)
    drawLine(canvas, rightShoulder, rightHip, rightPaint)

    // Left arm
    drawLine(canvas, leftShoulder, leftElbow, leftPaint)
    drawLine(canvas, leftElbow, leftWrist, leftPaint)
    drawLine(canvas, leftWrist, leftThumb, leftPaint)
    drawLine(canvas, leftWrist, leftPinky, leftPaint)
    drawLine(canvas, leftWrist, leftIndex, leftPaint)
    drawLine(canvas, leftIndex, leftPinky, leftPaint)

    // Right arm
    drawLine(canvas, rightShoulder, rightElbow, rightPaint)
    drawLine(canvas, rightElbow, rightWrist, rightPaint)
    drawLine(canvas, rightWrist, rightThumb, rightPaint)
    drawLine(canvas, rightWrist, rightPinky, rightPaint)
    drawLine(canvas, rightWrist, rightIndex, rightPaint)
    drawLine(canvas, rightIndex, rightPinky, rightPaint)

    // Legs
    drawLine(canvas, leftHip, leftKnee, leftPaint)
    drawLine(canvas, leftKnee, leftAnkle, leftPaint)
    drawLine(canvas, leftAnkle, leftHeel, leftPaint)
    drawLine(canvas, leftHeel, leftFootIndex, leftPaint)

    drawLine(canvas, rightHip, rightKnee, rightPaint)
    drawLine(canvas, rightKnee, rightAnkle, rightPaint)
    drawLine(canvas, rightAnkle, rightHeel, rightPaint)
    drawLine(canvas, rightHeel, rightFootIndex, rightPaint)
  }

  private fun drawPoint(canvas: Canvas, landmark: PoseLandmark, paint: Paint) {
    val point = landmark.position3D
    maybeUpdatePaintColor(paint, canvas, point.z)
    canvas.drawCircle(
      translateX(point.x + xOffset),
      translateY(point.y),
      DOT_RADIUS,
      paint
    )
  }

  private fun drawLine(
    canvas: Canvas,
    startLandmark: PoseLandmark?,
    endLandmark: PoseLandmark?,
    paint: Paint
  ) {
    if (startLandmark == null || endLandmark == null) {
      return
    }

    val start = startLandmark.position3D
    val end = endLandmark.position3D

    // Gets average z for the current body line
    val avgZInImagePixel = (start.z + end.z) / 2
    maybeUpdatePaintColor(paint, canvas, avgZInImagePixel)

    canvas.drawLine(
      translateX(start.x + xOffset),
      translateY(start.y),
      translateX(end.x + xOffset),
      translateY(end.y),
      paint
    )
  }

  private fun maybeUpdatePaintColor(paint: Paint, canvas: Canvas, zInImagePixel: Float) {
    if (!visualizeZ) {
      return
    }

    // Áp dụng màu dựa trên giá trị z
    val zLowerBoundInScreenPixel: Float
    val zUpperBoundInScreenPixel: Float

    if (rescaleZForVisualization) {
      val zMin = PROJECTION_Z_MIN
      val zMax = PROJECTION_Z_MAX

      zLowerBoundInScreenPixel = canvas.width / 2 * (1f - zMin)
      zUpperBoundInScreenPixel = canvas.width / 2 * (1f - zMax)
    } else {
      zLowerBoundInScreenPixel = 0f
      zUpperBoundInScreenPixel = (canvas.width * CAMERA_Z_DIFFERENCE_THRESHOLD).toFloat()
    }

    var v = zInImagePixel
    if (rescaleZForVisualization) {
      val zMinInImagePixel = MIN_Z
      val zMaxInImagePixel = MAX_Z
      v = v / (zMaxInImagePixel - zMinInImagePixel) * (zMaxInImagePixel - zMinInImagePixel) + zMinInImagePixel
    }

    val zInScreenPixel = canvas.width / 2 * (1f - v)
    if (zInScreenPixel < zLowerBoundInScreenPixel) {
      paint.setARGB(255, 255, 0, 0)
    } else if (zInScreenPixel > zUpperBoundInScreenPixel) {
      paint.setARGB(255, 0, 0, 255)
    } else {
      val ratio = (zInScreenPixel - zLowerBoundInScreenPixel) / (zUpperBoundInScreenPixel - zLowerBoundInScreenPixel)
      paint.setARGB(
        255,
        (255 * (1 - ratio)).toInt(),
        0,
        (255 * ratio).toInt()
      )
    }
  }

  companion object {
    private const val DOT_RADIUS = 8.0f
    private const val IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f
    private const val STROKE_WIDTH = 10.0f
    private const val CAMERA_Z_DIFFERENCE_THRESHOLD = 0.01f
    private const val PROJECTION_Z_MIN = -0.5f
    private const val PROJECTION_Z_MAX = 0.5f
    private const val MIN_Z = 0f
    private const val MAX_Z = 100f
  }
}