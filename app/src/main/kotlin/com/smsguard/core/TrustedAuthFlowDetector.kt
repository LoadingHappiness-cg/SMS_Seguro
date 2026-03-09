package com.smsguard.core

object TrustedAuthFlowDetector {

    private val trustedDomains =
        setOf(
            "accounts.google.com",
            "login.microsoftonline.com",
            "appleid.apple.com",
            "auth0.com",
            "okta.com",
        )

    private val oauthParams =
        listOf(
            "code=",
            "access_token=",
            "id_token=",
            "client_id=",
            "redirect_uri=",
        )

    private val authKeywords =
        listOf(
            "login",
            "entrar",
            "iniciar sessao",
            "conta",
            "autenticacao",
            "verificacao",
            "verification",
            "oauth",
        )

    fun isTrustedAuthFlow(normalizedText: String, urls: List<String>): Boolean {
        if (urls.isEmpty()) return false
        if (authKeywords.none { normalizedText.contains(it) }) return false

        return urls.any { url ->
            val lowerUrl = url.lowercase()
            val host = UrlExtractor.getDomain(url).lowercase()
            val trustedHost =
                trustedDomains.any { trusted ->
                    host == trusted || host.endsWith(".$trusted")
                }

            trustedHost && oauthParams.any { param -> lowerUrl.contains(param) }
        }
    }
}
