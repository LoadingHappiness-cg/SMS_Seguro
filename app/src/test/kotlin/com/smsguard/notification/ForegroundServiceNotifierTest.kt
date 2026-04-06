package com.smsguard.notification

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundServiceNotifierTest {

    @Test
    fun discreetModeUsesNeutralNotificationCopy() {
        val content = ForegroundServiceNotifier.contentFor(discreetMode = true)

        assertEquals(R.string.foreground_notification_title_discreet, content.titleResId)
        assertEquals(R.string.foreground_notification_text_discreet, content.textResId)
    }

    @Test
    fun visibleModeKeepsExistingCopy() {
        val content = ForegroundServiceNotifier.contentFor(discreetMode = false)

        assertEquals(R.string.foreground_notification_title, content.titleResId)
        assertEquals(R.string.foreground_notification_text, content.textResId)
    }
}
