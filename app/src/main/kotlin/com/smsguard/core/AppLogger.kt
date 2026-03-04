package com.smsguard.core

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.smsguard.BuildConfig

object AppLogger {

    private const val TAG = "SMS_SEGURO"

    private val crashlytics: FirebaseCrashlytics?
        get() = try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            null
        }

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

        // Report to Crashlytics in release builds
        if (!BuildConfig.DEBUG) {
            crashlytics?.let { fc ->
                try {
                    if (error != null) {
                        fc.recordException(error)
                    } else {
                        // Log non-exception errors as custom keys
                        fc.log("$TAG: $message")
                    }
                } catch (e: Exception) {
                    // Silently ignore Crashlytics failures
                }
            }
        }
    }
}
