package com.example.tiptime.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tiptime.R
import com.example.tiptime.data.HistoryEntry
import com.example.tiptime.engine.AngleMode
import com.example.tiptime.sensor.TiltSensor
import com.example.tiptime.ui.AppSettings
import com.example.tiptime.ui.CalculatorUiState
import com.example.tiptime.ui.KeyAction
import com.example.tiptime.ui.components.CalculatorDisplay
import com.example.tiptime.ui.components.HistoryPanel
import com.example.tiptime.ui.components.MainKeys
import com.example.tiptime.ui.components.ScientificKeys
import com.example.tiptime.ui.components.parallaxShift

/** Todo lo que la pantalla puede pedirle al resto de la app (estado hacia abajo, eventos hacia arriba). */
class CalculatorActions(
    val onKey: (KeyAction) -> Unit,
    val onToggleAngle: () -> Unit,
    val onToggleHistory: () -> Unit,
    val onOpenGraph: () -> Unit,
    val onToggleTheme: () -> Unit,
    val onToggleSound: () -> Unit,
    val onToggleParallax: () -> Unit,
    val onRecallExpression: (HistoryEntry) -> Unit,
    val onUseResult: (HistoryEntry) -> Unit,
    val onClearHistory: () -> Unit,
    val onOpenTools: () -> Unit,
)

private val PaneGap = 12.dp
private val MaxPortraitWidth = 520.dp

/**
 * Pantalla principal. Elige entre tres disposiciones según el espacio disponible:
 *
 *  - **Vertical** (teléfono): barra, pantalla, teclas científicas y teclado numérico apilados.
 *  - **Horizontal** (≥ 560 dp de ancho y más ancho que alto): pantalla + teclas científicas a la
 *    izquierda, teclado numérico a la derecha.
 *  - **Expandida** (≥ 900 dp): lo anterior más el historial siempre visible a la izquierda.
 */
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    settings: AppSettings,
    isDark: Boolean,
    tilt: TiltSensor,
    parallaxActive: Boolean,
    actions: CalculatorActions,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
        val permanentHistory = landscape && maxWidth >= 900.dp

        val topBar: @Composable (Boolean) -> Unit = { showHistoryButton ->
            CalculatorTopBar(
                state = state,
                settings = settings,
                isDark = isDark,
                showHistoryButton = showHistoryButton,
                actions = actions,
            )
        }

        when {
            permanentHistory -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaneGap),
                horizontalArrangement = Arrangement.spacedBy(PaneGap),
            ) {
                GlassCard(modifier = Modifier.weight(0.8f).fillMaxHeight()) {
                    HistoryPanel(
                        history = state.history,
                        onRecallExpression = actions.onRecallExpression,
                        onUseResult = actions.onUseResult,
                        onClear = actions.onClearHistory,
                    )
                }
                LeftColumn(
                    state = state,
                    actions = actions,
                    tilt = tilt,
                    parallaxActive = parallaxActive,
                    topBar = { topBar(false) },
                    showHistoryInDisplay = false,
                    modifier = Modifier.weight(1.1f),
                )
                MainKeys(onKey = actions.onKey, modifier = Modifier.weight(1f).fillMaxHeight())
            }

            landscape -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaneGap),
                horizontalArrangement = Arrangement.spacedBy(PaneGap),
            ) {
                LeftColumn(
                    state = state,
                    actions = actions,
                    tilt = tilt,
                    parallaxActive = parallaxActive,
                    topBar = { topBar(true) },
                    showHistoryInDisplay = true,
                    modifier = Modifier.weight(1f),
                )
                MainKeys(onKey = actions.onKey, modifier = Modifier.weight(0.9f).fillMaxHeight())
            }

            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = MaxPortraitWidth)
                        .fillMaxWidth()
                        .padding(start = PaneGap, end = PaneGap, bottom = PaneGap),
                ) {
                    topBar(true)
                    DisplayCard(
                        state = state,
                        actions = actions,
                        tilt = tilt,
                        parallaxActive = parallaxActive,
                        showHistoryInDisplay = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.weight(2.3f)) {
                        ScientificKeys(onKey = actions.onKey, modifier = Modifier.weight(2.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        MainKeys(onKey = actions.onKey, modifier = Modifier.weight(5f))
                    }
                }
            }
        }
    }
}

/** Columna izquierda de las disposiciones horizontales: barra, pantalla y teclas científicas. */
@Composable
private fun LeftColumn(
    state: CalculatorUiState,
    actions: CalculatorActions,
    tilt: TiltSensor,
    parallaxActive: Boolean,
    topBar: @Composable () -> Unit,
    showHistoryInDisplay: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        topBar()
        DisplayCard(
            state = state,
            actions = actions,
            tilt = tilt,
            parallaxActive = parallaxActive,
            showHistoryInDisplay = showHistoryInDisplay,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ScientificKeys(onKey = actions.onKey, modifier = Modifier.weight(1.25f))
    }
}

