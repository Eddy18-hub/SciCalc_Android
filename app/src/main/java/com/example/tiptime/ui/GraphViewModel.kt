package com.example.tiptime.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.tiptime.engine.AngleMode
import com.example.tiptime.engine.Viewport

/**
 * Estado del graficador.
 *
 * @property viewport ventana visible (ver [Viewport]).
 * @property traceX coordenada x del punto marcado con un toque, o `null` si no hay ninguno.
 * @property traceFunction función marcada: 0 = f(x), 1 = g(x).
 */
data class GraphUiState(
    val functionF: String = "sin(x)",
    val functionG: String = "",
    val angleMode: AngleMode = AngleMode.RADIANS,
    val viewport: Viewport = Viewport(),
    val traceX: Double? = null,
    val traceFunction: Int = 0,
    /** Muestra raíces, máximos, mínimos e intersecciones sobre la gráfica. */
    val analysisOn: Boolean = false,
    /** Muestra el área bajo f(x) entre [integralA] y [integralB] y calcula la integral. */
    val integralOn: Boolean = false,
    val integralA: String = "0",
    val integralB: String = "1",
)

class GraphViewModel : ViewModel() {

    var state by mutableStateOf(GraphUiState())
        private set

    fun setFunctionF(text: String) {
        state = state.copy(functionF = text, traceX = null)
    }

    fun setFunctionG(text: String) {
        state = state.copy(functionG = text, traceX = null)
    }

    fun toggleAngleMode() {
        val mode = if (state.angleMode == AngleMode.RADIANS) AngleMode.DEGREES else AngleMode.RADIANS
        state = state.copy(angleMode = mode, traceX = null)
    }

    fun toggleAnalysis() {
        state = state.copy(analysisOn = !state.analysisOn)
    }

    fun toggleIntegral() {
        state = state.copy(integralOn = !state.integralOn)
    }

    fun setIntegralA(text: String) {
        state = state.copy(integralA = text)
    }

    fun setIntegralB(text: String) {
        state = state.copy(integralB = text)
    }

    /**
     * Restablece TODO el graficador al estado inicial: funciones f(x) y g(x), zoom y posición,
     * punto marcado, análisis, integral y modo de ángulo.
     */
    fun resetView() {
        state = GraphUiState()
    }

    fun setTrace(x: Double?, function: Int) {
        state = state.copy(traceX = x, traceFunction = function)
    }

    /** Aplica un gesto de pellizco/arrastre sobre un lienzo de [width] × [height] píxeles. */
    fun transform(
        centroidX: Float,
        centroidY: Float,
        panX: Float,
        panY: Float,
        zoom: Float,
        width: Float,
        height: Float,
    ) {
        state = state.copy(
            viewport = state.viewport.transformed(centroidX, centroidY, panX, panY, zoom, width, height)
        )
    }
}
