package com.smsguard

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SmsGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable Crashlytics in release builds
        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }
}
