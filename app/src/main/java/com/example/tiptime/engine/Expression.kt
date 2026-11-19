package com.example.tiptime.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cosh
import kotlin.math.sinh
import kotlin.math.tanh
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Motor de expresiones matemáticas (Kotlin puro, sin dependencias de Android).
 *
 * Pipeline:   texto  ──tokenize──▶  tokens  ──parse──▶  árbol (Node)  ──eval──▶  Double
 *
 * Gramática (de menor a mayor precedencia):
 *
 *   expr    := term (('+' | '-') term)*
 *   term    := factor (('*' | '/') factor | <multiplicación implícita> factor)*
 *   factor  := ('-' | '+') factor | power
 *   power   := postfix ('^' factor)?            // asociativa a la derecha: 2^3^2 = 2^(3^2)
 *   postfix := primary ('%' | '!')*
 *   primary := número | π | e | x | Ans | '(' expr ')' | función '(' expr ')' | '√' primary
 *
 * Detalles de diseño:
 *  - El menos unario tiene MENOR precedencia que '^'  →  -2^2 = -4  (convención matemática).
 *  - Multiplicación implícita: 2π, 2(3+1), 3sen(30), (1+2)(3+4).
 *  - Un porcentaje a la derecha de + o - es relativo al operando izquierdo (como en las
 *    calculadoras de bolsillo):  200 + 10% = 220.  En cualquier otro contexto vale /100.
 *  - Paréntesis sin cerrar al final se cierran solos (permite vista previa en vivo).
 */

/** Unidad usada por sen, cos y tan. */
enum class AngleMode { DEGREES, RADIANS }

/** Causas de error que la interfaz traduce a mensajes localizados. */
enum class CalcError { SYNTAX, DIVISION_BY_ZERO, DOMAIN, OVERFLOW }

/** Excepción interna del motor. No captura la traza de pila: es barata de lanzar (útil al graficar). */
class CalcException(val error: CalcError) : RuntimeException(error.name) {
    override fun fillInStackTrace(): Throwable = this
}

/** Resultado de evaluar una expresión sin lanzar excepciones. */
sealed interface EvalResult {
    data class Success(val value: Double) : EvalResult
    data class Failure(val error: CalcError) : EvalResult
}

/** Expresión ya analizada; puede evaluarse muchas veces (p. ej. una vez por columna al graficar). */
class CompiledExpression internal constructor(private val root: Node) {

    /** Evalúa la expresión. Lanza [CalcException] si el resultado no es un número real finito. */
    fun eval(x: Double = 0.0, angleMode: AngleMode = AngleMode.RADIANS, ans: Double = 0.0): Double =
        root.eval(Env(x, angleMode, ans))

    /** Igual que [eval] pero devuelve NaN en lugar de lanzar excepción (para graficar). */
    fun evalOrNaN(x: Double, angleMode: AngleMode, ans: Double = 0.0): Double =
        try {
            eval(x, angleMode, ans)
        } catch (e: CalcException) {
            Double.NaN
        }
}

object ExpressionEngine {

    /**
     * Analiza [text] y devuelve una expresión evaluable.
     *
     * @param allowVariable si es `true` se reconoce la variable `x` (para graficar).
     * @param autoCloseParens si es `true` los paréntesis abiertos al final se cierran automáticamente.
     * @throws CalcException con [CalcError.SYNTAX] si el texto no es una expresión válida.
     */
    fun compile(
        text: String,
        allowVariable: Boolean = false,
        autoCloseParens: Boolean = true,
    ): CompiledExpression {
        val tokens = Tokenizer(text, allowVariable).tokenize()
        return CompiledExpression(Parser(tokens, autoCloseParens).parse())
    }

