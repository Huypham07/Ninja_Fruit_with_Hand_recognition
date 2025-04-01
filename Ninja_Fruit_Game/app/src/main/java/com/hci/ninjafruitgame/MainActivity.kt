package com.hci.ninjafruitgame

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.hci.ninjafruitgame.view.CountdownOverlay
import com.hci.ninjafruitgame.view.FruitSliceView
import com.hci.ninjafruitgame.view.GameView
import com.hci.ninjafruitgame.view.PauseMenuView
import com.hci.ninjafruitgame.view.SliceEffectReceiver
import com.hci.ninjafruitgame.view.StartScreenView

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
            updatePauseMenuBackground()
        }

        pauseMenu.onResume = {
            Log.d("MainActivity", "Resuming game from pause")
            pauseMenu.hide()
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
            if (countdownOverlay.visibility == View.VISIBLE) {
                countdownOverlay.cancelCountdown()
            }

            gameView.pauseGame()
            btnPause.visibility = View.GONE
            pauseMenu.show()


        }

        val bgButtons = listOf(
            R.id.bg5 to R.drawable.bg,
            R.id.bg1 to R.drawable.bg1,
            R.id.bg2 to R.drawable.bg2,
            R.id.bg3 to R.drawable.bg3,
            R.id.bg4 to R.drawable.bg4,
            R.id.bg5 to R.drawable.bg,
        )
        bgButtons.forEach { (id, resId) ->
            pauseMenu.findViewById<Button>(id).setOnClickListener {
                gameView.setBackground(resId)
            }
        }

        gameView.setOnPauseRequestedListener {
            if (countdownOverlay.visibility == View.VISIBLE) {
                countdownOverlay.cancelCountdown()
            }

            gameView.pauseGame()
            pauseMenu.visibility = View.VISIBLE
        }
    }

    private fun updatePauseMenuBackground() {
        if (isGameStarted) {
            btnPause.setImageResource(R.drawable.pause)
        }  else {
            btnPause.setImageResource(R.drawable.settings)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (gameView.getIsPaused()) {
            return super.dispatchTouchEvent(ev)
        }
        fruitSliceView.onTouch(ev) // hiển thị hiệu ứng dao

        if (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE) {
            val x = ev.x
            val y = ev.y

            val receiver: SliceEffectReceiver = if (!isGameStarted) startScreen else gameView
            receiver.onSliceAt(x, y)
        }

        return super.dispatchTouchEvent(ev)
    }

}

