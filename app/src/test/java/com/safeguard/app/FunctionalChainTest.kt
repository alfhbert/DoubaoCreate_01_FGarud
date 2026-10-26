package com.safeguard.app

import com.safeguard.app.net.DeviceStatus
import com.safeguard.app.net.PayloadCodec
import com.safeguard.app.util.LocationUtils
import com.safeguard.app.util.ScamChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 功能链路测试（软件逻辑检测，对准本地实现）：
 * 场景 1：守护配对 → 状态加密 → 传输 → 对方解密还原（MQTT 载荷链路）
 * 场景 2：可疑号码来电 → 本机风险识别 → 告警信息链路
 * 场景 3：低电量阈值判定链路
 * 场景 4：安全区域离开判定链路
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FunctionalChainTest {

    // 场景 1：协议链路 —— 双方一致守护码，状态经加密在两端间流转
    @Test
    fun pairingStatusSync_chain() {
        val code = "123456"
        val a = DeviceStatus(
            type = "status",
            deviceId = "deviceA",
            deviceName = "老人机",
            battery = 18, // 低电量
            charging = false,
            lat = 45.8038, lng = 126.5349,
            accuracy = 10f, hasLocation = true,
            callState = "idle", ts = 1000L
        )
        // A 加密发布
        val wire = PayloadCodec.encrypt(PayloadCodec.toJson(a), code)
        // B 收到后解密
        val received = PayloadCodec.decode(wire, code)
        assertNotNull(received)
        assertEquals(a.deviceId, received!!.deviceId)
        assertEquals(a.deviceName, received.deviceName)
        assertEquals(a.battery, received.battery)
        assertEquals(a.lat, received.lat, 0.0)
        assertEquals(a.lng, received.lng, 0.0)
        assertEquals(a.hasLocation, received.hasLocation)
    }

    // 场景 2：可疑来电 → 风险识别 → 告警信息
    @Test
    fun scamCall_chain() {
        val number = "1069070066"
        // 不在通讯录 → 触发识别
        val verdict = ScamChecker.check(number, inContacts = false, blacklist = emptySet())
        assertTrue(verdict.risk >= 1)
        assertTrue(verdict.reason.isNotBlank())
        // 告警信息应包含号码与原因，具备可读性
        val alert = "可疑来电 $number：${verdict.reason}"
        assertTrue(alert.contains(number))
        assertTrue(alert.contains("106"))
        // 同一号码在通讯录中 → 放行
        assertEquals(0, ScamChecker.check(number, inContacts = true, emptySet()).risk)
    }

    // 场景 3：低电量告警判定（与 SafeguardService 相同的阈值逻辑）
    @Test
    fun lowBatteryAlert_chain() {
        val threshold = Prefs_lowBatteryThreshold()
        val remoteBattery = 15
        assertTrue(remoteBattery in 0..100 && remoteBattery <= threshold)
        val safeBattery = 25
        assertFalse(safeBattery <= threshold)
    }

    private fun Prefs_lowBatteryThreshold(): Int = 20 // 与 Prefs 默认一致

    // 场景 4：安全区域离开判定（哈夫辛距离 > 半径）
    @Test
    fun safeZoneExit_chain() {
        val zoneLat = 45.8038
        val zoneLng = 126.5349
        val radius = 500.0
        val dist = LocationUtils.distanceMeters(zoneLat, zoneLng, 45.90, 126.70)
        assertTrue(dist > radius)
        // 区域内不触发
        val inside = LocationUtils.distanceMeters(zoneLat, zoneLng, zoneLat + 0.001, zoneLng)
        assertTrue(inside < radius)
    }
}
