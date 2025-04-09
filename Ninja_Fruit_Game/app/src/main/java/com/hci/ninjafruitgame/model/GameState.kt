package com.hci.ninjafruitgame.model

object  GameState {
    private var score = 0
    private val maxLives = 3
    private var lives = maxLives
    private var bestScore = 0
    private var isGameOver = false
    private var isPaused = false
    private var isGameStarted = false
    private var isUseHandTracker = false
    private var isUseCamera = false
    private var isMusicEnabled = true
    private var isGuard = false
    private var isFreeze = false
    private var freezeStartTime = 0L
    private val freezeDuration = 10000L
    private var isExplode = false

    fun getScore(): Int {
        return score
    }

    fun setScore(newScore: Int) {
        score = newScore
    }

    fun getBestScore(): Int {
        return bestScore
    }

    fun setBestScore(newBestScore: Int) {
        bestScore = newBestScore
    }

    fun getLives(): Int {
        return lives
    }

    fun setLives(newLives: Int) {
        lives = newLives
    }

    fun getMaxLives(): Int {
        return maxLives
    }

    fun isGameOver(): Boolean {
        return isGameOver
    }

    fun setGameOver(gameOver: Boolean) {
        isGameOver = gameOver
    }

    fun isPaused(): Boolean {
        return isPaused
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun isGameStarted(): Boolean {
        return isGameStarted
    }

    fun setGameStarted(gameStarted: Boolean) {
        isGameStarted = gameStarted
    }

    fun isUseHandTracker(): Boolean {
        return isUseHandTracker
    }

    fun setUseHandTracker(useHandTracker: Boolean) {
        isUseHandTracker = useHandTracker
    }

    fun isUseCamera(): Boolean {
        return isUseCamera
    }

    fun setUseCamera(useCamera: Boolean) {
        isUseCamera = useCamera
    }

    fun isMusicEnabled(): Boolean {
        return isMusicEnabled
    }

    fun setMusicEnabled(musicEnabled: Boolean) {
        isMusicEnabled = musicEnabled
    }


    fun isGuard(): Boolean {
        return isGuard
    }

    fun setGuard(guard: Boolean) {
        isGuard = guard
    }

    fun isFreeze(): Boolean {
        return isFreeze
    }

    fun setFreeze(freeze: Boolean) {
        isFreeze = freeze
    }

    fun getFreezeStartTime(): Long {
        return freezeStartTime
    }

    fun setFreezeStartTime(startTime: Long) {
        freezeStartTime = startTime
    }

    fun getFreezeDuration(): Long {
        return freezeDuration
    }

    fun isExplode(): Boolean {
        return isExplode
    }

    fun setExplode(explode: Boolean) {
        isExplode = explode
    }

    fun resetGame() {
        score = 0
        lives = maxLives
        isGameOver = false
        isPaused = false
        isGameStarted = false
        isGuard = false
        isFreeze = false
        isExplode = false
    }
}
