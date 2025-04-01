package com.hci.ninjafruitgame.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ToggleButton
import com.hci.ninjafruitgame.R

class PauseMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onResume: (() -> Unit)? = null
    var onBackgroundChange: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.pause_menu, this, true)
        visibility = GONE

        findViewById<Button>(R.id.btnResume).setOnClickListener {
            onResume?.invoke()
        }

        val backgrounds = listOf(
            R.id.bg5 to R.drawable.bg,
            R.id.bg1 to R.drawable.bg1,
            R.id.bg2 to R.drawable.bg2,
            R.id.bg3 to R.drawable.bg3,
            R.id.bg4 to R.drawable.bg4
        )

        backgrounds.forEach { (btnId, bgResId) ->
            findViewById<Button>(btnId).setOnClickListener {
                onBackgroundChange?.invoke(bgResId)
            }
        }

        findViewById<ToggleButton>(R.id.toggleCamera).setOnCheckedChangeListener { _, isChecked ->
            // TODO: Xử lý camera toggle nếu cần
        }

        findViewById<ToggleButton>(R.id.toggleSound).setOnCheckedChangeListener { _, isChecked ->
            // TODO: Xử lý sound toggle nếu cần
        }
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