    /** Analiza y evalúa en un solo paso, sin lanzar excepciones. */
    fun evaluate(
        text: String,
        angleMode: AngleMode = AngleMode.DEGREES,
        autoCloseParens: Boolean = true,
        ans: Double = 0.0,
    ): EvalResult =
        try {
            EvalResult.Success(compile(text, autoCloseParens = autoCloseParens).eval(0.0, angleMode, ans))
        } catch (e: CalcException) {
            EvalResult.Failure(e.error)
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tokenizador
// ─────────────────────────────────────────────────────────────────────────────

internal enum class Fn { SIN, COS, TAN, ASIN, ACOS, ATAN, SINH, COSH, TANH, SQRT, LN, LOG, ABS }

private enum class TokenType {
    NUMBER, PLUS, MINUS, STAR, SLASH, CARET, PERCENT, BANG, LPAREN, RPAREN, SQRT_SIGN, PI, E, X, ANS, FUNCTION,
}

private class Token(val type: TokenType, val number: Double = 0.0, val fn: Fn? = null)

private class Tokenizer(private val src: String, private val allowVariable: Boolean) {

    private var pos = 0

    /** Nombres reconocidos, de más largo a más corto para que "sqrt" gane a "s…" y "sen" a "e". */
    private val names: List<Pair<String, Token>> = buildList {
        add("sqrt" to Token(TokenType.FUNCTION, fn = Fn.SQRT))
        add("asin" to Token(TokenType.FUNCTION, fn = Fn.ASIN))
        add("asen" to Token(TokenType.FUNCTION, fn = Fn.ASIN))
        add("acos" to Token(TokenType.FUNCTION, fn = Fn.ACOS))
        add("atan" to Token(TokenType.FUNCTION, fn = Fn.ATAN))
        add("sinh" to Token(TokenType.FUNCTION, fn = Fn.SINH))
        add("senh" to Token(TokenType.FUNCTION, fn = Fn.SINH))
        add("cosh" to Token(TokenType.FUNCTION, fn = Fn.COSH))
        add("tanh" to Token(TokenType.FUNCTION, fn = Fn.TANH))
        add("ans" to Token(TokenType.ANS))
        add("sin" to Token(TokenType.FUNCTION, fn = Fn.SIN))
        add("sen" to Token(TokenType.FUNCTION, fn = Fn.SIN)) // español / italiano
        add("cos" to Token(TokenType.FUNCTION, fn = Fn.COS))
        add("tan" to Token(TokenType.FUNCTION, fn = Fn.TAN))
        add("log" to Token(TokenType.FUNCTION, fn = Fn.LOG))
        add("abs" to Token(TokenType.FUNCTION, fn = Fn.ABS))
        add("ln" to Token(TokenType.FUNCTION, fn = Fn.LN))
        add("pi" to Token(TokenType.PI))
        add("e" to Token(TokenType.E))
        if (allowVariable) add("x" to Token(TokenType.X))
    }.sortedByDescending { it.first.length }

    fun tokenize(): List<Token> {
        val out = ArrayList<Token>()
        while (pos < src.length) {
            val c = src[pos]
            when {
                c.isWhitespace() -> pos++
                c.isDigit() || c == '.' -> out += readNumber()
                else -> out += readSymbolOrName(c)
            }
        }
        return out
    }

    private fun readNumber(): Token {
        val start = pos
        var digits = 0
        while (pos < src.length && src[pos].isDigit()) { pos++; digits++ }
        if (pos < src.length && src[pos] == '.') {
            pos++
            while (pos < src.length && src[pos].isDigit()) { pos++; digits++ }
        }
        if (digits == 0) throw CalcException(CalcError.SYNTAX) // un "." suelto

        // Notación científica (1.5E-3). Solo si realmente le siguen dígitos, para no
        // confundirla con la constante e (por ejemplo "2e" = 2·e).
        if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
            var look = pos + 1
            if (look < src.length && (src[look] == '+' || src[look] == '-' || src[look] == '−')) look++
            if (look < src.length && src[look].isDigit()) {
                pos = look
                while (pos < src.length && src[pos].isDigit()) pos++
            }
        }
        // "1.2.3" no es válido (evita que se lea como 1.2 × 0.3)
        if (pos < src.length && src[pos] == '.') throw CalcException(CalcError.SYNTAX)

        val value = src.substring(start, pos).replace('−', '-').toDoubleOrNull()
            ?: throw CalcException(CalcError.SYNTAX)
        return Token(TokenType.NUMBER, number = value)
    }

    private fun readSymbolOrName(c: Char): Token {
        val simple = when (c) {
            '+' -> TokenType.PLUS
            '-', '−', '–' -> TokenType.MINUS
            '*', '×', '·' -> TokenType.STAR
            '/', '÷' -> TokenType.SLASH
            '^' -> TokenType.CARET
            '%' -> TokenType.PERCENT
            '!' -> TokenType.BANG
            '(' -> TokenType.LPAREN
            ')' -> TokenType.RPAREN
            '√' -> TokenType.SQRT_SIGN
            'π' -> TokenType.PI
            else -> null
        }
        if (simple != null) {
            pos++
            return Token(simple)
        }
        for ((name, token) in names) {
            if (src.regionMatches(pos, name, 0, name.length, ignoreCase = true)) {
                pos += name.length
                return token
            }
        }
        throw CalcException(CalcError.SYNTAX)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Árbol de sintaxis y evaluación
// ─────────────────────────────────────────────────────────────────────────────

internal class Env(val x: Double, val angleMode: AngleMode, val ans: Double = 0.0)

internal sealed interface Node {
    fun eval(env: Env): Double
}

private class NumberNode(val value: Double) : Node {
    override fun eval(env: Env) = value
}

private object VariableNode : Node {
    override fun eval(env: Env) = env.x
}

/** "Ans": el resultado anterior. */
private object AnswerNode : Node {
    override fun eval(env: Env) = env.ans
}

/** n! — solo enteros no negativos; 171! ya no cabe en un Double. */
private class FactorialNode(val operand: Node) : Node {
    override fun eval(env: Env): Double {
        val v = operand.eval(env)
        if (v < 0.0 || v != Math.floor(v)) throw CalcException(CalcError.DOMAIN)
        if (v > 170.0) throw CalcException(CalcError.OVERFLOW)
        var result = 1.0
        for (i in 2..v.toInt()) result *= i
        return result
    }
}

private class NegateNode(val operand: Node) : Node {
    override fun eval(env: Env) = -operand.eval(env)
}

private class PercentNode(val operand: Node) : Node {
    override fun eval(env: Env) = operand.eval(env) / 100.0
}

private class BinaryNode(val op: Char, val left: Node, val right: Node) : Node {
    override fun eval(env: Env): Double {
        val a = left.eval(env)
        val b = right.eval(env)
        val result = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw CalcException(CalcError.DIVISION_BY_ZERO)
                a / b
            }
            '^' -> {
                if (a == 0.0 && b < 0.0) throw CalcException(CalcError.DIVISION_BY_ZERO)
                a.pow(b)
            }
            else -> throw IllegalStateException("Operador desconocido: $op")
        }
        return checked(result)
    }
}

private class FunctionNode(val fn: Fn, val argument: Node) : Node {
    override fun eval(env: Env): Double {
        val v = argument.eval(env)
        val result = when (fn) {
            Fn.SIN -> sinOf(v, env.angleMode)
            Fn.COS -> cosOf(v, env.angleMode)
            Fn.TAN -> tanOf(v, env.angleMode)
            Fn.SQRT -> {
                if (v < 0.0) throw CalcException(CalcError.DOMAIN)
                sqrt(v)
            }
            Fn.LN -> {
                if (v <= 0.0) throw CalcException(CalcError.DOMAIN)
                ln(v)
            }
            Fn.LOG -> {
                if (v <= 0.0) throw CalcException(CalcError.DOMAIN)
                log10(v)
            }
            Fn.ABS -> abs(v)
            Fn.ASIN -> {
                if (v < -1.0 || v > 1.0) throw CalcException(CalcError.DOMAIN)
                fromRadians(asin(v), env.angleMode)
            }
            Fn.ACOS -> {
                if (v < -1.0 || v > 1.0) throw CalcException(CalcError.DOMAIN)
                fromRadians(acos(v), env.angleMode)
            }
            Fn.ATAN -> fromRadians(atan(v), env.angleMode)
            Fn.SINH -> sinh(v)
            Fn.COSH -> cosh(v)
            Fn.TANH -> tanh(v)
        }
        return checked(result)
    }
}

/** NaN → fuera de dominio, ±∞ → desbordamiento. */
private fun checked(v: Double): Double {
    if (v.isNaN()) throw CalcException(CalcError.DOMAIN)
    if (v.isInfinite()) throw CalcException(CalcError.OVERFLOW)
    return v
}

// ── Trigonometría ────────────────────────────────────────────────────────────
// Los valores "redondos" (sen 180°, cos 90°, sen π…) se ajustan a 0 exacto; sin esto
// aparecería 1.22E-16 en lugar de 0. Se trabaja siempre en grados para decidir el ajuste.

private const val SNAP_EPSILON = 1e-10

private fun toDegrees(v: Double, mode: AngleMode): Double =
    if (mode == AngleMode.DEGREES) v else Math.toDegrees(v)

private fun toRadians(v: Double, mode: AngleMode): Double =
    if (mode == AngleMode.RADIANS) v else Math.toRadians(v)

/** Resultado de una función trigonométrica inversa, en la unidad activa. Ajusta ruido (acos(0.5) = 60 exacto). */
private fun fromRadians(v: Double, mode: AngleMode): Double {
    if (mode == AngleMode.RADIANS) return v
    val degrees = Math.toDegrees(v)
    val rounded = round(degrees * 1e9) / 1e9
    return if (abs(degrees - rounded) < 1e-9) rounded else degrees
}

/** ¿[m] está (casi) en un entero? */
private fun nearInteger(m: Double): Boolean = abs(m - round(m)) <= SNAP_EPSILON * max(1.0, abs(m))

private fun sinOf(v: Double, mode: AngleMode): Double {
    val m = toDegrees(v, mode) / 180.0           // múltiplos de 180° → 0
    if (round(m) != 0.0 && nearInteger(m)) return 0.0
    return sin(toRadians(v, mode))
}

private fun cosOf(v: Double, mode: AngleMode): Double {
    val m = (toDegrees(v, mode) - 90.0) / 180.0  // 90° + k·180° → 0
    if (nearInteger(m)) return 0.0
    return cos(toRadians(v, mode))
}

private fun tanOf(v: Double, mode: AngleMode): Double {
    val m = (toDegrees(v, mode) - 90.0) / 180.0
    if (nearInteger(m)) throw CalcException(CalcError.DOMAIN) // tan(90°) no está definida
    return sinOf(v, mode) / cosOf(v, mode)
}

// ─────────────────────────────────────────────────────────────────────────────
// Parser de descenso recursivo
// ─────────────────────────────────────────────────────────────────────────────

private class Parser(private val tokens: List<Token>, private val autoCloseParens: Boolean) {

