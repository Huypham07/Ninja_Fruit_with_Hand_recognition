package com.hci.ninjafruitgame

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hci.ninjafruitgame.view.FruitSliceView
import com.hci.ninjafruitgame.view.GameView
import com.hci.ninjafruitgame.view.SliceEffectReceiver
import com.hci.ninjafruitgame.view.StartScreenView

class MainActivity : AppCompatActivity() {

    private lateinit var fruitSliceView: FruitSliceView
    private lateinit var gameView: GameView
    private lateinit var startScreen: StartScreenView

    private var isGameStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fruitSliceView = findViewById(R.id.view)
        gameView = findViewById(R.id.gameView)
        startScreen = findViewById(R.id.startScreen)

        startScreen.onStartGame = {
            startGame()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        fruitSliceView.onTouch(ev) // hiển thị hiệu ứng dao

        if (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE) {
            val x = ev.x
            val y = ev.y

            val receiver: SliceEffectReceiver = if (!isGameStarted) startScreen else gameView
            receiver.onSliceAt(x, y)
        }

        return super.dispatchTouchEvent(ev)
    }


    private fun startGame() {
        isGameStarted = true
        startScreen.visibility = View.GONE
        gameView.visibility = View.VISIBLE
    }
}

