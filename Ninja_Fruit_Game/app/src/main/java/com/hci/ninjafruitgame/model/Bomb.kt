package com.hci.ninjafruitgame.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.withRotation

data class Bomb(
    val bitmap: Bitmap,
    var position: PointF,
    var velocity: PointF,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 5f,
    var isSliced: Boolean = false
) {
    fun update(gravity: Float) {
        if (!isSliced) {
            position.x += velocity.x
            position.y += velocity.y
            velocity.y += gravity
            rotation += rotationSpeed
        }
    }

    fun draw(canvas: Canvas) {
        if (!isSliced) {
            canvas.withRotation(
                rotation,
                position.x + bitmap.width / 2,
                position.y + bitmap.height / 2
            ) {
                canvas.drawBitmap(bitmap, position.x, position.y, null)
            }
        }
    }

    fun getBounds(): RectF {
        return RectF(
            position.x,
            position.y,
            position.x + bitmap.width,
            position.y + bitmap.height
        )
    }
}