package com.example.tiptime.engine

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/**
 * Ventana visible del graficador (Kotlin puro: sin Compose, fácil de probar).
 *
 * Se describe con el centro ([centerX], [centerY]) en coordenadas del mundo y con
 * [unitsAcross], las unidades que caben en el lado MÁS CORTO del lienzo. La escala en
 * píxeles se deriva del tamaño real, así la vista sobrevive a rotaciones y cambios de tamaño.
 * Los ejes comparten escala (1 unidad en x mide lo mismo que 1 unidad en y).
 */
data class Viewport(
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val unitsAcross: Double = DEFAULT_UNITS_ACROSS,
) {
    fun pixelsPerUnit(width: Float, height: Float): Double = min(width, height) / unitsAcross

    fun worldToScreenX(x: Double, width: Float, height: Float): Double =
        width / 2.0 + (x - centerX) * pixelsPerUnit(width, height)

    fun worldToScreenY(y: Double, width: Float, height: Float): Double =
        height / 2.0 - (y - centerY) * pixelsPerUnit(width, height)

    fun screenToWorldX(px: Float, width: Float, height: Float): Double =
        centerX + (px - width / 2.0) / pixelsPerUnit(width, height)

    fun screenToWorldY(py: Float, width: Float, height: Float): Double =
        centerY - (py - height / 2.0) / pixelsPerUnit(width, height)

    /**
     * Aplica un gesto de pellizco/arrastre. El punto del mundo que estaba bajo el centroide de
     * los dedos termina en (centroide + desplazamiento): el zoom queda "anclado" a los dedos.
     *
     * @param zoom factor de escala del gesto (> 1 acerca).
     */
    fun transformed(
        centroidX: Float,
        centroidY: Float,
        panX: Float,
        panY: Float,
        zoom: Float,
        width: Float,
        height: Float,
    ): Viewport {
        if (width <= 0f || height <= 0f || zoom <= 0f) return this

        val anchorX = screenToWorldX(centroidX, width, height)
        val anchorY = screenToWorldY(centroidY, width, height)

        val newUnits = (unitsAcross / zoom).coerceIn(MIN_UNITS_ACROSS, MAX_UNITS_ACROSS)
        val newPixelsPerUnit = min(width, height) / newUnits

        return Viewport(
            centerX = anchorX - (centroidX + panX - width / 2.0) / newPixelsPerUnit,
            centerY = anchorY + (centroidY + panY - height / 2.0) / newPixelsPerUnit,
            unitsAcross = newUnits,
        )
    }

    companion object {
        const val DEFAULT_UNITS_ACROSS = 20.0
        const val MIN_UNITS_ACROSS = 1e-3
        const val MAX_UNITS_ACROSS = 1e6
    }
}

/**
 * Menor paso "redondo" (1, 2, 5 × 10ⁿ) que sea ≥ [rawStep]; se usa para espaciar la
 * cuadrícula y las etiquetas de los ejes.
 */
fun niceStep(rawStep: Double): Double {
    if (rawStep <= 0.0 || rawStep.isNaN() || rawStep.isInfinite()) return 1.0
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val fraction = rawStep / magnitude
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}
