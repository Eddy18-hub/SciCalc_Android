package com.example.tiptime.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.tiptime.data.CalculatorStore
import com.example.tiptime.data.HistoryEntry
import com.example.tiptime.engine.AngleMode
import com.example.tiptime.engine.CalcError
import com.example.tiptime.engine.EvalResult
import com.example.tiptime.engine.ExpressionEditor
import com.example.tiptime.engine.ExpressionEngine
import com.example.tiptime.engine.NumberFormatter

enum class Screen { CALCULATOR, GRAPH, GAME, TOOLS }

/**
 * Estado de la pantalla de la calculadora.
 *
 * @property expression texto que está escribiendo el usuario (con × ÷ − tal como se muestran).
 * @property preview resultado en vivo mientras se escribe (vacío si no aporta nada).
 * @property result resultado definitivo tras pulsar "=" (solo válido si [justEvaluated]).
 * @property error error de la última evaluación con "=", o `null`.
 * @property justEvaluated `true` entre pulsar "=" y la siguiente pulsación.
 */
data class CalculatorUiState(
    val expression: String = "",
    val preview: String = "",
    val result: String = "",
    val error: CalcError? = null,
    val justEvaluated: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEGREES,
    val history: List<HistoryEntry> = emptyList(),
    val historyOpen: Boolean = false,
    val screen: Screen = Screen.CALCULATOR,
    /** Último resultado (lo que representa "Ans"). */
    val answer: Double = 0.0,
    /** Valor guardado con M+ / M−, o `null` si la memoria está vacía. */
    val memory: Double? = null,
)

