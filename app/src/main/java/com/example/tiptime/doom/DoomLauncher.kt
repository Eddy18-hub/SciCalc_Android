package com.example.tiptime.doom

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Prepara los archivos que necesita el motor. Doom lee su WAD como archivo normal, así que no puede usar
 * directamente el empaquetado en `assets/`: se copia una sola vez a la carpeta privada de la app.
 *
 * WAD que se usa (en este orden):
 *  1. `DOOM.WAD` (juego completo) si el usuario lo copió a la carpeta de archivos de la app:
 *     `Android/data/com.example.tiptime/files/DOOM.WAD`.
 *  2. `doom1.wad`, el Doom original **shareware** (episodio 1), incluido en `assets/` — id Software
 *     permite distribuirlo sin modificar (ver assets/DOOM1-SHAREWARE-NOTICE.txt).
 */
object DoomLauncher {

    private const val WAD_ASSET = "doom1.wad"
    private const val FULL_WAD = "DOOM.WAD"

    /** Carpeta donde el motor guarda su configuración y partidas. */
    fun workDir(context: Context): File =
        File(context.filesDir, "doom").also { it.mkdirs() }

    /** Devuelve el WAD a usar, copiando el empaquetado la primera vez (~4 MB). Bloquea: no usar en el hilo principal. */
    @Throws(IOException::class)
    fun prepareWadBlocking(context: Context): File {
        val full = context.getExternalFilesDir(null)?.let { File(it, FULL_WAD) }
        if (full != null && full.isFile && full.length() > 0L) return full
        val target = File(workDir(context), WAD_ASSET)
        if (!target.exists() || target.length() == 0L) {
            // Se copia a un temporal y se renombra: si la app se cierra a medias no queda un WAD corrupto.
            val temp = File(target.parentFile, "$WAD_ASSET.tmp")
            context.assets.open(WAD_ASSET).use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temp.renameTo(target)) throw IOException("No se pudo guardar $WAD_ASSET")
        }
        return target
    }
}
