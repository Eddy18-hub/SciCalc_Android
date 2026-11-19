package com.example.tiptime.ui.screens

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiptime.R
import com.example.tiptime.engine.AngleMode
import com.example.tiptime.engine.Calculus
import com.example.tiptime.engine.CurvePoint
import com.example.tiptime.engine.EvalResult
import com.example.tiptime.engine.PointKind
import com.example.tiptime.engine.CalcException
import com.example.tiptime.engine.CompiledExpression
import com.example.tiptime.engine.ExpressionEngine
import com.example.tiptime.engine.NumberFormatter
import com.example.tiptime.engine.Viewport
import com.example.tiptime.engine.niceStep
import com.example.tiptime.ui.GraphUiState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import android.graphics.Paint as AndroidPaint

/** Eventos del graficador hacia el resto de la app. */
class GraphActions(
    val onBack: () -> Unit,
    val onFunctionF: (String) -> Unit,
    val onFunctionG: (String) -> Unit,
    val onToggleAngle: () -> Unit,
    val onResetView: () -> Unit,
    val onTransform: (
        centroidX: Float, centroidY: Float, panX: Float, panY: Float,
        zoom: Float, width: Float, height: Float,
    ) -> Unit,
    val onTrace: (x: Double?, function: Int) -> Unit,
    val onToggleAnalysis: () -> Unit,
    val onToggleIntegral: () -> Unit,
    val onIntegralA: (String) -> Unit,
    val onIntegralB: (String) -> Unit,
)

private val ExampleFunctions =
    listOf("sin(x)", "cos(x)", "tan(x)", "x^2", "x^3-3x", "1/x", "sqrt(x)", "ln(x)", "abs(x)")

private const val MAX_GRID_LINES = 200L
private const val MAX_PIXEL_Y = 1.0e5
private const val CURVE_STEP_PX = 1.5f

/** Resultado de analizar el texto de una función. */
private sealed interface ParsedFunction {
    data object Empty : ParsedFunction
    data object Invalid : ParsedFunction
    data class Valid(val expression: CompiledExpression) : ParsedFunction
}

private fun parseFunction(text: String): ParsedFunction {
    if (text.isBlank()) return ParsedFunction.Empty
    return try {
        ParsedFunction.Valid(ExpressionEngine.compile(text, allowVariable = true))
    } catch (e: CalcException) {
        ParsedFunction.Invalid
    }
}

private val ParsedFunction.compiled: CompiledExpression?
    get() = (this as? ParsedFunction.Valid)?.expression

/** Puntos notables de las curvas dentro de la ventana visible. */
private class GraphAnalysis(
    val fPoints: List<CurvePoint>,
    val gPoints: List<CurvePoint>,
    val crossings: List<CurvePoint>,
) {
    val all: List<CurvePoint> get() = fPoints + gPoints + crossings
}

private val EmptyAnalysis = GraphAnalysis(emptyList(), emptyList(), emptyList())

/** Raíces y extremos de una curva en [lo, hi]. */
private fun analyze(function: CompiledExpression?, mode: AngleMode, lo: Double, hi: Double): List<CurvePoint> {
    if (function == null) return emptyList()
    val fn = { x: Double -> function.evalOrNaN(x, mode) }
    return Calculus.roots(fn, lo, hi) + Calculus.extrema(fn, lo, hi)
}

private fun analyzeAll(
    f: CompiledExpression?, g: CompiledExpression?, mode: AngleMode, lo: Double, hi: Double,
): GraphAnalysis {
    val crossings = if (f != null && g != null) {
        Calculus.intersections(
            { f.evalOrNaN(it, mode) }, { g.evalOrNaN(it, mode) }, lo, hi,
        )
    } else {
        emptyList()
    }
    return GraphAnalysis(analyze(f, mode, lo, hi), analyze(g, mode, lo, hi), crossings)
}

/** Evalúa un límite escrito por el usuario ("2", "pi", "−1.5"); `null` si no es un número. */
private fun parseLimit(text: String): Double? =
    (ExpressionEngine.evaluate(text, AngleMode.RADIANS) as? EvalResult.Success)?.value

