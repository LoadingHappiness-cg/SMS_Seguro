package com.smsguard.core

import com.smsguard.BuildConfig

enum class BuildChannel {
    TEST,
    PROD,
}

object BuildChannelResolver {
    fun current(isDebugBuild: Boolean = BuildConfig.DEBUG): BuildChannel =
        if (isDebugBuild) {
            BuildChannel.TEST
        } else {
            BuildChannel.PROD
        }
}
