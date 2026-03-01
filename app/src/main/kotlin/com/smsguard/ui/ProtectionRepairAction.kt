package com.smsguard.ui

import androidx.annotation.StringRes
import com.smsguard.R
import com.smsguard.core.ProtectionStatusReport

enum class ProtectionRepairAction {
    NONE,
    ENABLE_LISTENER,
    ENABLE_ALERTS,
    FIX_FOREGROUND_NOTIFICATION,
}

internal fun primaryRepairActionFor(report: ProtectionStatusReport): ProtectionRepairAction =
    when {
        !report.listenerOk -> ProtectionRepairAction.ENABLE_LISTENER
        !report.alertsReady -> ProtectionRepairAction.ENABLE_ALERTS
        !report.foregroundNotificationAllowed -> ProtectionRepairAction.FIX_FOREGROUND_NOTIFICATION
        else -> ProtectionRepairAction.NONE
    }

@StringRes
internal fun primaryActionLabelResIdFor(action: ProtectionRepairAction): Int =
    when (action) {
        ProtectionRepairAction.NONE -> R.string.setup_action_check_rules
        ProtectionRepairAction.ENABLE_LISTENER,
        ProtectionRepairAction.ENABLE_ALERTS,
        ProtectionRepairAction.FIX_FOREGROUND_NOTIFICATION,
        -> R.string.setup_action_activate
    }
