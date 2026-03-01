package com.smsguard.ui

import com.smsguard.core.protectionStatusReport
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionRepairActionTest {

    @Test
    fun primaryRepairAction_prioritizesNotificationListener() {
        val action =
            primaryRepairActionFor(
                protectionStatusReport(
                    listenerOk = false,
                    notificationsAllowed = false,
                    postNotificationsOk = false,
                    alertChannelOk = false,
                    foregroundNotificationAllowed = false,
                ),
            )

        assertEquals(ProtectionRepairAction.ENABLE_LISTENER, action)
    }

    @Test
    fun primaryRepairAction_usesAlertsBeforeForeground() {
        val action =
            primaryRepairActionFor(
                protectionStatusReport(
                    listenerOk = true,
                    notificationsAllowed = false,
                    postNotificationsOk = false,
                    alertChannelOk = false,
                    foregroundNotificationAllowed = false,
                ),
            )

        assertEquals(ProtectionRepairAction.ENABLE_ALERTS, action)
    }

    @Test
    fun primaryRepairAction_usesForegroundAfterCriticalAlertRequirements() {
        val action =
            primaryRepairActionFor(
                protectionStatusReport(
                    listenerOk = true,
                    notificationsAllowed = true,
                    postNotificationsOk = true,
                    alertChannelOk = true,
                    foregroundNotificationAllowed = false,
                ),
            )

        assertEquals(ProtectionRepairAction.FIX_FOREGROUND_NOTIFICATION, action)
    }

    @Test
    fun primaryRepairAction_returnsNoneWhenReady() {
        val action =
            primaryRepairActionFor(
                protectionStatusReport(
                    listenerOk = true,
                    notificationsAllowed = true,
                    postNotificationsOk = true,
                    alertChannelOk = true,
                    foregroundNotificationAllowed = true,
                ),
            )

        assertEquals(ProtectionRepairAction.NONE, action)
    }
}
