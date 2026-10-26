package com.safeguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 单元测试：状态中枢（StatusHub，本地实现）
 * 覆盖监听器注册、通知与注销。
 */
class StatusHubTest {

    @Test
    fun listener_notifiedOnChange() {
        var calls = 0
        val l = { calls += 1 }
        StatusHub.addListener(l)
        StatusHub.notifyChanged()
        assertEquals(1, calls)
        StatusHub.removeListener(l)
    }

    @Test
    fun removedListener_noLongerNotified() {
        var calls = 0
        val l = { calls += 1 }
        StatusHub.addListener(l)
        StatusHub.removeListener(l)
        StatusHub.notifyChanged()
        assertEquals(0, calls)
    }

    @Test
    fun multipleListeners_allNotified() {
        var a = 0
        var b = 0
        StatusHub.addListener { a += 1 }
        StatusHub.addListener { b += 1 }
        StatusHub.notifyChanged()
        assertEquals(1, a)
        assertEquals(1, b)
    }

    @Test
    fun listenerException_doesNotBreakOthers() {
        var b = 0
        StatusHub.addListener { throw IllegalStateException("boom") }
        StatusHub.addListener { b += 1 }
        StatusHub.notifyChanged()
        assertEquals(1, b)
    }
}
