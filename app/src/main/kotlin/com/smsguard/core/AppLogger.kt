package com.smsguard.core

import android.util.Log
import com.smsguard.BuildConfig

object AppLogger {

    private const val TAG = "SMS_SEGURO"

    val isDebugEnabled: Boolean
        get() = BuildConfig.DEBUG

    fun d(message: String) {
        if (isDebugEnabled) {
            Log.d(TAG, message)
        }
    }

    fun w(message: String) {
        if (isDebugEnabled) {
            Log.w(TAG, message)
        }
    }

    fun e(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.e(TAG, message)
        } else {
            Log.e(TAG, message, error)
        }
    }
}
