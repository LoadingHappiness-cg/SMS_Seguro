package com.smsguard.notification

import android.content.Context
import androidx.core.content.edit

object ForegroundNotificationPreferences {

    private const val PREFS_NAME = "foreground_notification_prefs"
    private const val KEY_DISCREET_MODE = "foreground_discreet_mode"

    fun isDiscreetModeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISCREET_MODE, false)

    fun setDiscreetModeEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DISCREET_MODE, enabled)
            .apply()
    }
}
