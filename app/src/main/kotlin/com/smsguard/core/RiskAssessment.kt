package com.smsguard.core

data class RiskAssessment(
    val alertType: AlertType,
    val primaryUrl: String,
    val primaryDomain: String,
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    val multibancoData: MultibancoData? = null,
)
