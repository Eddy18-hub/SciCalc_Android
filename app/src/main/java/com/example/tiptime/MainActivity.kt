package com.example.tiptime

import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.tiptime.audio.SoundManager
import com.example.tiptime.doom.DoomEngine
import com.example.tiptime.sensor.TiltSensor
import com.example.tiptime.ui.CalculatorViewModel
import com.example.tiptime.ui.GameViewModel
import com.example.tiptime.ui.GraphViewModel
import com.example.tiptime.ui.SciCalcApp

/**
 * Activity única. Hereda de AppCompatActivity (no de ComponentActivity) para que el cambio de
 * idioma dentro de la app (AppCompatDelegate.setApplicationLocales) funcione en todas las versiones.
 */
class MainActivity : AppCompatActivity() {

    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private val graphViewModel: GraphViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()

    private lateinit var tilt: TiltSensor
    private lateinit var sound: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        tilt = TiltSensor(this)
        sound = SoundManager(this)

        setContent {
            SciCalcApp(
                calculator = calculatorViewModel,
                graph = graphViewModel,
                game = gameViewModel,
                tilt = tilt,
                sound = sound,
                reduceMotion = isReduceMotionRequested(),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        DoomEngine.setAppInForeground(true)
        if (calculatorViewModel.settings.parallaxEnabled && !isReduceMotionRequested()) tilt.start()
    }

    override fun onPause() {
        DoomEngine.setAppInForeground(false)
        tilt.stop()
        super.onPause()
    }

    override fun onDestroy() {
        sound.release()
        super.onDestroy()
    }

    /** "Quitar animaciones" en Accesibilidad / Opciones de desarrollador desactiva el parallax. */
    private fun isReduceMotionRequested(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
