package com.hci.ninjafruitgame.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF

data class ScorePop(
    var position: PointF,
    var alpha: Int = 255,
    var scale: Float = 1.5f,
    var text: String = "+1"
) {
    fun update() {
        position.y -= 2f
        alpha -= 8
        scale -= 0.02f
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (alpha <= 0) return

        paint.alpha = alpha
        paint.textSize = 50f * scale
        canvas.drawText(text, position.x, position.y, paint)
    }

    fun isAlive() = alpha > 0
}
