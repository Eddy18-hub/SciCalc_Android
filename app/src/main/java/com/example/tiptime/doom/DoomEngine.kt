package com.example.tiptime.doom

import android.content.Context
import android.util.Log
import g.Signals.ScanCode
import java.util.concurrent.atomic.AtomicBoolean
import mochadoom.Engine
import mochadoom.Exit
import mochadoom.Storage

/**
 * Puente Kotlin hacia el motor Doom (Mocha Doom, Java puro, en los paquetes `doom`, `rr`, `v`, ...).
 *
 * Todo corre dentro de la JVM de Android (ART): no hay NDK, CMake ni JNI. El motor vive en su propio
 * hilo; esta clase solo lo arranca, lo pausa, le manda teclas y entrega el último cuadro.
 *
 * Ciclo de vida: el motor se crea una sola vez por proceso. Si el jugador sale desde el menú de Doom
 * ("Quit game") el estado pasa a [State.FINISHED] y no se puede reiniciar hasta que se cierre la app.
 */
object DoomEngine {

    enum class State { IDLE, STARTING, RUNNING, FINISHED, FAILED }

    /**
     * Resolución = 320x200 × este factor. 2 → 640x400 (buen equilibrio entre nitidez y velocidad en
     * el software renderer). Bájalo a 1 si el teléfono va lento; sube a 3 en equipos potentes.
     */
    const val SCALE = 2
    const val FRAME_WIDTH = 320 * SCALE
    const val FRAME_HEIGHT = 200 * SCALE

    @Volatile var state: State = State.IDLE
        private set

    @Volatile var lastError: String = ""
        private set

    private val started = AtomicBoolean(false)

    // ── Último cuadro ────────────────────────────────────────────────────────────────────
    // El hilo del juego escribe en [staging] y lo intercambia con [ready] bajo el candado; la UI copia
    // [ready]. Así la UI nunca ve un cuadro a medio dibujar.
    private val frameLock = Any()
    private var staging = IntArray(FRAME_WIDTH * FRAME_HEIGHT)
    private var ready = IntArray(FRAME_WIDTH * FRAME_HEIGHT)
    private var hasNewFrame = false

    /** Copia el cuadro más reciente (ARGB) a [dst]; `true` si había uno nuevo desde la última vez. */
    fun copyFrame(dst: IntArray): Boolean = synchronized(frameLock) {
        if (!hasNewFrame) return false
        System.arraycopy(ready, 0, dst, 0, minOf(dst.size, ready.size))
        hasNewFrame = false
        true
    }

    private val sink = Engine.FrameSink { engine ->
        engine.writeFrame(staging)
        synchronized(frameLock) {
            val t = ready; ready = staging; staging = t
            hasNewFrame = true
        }
    }

    // ── Arranque ─────────────────────────────────────────────────────────────────────────

    /**
     * Arranca el motor (solo la primera vez en este proceso) en un hilo propio: copia el WAD si hace
     * falta (unos segundos la primera vez), crea el motor y ejecuta su bucle. Vuelve de inmediato;
     * el progreso se ve en [state]. Al no depender de una corrutina de la pantalla, girar el teléfono
     * o salir de la pantalla durante la carga no cancela el arranque.
     */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        state = State.STARTING
        val app = context.applicationContext
        // El motor hace mucha recursión (BSP, clipping): se le da una pila grande, no la de 1 MB.
        engineThread = Thread(null, { boot(app) }, "doom-engine", STACK_BYTES).apply {
            isDaemon = true
            start()
        }
    }

    private fun boot(context: Context) {
        try {
            val wad = DoomLauncher.prepareWadBlocking(context)
            Storage.setDir(DoomLauncher.workDir(context))
            val engine = Engine.create(
                sink,
                "-iwad", wad.absolutePath,
                "-nosound", "-nomusic",
                "-multiply", SCALE.toString(),
            )
            if (engine.screenWidth != FRAME_WIDTH || engine.screenHeight != FRAME_HEIGHT) {
                error("Resolución inesperada ${engine.screenWidth}x${engine.screenHeight}")
            }
            state = State.RUNNING
            applyPause()
            engine.run()
            state = State.FINISHED
        } catch (q: Exit.Quit) {
            if (q.isNormal) state = State.FINISHED else fail(q)
        } catch (t: Throwable) {
            fail(t)
        }
    }

    private fun fail(t: Throwable) {
        // Se guarda la traza completa (con sus causas) para poder mostrarla en pantalla y diagnosticar.
        lastError = buildString {
            var cur: Throwable? = t
            var depth = 0
            while (cur != null && depth < 4) {
                if (depth > 0) append("Causa: ")
                append(cur.javaClass.name).append(": ").append(cur.message ?: "").append('\n')
                cur.stackTrace.take(12).forEach { append("  en ").append(it).append('\n') }
                cur = cur.cause
                depth++
            }
        }
        Log.e("DoomEngine", lastError, t)
        state = State.FAILED
    }

    private var engineThread: Thread? = null

    /**
     * Informe para diagnóstico: estado, último error y, si el motor sigue vivo, dónde está su hilo
     * (sirve cuando el juego "se queda cargando" sin lanzar error).
     */
    fun diagnostics(): String = buildString {
        append("Estado: ").append(state).append('\n')
        append("Android ").append(android.os.Build.VERSION.SDK_INT).append(" · ").append(android.os.Build.MODEL).append('\n')
        if (lastError.isNotEmpty()) append(lastError)
        engineThread?.takeIf { it.isAlive }?.let { t ->
            append("Hilo del motor:\n")
            t.stackTrace.take(25).forEach { append("  en ").append(it).append('\n') }
        }
    }

    // ── Entrada ──────────────────────────────────────────────────────────────────────────

    /** Manda una tecla al juego. Ignorada si el motor aún no corre. */
    fun key(code: Key, pressed: Boolean) {
        if (state != State.RUNNING) return
        Engine.getEngine().postKey(code.scan, pressed)
    }

    /** Teclas que usa la interfaz táctil (los valores son los scancodes del motor). */
    enum class Key(val scan: ScanCode) {
        UP(ScanCode.SC_UP),
        DOWN(ScanCode.SC_DOWN),
        LEFT(ScanCode.SC_LEFT),
        RIGHT(ScanCode.SC_RIGHT),
        FIRE(ScanCode.SC_LCTRL),
        STRAFE_LEFT(ScanCode.SC_A),
        STRAFE_RIGHT(ScanCode.SC_D),
        USE(ScanCode.SC_SPACE),
        ENTER(ScanCode.SC_ENTER),
        ESCAPE(ScanCode.SC_ESCAPE),
    }

    // ── Pausa automática ─────────────────────────────────────────────────────────────────
    // El juego solo avanza mientras su pantalla está visible Y la app está en primer plano; en pausa el
    // hilo del juego queda bloqueado (0 % de CPU).

    @Volatile private var screenVisible = false
    @Volatile private var inForeground = true

    fun setScreenVisible(visible: Boolean) {
        screenVisible = visible
        applyPause()
    }

    fun setAppInForeground(foreground: Boolean) {
        inForeground = foreground
        applyPause()
    }

    private fun applyPause() {
        val paused = !(screenVisible && inForeground)
        // Si había un botón apretado, el "soltar" nunca llegaría con el juego en pausa: se sueltan todos.
        if (paused && state == State.RUNNING) Key.entries.forEach { Engine.getEngine().postKey(it.scan, false) }
        Engine.setPaused(paused)
    }

    private const val STACK_BYTES = 32L * 1024 * 1024
}
