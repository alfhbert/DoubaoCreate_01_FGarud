package com.safeguard.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 模块测试：本地偏好存储（Prefs，本地实现）
 * 覆盖默认值、配对状态（isPaired=pairCode 非空）、黑名单增删查、deviceId 稳定性。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrefsTest {

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() {
        Prefs.init(ctx)
        Prefs.pairCode = ""
    }

    @Test
    fun defaults() {
        assertEquals("我的手机", Prefs.deviceName)
        assertFalse(Prefs.isPaired)
        assertEquals(20, Prefs.lowBatteryThreshold)
        assertTrue(Prefs.largeFont)
        assertFalse(Prefs.safeZoneEnabled)
        assertEquals(500, Prefs.safeZoneRadius)
        assertTrue(Prefs.deviceId.isNotBlank())
    }

    @Test
    fun isPaired_followsPairCode() {
        assertFalse(Prefs.isPaired)
        Prefs.pairCode = "123456"
        assertTrue(Prefs.isPaired)
        Prefs.pairCode = ""
        assertFalse(Prefs.isPaired)
    }

    @Test
    fun blacklist_addListRemove() {
        Prefs.addBlacklist("4008205555")
        Prefs.addBlacklist("13800138000")
        // 返回排序后的列表
        assertEquals(listOf("13800138000", "4008205555"), Prefs.blacklistList())
        Prefs.removeBlacklist("4008205555")
        assertEquals(listOf("13800138000"), Prefs.blacklistList())
    }

    @Test
    fun blacklist_deduplicates() {
        Prefs.addBlacklist("13800138000")
        Prefs.addBlacklist("13800138000")
        assertEquals(1, Prefs.blacklistList().size)
    }

    @Test
    fun deviceId_isStable() {
        assertEquals(Prefs.deviceId, Prefs.deviceId)
    }

    @Test
    fun thresholdRoundtrip() {
        Prefs.lowBatteryThreshold = 30
        assertEquals(30, Prefs.lowBatteryThreshold)
    }
}
