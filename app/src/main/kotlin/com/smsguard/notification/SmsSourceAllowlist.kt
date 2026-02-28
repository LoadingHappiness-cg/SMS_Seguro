package com.smsguard.notification

import android.content.Context
import android.os.Build
import android.provider.Telephony

object SmsSourceAllowlist {

    private val knownSmsPackages =
        setOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.miui.mms",
            "com.huawei.message",
        )

    fun isAllowed(context: Context, pkg: String): Boolean {
        val defaultSmsPackage =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Telephony.Sms.getDefaultSmsPackage(context)
            } else {
                null
            }

        return isAllowedPackage(pkg = pkg, defaultSmsPackage = defaultSmsPackage)
    }

    internal fun isAllowedPackage(
        pkg: String,
        defaultSmsPackage: String?,
    ): Boolean {
        if (pkg == defaultSmsPackage) return true
        return pkg in knownSmsPackages
    }
}
