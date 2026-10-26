package com.safeguard.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 模块测试：事件存储模块（EventsStore，本地实现）
 * 覆盖增删查、最新在前、200 条上限。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EventsStoreTest {

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        EventsStore.clear(ctx)
    }

    @Test
    fun add_load_latestFirst_clear() {
        EventsStore.add(ctx, "sos", "t1", "d1")
        EventsStore.add(ctx, "scam", "t2", "d2")
        val list = EventsStore.load(ctx)
        assertEquals(2, list.size)
        assertEquals("t2", list[0].title) // 最新在前
        assertEquals("d1", list[1].detail)
        assertEquals("sos", list[1].kind)
        EventsStore.clear(ctx)
        assertTrue(EventsStore.load(ctx).isEmpty())
    }

    @Test
    fun capAt200Entries() {
        repeat(250) { EventsStore.add(ctx, "kind", "t$it", "d") }
        assertEquals(200, EventsStore.load(ctx).size)
    }

    @Test
    fun load_whenEmpty_returnsEmpty() {
        assertTrue(EventsStore.load(ctx).isEmpty())
    }

    @Test
    fun corruptedJson_fallsBackToEmpty() {
        ctx.getSharedPreferences("safeguard", Context.MODE_PRIVATE)
            .edit().putString("events_v1", "not-json{{").apply()
        assertTrue(EventsStore.load(ctx).isEmpty())
    }
}