    private var pos = 0

    fun parse(): Node {
        if (tokens.isEmpty()) throw CalcException(CalcError.SYNTAX)
        val node = parseExpression()
        if (pos != tokens.size) throw CalcException(CalcError.SYNTAX) // sobran tokens, p. ej. "2)"
        return node
    }

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun parseExpression(): Node {
        var left = parseTerm()
        while (true) {
            val op = when (peek()?.type) {
                TokenType.PLUS -> '+'
                TokenType.MINUS -> '-'
                else -> return left
            }
            pos++
            val right = parseTerm()
            // "200 + 10%" → 200 + (200 × 10%): el porcentaje es relativo al operando izquierdo.
            left = if (right is PercentNode) {
                BinaryNode(op, left, BinaryNode('*', left, right))
            } else {
                BinaryNode(op, left, right)
            }
        }
    }

    private fun parseTerm(): Node {
        var left = parseFactor()
        while (true) {
            val type = peek()?.type
            left = when {
                type == TokenType.STAR -> { pos++; BinaryNode('*', left, parseFactor()) }
                type == TokenType.SLASH -> { pos++; BinaryNode('/', left, parseFactor()) }
                type != null && startsImplicitFactor(type) -> BinaryNode('*', left, parseFactor())
                else -> return left
            }
        }
    }

