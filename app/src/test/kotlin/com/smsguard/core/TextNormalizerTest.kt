package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun normalize_lowercases() {
        assertEquals("hello world", TextNormalizer.normalize("Hello World"))
    }

    @Test
    fun normalize_stripsDiacritics() {
        assertEquals("acao referencia entao", TextNormalizer.normalize("Ação Referência Então"))
    }

    @Test
    fun normalize_portugueseAccents() {
        assertEquals("avaliacao publica", TextNormalizer.normalize("Avaliação Pública"))
    }

    @Test
    fun normalize_collapsesWhitespace() {
        assertEquals("a b c", TextNormalizer.normalize("  a   b   c  "))
    }

    @Test
    fun normalize_trims() {
        assertEquals("trimmed", TextNormalizer.normalize("  trimmed  "))
    }

    @Test
    fun normalize_blankInput_returnsEmpty() {
        assertEquals("", TextNormalizer.normalize(""))
        assertEquals("", TextNormalizer.normalize("   "))
    }

    @Test
    fun normalize_preservesNumbers() {
        assertEquals("ref 123456789", TextNormalizer.normalize("REF 123456789"))
    }

    @Test
    fun normalize_mixedContent() {
        assertEquals(
            "pague ja! entidade: 12345 ref: 987654321 valor: 49,99eur",
            TextNormalizer.normalize("PAGUE JÁ! Entidade: 12345 Ref: 987654321 Valor: 49,99EUR"),
        )
    }

    @Test
    fun normalize_tabsAndNewlines() {
        assertEquals("a b", TextNormalizer.normalize("a\t\nb"))
    }

    @Test
    fun normalize_cyrillicPreserved() {
        // Cyrillic characters are not diacritics and should be preserved (lowercase)
        val result = TextNormalizer.normalize("раураl")
        assertEquals("раураl", result)
    }
}
