package com.hci.ninjafruitgame.ml

import android.graphics.Bitmap
import com.hci.ninjafruitgame.data.Person

interface PoseDetector : AutoCloseable {

    fun estimatePoses(bitmap: Bitmap): List<Person>

    fun lastInferenceTimeNanos(): Long
}
