package com.smsguard.core

import kotlinx.serialization.Serializable

@Serializable
enum class AlertType {
    URL,
    MULTIBANCO
}

