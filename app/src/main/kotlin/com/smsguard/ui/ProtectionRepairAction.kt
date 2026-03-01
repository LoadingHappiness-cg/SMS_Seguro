package com.smsguard.ui

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
