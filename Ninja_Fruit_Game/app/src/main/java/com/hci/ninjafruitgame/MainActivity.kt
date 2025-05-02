package com.hci.ninjafruitgame

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.hci.ninjafruitgame.posedetector.HandLandmarkListener
import com.hci.ninjafruitgame.posedetector.HandTracker
import com.hci.ninjafruitgame.posedetector.PoseDetectorProcessor
import com.hci.ninjafruitgame.preference.PreferenceUtils
import com.hci.ninjafruitgame.utils.SoundManager
import com.hci.ninjafruitgame.view.game.CountdownOverlay
import com.hci.ninjafruitgame.view.game.FruitSliceView
import com.hci.ninjafruitgame.view.game.GameView
import com.hci.ninjafruitgame.view.game.PauseMenuView
import com.hci.ninjafruitgame.view.game.SliceEffectReceiver
import com.hci.ninjafruitgame.view.game.StartScreenView
import com.hci.ninjafruitgame.view.vision.CameraSource
import com.hci.ninjafruitgame.view.vision.CameraSourcePreview
import com.hci.ninjafruitgame.view.vision.GraphicOverlay
import java.io.IOException
import kotlin.system.exitProcess
import com.hci.ninjafruitgame.model.GameState as GS

class MainActivity : AppCompatActivity() {
    private lateinit var cameraSource: CameraSource
    private lateinit var poseViewFinder: CameraSourcePreview
    private lateinit var graphicOverlay: GraphicOverlay

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var fruitSliceView: FruitSliceView
    private lateinit var gameView: GameView
    private lateinit var gameView2: GameView
    private lateinit var startScreen: StartScreenView
    private lateinit var countdownOverlay: CountdownOverlay
    private lateinit var pauseMenu: PauseMenuView
    private lateinit var btnPause: ImageView


    @ExperimentalGetImage
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enableFullScreen()
        // Initialize our background executor

        // Wait for the views to be properly laid out
        fruitSliceView = findViewById(R.id.view)
        gameView = findViewById(R.id.gameView)
        gameView2 = findViewById(R.id.gameView2)
        startScreen = findViewById(R.id.startScreen)
        pauseMenu = findViewById(R.id.pauseMenuContent)
        countdownOverlay = findViewById(R.id.countdownOverlay)
        btnPause = findViewById(R.id.btnPause)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        poseViewFinder = findViewById(R.id.poseViewFinder)

        PreferenceUtils.hideDetectionInfo(this)

        updatePauseMenuBackground()

        SoundManager.init(applicationContext)
        startMusic(applicationContext)

        val bestScore =
            getSharedPreferences("game_prefs", MODE_PRIVATE).getInt("best_score", 0)
        GS.bestScore = bestScore

        startScreen.onStartGame = {
            startScreen.visibility = View.GONE
            gameView.visibility = View.VISIBLE
            gameView.resetGame()
            if (GS.isVersusMode) {
                gameView2.visibility = View.VISIBLE
                gameView2.resetGame()
                processor?.multiPlayerMode = true
            }
            pauseMenu.visibility = View.GONE
            GS.isGameStarted = true
            GS.isPaused = false
            updatePauseMenuBackground()
        }

        startScreen.onOpenSettings = {
            startScreen.playExitAnimation { pauseMenu.show() }
        }


