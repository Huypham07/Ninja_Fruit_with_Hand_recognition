package com.hci.ninjafruitgame.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF

data class GameObject(
    val bitmap: Bitmap,
    val bitmapResId: Int,
    var position: PointF,
    var velocity: PointF,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 5f,
    var isSliced: Boolean = false,
    val type: Int
) {
    fun update(gravity: Float, speedFactor: Float = 1f) {
        if (!isSliced) {
            position.x += velocity.x * speedFactor
            position.y += velocity.y * speedFactor
            velocity.y += gravity * speedFactor
            rotation += rotationSpeed * speedFactor
        }
    }

    fun draw(canvas: Canvas) {
        if (!isSliced) {
            val save = canvas.save()
            canvas.rotate(rotation, position.x + bitmap.width / 2, position.y + bitmap.height / 2)
            canvas.drawBitmap(bitmap, position.x, position.y, null)
            canvas.restoreToCount(save)
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