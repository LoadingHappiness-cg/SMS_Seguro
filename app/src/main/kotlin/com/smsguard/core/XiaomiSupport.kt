package com.smsguard.core

import android.content.Context
import android.os.Build

data class XiaomiSupportInfo(
    val isXiaomiBrand: Boolean,
    val isMiuiLike: Boolean,
) {
    val shouldShowGuidance: Boolean
        get() = isXiaomiBrand || isMiuiLike
}

internal fun isXiaomiManufacturer(manufacturer: String?): Boolean {
    val normalized = manufacturer.orEmpty().trim().lowercase()
    return normalized.contains("xiaomi") || normalized.contains("redmi") || normalized.contains("poco")
}

internal fun hasMiuiOrHyperOsProperties(properties: Map<String, String?>): Boolean =
    properties
        .filterKeys {
            it == "ro.miui.ui.version.name" ||
                it == "ro.miui.ui.version.code" ||
                it == "ro.miui.ui.version.incremental"
        }.values
        .any { !it.isNullOrBlank() }

fun Context.xiaomiSupportInfo(): XiaomiSupportInfo {
    val properties =
        listOf(
            "ro.miui.ui.version.name",
            "ro.miui.ui.version.code",
            "ro.miui.ui.version.incremental",
        ).associateWith(::readSystemProperty)

    return XiaomiSupportInfo(
        isXiaomiBrand = isXiaomiManufacturer(Build.MANUFACTURER),
        isMiuiLike = hasMiuiOrHyperOsProperties(properties),
    )
}

private fun readSystemProperty(key: String): String? =
    runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java)
        (method.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()
