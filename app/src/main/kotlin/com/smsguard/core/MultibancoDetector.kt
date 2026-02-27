package com.smsguard.core

import kotlinx.serialization.Serializable

@Serializable
data class MultibancoData(
    val entidade: String,
    val referencia: String,
    val valor: String?,
)

object MultibancoDetector {

    private val entidadeRegex =
        Regex("""entidade[:\s]*([0-9]{5})""", RegexOption.IGNORE_CASE)

    private val referenciaRegex =
        Regex("""refer[eê]ncia[:\s]*([0-9]{9})""", RegexOption.IGNORE_CASE)

    private val valorRegex =
        Regex("""([0-9]+[,.][0-9]{2})\s*€""", RegexOption.IGNORE_CASE)

    fun detect(text: String): MultibancoData? {
        val entidadeMatch = entidadeRegex.find(text)
        val referenciaMatch = referenciaRegex.find(text)

        if (entidadeMatch == null || referenciaMatch == null) return null

        val entidade = entidadeMatch.groupValues[1]
        val referencia = referenciaMatch.groupValues[1]

        val valorMatch = valorRegex.find(text)
        val valor = valorMatch?.groupValues?.get(1)

        return MultibancoData(
            entidade = entidade,
            referencia = referencia,
            valor = valor
        )
    }
}

