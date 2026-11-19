package com.example.tiptime.sensor

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * Inclinación del teléfono, normalizada a [-1, 1] en cada eje de la pantalla, para el efecto parallax.
 *
 * Usa el acelerómetro (no requiere permisos). Para que el efecto sea sutil y no "se quede pegado"
 * en un extremo cuando el teléfono se sostiene inclinado, se resta una media lenta (la postura
 * habitual) a una media rápida (la postura actual): solo importan los *cambios* de inclinación.
 *
 * [x] y [y] son estados de Compose; leerlos dentro de un bloque de dibujo o de `graphicsLayer`
 * invalida únicamente el dibujo, sin recomponer.
 *
 *  - [x] > 0: la capa debe desplazarse hacia la derecha de la pantalla.
 *  - [y] > 0: la capa debe desplazarse hacia abajo.
 */
class TiltSensor(private val activity: Activity) : SensorEventListener {

    private val sensorManager =
        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** `false` en dispositivos sin acelerómetro (la interfaz usa entonces una deriva suave). */
    val isAvailable: Boolean get() = accelerometer != null

    var x by mutableFloatStateOf(0f)
        private set
    var y by mutableFloatStateOf(0f)
        private set

    private var running = false
    private var initialised = false
    private var fastX = 0f
    private var fastY = 0f
    private var slowX = 0f
    private var slowY = 0f

    fun start() {
        val sensor = accelerometer ?: return
        if (running) return
        initialised = false
        running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        sensorManager.unregisterListener(this)
        running = false
        x = 0f
        y = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Aceleración (m/s²) en los ejes del dispositivo → ejes de la pantalla actual.
        val ax = event.values[0]
        val ay = event.values[1]
        val screenRight: Float
        val screenUp: Float
        when (displayRotation()) {
            Surface.ROTATION_90 -> { screenRight = -ay; screenUp = ax }
            Surface.ROTATION_180 -> { screenRight = -ax; screenUp = -ay }
            Surface.ROTATION_270 -> { screenRight = ay; screenUp = -ax }
            else -> { screenRight = ax; screenUp = ay }
        }

        if (!initialised) {
            fastX = screenRight; slowX = screenRight
            fastY = screenUp; slowY = screenUp
            initialised = true
        }
        fastX += (screenRight - fastX) * FAST_ALPHA
        fastY += (screenUp - fastY) * FAST_ALPHA
        slowX += (screenRight - slowX) * SLOW_ALPHA
        slowY += (screenUp - slowY) * SLOW_ALPHA

        // Las capas "resbalan" hacia el borde que queda más bajo.
        x = (-(fastX - slowX) / FULL_DEFLECTION).coerceIn(-1f, 1f)
        y = ((fastY - slowY) / FULL_DEFLECTION).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun displayRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display?.rotation ?: Surface.ROTATION_0
        } else {
            legacyDisplayRotation()
        }

    @Suppress("DEPRECATION")
    private fun legacyDisplayRotation(): Int = activity.windowManager.defaultDisplay.rotation

    private companion object {
        const val FAST_ALPHA = 0.15f        // suaviza el ruido del sensor
        const val SLOW_ALPHA = 0.004f       // ≈ 5 s para "olvidar" la postura habitual
        const val FULL_DEFLECTION = 3f      // m/s² de cambio (≈ 17°) para el desplazamiento máximo
    }
}
