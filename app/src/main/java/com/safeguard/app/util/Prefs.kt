package com.safeguard.app.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object Prefs {

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
    }

    val deviceId: String by lazy {
        sp.getString("device_id", null) ?: UUID.randomUUID().toString().replace("-", "")
            .also { sp.edit().putString("device_id", it).apply() }
    }

    var deviceName: String
        get() = sp.getString("device_name", "我的手机") ?: "我的手机"
        set(v) = sp.edit().putString("device_name", v).apply()

    var pairCode: String
        get() = sp.getString("pair_code", "") ?: ""
        set(v) = sp.edit().putString("pair_code", v).apply()

    var paired: Boolean
        get() = sp.getBoolean("paired", false)
        set(v) = sp.edit().putBoolean("paired", v).apply()

    val isPaired: Boolean get() = paired && pairCode.isNotBlank()

    var serviceEnabled: Boolean
        get() = sp.getBoolean("service_enabled", true)
        set(v) = sp.edit().putBoolean("service_enabled", v).apply()

    var bigFont: Boolean
        get() = sp.getBoolean("big_font", true)
        set(v) = sp.edit().putBoolean("big_font", v).apply()

    var lowBatteryThreshold: Int
        get() = sp.getInt("low_battery_threshold", 20)
        set(v) = sp.edit().putInt("low_battery_threshold", v).apply()

    var safeZoneEnabled: Boolean
        get() = sp.getBoolean("safe_zone_enabled", false)
        set(v) = sp.edit().putBoolean("safe_zone_enabled", v).apply()

    var safeZoneLat: Double
        get() = sp.getFloat("safe_zone_lat", 0f).toDouble()
        set(v) = sp.edit().putFloat("safe_zone_lat", v.toFloat()).apply()

    var safeZoneLng: Double
        get() = sp.getFloat("safe_zone_lng", 0f).toDouble()
        set(v) = sp.edit().putFloat("safe_zone_lng", v.toFloat()).apply()

    var safeZoneRadius: Int
        get() = sp.getInt("safe_zone_radius", 500)
        set(v) = sp.edit().putInt("safe_zone_radius", v).apply()

    var locationGranted: Boolean
        get() = sp.getBoolean("loc_granted", false)
        set(v) = sp.edit().putBoolean("loc_granted", v).apply()

    var backgroundLocationGranted: Boolean
        get() = sp.getBoolean("bg_loc_granted", false)
        set(v) = sp.edit().putBoolean("bg_loc_granted", v).apply()

    var notificationGranted: Boolean
        get() = sp.getBoolean("notif_granted", false)
        set(v) = sp.edit().putBoolean("notif_granted", v).apply()

    var phoneGranted: Boolean
        get() = sp.getBoolean("phone_granted", false)
        set(v) = sp.edit().putBoolean("phone_granted", v).apply()

    var contactsGranted: Boolean
        get() = sp.getBoolean("contacts_granted", false)
        set(v) = sp.edit().putBoolean("contacts_granted", v).apply()

    fun blacklistList(): List<String> {
        val raw = sp.getString("blacklist", null) ?: return emptyList()
        return try {
            com.google.gson.Gson().fromJson(raw, Array<String>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun blacklistSave(list: List<String>) {
        sp.edit().putString("blacklist", com.google.gson.Gson().toJson(list)).apply()
    }
}
