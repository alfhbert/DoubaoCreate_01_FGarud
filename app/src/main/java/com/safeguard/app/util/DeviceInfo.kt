package com.safeguard.app.util

import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager

object DeviceInfo {

    fun batteryLevel(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    fun isCharging(context: Context): Boolean {
        return try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        } catch (e: Exception) {
            false
        }
    }
}
