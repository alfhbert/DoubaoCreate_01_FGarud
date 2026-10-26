package com.safeguard.app.util

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

object FontScaler {

    fun apply(root: View, factor: Float) {
        if (factor <= 0f || factor == 1f) return
        walk(root, factor)
    }

    private fun walk(v: View, factor: Float) {
        if (v is TextView) {
            val sp = v.textSize / v.resources.displayMetrics.scaledDensity
            v.textSize = sp * factor
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                walk(v.getChildAt(i), factor)
            }
        }
    }
}
