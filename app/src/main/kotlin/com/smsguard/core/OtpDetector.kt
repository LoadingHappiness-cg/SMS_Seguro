package com.smsguard.core

object OtpDetector {

    private val otpCodeRegex = Regex("""\b\d{4,8}\b""")

    // Após TextNormalizer.normalize() → sem diacríticos, minúsculas
    private val otpKeywords = listOf(
        "codigo", "verificacao", "verification", "otp", "pin",
        "login", "acesso", "autenticacao", "confirmar", "confirme",
        "confirmation", "one-time",
    )

    private val paymentThreatKeywords = listOf(
        "pague", "pagamento", "multa", "penhora", "bloqueio",
        "divida", "urgente", "24h", "48h", "conta bloqueada", "suspensa",
    )

    private val sensitiveDataKeywords = listOf(
        "iban", "cartao", "cvv", "nif", "senha", "password", "mbway",
    )

    private val callbackScamPhoneRegex = Regex("""(?:\+?351\s*)?\d{9}\b""")

    /**
     * Recebe texto já normalizado (TextNormalizer.normalize) e a lista de URLs
     * extraída por UrlExtractor.
     *
     * Retorna true apenas se a mensagem for um OTP legítimo sem sinais de phishing.
     */
    fun isSafeOtp(normalizedText: String, urls: List<String>): Boolean {
        if (!otpCodeRegex.containsMatchIn(normalizedText)) return false
        if (otpKeywords.none { normalizedText.contains(it) }) return false
        if (urls.isNotEmpty()) return false
        if (paymentThreatKeywords.any { normalizedText.contains(it) }) return false
        if (sensitiveDataKeywords.any { normalizedText.contains(it) }) return false
        if (looksLikeCallbackScam(normalizedText)) return false
        return true
    }

    private fun looksLikeCallbackScam(normalizedText: String): Boolean {
        val callbackKeywords = listOf(
            "saque",
            "levantamento",
            "ligue",
            "telefone",
            "chame",
            "chamar",
        )

        return callbackKeywords.any { normalizedText.contains(it) } &&
            callbackScamPhoneRegex.containsMatchIn(normalizedText)
    }
}
