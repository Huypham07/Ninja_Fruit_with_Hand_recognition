package com.hci.ninjafruitgame.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object BitmapUtils {
    fun loadBitmap(context: Context, resId: Int, width: Int, height: Int): Bitmap {
        val raw = BitmapFactory.decodeResource(context.resources, resId)
        return Bitmap.createScaledBitmap(raw, width, height, true)
    }
}