package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildChannelTest {

    @Test
    fun current_returnsTestForDebugBuild() {
        assertEquals(BuildChannel.TEST, BuildChannelResolver.current(isDebugBuild = true))
    }

    @Test
    fun current_returnsProdForReleaseBuild() {
        assertEquals(BuildChannel.PROD, BuildChannelResolver.current(isDebugBuild = false))
    }
}
