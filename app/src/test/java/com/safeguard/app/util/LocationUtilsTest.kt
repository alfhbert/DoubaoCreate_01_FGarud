package com.safeguard.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模块测试：定位工具模块（LocationUtils，本地实现）
 */
class LocationUtilsTest {

    @Test
    fun distance_samePoint_isZero() {
        assertEquals(0.0, LocationUtils.distanceMeters(45.0, 126.0, 45.0, 126.0), 0.001)
    }

    @Test
    fun distance_HarbinToBeijing_about1000km() {
        val d = LocationUtils.distanceMeters(45.8038, 126.5349, 39.9042, 116.4074)
        assertTrue("实际距离 $d 米", d in 1_000_000.0..1_150_000.0)
    }

    @Test
    fun distance_orderInvariant() {
        val d1 = LocationUtils.distanceMeters(45.0, 126.0, 44.0, 125.0)
        val d2 = LocationUtils.distanceMeters(44.0, 125.0, 45.0, 126.0)
        assertEquals(d1, d2, 0.001)
    }

    @Test
    fun format_underKilometer_usesMeters() {
        assertEquals("500米", LocationUtils.formatDistance(500.0))
        assertEquals("999米", LocationUtils.formatDistance(999.9))
    }

    @Test
    fun format_overKilometer_usesKilometers() {
        assertEquals("2.5公里", LocationUtils.formatDistance(2500.0))
        assertEquals("12.3公里", LocationUtils.formatDistance(12300.0))
    }

    @Test
    fun formatTime_isHHmm() {
        assertTrue(LocationUtils.formatTime(System.currentTimeMillis()).matches(Regex("^\\d{2}:\\d{2}$")))
    }
}
