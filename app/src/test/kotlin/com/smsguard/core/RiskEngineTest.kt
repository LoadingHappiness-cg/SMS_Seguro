package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    private val ruleSet =
        RuleSet(
            version = 3,
            publishedAt = "2026-02-27T00:00:00Z",
            scoring = ScoringConfig(ScoreThresholds(medium = 40, high = 70)),
            keywordGroups =
                KeywordGroups(
                    urgency = listOf("urgente", "imediato", "48h"),
                    threat = listOf("penhora", "bloqueio"),
                    payment = listOf("pagamento", "taxa"),
                    dataRequest = listOf("codigo", "pin", "mbway"),
                    publicServices = listOf("financas", "seguranca social", "sns"),
                    delivery = listOf("ctt", "dhl", "ups", "dpd", "entrega"),
                    banking = listOf("cgd", "bpi", "banco", "conta"),
                ),
            urlSignals =
                UrlSignals(
                    suspiciousTlds = listOf("xyz", "top", "club"),
                    shorteners = listOf("bit.ly", "tinyurl.com"),
                    weights =
                        UrlWeights(
                            hasUrl = 10,
                            shortener = 20,
                            punycode = 35,
                            cyrillicOrNonLatinHostname = 50,
                            mixedLatinCyrillicHostnameBonus = 20,
                            suspiciousTld = 25,
                        ),
                ),
            multibancoSignals =
                MultibancoSignals(
                    weights =
                        MultibancoWeights(
                            hasEntityRef = 25,
                            hasAmount = 10,
                            unknownEntity = 30,
                            intermediaryEntity = 20,
                            knownEntity = -10,
                        ),
                ),
            correlation =
                CorrelationConfig(
                    weights = CorrelationWeights(brandEntityMismatch = 50, brandUrlMismatch = 35),
                    brandEntityMap =
                        mapOf(
                            "ctt" to listOf("CTT"),
                            "financas" to listOf("Autoridade Tributária", "Pagamentos Estado"),
                        ),
                    brandAllowedDomains =
                        mapOf(
                            "ctt" to listOf("ctt.pt"),
                            "financas" to listOf("portaldasfinancas.gov.pt", "gov.pt"),
                        ),
                ),
            multibanco =
                MultibancoConfig(
                    entities =
                        mapOf(
                            "10095" to "Pagamentos Estado",
                            "10158" to "Vodafone",
                        ),
                    intermediaries = mapOf("10241" to "HiPay"),
                ),
        )

    @Test
    fun smishingWithUrgencyAndLink_isHighRisk() {
        val engine = RiskEngine(ruleSet)

        val result =
            engine.analyze(
                messageText = "URGENTE: bloqueio e penhora em 48h. Pague taxa pendente e confirme código em https://apoio-seguro.xyz/login",
                urls = listOf("https://apoio-seguro.xyz/login"),
                multibancoData = null,
            )

        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.reasons.contains("keyword_urgency"))
        assertTrue(result.reasons.contains("url_suspicious_tld"))
    }

    @Test
    fun multibancoUnknownEntity_isHighRisk() {
        val engine = RiskEngine(ruleSet)

        val result =
            engine.analyze(
                messageText = "Pagamento pendente. Entidade: 99999 Referência: 123456789 Valor: 20,00€",
                urls = emptyList(),
                multibancoData = MultibancoData(entidade = "99999", referencia = "123456789", valor = "20,00"),
            )

        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.reasons.contains("mb_unknown_entity"))
    }

    @Test
    fun knownEntityWithBrandMismatch_isHighRisk() {
        val engine = RiskEngine(ruleSet)

        val result =
            engine.analyze(
                messageText = "CTT: pagamento de desalfandegamento pendente. Entidade: 10158 Referência: 123456789",
                urls = listOf("https://ctt.pt/pagar"),
                multibancoData = MultibancoData(entidade = "10158", referencia = "123456789", valor = null),
            )

        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.reasons.contains("correlation_brand_entity_mismatch"))
    }

    @Test
    fun benignWithoutUrlOrMultibanco_isLowRisk() {
        val engine = RiskEngine(ruleSet)

        val result =
            engine.analyze(
                messageText = "Olá mãe, chego a casa às 19h. Até já.",
                urls = emptyList(),
                multibancoData = null,
            )

        assertEquals(RiskLevel.LOW, result.level)
        assertEquals(0, result.score)
    }

    @Test
    fun dataRequestOnly_isNeverLow() {
        val engine = RiskEngine(ruleSet)

        val result =
            engine.analyze(
                messageText = "Envie o código SMS para concluir a verificação.",
                urls = emptyList(),
                multibancoData = null,
            )

        assertTrue(result.level == RiskLevel.MEDIUM || result.level == RiskLevel.HIGH)
        assertTrue(result.reasons.contains("keyword_dataRequest"))
    }

    @Test
    fun cyrillicHostname_isNeverLow() {
        val engine = RiskEngine(ruleSet)

        val result =
            engine.analyze(
                messageText = "Valide aqui: https://раypal.com",
                urls = listOf("https://раypal.com"),
                multibancoData = null,
            )

        assertTrue(result.level == RiskLevel.MEDIUM || result.level == RiskLevel.HIGH)
        assertTrue(result.reasons.contains("url_non_latin_hostname"))
    }
}