/**
 * Graficador de funciones: hasta dos curvas f(x) y g(x).
 *
 *  - Arrastrar mueve la vista; pellizcar hace zoom (anclado a los dedos).
 *  - Tocar marca el punto de la curva más cercana y muestra sus coordenadas.
 *  - Las discontinuidades (tan x, 1/x…) se dejan sin unir.
 */
@Composable
fun GraphScreen(
    state: GraphUiState,
    actions: GraphActions,
    modifier: Modifier = Modifier,
) {
    val parsedF = remember(state.functionF) { parseFunction(state.functionF) }
    val parsedG = remember(state.functionG) { parseFunction(state.functionG) }
    val f = parsedF.compiled
    val g = parsedG.compiled

    // Tamaño real del lienzo: hace falta para saber qué rango de x se está viendo.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val analysis = remember(f, g, state.viewport, state.angleMode, state.analysisOn, canvasSize) {
        if (!state.analysisOn || canvasSize.width <= 0 || canvasSize.height <= 0) {
            EmptyAnalysis
        } else {
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()
            analyzeAll(
                f, g, state.angleMode,
                state.viewport.screenToWorldX(0f, w, h), state.viewport.screenToWorldX(w, w, h),
            )
        }
    }

    val integralLimits = remember(state.integralOn, state.integralA, state.integralB) {
        if (!state.integralOn) null else {
            val a = parseLimit(state.integralA)
            val b = parseLimit(state.integralB)
            if (a != null && b != null) a to b else null
        }
    }
    val integralValue = remember(f, integralLimits, state.angleMode) {
        val limits = integralLimits
        if (f == null || limits == null) Double.NaN
        else Calculus.integral({ f.evalOrNaN(it, state.angleMode) }, limits.first, limits.second)
    }

    // Un toque marca el punto de la curva cuyo valor en esa x queda más cerca del dedo.
    val onTap: (Offset, IntSize) -> Unit = { offset, size ->
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        val worldX = state.viewport.screenToWorldX(offset.x, width, height)
        val tappedY = state.viewport.screenToWorldY(offset.y, width, height)

        var bestIndex = -1
        var bestDistance = Double.MAX_VALUE
        listOf(f, g).forEachIndexed { index, function ->
            val y = function?.evalOrNaN(worldX, state.angleMode) ?: Double.NaN
            if (!y.isNaN() && abs(y - tappedY) < bestDistance) {
                bestDistance = abs(y - tappedY)
                bestIndex = index
            }
        }
        actions.onTrace(if (bestIndex >= 0) worldX else null, max(bestIndex, 0))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sideBySide = maxWidth > maxHeight && maxWidth >= 560.dp
        val contentModifier = if (sideBySide) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .fillMaxWidth()
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = contentModifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                GraphTopBar(state.angleMode, actions)

                val inputs: @Composable () -> Unit = {
                    FunctionInputs(
                        state = state,
                        fInvalid = parsedF is ParsedFunction.Invalid,
                        gInvalid = parsedG is ParsedFunction.Invalid,
                        actions = actions,
                    )
                }

                if (sideBySide) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        ) { inputs() }
                        GraphCard(
                            state = state,
                            f = f,
                            g = g,
                            actions = actions,
                            onTap = onTap,
                            analysis = analysis,
                            integralLimits = integralLimits,
                            integralValue = integralValue,
                            onCanvasSize = { canvasSize = it },
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight(),
                        )
                    }
                } else {
                    inputs()
                    Spacer(modifier = Modifier.height(8.dp))
                    GraphCard(
                        state = state,
                        f = f,
                        g = g,
                        actions = actions,
                        onTap = onTap,
                        analysis = analysis,
                        integralLimits = integralLimits,
                        integralValue = integralValue,
                        onCanvasSize = { canvasSize = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphTopBar(angleMode: AngleMode, actions: GraphActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Text(
            text = stringResource(R.string.graph_title),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        AngleModeChip(angleMode = angleMode, onClick = actions.onToggleAngle)
        TextButton(onClick = actions.onResetView) {
            Text(stringResource(R.string.graph_reset_view))
        }
    }
}

@Composable
private fun FunctionInputs(
    state: GraphUiState,
    fInvalid: Boolean,
    gInvalid: Boolean,
    actions: GraphActions,
) {
    Column {
        FunctionField(
            label = stringResource(R.string.graph_function_f),
            value = state.functionF,
            onValueChange = actions.onFunctionF,
            color = MaterialTheme.colorScheme.primary,
            isError = fInvalid,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FunctionField(
            label = stringResource(R.string.graph_function_g),
            value = state.functionG,
            onValueChange = actions.onFunctionG,
            color = MaterialTheme.colorScheme.tertiary,
            isError = gInvalid,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Atajos: tocar uno lo pone en f(x) sin necesidad de teclear.
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExampleFunctions.forEach { example ->
                AssistChip(
                    onClick = { actions.onFunctionF(example) },
                    label = { Text(example) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.analysisOn,
                onClick = actions.onToggleAnalysis,
                label = { Text(stringResource(R.string.graph_analysis)) },
            )
            FilterChip(
                selected = state.integralOn,
                onClick = actions.onToggleIntegral,
                label = { Text(stringResource(R.string.graph_integral)) },
            )
        }
        if (state.integralOn) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LimitField(
                    label = stringResource(R.string.graph_integral_from),
                    value = state.integralA,
                    onValueChange = actions.onIntegralA,
                    modifier = Modifier.weight(1f),
                )
                LimitField(
                    label = stringResource(R.string.graph_integral_to),
                    value = state.integralB,
                    onValueChange = actions.onIntegralB,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LimitField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = parseLimit(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
    )
}

@Composable
private fun FunctionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val errorMessage: (@Composable () -> Unit)? = if (isError) {
        { Text(stringResource(R.string.graph_function_invalid)) }
    } else {
        null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape),
            )
        },
        supportingText = errorMessage,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
    )
}

/** Lienzo con borde redondeado y la ayuda de gestos debajo. */
@Composable
private fun GraphCard(
    state: GraphUiState,
    f: CompiledExpression?,
    g: CompiledExpression?,
    actions: GraphActions,
    onTap: (Offset, IntSize) -> Unit,
    analysis: GraphAnalysis,
    integralLimits: Pair<Double, Double>?,
    integralValue: Double,
    onCanvasSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
        ) {
            GraphCanvas(
                state = state,
                f = f,
                g = g,
                onTransform = { centroid, pan, zoom, size ->
                    actions.onTransform(
                        centroid.x, centroid.y, pan.x, pan.y, zoom,
                        size.width.toFloat(), size.height.toFloat(),
                    )
                },
                onTap = onTap,
                analysis = analysis,
                integralLimits = integralLimits,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged(onCanvasSize),
            )
        }
        Text(
            text = stringResource(R.string.graph_hint),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 8.dp),
        )
        if (state.integralOn) {
            Text(
                text = if (integralValue.isNaN()) {
                    stringResource(R.string.graph_integral_invalid)
                } else {
                    stringResource(R.string.graph_integral_result, NumberFormatter.formatAxis(integralValue))
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp, start = 8.dp),
            )
        }
        if (state.analysisOn) AnalysisSummary(analysis, hasG = g != null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lienzo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GraphCanvas(
    state: GraphUiState,
    f: CompiledExpression?,
    g: CompiledExpression?,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float, size: IntSize) -> Unit,
    onTap: (offset: Offset, size: IntSize) -> Unit,
    analysis: GraphAnalysis,
    integralLimits: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val gridColor = scheme.onSurface.copy(alpha = 0.07f)
    val axisColor = scheme.onSurface.copy(alpha = 0.55f)
    val colorF = scheme.primary
    val colorG = scheme.tertiary
    val surfaceColor = scheme.surface
    val labelArgb = scheme.onSurfaceVariant.toArgb()
    val traceArgb = scheme.onSurface.toArgb()

    // Los textos del lienzo se dibujan con android.graphics (más barato que medir con TextMeasurer
    // en cada fotograma mientras el usuario arrastra).
    val labelPaint = remember(labelArgb, density) {
        AndroidPaint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = with(density) { 11.sp.toPx() }
        }
    }
    val tracePaint = remember(traceArgb, density) {
        AndroidPaint().apply {
            isAntiAlias = true
            color = traceArgb
            textSize = with(density) { 12.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    // pointerInput(Unit) no se reinicia al recomponer: hay que leer siempre la última lambda.
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnTap by rememberUpdatedState(onTap)

    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    currentOnTransform(centroid, pan, zoom, size)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> currentOnTap(offset, size) }
            },
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val viewport = state.viewport
        val pixelsPerUnit = viewport.pixelsPerUnit(width, height)
        val screenX = { x: Double -> viewport.worldToScreenX(x, width, height).toFloat() }
        val screenY = { y: Double -> viewport.worldToScreenY(y, width, height).toFloat() }

        val native = drawContext.canvas.nativeCanvas
        val labelPad = 4.dp.toPx()
        val step = niceStep(72.dp.toPx() / pixelsPerUnit)
        val axisX = screenX(0.0)
        val axisY = screenY(0.0)

        // Líneas verticales de la cuadrícula + etiquetas del eje x
        val firstX = ceil(viewport.screenToWorldX(0f, width, height) / step).toLong()
        val lastX = floor(viewport.screenToWorldX(width, width, height) / step).toLong()
        var index = firstX
        while (index <= lastX && index - firstX <= MAX_GRID_LINES) {
            val worldX = index * step
            val px = screenX(worldX)
            drawLine(gridColor, Offset(px, 0f), Offset(px, height), strokeWidth = 1.dp.toPx())
            if (index != 0L) {
                labelPaint.textAlign = AndroidPaint.Align.CENTER
                val minY = 16.dp.toPx()
                val y = (axisY + 16.dp.toPx()).coerceIn(minY, max(minY, height - labelPad))
                native.drawText(NumberFormatter.formatAxis(worldX), px, y, labelPaint)
            }
            index++
        }

        // Líneas horizontales + etiquetas del eje y
        val firstY = ceil(viewport.screenToWorldY(height, width, height) / step).toLong()
        val lastY = floor(viewport.screenToWorldY(0f, width, height) / step).toLong()
        index = firstY
        while (index <= lastY && index - firstY <= MAX_GRID_LINES) {
            val worldY = index * step
            val py = screenY(worldY)
            drawLine(gridColor, Offset(0f, py), Offset(width, py), strokeWidth = 1.dp.toPx())
            if (index != 0L) {
                val text = NumberFormatter.formatAxis(worldY)
                labelPaint.textAlign = AndroidPaint.Align.LEFT
                val textWidth = labelPaint.measureText(text)
                val x = (axisX + labelPad).coerceIn(labelPad, max(labelPad, width - textWidth - labelPad))
                native.drawText(text, x, py - labelPad, labelPaint)
            }
            index++
        }

        // Ejes
        if (axisY in 0f..height) {
            drawLine(axisColor, Offset(0f, axisY), Offset(width, axisY), strokeWidth = 1.5.dp.toPx())
        }
        if (axisX in 0f..width) {
            drawLine(axisColor, Offset(axisX, 0f), Offset(axisX, height), strokeWidth = 1.5.dp.toPx())
        }

        // Área de la integral (debajo de las curvas, así la línea queda nítida encima)
        if (f != null && integralLimits != null) {
            drawIntegralArea(f, state.angleMode, viewport, integralLimits.first, integralLimits.second, colorF)
        }

        // Curvas
        g?.let { drawCurve(it, state.angleMode, viewport, colorG) }
        f?.let { drawCurve(it, state.angleMode, viewport, colorF) }

        // Puntos notables
        analysis.all.forEach { point ->
            val px = screenX(point.x)
            val py = screenY(point.y)
            if (px in 0f..width && py in 0f..height) {
                drawCircle(surfaceColor, radius = 7.dp.toPx(), center = Offset(px, py))
                drawCircle(pointColor(point.kind), radius = 5.dp.toPx(), center = Offset(px, py))
            }
        }

        // Punto marcado
        val traceX = state.traceX
        if (traceX != null) {
            val isG = state.traceFunction == 1
            val function = if (isG) g else f
            if (function != null) {
                drawTrace(
                    function = function,
                    worldX = traceX,
                    angleMode = state.angleMode,
                    viewport = viewport,
                    color = if (isG) colorG else colorF,
                    surfaceColor = surfaceColor,
                    paint = tracePaint,
                )
            }
        }
    }
}

private fun pointColor(kind: PointKind): Color = when (kind) {
    PointKind.ROOT -> Color(0xFFE53935)
    PointKind.MAXIMUM -> Color(0xFF43A047)
    PointKind.MINIMUM -> Color(0xFF1E88E5)
    PointKind.INTERSECTION -> Color(0xFFFB8C00)
}

/** Rellena el área entre la curva y el eje x, de [a] a [b] (negativa por debajo, como la integral). */
private fun DrawScope.drawIntegralArea(
    function: CompiledExpression,
    angleMode: AngleMode,
    viewport: Viewport,
    a: Double,
    b: Double,
    color: Color,
) {
    val width = size.width
    val height = size.height
    val lo = minOf(a, b)
    val hi = maxOf(a, b)
    val startPx = viewport.worldToScreenX(lo, width, height).toFloat().coerceIn(0f, width)
    val endPx = viewport.worldToScreenX(hi, width, height).toFloat().coerceIn(0f, width)
    if (endPx <= startPx) return

    val axisY = viewport.worldToScreenY(0.0, width, height).toFloat().coerceIn(-MAX_PIXEL_Y.toFloat(), MAX_PIXEL_Y.toFloat())
    val path = Path()
    path.moveTo(startPx, axisY)
    var px = startPx
    while (px < endPx) {
        val worldY = function.evalOrNaN(viewport.screenToWorldX(px, width, height), angleMode)
        val y = if (worldY.isNaN()) axisY else {
            viewport.worldToScreenY(worldY, width, height).coerceIn(-MAX_PIXEL_Y, MAX_PIXEL_Y).toFloat()
        }
        path.lineTo(px, y)
        px += CURVE_STEP_PX
    }
    path.lineTo(endPx, axisY)
    path.close()
    drawPath(path, color.copy(alpha = 0.22f))
}

/** Resumen escrito de los puntos notables (la lista complementa a los puntos de color en la gráfica). */
@Composable
private fun AnalysisSummary(analysis: GraphAnalysis, hasG: Boolean) {
    val none = stringResource(R.string.graph_none)
    fun List<CurvePoint>.xs(kind: PointKind) = filter { it.kind == kind }.take(6)

    @Composable
    fun line(title: String, label: String, points: List<CurvePoint>, kind: PointKind, withY: Boolean) {
        val items = points.xs(kind)
        val text = if (items.isEmpty()) none else items.joinToString("   ") {
            if (withY) "(${NumberFormatter.formatAxis(it.x)}, ${NumberFormatter.formatAxis(it.y)})"
            else "x = ${NumberFormatter.formatAxis(it.x)}"
        }
        Text(
            text = "$title $label: $text",
            style = MaterialTheme.typography.labelMedium,
            color = pointColor(kind),
        )
    }

    Column(
        modifier = Modifier
            .heightIn(max = 110.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp, start = 8.dp),
    ) {
        line("f", stringResource(R.string.graph_roots), analysis.fPoints, PointKind.ROOT, withY = false)
        line("f", stringResource(R.string.graph_maximum), analysis.fPoints, PointKind.MAXIMUM, withY = true)
        line("f", stringResource(R.string.graph_minimum), analysis.fPoints, PointKind.MINIMUM, withY = true)
        if (hasG) {
            line("g", stringResource(R.string.graph_roots), analysis.gPoints, PointKind.ROOT, withY = false)
            line("g", stringResource(R.string.graph_maximum), analysis.gPoints, PointKind.MAXIMUM, withY = true)
            line("g", stringResource(R.string.graph_minimum), analysis.gPoints, PointKind.MINIMUM, withY = true)
            line("f∩g", stringResource(R.string.graph_intersections), analysis.crossings, PointKind.INTERSECTION, withY = true)
        }
    }
}

/**
 * Traza una curva muestreando una vez cada [CURVE_STEP_PX] píxeles. Se levanta el "lápiz" donde la
 * función no está definida y donde la curva atraviesa toda la pantalla de un salto (asíntotas).
 */
private fun DrawScope.drawCurve(
    function: CompiledExpression,
    angleMode: AngleMode,
    viewport: Viewport,
    color: Color,
) {
    val width = size.width
    val height = size.height
    val path = Path()
    var penDown = false
    var previousY = 0f

    var px = 0f
    while (px <= width + CURVE_STEP_PX) {
        val worldY = function.evalOrNaN(viewport.screenToWorldX(px, width, height), angleMode)
        if (worldY.isNaN()) {
            penDown = false
        } else {
            val y = viewport.worldToScreenY(worldY, width, height)
                .coerceIn(-MAX_PIXEL_Y, MAX_PIXEL_Y)
                .toFloat()
            val jumpsAcrossScreen = (previousY < 0f && y > height) || (previousY > height && y < 0f)
            if (!penDown || jumpsAcrossScreen) {
                path.moveTo(px, y)
                penDown = true
            } else {
                path.lineTo(px, y)
            }
            previousY = y
        }
        px += CURVE_STEP_PX
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Marcador con guía vertical y una etiqueta con las coordenadas (x, f(x)). */
private fun DrawScope.drawTrace(
    function: CompiledExpression,
    worldX: Double,
    angleMode: AngleMode,
    viewport: Viewport,
    color: Color,
    surfaceColor: Color,
    paint: AndroidPaint,
) {
    val worldY = function.evalOrNaN(worldX, angleMode)
    if (worldY.isNaN()) return

    val width = size.width
    val height = size.height
    val px = viewport.worldToScreenX(worldX, width, height).toFloat()
    val py = viewport.worldToScreenY(worldY, width, height)
        .coerceIn(-MAX_PIXEL_Y, MAX_PIXEL_Y)
        .toFloat()

    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(px, 0f),
        end = Offset(px, height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
    )
    drawCircle(surfaceColor, radius = 7.dp.toPx(), center = Offset(px, py))
    drawCircle(color, radius = 5.dp.toPx(), center = Offset(px, py))

    // Etiqueta: junto al punto, sin salirse del lienzo.
    // Además de las coordenadas se muestra la pendiente (derivada) en ese punto.
    val slope = Calculus.derivative({ function.evalOrNaN(it, angleMode) }, worldX)
    val slopeText = if (slope.isNaN()) "" else "  ′ ${NumberFormatter.formatAxis(slope)}"
    val text = "(${NumberFormatter.formatAxis(worldX)}, ${NumberFormatter.formatAxis(worldY)})$slopeText"
    val padding = 8.dp.toPx()
    val boxWidth = paint.measureText(text) + 2 * padding
    val boxHeight = 26.dp.toPx()
    val gap = 12.dp.toPx()
    var left = px + gap
    if (left + boxWidth > width) left = px - gap - boxWidth
    left = left.coerceIn(0f, max(0f, width - boxWidth))
    var top = py - boxHeight - gap
    if (top < 0f) top = py + gap

    val corner = CornerRadius(8.dp.toPx())
    drawRoundRect(surfaceColor, Offset(left, top), Size(boxWidth, boxHeight), corner)
    drawRoundRect(color, Offset(left, top), Size(boxWidth, boxHeight), corner, style = Stroke(1.dp.toPx()))
    drawContext.canvas.nativeCanvas.drawText(
        text,
        left + padding,
        top + boxHeight / 2f + paint.textSize * 0.35f,
        paint,
    )
}
