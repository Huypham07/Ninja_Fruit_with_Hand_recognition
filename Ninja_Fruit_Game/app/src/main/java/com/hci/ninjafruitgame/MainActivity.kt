package com.hci.ninjafruitgame

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.hci.ninjafruitgame.view.CountdownOverlay
import com.hci.ninjafruitgame.view.FruitSliceView
import com.hci.ninjafruitgame.view.GameView
import com.hci.ninjafruitgame.view.PauseMenuView
import com.hci.ninjafruitgame.view.SliceEffectReceiver
import com.hci.ninjafruitgame.view.StartScreenView
import kotlin.system.exitProcess
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {

    private lateinit var fruitSliceView: FruitSliceView
    private lateinit var gameView: GameView
    private lateinit var startScreen: StartScreenView
    private lateinit var countdownOverlay: CountdownOverlay
    private lateinit var pauseMenu: PauseMenuView
    private lateinit var btnPause: ImageView

    private var isGameStarted = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // FULL SCREEN MODE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }


        fruitSliceView = findViewById(R.id.view)
        gameView = findViewById(R.id.gameView)
        startScreen = findViewById(R.id.startScreen)
        pauseMenu = findViewById(R.id.pauseMenuContent)
        countdownOverlay = findViewById(R.id.countdownOverlay)
        btnPause = findViewById(R.id.btnPause)

        updatePauseMenuBackground()

        startScreen.onStartGame = {
            isGameStarted = true
            startScreen.visibility = View.GONE
            gameView.visibility = View.VISIBLE
            pauseMenu.visibility = View.GONE
            gameView.resetGame()
            updatePauseMenuBackground()
        }

        startScreen.onOpenSettings = {
            startScreen.playExitAnimation { pauseMenu.show(isGameStarted) }
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
            if (isGameStarted) {
                countdownOverlay.startCountdown {
                    gameView.resumeGame()
                }
            } else {
                gameView.resumeGame()
            }
        }

        btnPause.setOnClickListener {
            if (countdownOverlay.isVisible) {
                countdownOverlay.cancelCountdown()
            }

            gameView.pauseGame()
            btnPause.visibility = View.GONE
            pauseMenu.show(isGameStarted)
        }

        pauseMenu.onBackToStart = {
            gameView.resetGame()
            gameView.pauseGame()
            gameView.visibility = View.GONE
            startScreen.show()
            isGameStarted = false
            updatePauseMenuBackground()
        }

        pauseMenu.onRestart = {
            gameView.resetGame()
            gameView.pauseGame()
            countdownOverlay.startCountdown {
                gameView.resumeGame()
            }
            btnPause.visibility = View.VISIBLE
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
            startScreen.setBackground(resId)
        }

        gameView.setOnPauseRequestedListener {
            if (countdownOverlay.isVisible) {
                countdownOverlay.cancelCountdown()
            }

            gameView.pauseGame()
            pauseMenu.visibility = View.VISIBLE
        }
    }

    private fun updatePauseMenuBackground() {
        if (isGameStarted) {
            btnPause.visibility = View.VISIBLE
        } else {
            btnPause.visibility = View.GONE
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        if (!gameView.getIsPaused() || (gameView.getIsPaused() && !isGameStarted)) {
            fruitSliceView.onTouch(ev) // hiển thị hiệu ứng dao
        }
        if (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE) {

            val x = ev.x
            val y = ev.y

            val receiver: SliceEffectReceiver = when {
                pauseMenu.isVisible -> pauseMenu
                !isGameStarted -> startScreen
                !gameView.getIsPaused() -> gameView
                else -> return super.dispatchTouchEvent(ev)
            }
            receiver.onSliceAt(x, y)
        }

        return super.dispatchTouchEvent(ev)
    }

}

