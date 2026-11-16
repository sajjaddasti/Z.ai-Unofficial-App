package com.unofficial.zai.ui.theme

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode { System, Light, Dark }

/** Tiny persisted preference so the appearance choice survives restarts. */
object ThemePrefs {
    private const val FILE = "zai_prefs"
    private const val KEY_MODE = "theme_mode"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(context: Context): ThemeMode {
        val raw = prefs(context).getString(KEY_MODE, ThemeMode.System.name)
        return runCatching { ThemeMode.valueOf(raw ?: ThemeMode.System.name) }
            .getOrDefault(ThemeMode.System)
    }

    fun write(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }
}
