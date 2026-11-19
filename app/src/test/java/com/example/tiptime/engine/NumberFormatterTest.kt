package com.example.tiptime.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun integersAndDecimals() {
        assertEquals("0", NumberFormatter.format(0.0))
        assertEquals("0", NumberFormatter.format(-0.0))
        assertEquals("5", NumberFormatter.format(5.0))
        assertEquals("-5", NumberFormatter.format(-5.0))
        assertEquals("2.5", NumberFormatter.format(2.5))
        assertEquals("1000", NumberFormatter.format(1000.0))
        assertEquals("123456789012", NumberFormatter.format(123456789012.0))
    }

    @Test
    fun removesBinaryNoise() {
        assertEquals("0.3", NumberFormatter.format(0.1 + 0.2))
        assertEquals("0.5", NumberFormatter.format(Math.sin(Math.toRadians(30.0))))
        assertEquals("1", NumberFormatter.format(Math.tan(Math.toRadians(45.0))))
        assertEquals("3.14159265359", NumberFormatter.format(Math.PI))
        assertEquals("0.333333333333", NumberFormatter.format(1.0 / 3.0))
    }

    @Test
    fun scientificNotationOutsideReadableRange() {
        assertEquals("1E12", NumberFormatter.format(1e12))
        assertEquals("1.2345E15", NumberFormatter.format(1.2345e15))
        assertEquals("-1.5E20", NumberFormatter.format(-1.5e20))
        assertEquals("1E-8", NumberFormatter.format(1e-8))
        assertEquals("0.0000001", NumberFormatter.format(1e-7))
        assertEquals("1.5E-10", NumberFormatter.format(1.5e-10))
    }

    @Test
    fun formattedOutputCanBeReparsed() {
        listOf(1e12, 1.2345e15, -1.5e20, 1e-8, 1.5e-10, 0.3, 123.456).forEach { v ->
            val text = NumberFormatter.format(v)
            val back = (ExpressionEngine.evaluate(text) as EvalResult.Success).value
            assertEquals(text, v, back, Math.abs(v) * 1e-11)
        }
    }

    @Test
    fun nonFiniteValuesDoNotCrash() {
        assertEquals("—", NumberFormatter.format(Double.NaN))
        assertEquals("—", NumberFormatter.format(Double.POSITIVE_INFINITY))
    }
}
