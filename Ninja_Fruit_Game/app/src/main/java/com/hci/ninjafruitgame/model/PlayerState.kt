package com.hci.ninjafruitgame.model

import com.hci.ninjafruitgame.model.GameState.isGameStarted
import com.hci.ninjafruitgame.model.GameState.isPaused

data class PlayerState(
    var score: Int = 0,
    val maxLives: Int = 3,
    var lives: Int = maxLives,
    var bestScore: Int = 0,
    var isGameOver: Boolean = false,
    var isGuard: Boolean = false,
    var isFreeze: Boolean = false,
    var freezeStartTime: Long = 0L,
    val freezeDuration: Long = 10000L,
    var isExplode: Boolean = false
) {
    fun reset() {
        score = 0
        lives = 3
        isGameOver = false
        isGuard = false
        isFreeze = false
        isExplode = false
    }
}