package com.hci.ninjafruitgame.view.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF

data class SplashMark(
    val bitmap: Bitmap,
    val position: PointF,
    val createTime: Long = System.currentTimeMillis(),
    val duration: Long = (1500L..2000L).random()
) {
    fun draw(canvas: Canvas, paint: Paint) {
        val elapsed = System.currentTimeMillis() - createTime
        val alpha = ((1f - elapsed.toFloat() / duration) * 200).toInt().coerceIn(0, 200)

        if (alpha <= 0) return

        paint.alpha = alpha
        val halfW = bitmap.width / 2f
        val halfH = bitmap.height / 2f
        canvas.drawBitmap(bitmap, position.x - halfW, position.y - halfH, paint)
    }

    fun isAlive(): Boolean {
        return System.currentTimeMillis() - createTime < duration
    }
}