/** Tarjeta translúcida con borde suave; flota con el parallax. */
@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        content = content,
    )
}

/** Tarjeta de la pantalla: muestra el resultado o, si se pidió, el historial. */
@Composable
private fun DisplayCard(
    state: CalculatorUiState,
    actions: CalculatorActions,
    tilt: TiltSensor,
    parallaxActive: Boolean,
    showHistoryInDisplay: Boolean,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .parallaxShift(tilt, parallaxActive, depth = 6.dp),
    ) {
        AnimatedContent(
            targetState = showHistoryInDisplay && state.historyOpen,
            label = "display-or-history",
        ) { showHistory ->
            if (showHistory) {
                HistoryPanel(
                    history = state.history,
                    onRecallExpression = actions.onRecallExpression,
                    onUseResult = actions.onUseResult,
                    onClear = actions.onClearHistory,
                )
            } else {
                CalculatorDisplay(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onRecallExpression = actions.onRecallExpression,
                    onUseResult = actions.onUseResult,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Barra superior
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CalculatorTopBar(
    state: CalculatorUiState,
    settings: AppSettings,
    isDark: Boolean,
    showHistoryButton: Boolean,
    actions: CalculatorActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AngleModeChip(angleMode = state.angleMode, onClick = actions.onToggleAngle)
        Spacer(modifier = Modifier.weight(1f))

        if (showHistoryButton) {
            BarIconButton(
                iconRes = R.drawable.ic_history,
                description = stringResource(R.string.cd_history),
                highlighted = state.historyOpen,
                onClick = actions.onToggleHistory,
            )
        }
        BarIconButton(
            iconRes = R.drawable.ic_graph,
            description = stringResource(R.string.cd_graph),
            onClick = actions.onOpenGraph,
        )
        BarIconButton(
            iconRes = if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode,
            description = stringResource(
                if (isDark) R.string.cd_theme_to_light else R.string.cd_theme_to_dark
            ),
            onClick = actions.onToggleTheme,
        )
        OverflowMenu(
            soundEnabled = settings.soundEnabled,
            parallaxEnabled = settings.parallaxEnabled,
            hasHistory = state.history.isNotEmpty(),
            actions = actions,
        )
    }
}

@Composable
private fun BarIconButton(
    @DrawableRes iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = modifier.size(44.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Selector DEG / RAD: muestra ambas opciones y resalta la activa. */
@Composable
internal fun AngleModeChip(
    angleMode: AngleMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val degrees = stringResource(R.string.angle_degrees)
    val radians = stringResource(R.string.angle_radians)
    val active = if (angleMode == AngleMode.DEGREES) degrees else radians
    val description = stringResource(R.string.cd_angle_mode, active)

    val onContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val inactive = onContainer.copy(alpha = 0.45f)

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeLabel(degrees, selected = angleMode == AngleMode.DEGREES, active = onContainer, inactive = inactive)
            Text(text = " · ", color = inactive, style = MaterialTheme.typography.labelLarge)
            ModeLabel(radians, selected = angleMode == AngleMode.RADIANS, active = onContainer, inactive = inactive)
        }
    }
}

@Composable
private fun ModeLabel(text: String, selected: Boolean, active: Color, inactive: Color) {
    Text(
        text = text,
        color = if (selected) active else inactive,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        style = MaterialTheme.typography.labelLarge,
    )
}

/** Menú "⋮": parallax, idioma y borrar historial. */
@Composable
private fun OverflowMenu(
    soundEnabled: Boolean,
    parallaxEnabled: Boolean,
    hasHistory: Boolean,
    actions: CalculatorActions,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        BarIconButton(
            iconRes = R.drawable.ic_more_vert,
            description = stringResource(R.string.cd_more),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (soundEnabled) R.string.cd_sound_mute else R.string.cd_sound_unmute))
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(if (soundEnabled) R.drawable.ic_volume_on else R.drawable.ic_volume_off),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
                onClick = actions.onToggleSound,
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_parallax)) },
                trailingIcon = {
                    Switch(
                        checked = parallaxEnabled,
                        onCheckedChange = null, // el toque lo recibe todo el elemento del menú
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
                onClick = actions.onToggleParallax,
            )

            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_tools)) },
                onClick = {
                    expanded = false
                    actions.onOpenTools()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_clear_history)) },
                enabled = hasHistory,
                onClick = {
                    expanded = false
                    actions.onClearHistory()
                },
            )
        }
    }
}
