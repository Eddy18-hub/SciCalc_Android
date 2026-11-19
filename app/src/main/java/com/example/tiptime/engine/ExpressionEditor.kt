package com.example.tiptime.engine

/**
 * Reglas de edición del texto de la expresión (Kotlin puro, fácil de probar).
 *
 * Cada función recibe la expresión actual y devuelve la nueva; si la pulsación no tiene
 * sentido en ese contexto (p. ej. dos puntos decimales en un número) devuelve la misma
 * expresión sin cambios.
 */
object ExpressionEditor {

    const val MINUS = '−'
    const val TIMES = '×'
    const val DIVIDE = '÷'
    const val PLUS = '+'
    const val POWER = '^'

    private const val BINARY_OPERATORS = "+−×÷^"
    private const val MAX_LENGTH = 200

    /** Funciones que se insertan como un bloque ("sin(") y se borran de una sola vez. */
    private val FUNCTION_TOKENS = listOf(
        "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(",
        "sin(", "sen(", "cos(", "tan(", "log(", "ln(", "√(", "abs(",
    )

    fun appendDigit(expr: String, digit: Char): String {
        if (expr.length >= MAX_LENGTH) return expr
        val number = trailingNumber(expr)
        // "0" seguido de otro dígito reemplaza al cero ("05" → "5"), pero "0.5" se respeta.
        if (number == "0") return expr.dropLast(1) + digit
        return expr + digit
    }

    fun appendDecimal(expr: String): String {
        if (expr.length >= MAX_LENGTH) return expr
        val number = trailingNumber(expr)
        if (number.contains('.')) return expr
        return if (number.isEmpty()) "${expr}0." else "$expr."
    }

    fun appendOperator(expr: String, op: Char): String {
        if (expr.isEmpty()) return if (op == MINUS) "$MINUS" else expr
        if (expr.length >= MAX_LENGTH && !expr.last().isBinaryOperator()) return expr

        val last = expr.last()
        return when {
            // Dos operadores seguidos: el nuevo reemplaza al anterior…
            last.isBinaryOperator() -> {
                // …salvo "×−" / "÷−" / "^−", que es un signo negativo legítimo.
                if (op == MINUS && last in "$TIMES$DIVIDE$POWER") {
                    expr + op
                } else {
                    val stripped = expr.trimEnd { it.isBinaryOperator() }
                    when {
                        stripped.isEmpty() -> if (op == MINUS) "$MINUS" else ""
                        stripped.last() == '(' -> if (op == MINUS) stripped + op else stripped
                        else -> stripped + op
                    }
                }
            }
            // Tras "(" solo tiene sentido el signo menos.
            last == '(' -> if (op == MINUS) expr + op else expr
            else -> expr + op
        }
    }

    fun appendPercent(expr: String): String =
        if (endsWithOperand(expr, allowPercent = true)) expr + '%' else expr

    /** n!  — igual que el porcentaje, solo tiene sentido tras un operando. */
    fun appendFactorial(expr: String): String =
        if (endsWithOperand(expr, allowPercent = false)) expr + '!' else expr

    /** ¿Termina la expresión en algo a lo que se le puede aplicar % o ! (número, ")", π, e, Ans)? */
    private fun endsWithOperand(expr: String, allowPercent: Boolean): Boolean {
        if (expr.isEmpty() || expr.length >= MAX_LENGTH) return false
        val last = expr.last()
        return last.isDigit() || last == ')' || last == 'π' || last == 'e' ||
            expr.endsWith("Ans") || last == '!' || (allowPercent && last == '%')
    }

    fun appendOpenParenthesis(expr: String): String =
        if (expr.length >= MAX_LENGTH) expr else "$expr("

    fun appendCloseParenthesis(expr: String): String {
        if (openParentheses(expr) <= 0) return expr
        val last = expr.lastOrNull() ?: return expr
        return if (last.isBinaryOperator() || last == '(') expr else "$expr)"
    }

    fun appendConstant(expr: String, constant: String): String =
        if (expr.length >= MAX_LENGTH) expr else expr + constant

    /** Inserta una función; [token] debe incluir el paréntesis de apertura, p. ej. "sin(". */
    fun appendFunction(expr: String, token: String): String =
        if (expr.length >= MAX_LENGTH) expr else expr + token

    /** Borra el último carácter, o la función completa si la expresión termina en una ("sin(" → ""). */
    fun backspace(expr: String): String {
        if (expr.isEmpty()) return expr
        val function = FUNCTION_TOKENS.firstOrNull { expr.endsWith(it) }
        return expr.dropLast(function?.length ?: 1)
    }

    /** Paréntesis abiertos menos cerrados. */
    fun openParentheses(expr: String): Int = expr.count { it == '(' } - expr.count { it == ')' }

    /** Número que está siendo escrito al final de la expresión ("2+3.5" → "3.5"). */
    private fun trailingNumber(expr: String): String = expr.takeLastWhile { it.isDigit() || it == '.' }

    private fun Char.isBinaryOperator() = this in BINARY_OPERATORS
}
