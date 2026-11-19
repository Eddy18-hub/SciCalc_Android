package com.example.tiptime.engine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoomGameTest {

    private val corridor = GameMap(
        listOf(
            "11111111",
            "1P..E..1",
            "11111111",
        )
    )

    @Test
    fun levelIsRectangularAndEveryEnemyIsReachable() {
        val map = GameMap(GameMap.LEVEL)
        GameMap.LEVEL.forEach { assertEquals(16, it.length) }

        val start = map.find('P').single()
        val seen = HashSet<Pair<Int, Int>>()
        val queue = ArrayDeque(listOf(start.first.toInt() to start.second.toInt()))
        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.removeFirst()
            if (!seen.add(cx to cy) || map.wallAt(cx, cy) != 0) { continue }
            queue.add(cx + 1 to cy); queue.add(cx - 1 to cy); queue.add(cx to cy + 1); queue.add(cx to cy - 1)
        }
        // Nota: 'seen' incluye también paredes alcanzadas; las celdas libres se comprueban aparte.
        map.find('E').forEach { (ex, ey) ->
            assertTrue("enemigo en ($ex,$ey) inalcanzable", (ex.toInt() to ey.toInt()) in seen)
        }
    }

    @Test
    fun rayHitsTheWallAtTheRightDistance() {
        val hit = castRay(corridor, 1.5, 1.5, 0.0)       // hacia el este
        assertEquals(5.5, hit.distance, 1e-9)            // pared en x = 7
        val up = castRay(corridor, 1.5, 1.5, -Math.PI / 2) // hacia el norte
        assertEquals(0.5, up.distance, 1e-9)
    }

    @Test
    fun playerCannotWalkThroughWalls() {
        val game = DoomGame(corridor)
        // Mirando al oeste (π) y caminando 3 s: debe detenerse en la pared.
        game.update(0.0, 0.0, 0.0, false)
        repeat(50) { game.update(0.1, 0.0, 1.0, false) } // gira hasta quedar en otro ángulo
        val xBefore = game.x
        repeat(100) { game.update(0.05, 1.0, 0.0, false) }
        assertTrue(game.x in 1.0..7.0)
        assertTrue(game.y in 1.0..2.0)
        assertTrue(xBefore in 1.0..7.0)
    }

    @Test
    fun shootingTheEnemyInFrontKillsItAndWins() {
        val game = DoomGame(corridor)               // jugador mirando al este, enemigo a 3 celdas
        repeat(3) {
            game.shoot()
            game.update(0.4, 0.0, 0.0, false)       // deja pasar el enfriamiento
        }
        assertEquals(1, game.kills)
        assertEquals(GameStatus.WON, game.status)
        assertEquals(37, game.ammo)
    }

    @Test
    fun wallsBlockShots() {
        val map = GameMap(listOf("11111111", "1P..1E.1", "11111111")) // enemigo tras una pared
        val game = DoomGame(map)
        repeat(5) { game.shoot(); game.update(0.4, 0.0, 0.0, false) }
        assertEquals(0, game.kills)
        assertEquals(GameStatus.PLAYING, game.status)
    }

    @Test
    fun missingDoesNotHit() {
        val game = DoomGame(corridor)
        game.update(0.5, 0.0, 1.0, false)           // gira ≈ 1.1 rad: ya no apunta al enemigo
        repeat(3) { game.shoot(); game.update(0.4, 0.0, 0.0, false) }
        assertEquals(0, game.kills)
    }

    @Test
    fun enemiesHurtAndEventuallyKillAnIdlePlayer() {
        val game = DoomGame(corridor)
        repeat(2000) { game.update(0.05, 0.0, 0.0, false) }
        assertEquals(GameStatus.LOST, game.status)
        assertEquals(0, game.health)
    }

    @Test
    fun resetRestoresInitialState() {
        val game = DoomGame(corridor)
        repeat(3) { game.shoot(); game.update(0.4, 0.0, 0.0, false) }
        game.reset()
        assertEquals(GameStatus.PLAYING, game.status)
        assertEquals(100, game.health)
        assertEquals(40, game.ammo)
        assertEquals(0, game.kills)
        assertEquals(1, game.totalEnemies)
    }

    @Test
    fun outOfAmmoCannotShoot() {
        val map = GameMap(listOf("11111111", "1P..1E.1", "11111111")) // enemigo tras una pared
        val game = DoomGame(map)
        repeat(60) { game.shoot(); game.update(0.4, 0.0, 0.0, false) }
        assertEquals(0, game.ammo)
        assertEquals(40, game.shotsFired)
    }
}
