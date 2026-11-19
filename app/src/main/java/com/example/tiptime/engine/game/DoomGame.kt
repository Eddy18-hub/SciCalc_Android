package com.example.tiptime.engine.game

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class GameStatus { PLAYING, WON, LOST }

class Enemy(var x: Double, var y: Double) {
    var health = ENEMY_HEALTH
    var alerted = false
    var attackCooldown = 0.0
    /** Segundos restantes del destello blanco al recibir un disparo. */
    var hurtTimer = 0.0
    val alive: Boolean get() = health > 0
}

private const val ENEMY_HEALTH = 3

/**
 * Lógica del minijuego (huevo de pascua estilo Doom). Kotlin puro, sin dependencias de Android,
 * así que se puede probar sin emulador. La interfaz solo dibuja lo que expone esta clase.
 *
 * Cada fotograma la UI llama a [update] con el tiempo transcurrido y el estado de los controles.
 */
class DoomGame(
    val map: GameMap = GameMap(GameMap.LEVEL),
) {
    var x = 0.0; private set
    var y = 0.0; private set
    /** Dirección en radianes; crece hacia la derecha del jugador (el eje y del mapa apunta hacia abajo). */
    var angle = 0.0; private set
    var health = 100; private set
    var ammo = 40; private set
    var kills = 0; private set
    var shotsFired = 0; private set
    var status = GameStatus.PLAYING; private set
    var muzzleFlash = 0.0; private set
    var hurtFlash = 0.0; private set
    var enemies: List<Enemy> = emptyList(); private set

    val totalEnemies: Int get() = enemies.size

    private var fireCooldown = 0.0

    init {
        reset()
    }

    fun reset() {
        val start = map.find('P').firstOrNull() ?: (1.5 to 1.5)
        x = start.first
        y = start.second
        angle = 0.0
        health = 100
        ammo = 40
        kills = 0
        shotsFired = 0
        status = GameStatus.PLAYING
        muzzleFlash = 0.0
        hurtFlash = 0.0
        fireCooldown = 0.0
        enemies = map.find('E').map { Enemy(it.first, it.second) }
    }

    /**
     * @param dt segundos desde el fotograma anterior.
     * @param forward -1 (atrás) … 1 (adelante).
     * @param turn -1 (izquierda) … 1 (derecha).
     * @param fire `true` mientras el botón de disparo está pulsado.
     */
    fun update(dt: Double, forward: Double, turn: Double, fire: Boolean) {
        if (status != GameStatus.PLAYING) return

        angle = normalize(angle + turn * TURN_SPEED * dt)
        move(cos(angle) * forward * MOVE_SPEED * dt, sin(angle) * forward * MOVE_SPEED * dt)

        fireCooldown = (fireCooldown - dt).coerceAtLeast(0.0)
        muzzleFlash = (muzzleFlash - dt).coerceAtLeast(0.0)
        hurtFlash = (hurtFlash - dt).coerceAtLeast(0.0)
        if (fire) shoot()

        enemies.forEach { updateEnemy(it, dt) }

        if (health <= 0) {
            health = 0
            status = GameStatus.LOST
        } else if (enemies.none { it.alive }) {
            status = GameStatus.WON
        }
    }

    /** Disparo instantáneo: golpea al enemigo más cercano que esté en la mira y no tenga una pared delante. */
    fun shoot() {
        if (status != GameStatus.PLAYING || ammo <= 0 || fireCooldown > 0.0) return
        ammo--
        shotsFired++
        fireCooldown = FIRE_COOLDOWN
        muzzleFlash = 0.1

        val wallDistance = castRay(map, x, y, angle).distance
        val target = enemies
            .filter { it.alive }
            .mapNotNull { enemy ->
                val dx = enemy.x - x
                val dy = enemy.y - y
                val distance = hypot(dx, dy)
                val offset = normalize(atan2(dy, dx) - angle)
                val tolerance = atan(ENEMY_RADIUS / distance)
                if (distance < wallDistance && kotlin.math.abs(offset) < tolerance) enemy to distance else null
            }
            .minByOrNull { it.second }
            ?.first ?: return

        target.health--
        target.hurtTimer = 0.12
        target.alerted = true
        if (!target.alive) kills++
    }

    private fun updateEnemy(enemy: Enemy, dt: Double) {
        enemy.hurtTimer = (enemy.hurtTimer - dt).coerceAtLeast(0.0)
        if (!enemy.alive) return
        enemy.attackCooldown = (enemy.attackCooldown - dt).coerceAtLeast(0.0)

        val dx = x - enemy.x
        val dy = y - enemy.y
        val distance = hypot(dx, dy)

        if (!enemy.alerted && distance < SIGHT_RANGE) {
            val toPlayer = atan2(dy, dx)
            // Te ve si no hay pared entre los dos.
            if (castRay(map, enemy.x, enemy.y, toPlayer).distance > distance) enemy.alerted = true
        }
        if (!enemy.alerted) return

        if (distance > ATTACK_RANGE) {
            val stepX = dx / distance * ENEMY_SPEED * dt
            val stepY = dy / distance * ENEMY_SPEED * dt
            if (!blocked(enemy.x + stepX, enemy.y, ENEMY_RADIUS * 0.8)) enemy.x += stepX
            if (!blocked(enemy.x, enemy.y + stepY, ENEMY_RADIUS * 0.8)) enemy.y += stepY
        } else if (enemy.attackCooldown <= 0.0) {
            health -= ENEMY_DAMAGE
            hurtFlash = 0.25
            enemy.attackCooldown = ENEMY_ATTACK_INTERVAL
        }
    }

    /** Mueve al jugador por ejes separados para poder "resbalar" a lo largo de las paredes. */
    private fun move(dx: Double, dy: Double) {
        if (!blocked(x + dx, y, PLAYER_RADIUS)) x += dx
        if (!blocked(x, y + dy, PLAYER_RADIUS)) y += dy
    }

    private fun blocked(px: Double, py: Double, radius: Double): Boolean =
        map.isWall(px - radius, py - radius) || map.isWall(px + radius, py - radius) ||
            map.isWall(px - radius, py + radius) || map.isWall(px + radius, py + radius)

    private fun normalize(a: Double): Double {
        var r = a % (2 * PI)
        if (r > PI) r -= 2 * PI
        if (r < -PI) r += 2 * PI
        return r
    }

    companion object {
        const val FIELD_OF_VIEW_SCALE = 0.66   // longitud del "plano de cámara" (≈ 66° de campo visual)
        const val ENEMY_RADIUS = 0.3
        const val PLAYER_RADIUS = 0.2
        private const val MOVE_SPEED = 3.0
        private const val TURN_SPEED = 2.2
        private const val FIRE_COOLDOWN = 0.3
        private const val ENEMY_SPEED = 1.2
        private const val ENEMY_DAMAGE = 8
        private const val ENEMY_ATTACK_INTERVAL = 1.0
        private const val ATTACK_RANGE = 0.9
        private const val SIGHT_RANGE = 9.0
    }
}
