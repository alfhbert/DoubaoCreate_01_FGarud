package com.safeguard.app.util

import android.content.Context
import android.content.Intent
import android.os.BatteryManager

object DeviceInfo {

    fun batteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return -1
        return try {
            val extra = context.registerReceiver(
                null,
                Intent(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = extra?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = extra?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (scale <= 0) -1 else (level * 100f / scale).toInt()
        } catch (_: Exception) {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
    }

    fun isCharging(context: Context): Boolean {
        val extra = context.registerReceiver(
            null,
            Intent(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return false
        val status = extra.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
