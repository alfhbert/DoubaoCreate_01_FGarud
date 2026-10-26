package com.safeguard.app.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("safeguard", Context.MODE_PRIVATE)
    }

    val deviceId: String
        get() = sp.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            sp.edit().putString("device_id", it).apply()
        }

    var deviceName: String
        get() = sp.getString("device_name", "我的手机") ?: "我的手机"
        set(v) {
            sp.edit().putString("device_name", v).apply()
        }

    var pairCode: String
        get() = sp.getString("pair_code", "") ?: ""
        set(v) {
            sp.edit().putString("pair_code", v).apply()
        }

    val isPaired: Boolean
        get() = pairCode.isNotBlank()

    var serviceEnabled: Boolean
        get() = sp.getBoolean("service_enabled", true)
        set(v) {
            sp.edit().putBoolean("service_enabled", v).apply()
        }

    var largeFont: Boolean
        get() = sp.getBoolean("large_font", true)
        set(v) {
            sp.edit().putBoolean("large_font", v).apply()
        }

    var safeZoneEnabled: Boolean
        get() = sp.getBoolean("safe_zone_enabled", false)
        set(v) {
            sp.edit().putBoolean("safe_zone_enabled", v).apply()
        }

    var safeZoneLat: Double
        get() = sp.getFloat("safe_zone_lat", 0f).toDouble()
        set(v) {
            sp.edit().putFloat("safe_zone_lat", v.toFloat()).apply()
        }

    var safeZoneLng: Double
        get() = sp.getFloat("safe_zone_lng", 0f).toDouble()
        set(v) {
            sp.edit().putFloat("safe_zone_lng", v.toFloat()).apply()
        }

    var safeZoneRadius: Int
        get() = sp.getInt("safe_zone_radius", 500)
        set(v) {
            sp.edit().putInt("safe_zone_radius", v).apply()
        }

    var lowBatteryThreshold: Int
        get() = sp.getInt("low_battery_threshold", 20)
        set(v) {
            sp.edit().putInt("low_battery_threshold", v).apply()
        }

    var bgLocationRequested: Boolean
        get() = sp.getBoolean("bg_location_requested", false)
        set(v) {
            sp.edit().putBoolean("bg_location_requested", v).apply()
        }

    fun blacklistList(): List<String> {
        val s = sp.getStringSet("blacklist", mutableSetOf()) ?: mutableSetOf()
        return s.toList().sorted()
    }

    fun addBlacklist(number: String) {
        val s = sp.getStringSet("blacklist", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        s.add(number)
        sp.edit().putStringSet("blacklist", s).apply()
    }

    fun removeBlacklist(number: String) {
        val s = sp.getStringSet("blacklist", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        s.remove(number)
        sp.edit().putStringSet("blacklist", s).apply()
    }
}
