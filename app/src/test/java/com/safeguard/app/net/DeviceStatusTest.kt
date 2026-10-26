package com.safeguard.app.net

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 单元测试：设备状态模型（DeviceStatus）
 * 覆盖默认值与 Gson 序列化往返。
 */
class DeviceStatusTest {

    private val gson = Gson()

    @Test
    fun defaults() {
        val s = DeviceStatus()
        assertEquals("status", s.type)
        assertEquals("", s.deviceId)
        assertEquals(-1, s.battery)
        assertFalse(s.charging)
        assertEquals("idle", s.callState)
        assertFalse(s.hasLocation)
        assertEquals(0, s.scamRisk)
    }

    @Test
    fun gsonRoundTrip_preservesFields() {
        val s = DeviceStatus(
            type = "sos",
            deviceId = "d1",
            deviceName = "爸爸的手机",
            battery = 88,
            charging = false,
            lat = 45.75,
            lng = 126.65,
            accuracy = 5f,
            hasLocation = true,
            callState = "offhook",
            callNumber = "4008205555",
            callContact = "客服",
            scamRisk = 2,
            scamReason = "400 客服热线（注意核实）",
            ts = 123456789L
        )
        val json = gson.toJson(s)
        val back = gson.fromJson(json, DeviceStatus::class.java)
        assertEquals(s, back)
    }

    @Test
    fun unknownFields_ignored() {
        val json = """{"type":"status","deviceId":"x","battery":50,"unknownField":123}"""
        val s = gson.fromJson(json, DeviceStatus::class.java)
        assertEquals("x", s.deviceId)
        assertEquals(50, s.battery)
        assertEquals("status", s.type)
    }
}
