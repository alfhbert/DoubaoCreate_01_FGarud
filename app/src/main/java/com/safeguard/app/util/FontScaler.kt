package com.safeguard.app.util

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.app.AppCompatDelegate

object FontScaler {

    fun apply(context: Context) {
        val config = context.resources.configuration
        val scale = if (Prefs.bigFont) 1.3f else 1.0f
        if (config.fontScale != scale) {
            config.fontScale = scale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
