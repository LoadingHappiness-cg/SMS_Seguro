package com.smsguard.ui

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDisclosureTest {

    @Test
    fun notificationListenerDisclosure_containsRequiredPointsInOrder() {
        assertEquals(
            listOf(
                R.string.notification_listener_disclosure_data,
                R.string.notification_listener_disclosure_purpose,
                R.string.notification_listener_disclosure_local_processing,
                R.string.notification_listener_disclosure_no_server,
            ),
            notificationListenerDisclosureResIds(),
        )
    }
}
