package com.safeguard.app

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.safeguard.app.util.Prefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 集成测试：守护配对页（PairActivity，本地实现）
 * 覆盖生成守护码、开始配对、加入配对（合法/非法）、解除配对。
 * 测试结束主动停止守护服务，避免遗留 MQTT 线程。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PairActivityTest {

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun cleanup() {
        ctx.stopService(Intent(ctx, SafeguardService::class.java))
        Prefs.pairCode = ""
    }

    @Test
    fun createCode_shows6Digits_andStartPair_pairs() {
        ActivityScenario.launch(PairActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnCreate).performClick()
                val code = activity.findViewById<TextView>(R.id.textPairCode).text.toString()
                assertTrue(code.matches(Regex("^\\d{6}$")))
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btnStartPair).visibility)

                activity.findViewById<Button>(R.id.btnStartPair).performClick()
                assertEquals(code, Prefs.pairCode)
                assertTrue(Prefs.isPaired)
            }
        }
    }

    @Test
    fun invalidJoinCode_rejected_keepsUnpaired() {
        ActivityScenario.launch(PairActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.editCode).setText("123")
                activity.findViewById<Button>(R.id.btnJoin).performClick()
            }
            assertFalse(Prefs.isPaired)
            assertEquals("", Prefs.pairCode)
        }
    }

    @Test
    fun blankJoinCode_rejected_keepsUnpaired() {
        ActivityScenario.launch(PairActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.editCode).setText("   ")
                activity.findViewById<Button>(R.id.btnJoin).performClick()
            }
            assertFalse(Prefs.isPaired)
        }
    }

    @Test
    fun validJoinCode_acceptsAndPairs() {
        ActivityScenario.launch(PairActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.editCode).setText("654321")
                activity.findViewById<Button>(R.id.btnJoin).performClick()
            }
            assertTrue(Prefs.isPaired)
            assertEquals("654321", Prefs.pairCode)
        }
    }

    @Test
    fun alreadyPaired_showsUnpair_andUnpairClearsCode() {
        Prefs.pairCode = "123456"
        ActivityScenario.launch(PairActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btnUnpairPair).visibility)
                activity.findViewById<Button>(R.id.btnUnpairPair).performClick()
            }
            assertFalse(Prefs.isPaired)
            assertEquals("", Prefs.pairCode)
        }
    }
}
