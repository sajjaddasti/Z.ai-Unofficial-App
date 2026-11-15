package com.unofficial.zai

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class ZaiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force dark/light follows system; Material You dynamic colors handled in theme.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
}
