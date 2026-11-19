package com.example.tiptime.ui

/** Todo lo que el usuario puede pulsar en el teclado de la calculadora. */
sealed interface KeyAction {
    data class Digit(val digit: Char) : KeyAction
    data object Decimal : KeyAction
    data class Operator(val symbol: Char) : KeyAction
    data object Percent : KeyAction
    data object OpenParenthesis : KeyAction
    data object CloseParenthesis : KeyAction
    data class Constant(val symbol: String) : KeyAction

    /** [token] incluye el paréntesis de apertura, p. ej. "sin(". */
    data class MathFunction(val token: String) : KeyAction

    /** n! */
    data object Factorial : KeyAction

    /** Teclas de memoria (MC, MR, M+, M−). */
    data class Memory(val op: MemoryOp) : KeyAction

    data object Backspace : KeyAction
    data object Clear : KeyAction
    data object Equals : KeyAction
}

enum class MemoryOp { CLEAR, RECALL, ADD, SUBTRACT }

/**
 * Velocidad de reproducción del clic para cada tipo de tecla (ver SoundManager):
 * los operadores suenan más graves, "=" más agudo, y borrar el más grave de todos.
 */
fun KeyAction.soundRate(): Float = when (this) {
    is KeyAction.Digit, KeyAction.Decimal -> 1.0f
    is KeyAction.Operator -> 0.85f
    KeyAction.Equals -> 1.3f
    KeyAction.Clear -> 0.7f
    KeyAction.Backspace -> 0.75f
    else -> 0.95f
}
