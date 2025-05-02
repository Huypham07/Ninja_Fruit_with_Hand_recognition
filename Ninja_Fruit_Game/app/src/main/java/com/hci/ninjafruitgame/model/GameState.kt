package com.hci.ninjafruitgame.model

object  GameState {
    var bestScore = 0
    var isEndGame = false
    var isPaused = false
    var isGameStarted = false
    var isUseHandTracker = false
    var isUseCamera = false
    var isMusicEnabled = true
    var isVersusMode = false

    var p1GameOver = false
        set(value) {
            field = value
            if (isVersusMode) {
                isEndGame = value && p2GameOver
            } else {
                isEndGame = value
            }
        }
    var p2GameOver = false
        set(value) {
            field = value
            if (isVersusMode) {
                isEndGame = p1GameOver && value
            } else {
                isEndGame = value
            }
        }

}
