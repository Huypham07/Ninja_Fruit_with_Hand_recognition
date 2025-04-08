package com.hci.ninjafruitgame.utils

import android.content.Context
import android.media.SoundPool
import com.hci.ninjafruitgame.R

object SoundManager {
    private lateinit var soundPool: SoundPool
    private var sliceSoundId: Int = 0
    private var sliceBombSoundId: Int = 0
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        soundPool = SoundPool.Builder().setMaxStreams(5).build()
        sliceSoundId = soundPool.load(context, R.raw.slice, 1)
        sliceBombSoundId = soundPool.load(context, R.raw.slice_bomb, 1)
        initialized = true
    }

    fun playSlice() {
        if (initialized) {
            soundPool.play(sliceSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playSliceBomb() {
        if (initialized) {
            soundPool.play(sliceBombSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        if (initialized) {
            soundPool.release()
            initialized = false
        }
    }
}
