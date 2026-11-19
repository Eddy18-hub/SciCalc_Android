package com.example.tiptime.engine.tools

import java.math.BigInteger

/** Sistemas de numeración soportados por el conversor. */
enum class NumberBase(val radix: Int, val label: String) {
    BINARY(2, "BIN"),
    OCTAL(8, "OCT"),
    DECIMAL(10, "DEC"),
    HEXADECIMAL(16, "HEX"),
}

/**
 * Conversión entre binario, octal, decimal y hexadecimal para enteros de cualquier tamaño
 * (usa [BigInteger], así que no hay desbordamiento). Kotlin puro.
 */
object BaseConverter {

    /** Máximo de caracteres aceptados; evita entradas absurdamente largas. */
    const val MAX_DIGITS = 64

    /** ¿Es [c] un dígito válido en [base]? (las letras de hex se aceptan en mayúscula o minúscula) */
    fun isDigit(c: Char, base: NumberBase): Boolean = Character.digit(c, base.radix) >= 0

    /** Interpreta [text] en [base]. Acepta un "-" inicial y espacios; devuelve `null` si no es válido. */
    fun parse(text: String, base: NumberBase): BigInteger? {
        val clean = text.filterNot { it.isWhitespace() || it == '_' }
        if (clean.isEmpty() || clean.length > MAX_DIGITS + 1) return null
        val body = clean.removePrefix("-")
        if (body.isEmpty() || body.length > MAX_DIGITS || !body.all { isDigit(it, base) }) return null
        return BigInteger(clean, base.radix)
    }

    /** Escribe [value] en [base] (letras de hex en mayúscula). */
    fun format(value: BigInteger, base: NumberBase): String = value.toString(base.radix).uppercase()

    /** Convierte [text] de [from] a [to]; `null` si [text] no es válido en [from]. */
    fun convert(text: String, from: NumberBase, to: NumberBase): String? =
        parse(text, from)?.let { format(it, to) }

    /** Convierte a las cuatro bases a la vez (para mostrar todas juntas). `null` si la entrada no es válida. */
    fun convertAll(text: String, from: NumberBase): Map<NumberBase, String>? {
        val value = parse(text, from) ?: return null
        return NumberBase.entries.associateWith { format(value, it) }
    }
}
