package com.example.tiptime.ui.screens

import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.example.tiptime.engine.game.DoomGame
import com.example.tiptime.engine.game.castRay
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** Textos del marcador, ya traducidos. */
internal class HudLabels(val health: String, val ammo: String, val kills: String)

private val CeilingTop = Color(0xFF0B0B10)
private val CeilingBottom = Color(0xFF2A2A33)
private val FloorTop = Color(0xFF1B1511)
private val FloorBottom = Color(0xFF5A4636)

private fun wallColor(type: Int): Color = when (type) {
    2 -> Color(0.62f, 0.20f, 0.18f)   // ladrillo rojo
    3 -> Color(0.18f, 0.45f, 0.55f)   // panel tecnológico
    else -> Color(0.62f, 0.55f, 0.48f) // piedra
}

/**
 * Dibuja un fotograma en primera persona con la técnica de raycasting (la de Wolfenstein 3D):
 * un rayo por columna de pantalla; la altura de la pared es inversamente proporcional a la
 * distancia. Los enemigos son "sprites" dibujados por columnas y recortados con el z-buffer
 * (la distancia de pared de cada columna) para que las paredes los tapen.
 */
internal fun DrawScope.drawGame(game: DoomGame, hud: HudLabels, hudPaint: AndroidPaint) {
    val w = size.width
    val h = size.height
    val k = DoomGame.FIELD_OF_VIEW_SCALE

    // Techo y suelo
    drawRect(
        brush = Brush.verticalGradient(listOf(CeilingTop, CeilingBottom), startY = 0f, endY = h / 2f),
        size = Size(w, h / 2f),
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(FloorTop, FloorBottom), startY = h / 2f, endY = h),
        topLeft = Offset(0f, h / 2f),
        size = Size(w, h / 2f),
    )

    // Paredes
    val columnWidth = max(3f, w / 200f)
    val columns = ceil(w / columnWidth).toInt()
    val zBuffer = DoubleArray(columns)
    for (i in 0 until columns) {
        val cameraX = 2.0 * (i + 0.5) / columns - 1.0          // -1 (izquierda) … 1 (derecha)
        val rayAngle = game.angle + atan(cameraX * k)
        val hit = castRay(game.map, game.x, game.y, rayAngle)
        // La distancia perpendicular corrige el efecto "ojo de pez".
        val depth = (hit.distance * cos(rayAngle - game.angle)).coerceAtLeast(0.05)
        zBuffer[i] = depth

        val wallHeight = (h / depth).toFloat()
        val shade = (1.0 / (1.0 + depth * 0.22)).toFloat() * (if (hit.side == 1) 0.7f else 1f)
        val base = wallColor(hit.wallType)
        drawRect(
            color = Color(base.red * shade, base.green * shade, base.blue * shade),
            topLeft = Offset(i * columnWidth, (h - wallHeight) / 2f),
            size = Size(columnWidth + 1f, wallHeight),
        )
    }

    // Enemigos, del más lejano al más cercano
    val c = cos(game.angle)
    val s = sin(game.angle)
    game.enemies
        .filter { it.alive }
        .map { enemy ->
            val dx = enemy.x - game.x
            val dy = enemy.y - game.y
            Triple(enemy, c * dx + s * dy, -s * dx + c * dy) // (enemigo, profundidad, desplazamiento lateral)
        }
        .filter { it.second > 0.2 }
        .sortedByDescending { it.second }
        .forEach { (enemy, depth, lateral) ->
            val centerX = (w / 2.0 * (1.0 + lateral / (depth * k))).toFloat()
            val spriteHeight = (h / depth).toFloat()
            val spriteWidth = spriteHeight * 0.8f
            val left = centerX - spriteWidth / 2f
            val top = (h - spriteHeight) / 2f
            val shade = (1.0 / (1.0 + depth * 0.22)).toFloat().coerceIn(0.25f, 1f)
            val flash = enemy.hurtTimer > 0.0

            val first = floor(left / columnWidth).toInt().coerceAtLeast(0)
            val last = ceil((left + spriteWidth) / columnWidth).toInt().coerceAtMost(columns - 1)
            for (col in first..last) {
                if (depth >= zBuffer[col]) continue // hay una pared delante
                val x = col * columnWidth
                val u = (x + columnWidth / 2f - left) / spriteWidth // 0..1 a lo ancho del sprite
                drawEnemyStrip(x, columnWidth + 1f, top, spriteHeight, u, shade, flash)
            }
        }

    // Arma, destello y mira
    val gunWidth = w * 0.07f
    val gunTop = h - h * (if (game.muzzleFlash > 0.0) 0.20f else 0.17f)
    if (game.muzzleFlash > 0.0) {
        val center = Offset(w / 2f, gunTop - h * 0.02f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFF2A0), Color(0x99FF9A1F), Color.Transparent),
                center = center,
                radius = h * 0.12f,
            ),
            radius = h * 0.12f,
            center = center,
        )
    }
    drawRect(Color(0xFF3A3A40), Offset(w / 2f - gunWidth / 2f, gunTop), Size(gunWidth, h - gunTop))
    drawRect(Color(0xFF5A5A63), Offset(w / 2f - gunWidth * 0.8f, gunTop + h * 0.07f), Size(gunWidth * 1.6f, h))
    val arm = 8f
    drawLine(Color.White.copy(alpha = 0.8f), Offset(w / 2f - arm, h / 2f), Offset(w / 2f + arm, h / 2f), strokeWidth = 2f)
    drawLine(Color.White.copy(alpha = 0.8f), Offset(w / 2f, h / 2f - arm), Offset(w / 2f, h / 2f + arm), strokeWidth = 2f)

    // Destello rojo al recibir daño
    if (game.hurtFlash > 0.0) {
        drawRect(Color.Red.copy(alpha = (game.hurtFlash * 1.6).toFloat().coerceIn(0f, 0.45f)))
    }

    // Marcador
    val native = drawContext.canvas.nativeCanvas
    val margin = hudPaint.textSize
    native.drawText("${hud.health} ${game.health}", margin, margin * 1.6f, hudPaint)
    native.drawText("${hud.ammo} ${game.ammo}", margin, margin * 3.0f, hudPaint)
    native.drawText("${hud.kills} ${game.kills}/${game.totalEnemies}", margin, margin * 4.4f, hudPaint)
}

/** Una columna vertical del sprite del enemigo: cabeza, cuerpo, brazos y ojos según [u] (0..1 a lo ancho). */
private fun DrawScope.drawEnemyStrip(
    x: Float,
    width: Float,
    top: Float,
    height: Float,
    u: Float,
    shade: Float,
    flash: Boolean,
) {
    fun tint(r: Float, g: Float, b: Float) =
        if (flash) Color.White else Color(r * shade, g * shade, b * shade)

    if (u in 0.15f..0.85f) { // torso y brazos
        val arms = u < 0.26f || u > 0.74f
        val color = if (arms) tint(0.55f, 0.12f, 0.10f) else tint(0.75f, 0.16f, 0.12f)
        drawRect(color, Offset(x, top + height * 0.30f), Size(width, height * 0.65f))
    }
    if (u in 0.32f..0.68f) { // cabeza
        drawRect(tint(0.82f, 0.32f, 0.22f), Offset(x, top + height * 0.05f), Size(width, height * 0.27f))
    }
    if (u in 0.40f..0.47f || u in 0.53f..0.60f) { // ojos
        drawRect(if (flash) Color.White else Color(1f, 0.9f, 0.2f), Offset(x, top + height * 0.14f), Size(width, height * 0.05f))
    }
}
