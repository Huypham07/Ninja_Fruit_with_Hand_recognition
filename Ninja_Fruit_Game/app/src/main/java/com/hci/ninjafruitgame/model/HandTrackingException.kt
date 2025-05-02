package com.hci.ninjafruitgame.model

class HandTrackingException(val code: Int, message: String) : Exception(message) {
    companion object {
        const val ERROR_MODEL_NOT_FOUND = 1
        const val ERROR_NOT_INITIALIZED = 2
        const val ERROR_NATIVE = 3
    }
}
