package com.example.tiptime.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEngineTest {

    private fun eval(text: String, mode: AngleMode = AngleMode.DEGREES): Double {
        val result = ExpressionEngine.evaluate(text, mode)
        assertTrue("'$text' debía evaluarse pero dio $result", result is EvalResult.Success)
        return (result as EvalResult.Success).value
    }

    private fun errorOf(text: String, mode: AngleMode = AngleMode.DEGREES): CalcError {
        val result = ExpressionEngine.evaluate(text, mode)
        assertTrue("'$text' debía fallar pero dio $result", result is EvalResult.Failure)
        return (result as EvalResult.Failure).error
    }

    // ── Operaciones básicas ─────────────────────────────────────────────────

    @Test
    fun basicArithmetic() {
        assertEquals(5.0, eval("2+3"), 0.0)
        assertEquals(-1.0, eval("2−3"), 0.0)
        assertEquals(6.0, eval("2×3"), 0.0)
        assertEquals(2.5, eval("5÷2"), 0.0)
        assertEquals(6.0, eval("2*3"), 0.0)
        assertEquals(2.5, eval("5/2"), 0.0)
        assertEquals(1.0, eval("4-3"), 0.0)
    }

    @Test
    fun operatorPrecedence() {
        assertEquals(14.0, eval("2+3×4"), 0.0)
        assertEquals(10.0, eval("2×3+4"), 0.0)
        assertEquals(-1.0, eval("2−6÷2"), 0.0)
        assertEquals(1.0, eval("8÷4÷2"), 0.0)   // asociativa a la izquierda
        assertEquals(5.0, eval("10−3−2"), 0.0)
    }

    @Test
    fun parentheses() {
        assertEquals(20.0, eval("(2+3)×4"), 0.0)
        assertEquals(26.0, eval("2×(3+(4×2)+2)"), 0.0)
        assertEquals(7.0, eval("((7))"), 0.0)
    }

    @Test
    fun unclosedParenthesesAreClosedAutomatically() {
        assertEquals(20.0, eval("(2+3)×(4"), 0.0)
        assertEquals(0.5, eval("sin(30"), 1e-12)
    }

    @Test
    fun unaryMinusAndPower() {
        assertEquals(-4.0, eval("−2^2"), 0.0)       // -(2²)
        assertEquals(4.0, eval("(−2)^2"), 0.0)
        assertEquals(-6.0, eval("2×−3"), 0.0)
        assertEquals(0.125, eval("2^−3"), 0.0)
        assertEquals(512.0, eval("2^3^2"), 0.0)     // 2^(3^2), asociativa a la derecha
        assertEquals(5.0, eval("−(−5)"), 0.0)
        assertEquals(3.0, eval("+3"), 0.0)
    }

    @Test
    fun decimalsAndScientificNotation() {
        assertEquals(0.5, eval(".5"), 0.0)
        assertEquals(5.0, eval("5."), 0.0)
        assertEquals(1500.0, eval("1.5E3"), 0.0)
        assertEquals(0.00001, eval("1E-5"), 1e-18)
        assertEquals(0.00001, eval("1E−5"), 1e-18)   // con el menos tipográfico
    }

    // ── Trigonometría ───────────────────────────────────────────────────────

    @Test
    fun trigonometryInDegrees() {
        assertEquals(0.0, eval("sin(0)"), 0.0)
        assertEquals(0.5, eval("sin(30)"), 1e-12)
        assertEquals(1.0, eval("sin(90)"), 1e-12)
        assertEquals(0.0, eval("sin(180)"), 0.0)       // exacto, no 1.2E-16
        assertEquals(0.0, eval("sin(360)"), 0.0)
        assertEquals(-1.0, eval("sin(270)"), 1e-12)
        assertEquals(1.0, eval("cos(0)"), 0.0)
        assertEquals(0.5, eval("cos(60)"), 1e-12)
        assertEquals(0.0, eval("cos(90)"), 0.0)        // exacto
        assertEquals(-1.0, eval("cos(180)"), 1e-12)
        assertEquals(1.0, eval("tan(45)"), 1e-12)
        assertEquals(0.0, eval("tan(180)"), 0.0)
        assertEquals(-1.0, eval("tan(135)"), 1e-12)
    }

    @Test
    fun trigonometryInRadians() {
        val r = AngleMode.RADIANS
        assertEquals(0.0, eval("sin(π)", r), 0.0)
        assertEquals(1.0, eval("sin(π÷2)", r), 1e-12)
        assertEquals(-1.0, eval("cos(π)", r), 1e-12)
        assertEquals(0.0, eval("cos(π÷2)", r), 0.0)
        assertEquals(1.0, eval("tan(π÷4)", r), 1e-12)
        assertEquals(Math.sin(1.0), eval("sin(1)", r), 1e-15)
        assertEquals(Math.sin(2 * Math.PI + 1e-3), eval("sin(2π+0.001)", r), 1e-12)
    }

    @Test
    fun tangentOfRightAngleIsUndefined() {
        assertEquals(CalcError.DOMAIN, errorOf("tan(90)"))
        assertEquals(CalcError.DOMAIN, errorOf("tan(270)"))
        assertEquals(CalcError.DOMAIN, errorOf("tan(π÷2)", AngleMode.RADIANS))
    }

    @Test
    fun spanishAndEnglishFunctionNames() {
        assertEquals(eval("sin(30)"), eval("sen(30)"), 0.0)
        assertEquals(eval("sin(30)"), eval("SEN(30)"), 0.0)
    }

    @Test
    fun otherFunctions() {
        assertEquals(3.0, eval("√(9)"), 0.0)
        assertEquals(3.0, eval("√9"), 0.0)
        assertEquals(3.0, eval("sqrt(9)"), 0.0)
        assertEquals(2.0, eval("log(100)"), 1e-12)
        assertEquals(1.0, eval("ln(e)"), 1e-12)
        assertEquals(5.0, eval("abs(−5)"), 0.0)
        assertEquals(Math.PI, eval("π"), 0.0)
        assertEquals(Math.E, eval("e"), 0.0)
    }

    // ── Multiplicación implícita y porcentaje ───────────────────────────────

    @Test
    fun implicitMultiplication() {
        assertEquals(2 * Math.PI, eval("2π"), 1e-12)
        assertEquals(8.0, eval("2(4)"), 0.0)
        assertEquals(21.0, eval("(1+2)(3+4)"), 0.0)
        assertEquals(1.0, eval("2sin(30)"), 1e-12)
        assertEquals(6.0, eval("2√9"), 0.0)
    }

    @Test
    fun percentage() {
        assertEquals(0.5, eval("50%"), 0.0)
        assertEquals(20.0, eval("200×10%"), 1e-12)
        assertEquals(220.0, eval("200+10%"), 1e-12)   // relativo al operando izquierdo
        assertEquals(180.0, eval("200−10%"), 1e-12)
        assertEquals(1.5, eval("1+50%"), 1e-12)
    }

    // ── Errores ─────────────────────────────────────────────────────────────

    @Test
    fun divisionByZero() {
        assertEquals(CalcError.DIVISION_BY_ZERO, errorOf("1÷0"))
        assertEquals(CalcError.DIVISION_BY_ZERO, errorOf("5÷(3−3)"))
        assertEquals(CalcError.DIVISION_BY_ZERO, errorOf("0^−1"))
    }

    @Test
    fun domainErrors() {
        assertEquals(CalcError.DOMAIN, errorOf("√(−4)"))
        assertEquals(CalcError.DOMAIN, errorOf("ln(0)"))
        assertEquals(CalcError.DOMAIN, errorOf("log(−1)"))
        assertEquals(CalcError.DOMAIN, errorOf("(−8)^0.5"))
    }

    @Test
    fun overflow() {
        assertEquals(CalcError.OVERFLOW, errorOf("10^400"))
        assertEquals(CalcError.OVERFLOW, errorOf("10^200×10^200"))
    }

    @Test
    fun syntaxErrors() {
        listOf("", "   ", "2+", "×3", "2++", "(", ")", "2)", "()", "1.2.3", ".", "2 3", "sin", "sin30",
            "foo", "2x", "3@4", "1..2").forEach {
            assertEquals("'$it'", CalcError.SYNTAX, errorOf(it))
        }
    }

    // ── Expresiones con variable (graficador) ───────────────────────────────

    @Test
    fun variableExpressions() {
        val f = ExpressionEngine.compile("x^2−4", allowVariable = true)
        assertEquals(-4.0, f.eval(0.0), 0.0)
        assertEquals(0.0, f.eval(2.0), 0.0)
        assertEquals(5.0, f.eval(3.0), 0.0)

        val g = ExpressionEngine.compile("2x+1", allowVariable = true)   // multiplicación implícita con x
        assertEquals(7.0, g.eval(3.0), 0.0)

        val h = ExpressionEngine.compile("sin(x)", allowVariable = true)
        assertEquals(1.0, h.eval(Math.PI / 2, AngleMode.RADIANS), 1e-12)
        assertEquals(1.0, h.eval(90.0, AngleMode.DEGREES), 1e-12)
    }

    @Test
    fun variableIsRejectedWhenNotAllowed() {
        assertEquals(CalcError.SYNTAX, errorOf("x+1"))
    }

    @Test
    fun evalOrNaNNeverThrows() {
        val f = ExpressionEngine.compile("1÷x", allowVariable = true)
        assertTrue(f.evalOrNaN(0.0, AngleMode.RADIANS).isNaN())
        assertEquals(0.5, f.evalOrNaN(2.0, AngleMode.RADIANS), 0.0)

        val g = ExpressionEngine.compile("√x", allowVariable = true)
        assertTrue(g.evalOrNaN(-1.0, AngleMode.RADIANS).isNaN())
    }
}
