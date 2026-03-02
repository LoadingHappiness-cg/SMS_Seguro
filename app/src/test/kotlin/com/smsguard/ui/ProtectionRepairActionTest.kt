package com.smsguard.ui

import com.smsguard.R
import com.smsguard.core.protectionStatusReport
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionRepairActionTest {

    @Test
    fun primaryActionLabel_usesSingleMainCtaForRepairs() {
        assertEquals(
            R.string.setup_action_activate,
            primaryActionLabelResIdFor(ProtectionRepairAction.ENABLE_LISTENER),
        )
        assertEquals(
            R.string.setup_action_activate,
            primaryActionLabelResIdFor(ProtectionRepairAction.ENABLE_ALERTS),
        )
        assertEquals(
            R.string.setup_action_fix_foreground_notification,
            primaryActionLabelResIdFor(ProtectionRepairAction.FIX_FOREGROUND_NOTIFICATION),
        )
        assertEquals(
            R.string.setup_action_check_rules,
            primaryActionLabelResIdFor(ProtectionRepairAction.NONE),
        )
    }

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
                    foregroundNotificationAllowed = true,
                    foregroundChannelOk = true,
                    foregroundNotificationVisible = false,
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
