package com.smsguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionHealthTest {

    @Test
    fun protectionReady_requiresListenerNotificationsPermissionAndChannel() {
        val report =
            protectionStatusReport(
                listenerOk = true,
                notificationsAllowed = true,
                postNotificationsOk = true,
                alertChannelOk = true,
                foregroundNotificationAllowed = true,
            )

        assertTrue(report.isReady)
    }

    @Test
    fun protectionReady_failsWhenListenerIsMissing() {
        val report =
            protectionStatusReport(
                listenerOk = false,
                notificationsAllowed = true,
                postNotificationsOk = true,
                alertChannelOk = true,
                foregroundNotificationAllowed = true,
            )

        assertFalse(report.isReady)
    }

    @Test
    fun protectionReady_failsWhenNotificationsPermissionOrChannelAreMissing() {
        assertFalse(
            protectionStatusReport(
                listenerOk = true,
                notificationsAllowed = false,
                postNotificationsOk = true,
                alertChannelOk = true,
                foregroundNotificationAllowed = true,
            ).isReady,
        )
        assertFalse(
            protectionStatusReport(
                listenerOk = true,
                notificationsAllowed = true,
                postNotificationsOk = false,
                alertChannelOk = true,
                foregroundNotificationAllowed = true,
            ).isReady,
        )
        assertFalse(
            protectionStatusReport(
                listenerOk = true,
                notificationsAllowed = true,
                postNotificationsOk = true,
                alertChannelOk = false,
                foregroundNotificationAllowed = true,
            ).isReady,
        )
        assertFalse(
            protectionStatusReport(
                listenerOk = true,
                notificationsAllowed = true,
                postNotificationsOk = true,
                alertChannelOk = true,
                foregroundNotificationAllowed = false,
            ).isReady,
        )
    }
}
