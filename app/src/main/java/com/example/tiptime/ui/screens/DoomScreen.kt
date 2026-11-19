package com.example.tiptime.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiptime.R
import com.example.tiptime.doom.DoomEngine
import com.example.tiptime.doom.DoomEngine.State

/**
 * Doom real (Mocha Doom, motor en Java dentro de la JVM de Android). Muestra el último cuadro del motor y manda al motor las teclas
 * de los botones táctiles. Si algo falla, [onFallback] pasa al minijuego en Kotlin.
 */
@Composable
fun DoomScreen(
    onExit: () -> Unit,
    onFallback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var status by remember { mutableStateOf(DoomEngine.state) }
    var frameTick by remember { mutableIntStateOf(0) } // cambia con cada cuadro nuevo para redibujar

    // 1) Arrancar el motor (solo la primera vez en este proceso; copia el WAD si hace falta).
    LaunchedEffect(Unit) {
        DoomEngine.start(context)
        status = DoomEngine.state
    }

    // 2) El motor solo corre mientras esta pantalla está visible.
    DisposableEffect(Unit) {
        DoomEngine.setScreenVisible(true)
        onDispose { DoomEngine.setScreenVisible(false) }
    }

    // 3) Cada cuadro de pantalla: leer el estado y, si hay imagen nueva, copiarla al Bitmap.
    var stalled by remember { mutableStateOf(false) } // sin imagen tras 20 s: mostrar diagnóstico
    val bitmap = remember { Bitmap.createBitmap(FRAME_W, FRAME_H, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(FRAME_W * FRAME_H) }
    LaunchedEffect(Unit) {
        val startedAt = android.os.SystemClock.elapsedRealtime()
        var gotFrame = false
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            if (!gotFrame && android.os.SystemClock.elapsedRealtime() - startedAt > 20_000) stalled = true
            val s = DoomEngine.state
            if (s != status) status = s
            if (s == State.RUNNING && DoomEngine.copyFrame(pixels)) {
                bitmap.setPixels(pixels, 0, FRAME_W, 0, 0, FRAME_W, FRAME_H)
                gotFrame = true
                stalled = false
                frameTick++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                status == State.FAILED || stalled -> Message(
                    text = stringResource(R.string.doom_error),
                    details = DoomEngine.diagnostics(),
                    action = stringResource(R.string.game_ok),
                    onAction = onFallback,
                )
                status == State.FINISHED -> Message(
                    text = stringResource(R.string.doom_finished),
                    details = "",
                    action = stringResource(R.string.game_exit),
                    onAction = onExit,
                )
                status != State.RUNNING -> Text(
                    text = stringResource(R.string.doom_loading),
                    color = Color.White,
                )
                else -> {
                    val image = remember(bitmap) { bitmap.asImageBitmap() }
                    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                        frameTick.let { } // leer el estado para que se redibuje en cada cuadro
                        drawImage(
                            image = image,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.None,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.doom_hint),
            color = Color(0xFF90A4AE),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Controls(onExit = onExit)
    }
}

@Composable
private fun Message(text: String, details: String, action: String, onAction: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Text(text = text, color = Color.White, textAlign = TextAlign.Center)
        if (details.isNotEmpty()) {
            // Detalle técnico seleccionable y con scroll: se puede copiar o fotografiar para reportarlo.
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = details,
                    color = Color(0xFFB0BEC5),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                )
            }
        }
        TextButton(onClick = onAction) { Text(action) }
    }
}

/** Cruceta a la izquierda; disparar / usar / menú a la derecha. */
@Composable
private fun Controls(onExit: () -> Unit) {
    fun key(code: DoomEngine.Key): (Boolean) -> Unit = { down -> DoomEngine.key(code, down) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HoldButton("«", key(DoomEngine.Key.STRAFE_LEFT), container = Color(0xFF455A64))
                HoldButton("▲", key(DoomEngine.Key.UP))
                HoldButton("»", key(DoomEngine.Key.STRAFE_RIGHT), container = Color(0xFF455A64))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HoldButton("◀", key(DoomEngine.Key.LEFT))
                HoldButton("▼", key(DoomEngine.Key.DOWN))
                HoldButton("▶", key(DoomEngine.Key.RIGHT))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HoldButton("⏎", key(DoomEngine.Key.ENTER), size = 48)
                HoldButton("Esc", key(DoomEngine.Key.ESCAPE), size = 48)
                HoldButton("✕", { down -> if (!down) onExit() }, size = 48)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HoldButton(stringResource(R.string.game_use), key(DoomEngine.Key.USE), size = 64, container = Color(0xFF1565C0))
                HoldButton(stringResource(R.string.game_fire), key(DoomEngine.Key.FIRE), size = 64, container = Color(0xFFB71C1C))
            }
        }
    }
}

private const val FRAME_W = DoomEngine.FRAME_WIDTH
private const val FRAME_H = DoomEngine.FRAME_HEIGHT
