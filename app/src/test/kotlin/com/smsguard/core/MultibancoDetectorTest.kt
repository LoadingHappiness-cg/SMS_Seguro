package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MultibancoDetectorTest {

    // ---- Primary regex path (explicit keywords + formatted numbers) ----

    @Test
    fun detect_fullMultibanco_withAmount() {
        val text = "entidade: 12345 referencia: 123456789 19,99 €"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("12345", result!!.entidade)
        assertEquals("123456789", result.referencia)
        assertEquals("19,99", result.valor)
        assertTrue(result.entityDetected)
        assertTrue(result.referenceDetected)
        assertTrue(result.amountDetected)
    }

    @Test
    fun detect_entityAndReference_withoutAmount() {
        val text = "entidade: 54321 referencia: 999888777"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("54321", result!!.entidade)
        assertEquals("999888777", result.referencia)
        assertNull(result.valor)
        assertFalse(result.amountDetected)
    }

    @Test
    fun detect_refAbbreviation() {
        val text = "entidade: 11111 ref: 222333444"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("222333444", result!!.referencia)
    }

    @Test
    fun detect_amountWithEur() {
        val text = "entidade: 12345 referencia: 123456789 49,99 eur"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("49,99", result!!.valor)
        assertTrue(result.amountDetected)
    }

    @Test
    fun detect_amountWithDot() {
        val text = "entidade: 12345 referencia: 123456789 9.50 €"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("9.50", result!!.valor)
    }

    // ---- Fallback path (keywords present but numbers loosely formatted) ----

    @Test
    fun detect_fallback_entityAndRefKeywordsWithLooseNumbers() {
        val text = "entidade 99999 ref 111222333"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("99999", result!!.entidade)
        assertEquals("111222333", result.referencia)
    }

    // ---- Negative cases ----

    @Test
    fun detect_noKeywords_returnsNull() {
        assertNull(MultibancoDetector.detect("Olá, como está?"))
    }

    @Test
    fun detect_onlyEntityKeyword_noRef_returnsNull() {
        assertNull(MultibancoDetector.detect("entidade: 12345"))
    }

    @Test
    fun detect_onlyRefKeyword_noEntity_returnsNull() {
        assertNull(MultibancoDetector.detect("ref: 123456789"))
    }

    @Test
    fun detect_emptyString_returnsNull() {
        assertNull(MultibancoDetector.detect(""))
    }

    @Test
    fun detect_normalTextWithNumbers_returnsNull() {
        assertNull(MultibancoDetector.detect("O seu código é 123456789. Obrigado."))
    }

    // ---- Edge cases ----

    @Test
    fun detect_longReference() {
        val text = "entidade: 12345 referencia: 1234567890123"
        val result = MultibancoDetector.detect(text)
        assertNotNull(result)
        assertEquals("1234567890123", result!!.referencia)
    }

    @Test
    fun detect_entityAndRefSameNumber_fallback_skips() {
        // In fallback path, reference must differ from entity
        val text = "entidade 12345 ref 12345"
        val result = MultibancoDetector.detect(text)
        // Primary regex won't match (ref needs 9+ digits)
        // Fallback: entity=12345, ref candidates = [12345] but same as entity, so null
        assertNull(result)
    }
}
