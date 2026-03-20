package com.mrm.minierp.models

import kotlin.test.Test
import kotlin.test.assertEquals

class QuoteLineTest {

    @Test
    fun testCalculations() {
        val line = QuoteLine(
            quantity = 2.0,
            concept = "Test",
            unitPrice = 100.0,
            iva = 21
        )
        
        assertEquals(200.0, line.totalWithoutIva)
        assertEquals(42.0, line.ivaAmount)
        assertEquals(242.0, line.totalWithIva)
    }

    @Test
    fun testIva10() {
        val line = QuoteLine(
            quantity = 1.0,
            concept = "Test",
            unitPrice = 100.0,
            iva = 10
        )
        
        assertEquals(10.0, line.ivaAmount)
        assertEquals(110.0, line.totalWithIva)
    }
}
