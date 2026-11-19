package com.example.tiptime.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.tiptime.R

/**
 * Reproduce el "clic" de las teclas con baja latencia (SoundPool).
 *
 * Con un único archivo de audio se consiguen sonidos distintos variando la velocidad de
 * reproducción: las teclas de dígitos suenan normales, los operadores más graves y "="
 * más agudo. Hay que llamar a [release] cuando ya no se use (Activity.onDestroy).
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    @Volatile
    private var loaded = false

    init {
        // El listener se registra ANTES de cargar para no perder el aviso de "carga completa".
        soundPool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
    }

    private val clickSound: Int = soundPool.load(context.applicationContext, R.raw.click, 1)

    /** @param rate velocidad de reproducción (0.5 = una octava más grave, 2.0 = una más aguda). */
    fun playClick(rate: Float = 1f) {
        if (!loaded) return
        soundPool.play(clickSound, 1f, 1f, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val MAX_STREAMS = 4
    }
}