/** Preferencias de la app. [darkTheme] = `null` significa "seguir al sistema". */
data class AppSettings(
    val darkTheme: Boolean? = null,
    val soundEnabled: Boolean = true,
    val parallaxEnabled: Boolean = true,
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val store = CalculatorStore(application)

    var state by mutableStateOf(
        CalculatorUiState(angleMode = store.angleMode, history = store.loadHistory())
    )
        private set

    var settings by mutableStateOf(
        AppSettings(
            darkTheme = store.darkTheme,
            soundEnabled = store.soundEnabled,
            parallaxEnabled = store.parallaxEnabled,
        )
    )
        private set

    // ── Teclado ─────────────────────────────────────────────────────────────

    fun onKey(key: KeyAction) {
        val current = state
        if (key == KeyAction.Equals) {
            evaluate(current)
            return
        }
        if (key is KeyAction.Memory) {
            onMemory(key.op)
            return
        }

        val base = baseExpression(current, key)
        val next = when (key) {
            is KeyAction.Digit -> ExpressionEditor.appendDigit(base, key.digit)
            KeyAction.Decimal -> ExpressionEditor.appendDecimal(base)
            is KeyAction.Operator -> ExpressionEditor.appendOperator(base, key.symbol)
            KeyAction.Percent -> ExpressionEditor.appendPercent(base)
            KeyAction.Factorial -> ExpressionEditor.appendFactorial(base)
            KeyAction.OpenParenthesis -> ExpressionEditor.appendOpenParenthesis(base)
            KeyAction.CloseParenthesis -> ExpressionEditor.appendCloseParenthesis(base)
            is KeyAction.Constant -> ExpressionEditor.appendConstant(base, key.symbol)
            is KeyAction.MathFunction -> ExpressionEditor.appendFunction(base, key.token)
            KeyAction.Backspace -> ExpressionEditor.backspace(base)
            KeyAction.Clear -> ""
            KeyAction.Equals, is KeyAction.Memory -> base // ya tratados arriba
        }

        state = withPreview(
            current.copy(
                expression = next,
                result = "",
                error = null,
                justEvaluated = false,
                historyOpen = false, // escribir devuelve la vista a la expresión
            )
        )
    }

    /**
     * Texto sobre el que actúa la pulsación. Tras "=": un operador continúa con el resultado
     * (como "Ans" en una calculadora de bolsillo), borrar edita la expresión anterior y
     * cualquier otra tecla empieza una cuenta nueva.
     */
    private fun baseExpression(current: CalculatorUiState, key: KeyAction): String {
        if (!current.justEvaluated) return current.expression
        return when (key) {
            is KeyAction.Operator, KeyAction.Percent, KeyAction.Factorial -> current.result.asExpressionText()
            KeyAction.Backspace -> current.expression
            else -> ""
        }
    }

    private fun evaluate(current: CalculatorUiState) {
        if (current.expression.isBlank() || current.justEvaluated) return

        // Huevo de pascua: escribir 666 y pulsar "=" abre el minijuego (no se guarda en el historial).
        if (current.expression == EASTER_EGG_CODE) {
            state = current.copy(expression = "", preview = "", result = "", error = null, screen = Screen.GAME)
            return
        }

        when (val outcome = ExpressionEngine.evaluate(current.expression, current.angleMode, ans = current.answer)) {
            is EvalResult.Success -> {
                val text = NumberFormatter.format(outcome.value)
                // Un número suelto ("5" =) no aporta nada al historial.
                val isTrivial = text == current.expression.asPlainText()
                val history = if (isTrivial) {
                    current.history
                } else {
                    // Sin límite: el historial conserva todos los cálculos (lo más reciente primero).
                    listOf(HistoryEntry(current.expression, text, System.currentTimeMillis())) +
                        current.history
                }
                if (history !== current.history) store.saveHistory(history)
                state = current.copy(
                    preview = "",
                    result = text,
                    error = null,
                    justEvaluated = true,
                    history = history,
                    answer = outcome.value,
                )
            }
            is EvalResult.Failure -> {
                state = current.copy(preview = "", result = "", error = outcome.error, justEvaluated = false)
            }
        }
    }

    /** Resultado en vivo; no se muestra si es igual a lo escrito ni si la expresión aún es inválida. */
    private fun withPreview(s: CalculatorUiState): CalculatorUiState {
        if (s.expression.isEmpty()) return s.copy(preview = "")
        val preview = when (val outcome = ExpressionEngine.evaluate(s.expression, s.angleMode, ans = s.answer)) {
            is EvalResult.Success -> NumberFormatter.format(outcome.value)
                .takeIf { it != s.expression.asPlainText() }
                .orEmpty()
            is EvalResult.Failure -> ""
        }
        return s.copy(preview = preview)
    }

    fun toggleAngleMode() {
        val mode = if (state.angleMode == AngleMode.DEGREES) AngleMode.RADIANS else AngleMode.DEGREES
        store.angleMode = mode
        state = withPreview(state.copy(angleMode = mode))
    }

    // ── Historial ───────────────────────────────────────────────────────────

    fun toggleHistory() {
        state = state.copy(historyOpen = !state.historyOpen)
    }

    fun closeHistory() {
        if (state.historyOpen) state = state.copy(historyOpen = false)
    }

    /** Vuelve a cargar la expresión de una entrada para poder editarla. */
    fun recallExpression(entry: HistoryEntry) {
        state = withPreview(
            state.copy(
                expression = entry.expression,
                result = "",
                error = null,
                justEvaluated = false,
                historyOpen = false,
            )
        )
    }

    /** Inserta el resultado de una entrada en la expresión actual. */
    fun useHistoryResult(entry: HistoryEntry) = insertValue(entry.result.asExpressionText())

    /** Inserta [value] (ya con el signo "−" del teclado) en la expresión actual. */
    private fun insertValue(value: String) {
        val base = if (state.justEvaluated) "" else state.expression
        val last = base.lastOrNull()
        // Tras un operador o "(" el resultado se encadena; en otro caso reemplaza lo escrito.
        val next = if (last == null || last in "+−×÷^(") base + value else value
        state = withPreview(
            state.copy(
                expression = next,
                result = "",
                error = null,
                justEvaluated = false,
                historyOpen = false,
            )
        )
    }

    fun clearHistory() {
        store.saveHistory(emptyList())
        state = state.copy(history = emptyList())
    }

    // ── Memoria ─────────────────────────────────────────────────────────────

    private fun onMemory(op: MemoryOp) {
        val current = state
        when (op) {
            MemoryOp.CLEAR -> state = current.copy(memory = null)
            MemoryOp.RECALL -> current.memory?.let { insertValue(NumberFormatter.format(it).asExpressionText()) }
            MemoryOp.ADD, MemoryOp.SUBTRACT -> {
                val value = currentValue(current) ?: return
                val delta = if (op == MemoryOp.ADD) value else -value
                state = current.copy(memory = (current.memory ?: 0.0) + delta)
            }
        }
    }

    /** Valor numérico de lo que hay en pantalla (el resultado o la expresión si ya es evaluable). */
    private fun currentValue(s: CalculatorUiState): Double? {
        if (s.justEvaluated) return s.answer
        val outcome = ExpressionEngine.evaluate(s.expression, s.angleMode, ans = s.answer)
        return (outcome as? EvalResult.Success)?.value
    }

    // ── Voz ─────────────────────────────────────────────────────────────────

    // ── Navegación ──────────────────────────────────────────────────────────

    fun openGraph() {
        state = state.copy(screen = Screen.GRAPH, historyOpen = false)
    }

    fun openTools() {
        state = state.copy(screen = Screen.TOOLS, historyOpen = false)
    }

    fun showCalculator() {
        state = state.copy(screen = Screen.CALCULATOR)
    }

    // ── Preferencias ────────────────────────────────────────────────────────

    /** Alterna entre oscuro y claro a partir del tema que se está viendo ahora mismo. */
    fun toggleDarkTheme(currentlyDark: Boolean) {
        val newValue = !currentlyDark
        store.darkTheme = newValue
        settings = settings.copy(darkTheme = newValue)
    }

    fun toggleSound() {
        val newValue = !settings.soundEnabled
        store.soundEnabled = newValue
        settings = settings.copy(soundEnabled = newValue)
    }

    fun toggleParallax() {
        val newValue = !settings.parallaxEnabled
        store.parallaxEnabled = newValue
        settings = settings.copy(parallaxEnabled = newValue)
    }

    private companion object {
        const val EASTER_EGG_CODE = "666"

        /** "-5" (formato de salida) → "−5" (el signo que usa el teclado). */
        fun String.asExpressionText() = replace('-', ExpressionEditor.MINUS)

        /** Inverso: "2−3" → "2-3", para comparar con el texto de [NumberFormatter]. */
        fun String.asPlainText() = replace(ExpressionEditor.MINUS, '-')
    }
}
