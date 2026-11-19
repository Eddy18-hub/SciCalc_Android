package com.example.tiptime.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tiptime.sensor.TiltSensor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Fondo con parallax: tres capas a distinta "profundidad" que se desplazan al inclinar el
 * teléfono (cuanto más cerca, más se mueve).
 *
 *   1. Cuadrícula de papel milimetrado  (lejana, se mueve poco)
 *   2. Resplandores de color             (intermedia)
 *   3. Ondas seno / coseno               (cercana, se mueve más)
 *
 * Si el dispositivo no tiene acelerómetro, las capas derivan lentamente por sí solas.
 * Todo se lee dentro del bloque de dibujo, así que el movimiento no provoca recomposiciones.
 */
@Composable
fun ParallaxBackground(
    tilt: TiltSensor,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val gridColor = scheme.onSurface.copy(alpha = 0.05f)
    val primary = scheme.primary
    val tertiary = scheme.tertiary

    // La animación solo existe cuando hace falta: sin ella no se pide ningún fotograma extra.
    val drift: State<Float>? = if (enabled && !tilt.isAvailable) {
        rememberInfiniteTransition(label = "parallax-drift").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(durationMillis = 30_000, easing = LinearEasing)),
            label = "parallax-phase",
        )
    } else {
        null
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val tiltX: Float
        val tiltY: Float
        when {
            !enabled -> { tiltX = 0f; tiltY = 0f }
            tilt.isAvailable -> { tiltX = tilt.x; tiltY = tilt.y }
            else -> {
                val phase = drift?.value ?: 0f
                tiltX = sin(phase) * 0.6f
                tiltY = cos(phase * 2f) * 0.6f
            }
        }

        // Capa 1 — cuadrícula
        val spacing = 40.dp.toPx()
        val gridShiftX = tiltX * 6.dp.toPx()
        val gridShiftY = tiltY * 6.dp.toPx()
        var gx = (gridShiftX % spacing) - spacing
        while (gx < size.width + spacing) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
            gx += spacing
        }
        var gy = (gridShiftY % spacing) - spacing
        while (gy < size.height + spacing) {
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            gy += spacing
        }

        // Capa 2 — resplandores
        val glowShift = 18.dp.toPx()
        val glowRadius = max(size.width, size.height) * 0.55f
        val glowA = Offset(size.width * 0.12f + tiltX * glowShift, size.height * 0.10f + tiltY * glowShift)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
                center = glowA,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = glowA,
        )
        val glowB = Offset(
            size.width * 0.98f + tiltX * glowShift * 1.4f,
            size.height * 0.58f + tiltY * glowShift * 1.4f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.14f), Color.Transparent),
                center = glowB,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = glowB,
        )

        // Capa 3 — ondas
        val waveShift = 36.dp.toPx()
        translate(left = tiltX * waveShift, top = tiltY * waveShift) {
            drawWave(
                baseY = size.height * 0.20f,
                amplitude = 26.dp.toPx(),
                wavelength = size.width * 0.9f,
                phase = 0f,
                color = primary.copy(alpha = 0.16f),
                strokeWidth = 2.dp.toPx(),
            )
            drawWave(
                baseY = size.height * 0.46f,
                amplitude = 34.dp.toPx(),
                wavelength = size.width * 1.2f,
                phase = (PI / 2).toFloat(), // coseno
                color = tertiary.copy(alpha = 0.14f),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

/** Dibuja una onda seno que se extiende más allá de los bordes para que el desplazamiento no deje huecos. */
private fun DrawScope.drawWave(
    baseY: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    val step = 10.dp.toPx()
    var x = -wavelength
    var first = true
    while (x <= size.width + wavelength) {
        val y = baseY + amplitude * sin(x / wavelength * 2f * PI.toFloat() + phase)
        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }
        x += step
    }
    drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * Desplaza el contenido unos dp según la inclinación del teléfono (efecto de "flotar" sobre el
 * fondo). No hace nada si el parallax está desactivado o no hay acelerómetro.
 */
fun Modifier.parallaxShift(tilt: TiltSensor, enabled: Boolean, depth: Dp): Modifier =
    if (!enabled || !tilt.isAvailable) {
        this
    } else {
        graphicsLayer {
            translationX = tilt.x * depth.toPx()
            translationY = tilt.y * depth.toPx()
        }
    }
