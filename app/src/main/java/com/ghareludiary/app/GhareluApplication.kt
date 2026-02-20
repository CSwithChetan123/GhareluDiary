package com.ghareludiary.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GhareluApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ═══════════════════════════════════════════════════════════════
        // CRITICAL: Force light mode to disable dynamic colors
        // ═══════════════════════════════════════════════════════════════
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // DO NOT call this - it enables Material You dynamic colors:
        // DynamicColors.applyToActivitiesIfAvailable(this)

        android.util.Log.d("GhareluApplication", "✅ App initialized - Light mode FORCED")
        android.util.Log.d("GhareluApplication", "✅ Dynamic colors DISABLED")
    }
}