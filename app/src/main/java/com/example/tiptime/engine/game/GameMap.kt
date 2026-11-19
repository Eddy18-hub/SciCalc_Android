package com.example.tiptime.engine.game

import kotlin.math.floor

/**
 * Mapa del nivel como cuadrícula de caracteres:
 *
 *  - `1`, `2`, `3` → paredes de distinto tipo (color)
 *  - `.`           → suelo libre
 *  - `P`           → posición inicial del jugador (suelo)
 *  - `E`           → enemigo (suelo)
 *
 * Todo lo que quede fuera del mapa cuenta como pared, así nadie puede escaparse.
 */
class GameMap(rows: List<String>) {

    val width: Int = rows.maxOf { it.length }
    val height: Int = rows.size

    private val cells: Array<CharArray> = Array(height) { y ->
        CharArray(width) { x -> rows[y].getOrElse(x) { '1' } }
    }

    /** Tipo de pared de la celda (1..3) o 0 si está libre. */
    fun wallAt(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) return 1
        return when (val c = cells[y][x]) {
            '1', '2', '3' -> c - '0'
            else -> 0
        }
    }

    fun isWall(x: Double, y: Double): Boolean = wallAt(floor(x).toInt(), floor(y).toInt()) != 0

    /** Centros de todas las celdas con el carácter [marker]. */
    fun find(marker: Char): List<Pair<Double, Double>> = buildList {
        for (y in 0 until height) for (x in 0 until width) {
            if (cells[y][x] == marker) add((x + 0.5) to (y + 0.5))
        }
    }

    companion object {
        val LEVEL = listOf(
            "1111111111111111",
            "1P.....1.......1",
            "1......1...E...1",
            "1..22..1.......1",
            "1..22.....111..1",
            "1.........1E1..1",
            "1111.1111.1.1111",
            "1....1....1....1",
            "1.E..1.........1",
            "1....1111.1111.1",
            "1.........1..E.1",
            "1..3333...1....1",
            "1..3..3.....E..1",
            "1..3..3...1....1",
            "1.........1....1",
            "1111111111111111",
        )
    }
}
