package com.smsguard.core

object UnicodeSpoofingDetector {

    data class HostnameScriptInfo(
        val hasNonLatinLetters: Boolean,
        val hasMixedLatinCyrillic: Boolean,
    )

    fun analyzeHostname(hostname: String): HostnameScriptInfo {
        var hasLatin = false
        var hasCyrillic = false
        var hasOtherNonLatin = false

        hostname.forEach { char ->
            if (!char.isLetter()) return@forEach
            when (Character.UnicodeScript.of(char.code)) {
                Character.UnicodeScript.LATIN -> hasLatin = true
                Character.UnicodeScript.CYRILLIC -> {
                    hasCyrillic = true
                    hasOtherNonLatin = true
                }
                else -> hasOtherNonLatin = true
            }
        }

        return HostnameScriptInfo(
            hasNonLatinLetters = hasOtherNonLatin,
            hasMixedLatinCyrillic = hasLatin && hasCyrillic,
        )
    }
}
