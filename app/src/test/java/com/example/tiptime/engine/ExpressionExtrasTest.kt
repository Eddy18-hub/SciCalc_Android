package com.example.tiptime.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Funciones inversas, hiperbólicas, factorial y Ans. */
class ExpressionExtrasTest {

    private fun eval(text: String, mode: AngleMode = AngleMode.DEGREES, ans: Double = 0.0): Double {
        val result = ExpressionEngine.evaluate(text, mode, ans = ans)
        assertTrue("'$text' debía evaluarse pero dio $result", result is EvalResult.Success)
        return (result as EvalResult.Success).value
    }

    private fun errorOf(text: String, mode: AngleMode = AngleMode.DEGREES): CalcError {
        val result = ExpressionEngine.evaluate(text, mode)
        assertTrue("'$text' debía fallar pero dio $result", result is EvalResult.Failure)
        return (result as EvalResult.Failure).error
    }

    @Test
    fun inverseTrigInDegrees() {
        assertEquals(30.0, eval("asin(0.5)"), 0.0)
        assertEquals(60.0, eval("acos(0.5)"), 0.0)
        assertEquals(45.0, eval("atan(1)"), 0.0)
        assertEquals(90.0, eval("asin(1)"), 0.0)
        assertEquals(0.0, eval("acos(1)"), 0.0)
    }

    @Test
    fun inverseTrigInRadians() {
        assertEquals(Math.PI / 6, eval("asin(0.5)", AngleMode.RADIANS), 1e-12)
        assertEquals(Math.PI / 4, eval("atan(1)", AngleMode.RADIANS), 1e-12)
    }

    @Test
    fun inverseTrigDomain() {
        assertEquals(CalcError.DOMAIN, errorOf("asin(2)"))
        assertEquals(CalcError.DOMAIN, errorOf("acos(−1.5)"))
    }

    @Test
    fun hyperbolicFunctions() {
        assertEquals(0.0, eval("sinh(0)"), 0.0)
        assertEquals(1.0, eval("cosh(0)"), 0.0)
        assertEquals(Math.sinh(1.0), eval("sinh(1)"), 1e-12)
        assertEquals(Math.tanh(0.5), eval("tanh(0.5)"), 1e-12)
        assertEquals(Math.cosh(2.0), eval("cosh(2)"), 1e-12) // no depende del modo angular
        assertEquals(Math.cosh(2.0), eval("cosh(2)", AngleMode.RADIANS), 1e-12)
    }

    @Test
    fun factorial() {
        assertEquals(1.0, eval("0!"), 0.0)
        assertEquals(120.0, eval("5!"), 0.0)
        assertEquals(720.0, eval("(2+4)!"), 0.0)
        assertEquals(6.0, eval("3!"), 0.0)
        assertEquals(7.0, eval("1+3!÷1−0"), 0.0) // el ! se aplica antes que + y ÷
        assertEquals(24.0, eval("2×3!×2"), 0.0) // 2·6·2
        assertEquals(1.0, eval("0!"), 0.0)
        assertEquals(3628800.0, eval("10!"), 0.0)
    }

    @Test
    fun factorialErrors() {
        assertEquals(CalcError.DOMAIN, errorOf("(−1)!"))
        assertEquals(CalcError.DOMAIN, errorOf("2.5!"))
        assertEquals(CalcError.OVERFLOW, errorOf("171!"))
        assertTrue(eval("170!") > 7e306)
    }

    @Test
    fun answerVariable() {
        assertEquals(10.0, eval("Ans+4", ans = 6.0), 0.0)
        assertEquals(12.0, eval("2Ans", ans = 6.0), 0.0)
        assertEquals(36.0, eval("Ans^2", ans = 6.0), 0.0)
        assertEquals(0.0, eval("Ans"), 0.0)
        assertEquals(-3.0, eval("ans−Ans×2", ans = 3.0), 0.0)
    }

    @Test
    fun existingFunctionsStillParse() {
        // "asin" no debe confundirse con "abs" o "sin", ni "sinh" con "sin"
        assertEquals(5.0, eval("abs(−5)"), 0.0)
        assertEquals(0.5, eval("sin(30)"), 1e-12)
        assertEquals(1.0, eval("sen(90)"), 1e-12)
    }
}

class ExpressionEditorExtrasTest {

    @Test
    fun factorialOnlyAfterOperand() {
        assertEquals("", ExpressionEditor.appendFactorial(""))
        assertEquals("5!", ExpressionEditor.appendFactorial("5"))
        assertEquals("(2+1)!", ExpressionEditor.appendFactorial("(2+1)"))
        assertEquals("2+", ExpressionEditor.appendFactorial("2+"))
        assertEquals("sin(", ExpressionEditor.appendFactorial("sin("))
        assertEquals("Ans!", ExpressionEditor.appendFactorial("Ans"))
        assertEquals("π!", ExpressionEditor.appendFactorial("π"))
    }

    @Test
    fun newFunctionsDeleteAsOneBlock() {
        assertEquals("2+", ExpressionEditor.backspace("2+asin("))
        assertEquals("", ExpressionEditor.backspace("tanh("))
        assertEquals("sinh(1", ExpressionEditor.backspace("sinh(12"))
    }

    @Test
    fun percentStillWorks() {
        assertEquals("50%", ExpressionEditor.appendPercent("50"))
        assertEquals("50%%", ExpressionEditor.appendPercent("50%")) // porcentaje encadenado, ya permitido
    }
}
