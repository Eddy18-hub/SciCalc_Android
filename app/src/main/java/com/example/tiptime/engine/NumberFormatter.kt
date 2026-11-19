package com.example.tiptime.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Convierte un [Double] en el texto que ve el usuario.
 *
 * - Redondea a 12 cifras significativas, lo que elimina el ruido binario
 *   (0.1 + 0.2 = 0.30000000000000004 se muestra como 0.3).
 * - Usa notación científica ("1.2345E15", "1E-8") fuera del rango legible.
 * - Siempre usa punto decimal, de modo que el resultado puede volver a introducirse
 *   en una expresión sin ambigüedad (el motor lo interpreta igual).
 */
object NumberFormatter {

    private const val SIGNIFICANT_DIGITS = 12
    private const val MAX_PLAIN_EXPONENT = 11   // hasta 999 999 999 999
    private const val MIN_PLAIN_EXPONENT = -7   // hasta 0.0000001

    private val context = MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_EVEN)

    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        if (value == 0.0) return "0" // también cubre -0.0

        val rounded = BigDecimal(value).round(context).stripTrailingZeros()
        // Exponente de la primera cifra significativa: 1234.5 → 3,  0.0123 → -2
        val exponent = rounded.precision() - rounded.scale() - 1

        if (exponent in MIN_PLAIN_EXPONENT..MAX_PLAIN_EXPONENT) {
            return rounded.toPlainString()
        }

        val digits = rounded.unscaledValue().abs().toString()
        val mantissa = if (digits.length == 1) digits else "${digits[0]}.${digits.substring(1)}"
        val sign = if (rounded.signum() < 0) "-" else ""
        return "$sign${mantissa}E$exponent"
    }

    /** Formato para ejes de gráficas: igual que [format] pero evita "-0". */
    fun formatAxis(value: Double): String = format(if (abs(value) < 1e-12) 0.0 else value)
}