    /** Tokens que, tras un operando, indican multiplicación implícita (2π, 2(3), 3sen(30)). */
    private fun startsImplicitFactor(type: TokenType) = when (type) {
        TokenType.LPAREN, TokenType.FUNCTION, TokenType.SQRT_SIGN,
        TokenType.PI, TokenType.E, TokenType.X, TokenType.ANS -> true
        else -> false
    }

    private fun parseFactor(): Node =
        when (peek()?.type) {
            TokenType.MINUS -> { pos++; NegateNode(parseFactor()) }
            TokenType.PLUS -> { pos++; parseFactor() }
            else -> parsePower()
        }

    private fun parsePower(): Node {
        val base = parsePostfix()
        if (peek()?.type == TokenType.CARET) {
            pos++
            return BinaryNode('^', base, parseFactor()) // el exponente admite signo y es asociativo a la derecha
        }
        return base
    }

    private fun parsePostfix(): Node {
        var node = parsePrimary()
        while (true) {
            node = when (peek()?.type) {
                TokenType.PERCENT -> PercentNode(node)
                TokenType.BANG -> FactorialNode(node)
                else -> return node
            }
            pos++
        }
    }

    private fun parsePrimary(): Node {
        val token = peek() ?: throw CalcException(CalcError.SYNTAX)
        pos++
        return when (token.type) {
            TokenType.NUMBER -> NumberNode(token.number)
            TokenType.PI -> NumberNode(PI)
            TokenType.E -> NumberNode(Math.E)
            TokenType.X -> VariableNode
            TokenType.ANS -> AnswerNode
            TokenType.LPAREN -> parseGroup()
            TokenType.FUNCTION -> {
                if (peek()?.type != TokenType.LPAREN) throw CalcException(CalcError.SYNTAX)
                pos++
                FunctionNode(token.fn!!, parseGroup())
            }
            TokenType.SQRT_SIGN -> FunctionNode(Fn.SQRT, parsePrimary())
            else -> throw CalcException(CalcError.SYNTAX)
        }
    }

    /** Lee `expr ')'` (el paréntesis de apertura ya fue consumido). */
    private fun parseGroup(): Node {
        val inner = parseExpression()
        when {
            peek()?.type == TokenType.RPAREN -> pos++
            peek() == null && autoCloseParens -> Unit
            else -> throw CalcException(CalcError.SYNTAX)
        }
        return inner
    }
}
