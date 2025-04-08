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
import android.util.Log
import com.google.android.gms.tasks.Task
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
  private val rescaleZForVisualization: Boolean
) : VisionProcessorBase<Pose>(context) {

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

  override fun detectInImage(image: InputImage): Task<Pose> {
    return detector
      .process(image)
  }

  override fun detectInImage(image: MlImage): Task<Pose> {
    return detector
      .process(image)
  }

  override fun onSuccess(
    pose: Pose,
    graphicOverlay: GraphicOverlay
  ) {
    val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
    val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)

    val MIN_CONFIDENCE = 0.7f // Ngưỡng độ tin cậy

    var leftX: Float? = null
    var leftY: Float? = null
    var rightX: Float? = null
    var rightY: Float? = null

    // Chỉ truyền left nếu độ tin cậy đủ cao
    if (leftIndex != null && leftIndex.inFrameLikelihood >= MIN_CONFIDENCE) {
      leftX = graphicOverlay.translateXRaw(leftIndex.position.x)
      leftY = graphicOverlay.translateYRaw(leftIndex.position.y)
    }

    // Chỉ truyền right nếu độ tin cậy đủ cao
    if (rightIndex != null && rightIndex.inFrameLikelihood >= MIN_CONFIDENCE) {
      rightX = graphicOverlay.translateXRaw(rightIndex.position.x)
      rightY = graphicOverlay.translateYRaw(rightIndex.position.y)
    }

    handLandmarkListener?.onHandLandmarksReceived(leftX, leftY, rightX, rightY)

    graphicOverlay.add(
      PoseGraphic(
        graphicOverlay,
        pose,
        showInFrameLikelihood,
        visualizeZ,
        rescaleZForVisualization
      )
    )
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
