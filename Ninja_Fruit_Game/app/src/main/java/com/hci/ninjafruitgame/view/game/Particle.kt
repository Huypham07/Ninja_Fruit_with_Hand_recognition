package com.hci.ninjafruitgame.view.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF

data class Particle(
    var position: PointF,
    var velocity: PointF,
    var alpha: Int = 255,
    var radius: Float = 5f,
    val color: Int = Color.RED,
    val shrinkRate: Float = 0.1f
) {
    fun update() {
        position.x += velocity.x
        position.y += velocity.y
        alpha -= 5
        radius = maxOf(radius - shrinkRate, 1f)
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (alpha > 0 && radius > 0) {
            paint.color = color
            paint.alpha = alpha
            canvas.drawCircle(position.x, position.y, radius, paint)
        }
    }

    fun isAlive(): Boolean = alpha > 0 && radius > 0
}
