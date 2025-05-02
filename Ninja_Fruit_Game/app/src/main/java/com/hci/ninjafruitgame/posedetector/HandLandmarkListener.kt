package com.hci.ninjafruitgame.posedetector

interface HandLandmarkListener {
    // Phương thức mới cho người chơi 1
    fun onPlayer1HandLandmarksReceived(leftIndexX: Float?, leftIndexY: Float?, rightIndexX: Float?, rightIndexY: Float?)

    // Phương thức mới cho người chơi 2
    fun onPlayer2HandLandmarksReceived(leftIndexX: Float?, leftIndexY: Float?, rightIndexX: Float?, rightIndexY: Float?)
}