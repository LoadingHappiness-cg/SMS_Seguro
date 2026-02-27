package com.smsguard.core

import java.text.Normalizer

object TextNormalizer {

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val withoutAccents =
            Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{M}+".toRegex(), "")
        return withoutAccents
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
