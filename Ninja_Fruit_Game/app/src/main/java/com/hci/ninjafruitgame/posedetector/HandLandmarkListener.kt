package com.hci.ninjafruitgame.posedetector

interface HandLandmarkListener {
    fun onHandLandmarksReceived(leftIndexX: Float?, leftIndexY: Float?, rightIndexX: Float?, rightIndexY: Float?)
}
