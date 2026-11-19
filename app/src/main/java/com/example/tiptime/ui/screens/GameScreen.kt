package com.example.tiptime.ui.screens

import android.graphics.Typeface
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiptime.R
import com.example.tiptime.engine.game.DoomGame
import com.example.tiptime.engine.game.GameStatus

/**
 * Huevo de pascua: minijuego de disparos en primera persona (raycasting), activado escribiendo
 * 666 y pulsando "=". La lógica vive en [DoomGame]; esta pantalla solo dibuja y lee los botones.
 *
 * El ciclo de juego usa `withFrameNanos`: se ejecuta una vez por fotograma de pantalla.
 */
@Composable
fun GameScreen(
    game: DoomGame,
    onExit: () -> Unit,
    onSound: (rate: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // La partida es un objeto mutable normal; este contador solo sirve para invalidar el dibujo.
    var frame by remember { mutableLongStateOf(0L) }
    var status by remember { mutableStateOf(game.status) }

    var turnLeft by remember { mutableStateOf(false) }
    var turnRight by remember { mutableStateOf(false) }
    var forward by remember { mutableStateOf(false) }
    var backward by remember { mutableStateOf(false) }
    var firing by remember { mutableStateOf(false) }

    val currentOnSound by rememberUpdatedState(onSound)

    LaunchedEffect(game) {
        var last = withFrameNanos { it }
        var lastShots = game.shotsFired
        var lastHealth = game.health
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).coerceAtMost(0.05) // evita saltos tras una pausa
            last = now

            val move = (if (forward) 1.0 else 0.0) - (if (backward) 1.0 else 0.0)
            val turn = (if (turnRight) 1.0 else 0.0) - (if (turnLeft) 1.0 else 0.0)
            game.update(dt, move, turn, firing)

            if (game.shotsFired != lastShots) currentOnSound(0.5f)
            if (game.health < lastHealth) currentOnSound(0.65f)
            lastShots = game.shotsFired
            lastHealth = game.health

            if (game.status != status) status = game.status
            frame++
        }
    }

    val density = LocalDensity.current
    val hudPaint = remember(density) {
        AndroidPaint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
            textSize = with(density) { 14.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 0f, 0f, Color.Black.toArgb())
        }
    }
    val hud = HudLabels(
        health = stringResource(R.string.game_hp),
        ammo = stringResource(R.string.game_ammo),
        kills = stringResource(R.string.game_kills),
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White,
                    )
                }
                Text(
                    text = stringResource(R.string.game_title),
                    color = Color(0xFFB71C1C),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                @Suppress("UNUSED_VARIABLE")
                val invalidate = frame // leer el contador hace que el lienzo se redibuje en cada fotograma
                drawGame(game, hud, hudPaint)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoldButton("◀", { turnLeft = it })
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HoldButton("▲", { forward = it })
                        HoldButton("▼", { backward = it })
                    }
                    HoldButton("▶", { turnRight = it })
                }
                HoldButton(
                    label = stringResource(R.string.game_fire),
                    onHold = { firing = it },
                    size = 96,
                    container = Color(0xFFB71C1C),
                )
            }
        }

        if (status != GameStatus.PLAYING) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(
                        if (status == GameStatus.WON) R.string.game_won else R.string.game_lost
                    ),
                    color = if (status == GameStatus.WON) Color(0xFF4FD8CC) else Color(0xFFFF5252),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    game.reset()
                    status = game.status
                }) { Text(stringResource(R.string.game_restart)) }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onExit) {
                    Text(stringResource(R.string.game_exit), color = Color.White)
                }
            }
        }
    }
}

/** Botón que está "activo" mientras se mantiene pulsado; cada botón recibe sus propios toques (multitáctil). */
@Composable
internal fun HoldButton(
    label: String,
    onHold: (Boolean) -> Unit,
    size: Int = 56,
    container: Color = Color(0xFF37474F),
) {
    var pressed by remember { mutableStateOf(false) }
    val currentOnHold by rememberUpdatedState(onHold)

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container.copy(alpha = if (pressed) 1f else 0.7f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        currentOnHold(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                            currentOnHold(false)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
