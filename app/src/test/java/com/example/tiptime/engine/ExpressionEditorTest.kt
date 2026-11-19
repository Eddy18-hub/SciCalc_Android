package com.example.tiptime.engine

import com.example.tiptime.engine.ExpressionEditor as E
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressionEditorTest {

    @Test
    fun digitsReplaceLeadingZero() {
        assertEquals("5", E.appendDigit("0", '5'))
        assertEquals("0.5", E.appendDigit("0.", '5'))
        assertEquals("10", E.appendDigit("1", '0'))
        assertEquals("2+5", E.appendDigit("2+0", '5'))
        assertEquals("2+0.0", E.appendDigit("2+0.", '0'))
    }

    @Test
    fun decimalPoint() {
        assertEquals("0.", E.appendDecimal(""))
        assertEquals("2+0.", E.appendDecimal("2+"))
        assertEquals("3.", E.appendDecimal("3"))
        assertEquals("3.5", E.appendDecimal("3.5"))          // ya tiene punto
        assertEquals("1.5+2.", E.appendDecimal("1.5+2"))     // un número nuevo sí admite punto
    }

    @Test
    fun operators() {
        assertEquals("", E.appendOperator("", '×'))          // no se puede empezar con ×
        assertEquals("−", E.appendOperator("", '−'))         // pero sí con signo menos
        assertEquals("2+", E.appendOperator("2", '+'))
        assertEquals("2×", E.appendOperator("2+", '×'))      // reemplaza al operador anterior
        assertEquals("2×−", E.appendOperator("2×", '−'))     // signo negativo tras ×
        assertEquals("2+", E.appendOperator("2×−", '+'))     // y se reemplaza completo
        assertEquals("(−", E.appendOperator("(", '−'))
        assertEquals("(", E.appendOperator("(", '×'))        // nada tiene sentido tras "("
        assertEquals("−", E.appendOperator("−", '−'))
        assertEquals("", E.appendOperator("−", '×'))
    }

    @Test
    fun percent() {
        assertEquals("50%", E.appendPercent("50"))
        assertEquals("(2+3)%", E.appendPercent("(2+3)"))
        assertEquals("", E.appendPercent(""))
        assertEquals("2+", E.appendPercent("2+"))
    }

    @Test
    fun parentheses() {
        assertEquals("(", E.appendOpenParenthesis(""))
        assertEquals("", E.appendCloseParenthesis(""))        // nada que cerrar
        assertEquals("(2)", E.appendCloseParenthesis("(2"))
        assertEquals("(", E.appendCloseParenthesis("("))      // paréntesis vacío no
        assertEquals("(2+", E.appendCloseParenthesis("(2+"))  // tras operador no
        assertEquals("(2)", E.appendCloseParenthesis("(2)"))  // ya está balanceado
    }

    @Test
    fun backspaceRemovesWholeFunctionTokens() {
        assertEquals("", E.backspace(""))
        assertEquals("12", E.backspace("123"))
        assertEquals("2+", E.backspace("2+sin("))
        assertEquals("2+", E.backspace("2+√("))
        assertEquals("2+", E.backspace("2+ln("))
        assertEquals("2+sin(3", E.backspace("2+sin(3)"))
        assertEquals("sin(", E.backspace("sin(3"))
    }

    @Test
    fun expressionLengthIsCapped() {
        val long = "1".repeat(200)
        assertEquals(long, E.appendDigit(long, '2'))
        assertEquals(long, E.appendOperator(long, '+'))
    }
}
