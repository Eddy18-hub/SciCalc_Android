package com.example.tiptime.engine.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Resultado de lanzar un rayo.
 *
 * @property distance distancia euclidiana desde el origen hasta la pared.
 * @property side 0 si el rayo golpeó una cara "vertical" (norte-sur), 1 si fue "horizontal"; sirve para sombrear.
 * @property wallType tipo de pared (1..3).
 */
class RayHit(val distance: Double, val side: Int, val wallType: Int)

private const val MAX_DISTANCE = 64.0
private const val MAX_STEPS = 128

/**
 * Lanza un rayo desde ([originX], [originY]) con dirección [angle] (radianes) y devuelve la primera
 * pared que toca. Usa el algoritmo DDA: avanza celda por celda en lugar de muestrear, por lo que
 * es exacto y rápido (se llama una vez por columna de pantalla en cada fotograma).
 */
fun castRay(map: GameMap, originX: Double, originY: Double, angle: Double): RayHit {
    val dirX = cos(angle)
    val dirY = sin(angle)

    var cellX = floor(originX).toInt()
    var cellY = floor(originY).toInt()

    // Distancia a recorrer en el rayo para cruzar una celda completa en cada eje.
    val deltaX = if (dirX == 0.0) Double.MAX_VALUE else abs(1.0 / dirX)
    val deltaY = if (dirY == 0.0) Double.MAX_VALUE else abs(1.0 / dirY)

    val stepX = if (dirX < 0) -1 else 1
    val stepY = if (dirY < 0) -1 else 1
    var sideDistX = if (dirX < 0) (originX - cellX) * deltaX else (cellX + 1.0 - originX) * deltaX
    var sideDistY = if (dirY < 0) (originY - cellY) * deltaY else (cellY + 1.0 - originY) * deltaY

    var side = 0
    repeat(MAX_STEPS) {
        if (sideDistX < sideDistY) {
            sideDistX += deltaX
            cellX += stepX
            side = 0
        } else {
            sideDistY += deltaY
            cellY += stepY
            side = 1
        }
        val wall = map.wallAt(cellX, cellY)
        if (wall != 0) {
            val distance = if (side == 0) sideDistX - deltaX else sideDistY - deltaY
            return RayHit(distance, side, wall)
        }
    }
    return RayHit(MAX_DISTANCE, 0, 1)
}
