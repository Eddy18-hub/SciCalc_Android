package com.example.tiptime.data

import android.content.Context
import androidx.core.content.edit
import com.example.tiptime.engine.AngleMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Una operación guardada en el historial. */
data class HistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Long,
)

/**
 * Persistencia ligera (SharedPreferences) del historial y de las preferencias del usuario.
 *
 * Se usa SharedPreferences y no DataStore porque son pocos datos y así el proyecto no necesita
 * dependencias adicionales; las lecturas son síncronas y rápidas (se cachean en memoria).
 */
class CalculatorStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Historial ───────────────────────────────────────────────────────────

    fun loadHistory(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                HistoryEntry(
                    expression = item.getString(JSON_EXPRESSION),
                    result = item.getString(JSON_RESULT),
                    timestamp = item.optLong(JSON_TIMESTAMP),
                )
            }
        } catch (e: JSONException) {
            emptyList() // datos corruptos: se empieza con un historial vacío
        }
    }

    fun saveHistory(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put(JSON_EXPRESSION, entry.expression)
                    .put(JSON_RESULT, entry.result)
                    .put(JSON_TIMESTAMP, entry.timestamp)
            )
        }
        prefs.edit { putString(KEY_HISTORY, array.toString()) }
    }

    // ── Preferencias ────────────────────────────────────────────────────────

    /** `null` significa "seguir el tema del sistema". */
    var darkTheme: Boolean?
        get() = if (prefs.contains(KEY_DARK_THEME)) prefs.getBoolean(KEY_DARK_THEME, false) else null
        set(value) = prefs.edit {
            if (value == null) remove(KEY_DARK_THEME) else putBoolean(KEY_DARK_THEME, value)
        }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }

    var parallaxEnabled: Boolean
        get() = prefs.getBoolean(KEY_PARALLAX, true)
        set(value) = prefs.edit { putBoolean(KEY_PARALLAX, value) }

    var angleMode: AngleMode
        get() = prefs.getString(KEY_ANGLE_MODE, null)
            ?.let { name -> AngleMode.entries.firstOrNull { it.name == name } }
            ?: AngleMode.DEGREES
        set(value) = prefs.edit { putString(KEY_ANGLE_MODE, value.name) }

    private companion object {
        const val PREFS_NAME = "scientific_calculator"
        const val KEY_HISTORY = "history"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_PARALLAX = "parallax_enabled"
        const val KEY_ANGLE_MODE = "angle_mode"
        const val JSON_EXPRESSION = "e"
        const val JSON_RESULT = "r"
        const val JSON_TIMESTAMP = "t"
    }
}
