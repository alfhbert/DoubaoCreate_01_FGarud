package com.safeguard.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模块测试：反诈识别模块（ScamChecker，本地实现）
 * 覆盖号码归一化、境外/号段识别、黑名单优先级、通讯录放行。
 * 风险分级：0=安全，1=警惕，2=警告（黑名单最高）。
 */
class ScamCheckerTest {

    @Test
    fun normalize_stripsSeparators() {
        assertEquals("13800138000", ScamChecker.normalize("138-0013-8000"))
        assertEquals("13800138000", ScamChecker.normalize("138 (0013) 8000"))
        assertEquals("+8613800138000", ScamChecker.normalize("+86 138 0013 8000"))
    }

    @Test
    fun blankAndNull_areSafe() {
        assertEquals(0, ScamChecker.check(null, false, emptySet()).risk)
        assertEquals(0, ScamChecker.check("", false, emptySet()).risk)
        assertEquals(0, ScamChecker.check("   ", false, emptySet()).risk)
    }

    @Test
    fun normalMainlandNumber_isSafe() {
        assertEquals(0, ScamChecker.check("13800138000", false, emptySet()).risk)
    }

    @Test
    fun emergencyNumbers_areSafe() {
        assertEquals(0, ScamChecker.check("110", false, emptySet()).risk)
        assertEquals(0, ScamChecker.check("119", false, emptySet()).risk)
        assertEquals(0, ScamChecker.check("120", false, emptySet()).risk)
        assertEquals(0, ScamChecker.check("122", false, emptySet()).risk)
    }

    @Test
    fun plus86MainlandPrefix_isDomestic() {
        assertEquals(0, ScamChecker.check("+8613800138000", false, emptySet()).risk)
        assertEquals(0, ScamChecker.check("008613800138000", false, emptySet()).risk)
    }

    @Test
    fun overseasCall_warning() {
        val v = ScamChecker.check("0012345678901", false, emptySet())
        assertEquals(2, v.risk)
        assertTrue(v.reason.contains("境外"))
        val v2 = ScamChecker.check("+0012345678901", false, emptySet())
        assertEquals(2, v2.risk)
    }

    @Test
    fun smsSender106_caution() {
        val v = ScamChecker.check("1069070066", false, emptySet())
        assertEquals(1, v.risk)
        assertTrue(v.reason.contains("106"))
    }

    @Test
    fun serviceHotline400_caution() {
        val v = ScamChecker.check("4008205555", false, emptySet())
        assertEquals(1, v.risk)
        assertTrue(v.reason.contains("400"))
    }

    @Test
    fun freeHotline800_caution() {
        assertEquals(1, ScamChecker.check("8008208820", false, emptySet()).risk)
    }

    @Test
    fun service95_caution() {
        assertEquals(1, ScamChecker.check("955880", false, emptySet()).risk)
    }

    @Test
    fun abnormalLongNumber_caution() {
        val v = ScamChecker.check("1234567890123", false, emptySet())
        assertEquals(1, v.risk)
        assertTrue(v.reason.contains("偏长"))
    }

    @Test
    fun numberInContacts_isSafe() {
        assertEquals(0, ScamChecker.check("1069070066", true, emptySet()).risk)
    }

    @Test
    fun blacklist_isHighestRisk() {
        val v = ScamChecker.check("13800138000", false, setOf("13800138000"))
        assertEquals(2, v.risk)
        assertTrue(v.reason.contains("黑名单"))
    }

    @Test
    fun blacklist_overridesContacts() {
        assertEquals(2, ScamChecker.check("13800138000", true, setOf("13800138000")).risk)
    }
}
