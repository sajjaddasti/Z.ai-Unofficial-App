package com.unofficial.zai

import android.app.Application
import android.content.Context

/**
 * Records the last uncaught exception so the next launch can surface it.
 * Handy in builds where logcat is not reachable.
 */
object CrashStore {
    private const val PREFS = "zai_diag"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val writer = java.io.StringWriter()
                error.printStackTrace(java.io.PrintWriter(writer))
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY, writer.toString())
                    .commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun last(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}

class ZaiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashStore.install(this)
    }
}
