package com.example.tiptime.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiptime.R
import com.example.tiptime.data.HistoryEntry
import com.example.tiptime.ui.CalculatorUiState
import com.example.tiptime.ui.messageRes
import com.example.tiptime.ui.theme.DisplayTextStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Pantalla de la calculadora: la expresión arriba (con desplazamiento si es larga) y debajo el
 * resultado — atenuado mientras se escribe (vista previa), destacado tras pulsar "=" y en rojo
 * si hubo un error.
 *
 * Encima de la expresión actual se ve la "cinta" con las operaciones anteriores: se desplaza hacia
 * arriba para ver todo el historial (lo más antiguo queda más arriba).
 *
 * Los tamaños de letra se calculan a partir del ancho y alto disponibles para que el texto
 * quepa en cualquier pantalla sin medir el texto (sin pasadas de layout adicionales).
 */
@Composable
fun CalculatorDisplay(
    state: CalculatorUiState,
    modifier: Modifier = Modifier,
    onRecallExpression: (HistoryEntry) -> Unit = {},
    onUseResult: (HistoryEntry) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme

    val errorText = state.error?.let { stringResource(it.messageRes()) }
    val placeholder = stringResource(R.string.result_placeholder)
    val resultText = when {
        errorText != null -> errorText
        state.justEvaluated -> state.result
        state.preview.isNotEmpty() -> state.preview
        state.expression.isEmpty() -> placeholder
        else -> ""
    }

    val resultColor by animateColorAsState(
        targetValue = when {
            state.error != null -> scheme.error
            state.justEvaluated -> scheme.primary
            else -> scheme.onSurfaceVariant
        },
        label = "result-color",
    )
    val expressionColor by animateColorAsState(
        targetValue = if (state.justEvaluated) scheme.onSurfaceVariant else scheme.onSurface,
        label = "expression-color",
    )

    BoxWithConstraints(modifier = modifier) {
        val boxHeight = maxHeight
        val availableWidth = maxWidth.value - 2 * HORIZONTAL_PADDING.value
        val resultCap = min(60f, maxHeight.value * 0.34f)
        val resultFit = availableWidth / (max(resultText.length, 1) * AVERAGE_GLYPH_WIDTH)
        val resultSize = min(resultCap, resultFit).coerceAtLeast(18f)

        val expressionCap = min(34f, maxHeight.value * 0.20f)
        val expressionFit = availableWidth * EXPRESSION_LINES /
            (max(state.expression.length, 1) * AVERAGE_GLYPH_WIDTH)
        val expressionSize = min(expressionCap, expressionFit).coerceAtLeast(16f)

        val scroll = rememberScrollState()
        // Si la expresión es más larga que el espacio, se mantiene visible el final.
        LaunchedEffect(state.expression, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }

        if (state.memory != null) {
            // Indicador de que hay algo guardado en memoria (MR lo recupera).
            Text(
                text = "M",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = HORIZONTAL_PADDING, top = 10.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
        ) {
            // La operación que acaba de calcularse ya está en pantalla: no se repite en la cinta.
            val latest = state.history.firstOrNull()
            val tape = if (state.justEvaluated && latest?.expression == state.expression && latest.result == state.result) {
                state.history.drop(1)
            } else {
                state.history
            }
            if (tape.isNotEmpty()) {
                // reverseLayout: el elemento 0 (el más reciente) queda abajo, pegado a la expresión.
                val tapeState = rememberLazyListState()
                LaunchedEffect(tape.size) { tapeState.scrollToItem(0) }
                LazyColumn(
                    state = tapeState,
                    reverseLayout = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(tape) { entry ->
                        HistoryRow(entry, onRecallExpression, onUseResult, compact = true)
                    }
                }
            }
            Box(
                modifier = (if (tape.isEmpty()) Modifier.weight(1f) else Modifier.heightIn(max = boxHeight * 0.4f))
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Text(
                    text = state.expression,
                    style = DisplayTextStyle,
                    fontSize = expressionSize.sp,
                    color = expressionColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.verticalScroll(scroll),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = resultText.ifEmpty { " " }, // reserva la altura de la línea aunque no haya vista previa
                style = DisplayTextStyle,
                fontSize = resultSize.sp,
                fontWeight = if (state.justEvaluated) FontWeight.Normal else FontWeight.Light,
                color = resultColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private val HORIZONTAL_PADDING = 18.dp

/** Ancho medio de un carácter, en "em": con cifras tabulares los dígitos miden ≈ 0.56 em. */
private const val AVERAGE_GLYPH_WIDTH = 0.58f
private const val EXPRESSION_LINES = 3f

/**
 * Lista de operaciones anteriores (la más reciente arriba).
 *
 *  - Tocar la expresión la vuelve a cargar para editarla.
 *  - Tocar el resultado lo inserta en la expresión actual.
 */
@Composable
fun HistoryPanel(
    history: List<HistoryEntry>,
    onRecallExpression: (HistoryEntry) -> Unit,
    onUseResult: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.history_clear))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history) { entry ->
                    HistoryRow(entry, onRecallExpression, onUseResult)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onRecallExpression: (HistoryEntry) -> Unit,
    onUseResult: (HistoryEntry) -> Unit,
    compact: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 0.dp else 18.dp, vertical = if (compact) 4.dp else 10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = entry.expression,
            style = DisplayTextStyle,
            fontSize = if (compact) 14.sp else 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRecallExpression(entry) }
                .padding(vertical = 4.dp),
        )
        Text(
            text = "= ${entry.result}",
            style = DisplayTextStyle,
            fontSize = if (compact) 18.sp else 26.sp,
            fontWeight = FontWeight.Normal,
            color = if (compact) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier
                .clickable { onUseResult(entry) }
                .padding(vertical = 4.dp),
        )
    }
}
