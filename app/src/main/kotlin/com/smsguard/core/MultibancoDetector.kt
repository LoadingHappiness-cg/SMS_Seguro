package com.smsguard.core

import kotlinx.serialization.Serializable

@Serializable
data class MultibancoData(
    val entidade: String,
    val referencia: String,
    val valor: String?,
    val entityDetected: Boolean = true,
    val referenceDetected: Boolean = true,
    val amountDetected: Boolean = false,
)

object MultibancoDetector {

    private val entidadeRegex =
        Regex("""entidade[:\s]*([0-9]{5})""")

    private val referenciaRegex =
        Regex("""(?:referencia|ref)[:\s]*([0-9]{9,})""")

    private val valorRegex =
        Regex("""([0-9]+[,.][0-9]{2})\s*(?:€|eur)""")

    private val entityNumberRegex = Regex("""\b(\d{5})\b""")
    private val referenceNumberRegex = Regex("""\b(\d{9,})\b""")
    private val amountFallbackRegex = Regex("""(?:valor[:\s]*)?[0-9]+[,.][0-9]{2}\s*(?:€|eur)?""")

    fun detect(text: String): MultibancoData? {
        val entidadeMatch = entidadeRegex.find(text)
        val referenciaMatch = referenciaRegex.find(text)

        val valorMatch = valorRegex.find(text)
        val valor = valorMatch?.groupValues?.get(1)

        if (entidadeMatch != null && referenciaMatch != null) {
            val entidade = entidadeMatch.groupValues[1]
            val referencia = referenciaMatch.groupValues[1]

            return MultibancoData(
                entidade = entidade,
                referencia = referencia,
                valor = valor,
                entityDetected = true,
                referenceDetected = true,
                amountDetected = valor != null,
            )
        }

        val hasEntityKeyword = text.contains("entidade")
        val hasReferenceKeyword = text.contains("ref") || text.contains("referencia")
        if (!hasEntityKeyword || !hasReferenceKeyword) return null

        val entity =
            entityNumberRegex.find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?: return null
        val reference =
            referenceNumberRegex.findAll(text)
                .mapNotNull { it.groupValues.getOrNull(1) }
                .firstOrNull { it != entity }
                ?: return null
        val amountDetected = amountFallbackRegex.containsMatchIn(text)

        return MultibancoData(
            entidade = entity,
            referencia = reference,
            valor = valor,
            entityDetected = true,
            referenceDetected = true,
            amountDetected = amountDetected || valor != null,
        )
    }
}
