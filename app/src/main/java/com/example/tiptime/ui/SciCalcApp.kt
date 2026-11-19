package com.example.tiptime.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.tiptime.R
import com.example.tiptime.audio.SoundManager
import com.example.tiptime.sensor.TiltSensor
import com.example.tiptime.ui.components.ParallaxBackground
import com.example.tiptime.ui.screens.CalculatorActions
import com.example.tiptime.ui.screens.CalculatorScreen
import com.example.tiptime.ui.screens.DoomScreen
import com.example.tiptime.ui.screens.GameScreen
import com.example.tiptime.ui.screens.GraphActions
import com.example.tiptime.ui.screens.GraphScreen
import com.example.tiptime.ui.screens.ToolsScreen
import com.example.tiptime.ui.theme.SciCalcTheme

/**
 * Raíz de la interfaz: tema, fondo con parallax y navegación entre calculadora y gráficas.
 * Aquí se conectan los eventos de la UI con los ViewModels, el sonido y la vibración.
 */
@Composable
fun SciCalcApp(
    calculator: CalculatorViewModel,
    graph: GraphViewModel,
    game: GameViewModel,
    tilt: TiltSensor,
    sound: SoundManager,
    reduceMotion: Boolean,
) {
    val calcState = calculator.state
    val settings = calculator.settings
    val isDark = settings.darkTheme ?: isSystemInDarkTheme()
    val parallaxActive = settings.parallaxEnabled && !reduceMotion
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Si el Doom real falla (sin WAD, sin memoria...), se usa el minijuego en Kotlin.
    var useKotlinGame by remember { mutableStateOf(false) }

    // El acelerómetro solo se usa mientras el parallax está activo.
    LaunchedEffect(parallaxActive) {
        if (parallaxActive) tilt.start() else tilt.stop()
    }

    // El cambio de tema necesita saber si ahora se ve oscuro (puede venir del sistema).
    val actions = remember(calculator, sound, haptic, isDark, context) {
        CalculatorActions(
            onKey = { key ->
                if (calculator.settings.soundEnabled) sound.playClick(key.soundRate())
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                calculator.onKey(key)
            },
            onToggleAngle = calculator::toggleAngleMode,
            onToggleHistory = calculator::toggleHistory,
            onOpenGraph = calculator::openGraph,
            onToggleTheme = { calculator.toggleDarkTheme(currentlyDark = isDark) },
            onToggleSound = calculator::toggleSound,
            onToggleParallax = calculator::toggleParallax,
            onRecallExpression = calculator::recallExpression,
            onUseResult = calculator::useHistoryResult,
            onClearHistory = calculator::clearHistory,
            onOpenTools = calculator::openTools,
        )
    }

    val graphActions = remember(graph, calculator) {
        GraphActions(
            onBack = calculator::showCalculator,
            onFunctionF = graph::setFunctionF,
            onFunctionG = graph::setFunctionG,
            onToggleAngle = graph::toggleAngleMode,
            onResetView = graph::resetView,
            onTransform = graph::transform,
            onTrace = graph::setTrace,
            onToggleAnalysis = graph::toggleAnalysis,
            onToggleIntegral = graph::toggleIntegral,
            onIntegralA = graph::setIntegralA,
            onIntegralB = graph::setIntegralB,
        )
    }

    BackHandler(enabled = calcState.screen != Screen.CALCULATOR || calcState.historyOpen) {
        if (calcState.screen != Screen.CALCULATOR) calculator.showCalculator() else calculator.closeHistory()
    }

    SciCalcTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                ParallaxBackground(tilt = tilt, enabled = parallaxActive)

                AnimatedContent(
                    targetState = calcState.screen,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.CALCULATOR -> CalculatorScreen(
                            state = calcState,
                            settings = settings,
                            isDark = isDark,
                            tilt = tilt,
                            parallaxActive = parallaxActive,
                            actions = actions,
                            modifier = Modifier.safeDrawingPadding(),
                        )
                        Screen.GRAPH -> GraphScreen(
                            state = graph.state,
                            actions = graphActions,
                            modifier = Modifier.safeDrawingPadding(),
                        )
                        Screen.TOOLS -> ToolsScreen(
                            onBack = calculator::showCalculator,
                            modifier = Modifier.safeDrawingPadding(),
                        )
                        Screen.GAME -> if (useKotlinGame) {
                            GameScreen(
                                game = game.game,
                                onExit = calculator::showCalculator,
                                onSound = { rate ->
                                    if (calculator.settings.soundEnabled) sound.playClick(rate)
                                },
                                modifier = Modifier.safeDrawingPadding(),
                            )
                        } else {
                            DoomScreen(
                                onExit = calculator::showCalculator,
                                onFallback = { useKotlinGame = true },
                                modifier = Modifier.safeDrawingPadding(),
                            )
                        }
                    }
                }
            }
        }
    }
}