        startScreen.onQuit = {
            AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to quit the game?")
                .setPositiveButton("Yes") { _, _ ->
                    finish()
                    exitProcess(0)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        pauseMenu.onResume = {
            btnPause.visibility = View.VISIBLE
            if (GS.isVersusMode) {
                btnPause.x = gameView.width - btnPause.width / 2f
            } else {
                btnPause.x = gameView.width - btnPause.width - 80f
            }
            if (GS.isGameStarted) {
                countdownOverlay.startCountdown {
                    GS.isPaused = false
                }
            } else {
                GS.isPaused = false
            }
        }

        btnPause.setOnClickListener {
            if (countdownOverlay.isVisible) {
                countdownOverlay.cancelCountdown()
            }

            GS.isPaused = true
            btnPause.visibility = View.GONE
            pauseMenu.show()
        }

        pauseMenu.onBackToStart = {
            gameView.resetGame()
            gameView2.resetGame()
            GS.isPaused = true
            GS.isVersusMode = false
            gameView.visibility = View.GONE
            gameView2.visibility = View.GONE
            startScreen.show()
            GS.isGameStarted = false
            updatePauseMenuBackground()
        }

        pauseMenu.onRestart = {
            gameView.resetGame()
            gameView2.resetGame()
            GS.isPaused = true
            countdownOverlay.startCountdown {
                GS.isPaused = false
                GS.isGameStarted = true
            }
            btnPause.visibility = View.VISIBLE
            if (GS.isVersusMode) {
                btnPause.x = gameView.width - btnPause.width / 2f
            } else {
                btnPause.x = gameView.width - btnPause.width - 80f
            }
        }

        pauseMenu.onBackgroundChange = { index ->
            val resId = when (index) {
                1 -> R.drawable.bg1
                2 -> R.drawable.bg2
                3 -> R.drawable.bg3
                4 -> R.drawable.bg4
                5 -> R.drawable.bg5
                6 -> R.drawable.bg6
                else -> R.drawable.bg1
            }
            gameView.setBackground(resId)
            gameView2.setBackground(resId)
            startScreen.setBackground(resId)
        }

        pauseMenu.onToggleCameraBackground = { enabled ->
            if (GS.isUseHandTracker) {
                GS.isUseCamera = enabled
            } else {
                GS.isUseCamera = false
                Toast.makeText(
                    applicationContext,
                    "Enable hand detection to use this feature",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        pauseMenu.onToggleHandDetection = { enabled ->
            if (GS.isVersusMode) {
                Toast.makeText(
                    applicationContext,
                    "Hand detection is not supported in Versus mode",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            if (enabled) {
                GS.isUseHandTracker = enabled
                GS.isUseCamera = enabled
                graphicOverlay.visibility = View.VISIBLE
                poseViewFinder.visibility = View.VISIBLE
                createCameraSource()
                startCameraSource()
                Toast.makeText(applicationContext, "Hand detection enabled", Toast.LENGTH_SHORT)
                    .show()
            } else {
                GS.isUseHandTracker = enabled
                GS.isUseCamera = enabled
                graphicOverlay.visibility = View.GONE
                poseViewFinder.visibility = View.GONE
                cameraSource.stop()

                Toast.makeText(applicationContext, "Hand detection disabled", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        pauseMenu.onToggleMusicEnabled = { enable ->
            setMusicEnabled(enable)
        }

        gameView.setOnPauseRequestedListener {
            if (countdownOverlay.isVisible) {
                countdownOverlay.cancelCountdown()
            }

            GS.isPaused = true
            pauseMenu.visibility = View.VISIBLE
        }

        gameView.setOnGameOverListener {
            GS.p1GameOver = true
            if (GS.isEndGame) {
                btnPause.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    gameView.resetGame()
                    gameView2.resetGame()
                    GS.isPaused = true
                    GS.isVersusMode = false
                    gameView.visibility = View.GONE
                    gameView2.visibility = View.GONE
                    processor?.multiPlayerMode = false
                    startScreen.show()
                    GS.isGameStarted = false
                    GS.isVersusMode = false
                    updatePauseMenuBackground()
                }, 4000)
            }
        }

        gameView2.setOnPauseRequestedListener {
            if (countdownOverlay.isVisible) {
                countdownOverlay.cancelCountdown()
            }

            GS.isPaused = true
            pauseMenu.visibility = View.VISIBLE
        }

        gameView2.setOnGameOverListener {
            GS.p2GameOver = true
            if (GS.isEndGame) {
                btnPause.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    gameView.resetGame()
                    gameView2.resetGame()
                    GS.isPaused = true
                    gameView.visibility = View.GONE
                    gameView2.visibility = View.GONE
                    processor?.multiPlayerMode = false
                    startScreen.show()
                    GS.isGameStarted = false
                    GS.isVersusMode = false
                    updatePauseMenuBackground()
                }, 4000)
            }
        }

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    /** Stops the camera. */
    override fun onPause() {
        super.onPause()
        if (GS.isMusicEnabled) {
            mediaPlayer?.pause()
        }
        if (GS.isUseHandTracker) {
            poseViewFinder.stop()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (GS.isMusicEnabled) {
            mediaPlayer?.start()
        }
        if (GS.isUseHandTracker) {
            createCameraSource()
            startCameraSource()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        cameraSource.release()
        getSharedPreferences("game_prefs", MODE_PRIVATE).edit {
            putInt(
                "best_score",
                GS.bestScore
            )
        }
    }


    private fun updatePauseMenuBackground() {
        if (GS.isGameStarted) {
            btnPause.visibility = View.VISIBLE
            if (GS.isVersusMode) {
                btnPause.post {
                    btnPause.x = resources.displayMetrics.widthPixels / 2 - btnPause.width / 2f
                }
            } else {
                btnPause.post {
                    btnPause.x = resources.displayMetrics.widthPixels - btnPause.width - 80f
                }
            }
        } else {
            btnPause.visibility = View.GONE
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!GS.isPaused || (GS.isPaused && !GS.isGameStarted)) {
            fruitSliceView.onTouch(ev) // hiển thị hiệu ứng dao
        }
        if (
            ev.actionMasked == MotionEvent.ACTION_DOWN ||
            ev.actionMasked == MotionEvent.ACTION_MOVE ||
            ev.actionMasked == MotionEvent.ACTION_POINTER_DOWN
        ) {
            for (i in 0 until ev.pointerCount) {
                var x = ev.getX(i)
                val y = ev.getY(i)

                val receiver: SliceEffectReceiver = when {
                    pauseMenu.isVisible -> pauseMenu
                    !GS.isGameStarted -> startScreen
                    !GS.isPaused -> {
                        if (x < gameView.width) gameView else {
                            x -= gameView.width
                            gameView2
                        }
                    }

                    else -> continue
                }

                receiver.onSliceAt(x, y)
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private var handTracker: HandTracker? = null

    private var processor: PoseDetectorProcessor? = null

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.

        cameraSource = CameraSource(this, graphicOverlay)

        try {
            val poseDetectorOptions = PreferenceUtils.getPoseDetectorOptionsForLivePreview(this)
            Log.i(TAG, "Using Pose Detector with options $poseDetectorOptions")
            val shouldShowInFrameLikelihood =
                PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this)
            val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this)
            val rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this)

            handTracker = HandTracker(
                movementThreshold = 3f,
                listener = object : HandLandmarkListener {
                    override fun onPlayer1HandLandmarksReceived(
                        leftIndexX: Float?,
                        leftIndexY: Float?,
                        rightIndexX: Float?,
                        rightIndexY: Float?
                    ) {
//                        Log.d("pos1", "${leftIndexX} - ${leftIndexY}")
                        processHandOfPlayer(1, "left", leftIndexX, leftIndexY)
                        processHandOfPlayer(1, "right", rightIndexX, rightIndexY)
                    }

                    override fun onPlayer2HandLandmarksReceived(
                        leftIndexX: Float?,
                        leftIndexY: Float?,
                        rightIndexX: Float?,
                        rightIndexY: Float?
                    ) {
                        Log.d("pos2", "${leftIndexX} - ${leftIndexY}")
                        processHandOfPlayer(2, "left", leftIndexX, leftIndexY)
                        processHandOfPlayer(2, "right", rightIndexX, rightIndexY)
                    }

                    private fun processHandOfPlayer(
                        player: Int,
                        hand: String,
                        x: Float?,
                        y: Float?
                    ) {
                        val posX: Float
                        if (x == null) posX = -200f
                        else {
                            if (player == 2) {
                                posX = x
                            } else {
                                posX = x - gameView.width
                            }
                        }
//                        val posX = x ?: -200f
                        val posY = y ?: -200f

                        val views = listOf(if (player == 2) gameView else gameView2, pauseMenu, startScreen)

                        views.forEach { view ->
                            if (hand == "left") view.updateLeftHandPosition(posX, posY)
                            else view.updateRightHandPosition(posX, posY)
                        }
                        if (x == null || y == null) {
                            return
                        }
                        if (player == 2) {
                            fruitSliceView.registerHandSlice(
                                if (hand == "left") 1001 else 1002,
                                posX,
                                posY
                            ) // nếu có
                            if (!GS.isEndGame) {
                                // Gọi slice effect như dispatchTouchEvent
                                val receiver: SliceEffectReceiver = when {
                                    pauseMenu.isVisible -> pauseMenu
                                    !GS.isGameStarted -> startScreen
                                    !GS.isPaused -> gameView
                                    else -> return
                                }
                                receiver.onSliceAt(posX, posY)
                            }
                        } else {
                            fruitSliceView.registerHandSlice(
                                if (hand == "left") 1003 else 1004,
                                posX,
                                posY
                            )
                            if (!GS.isEndGame) {
                                // Gọi slice effect như dispatchTouchEvent
                                val receiver: SliceEffectReceiver = when {
                                    pauseMenu.isVisible -> pauseMenu
                                    !GS.isGameStarted -> startScreen
                                    !GS.isPaused -> gameView2
                                    else -> return
                                }
                                receiver.onSliceAt(posX, posY)
                            }
                        }

                    }
                }
            )

            processor = PoseDetectorProcessor(
                this,
                poseDetectorOptions,
                shouldShowInFrameLikelihood,
                visualizeZ,
                rescaleZ,
                multiPlayerMode = false
            )

            processor?.setHandLandmarkListener(object : HandLandmarkListener {
                override fun onPlayer1HandLandmarksReceived(
                    leftIndexX: Float?,
                    leftIndexY: Float?,
                    rightIndexX: Float?,
                    rightIndexY: Float?
                ) {
                    handTracker?.updatePlayer1(leftIndexX, leftIndexY, rightIndexX, rightIndexY)
                }

                override fun onPlayer2HandLandmarksReceived(
                    leftIndexX: Float?,
                    leftIndexY: Float?,
                    rightIndexX: Float?,
                    rightIndexY: Float?
                ) {
                    handTracker?.updatePlayer2(leftIndexX, leftIndexY, rightIndexX, rightIndexY)
                }
            })

            cameraSource.setMachineLearningFrameProcessor(processor)
        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor", e)
            Toast.makeText(
                applicationContext,
                "Can not create image processor: " + e.message,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        try {
            poseViewFinder.start(cameraSource, graphicOverlay)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to start camera source.", e)
            cameraSource.release()
        }
    }

    private fun enableFullScreen() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    fun startMusic(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.theme_song)
            mediaPlayer?.isLooping = true
        }
        if (!GS.isMusicEnabled) {
            mediaPlayer?.start()
        }
    }

    fun setMusicEnabled(enable: Boolean) {
        GS.isMusicEnabled = enable
        if (mediaPlayer == null) {
            return
        }
        if (enable) {
            mediaPlayer?.start()
        } else {
            mediaPlayer?.pause()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
    }
}


