package com.hci.ninjafruitgame.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF

data class SlicedPiece(
    val bitmap: Bitmap,
    var position: PointF,
    var velocity: PointF,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f
) {
    fun update(gravity: Float, speedFactor: Float = 1f) {
            position.x += velocity.x * speedFactor
            position.y += velocity.y * speedFactor
            velocity.y += gravity * speedFactor
            rotation += rotationSpeed * speedFactor
    }

    fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.rotate(rotation, position.x + bitmap.width / 2, position.y + bitmap.height / 2)
        canvas.drawBitmap(bitmap, position.x, position.y, null)
        canvas.restoreToCount(save)
    }
}
