package com.safeguard.app

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.safeguard.app.net.DeviceStatus
import com.safeguard.app.ui.HomeFragment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 集成测试：主界面（MainActivity，本地实现）
 * 覆盖应用启动、标题、底部导航、默认 Tab、首页 Fragment 挂载，
 * 以及首页对对方设备状态的渲染链路（软件逻辑检测）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

    @After
    fun resetHub() {
        StatusHub.myStatus = null
        StatusHub.remoteStatus = null
        StatusHub.connected = false
    }

    @Test
    fun launch_rendersMainUi() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("安全守护", activity.supportActionBar?.title?.toString())
                assertNotNull(activity.findViewById<BottomNavigationView>(R.id.bottomNav))
                assertNotNull(activity.findViewById<View>(R.id.container))
            }
        }
    }

    @Test
    fun launch_defaultTabIsHome() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                assertEquals(R.id.nav_home, nav.selectedItemId)
            }
        }
    }

    @Test
    fun launch_homeFragmentIsAttached() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val frag = activity.supportFragmentManager.findFragmentById(R.id.container)
                assertNotNull(frag)
                assertTrue(frag is HomeFragment)
            }
        }
    }

    @Test
    fun home_rendersRemoteStatus() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 对方设备：电量 88、正常通话、位置可用、无诈骗风险、状态新鲜
                StatusHub.remoteStatus = DeviceStatus(
                    deviceId = "remote1", deviceName = "爸爸的手机",
                    battery = 88, charging = false,
                    lat = 45.75, lng = 126.65, accuracy = 5f, hasLocation = true,
                    callState = "ringing", scamRisk = 0,
                    ts = System.currentTimeMillis()
                )
                StatusHub.notifyChanged()

                val name = activity.findViewById<TextView>(R.id.textRemoteName)
                assertEquals("爸爸的手机", name.text.toString())
                assertEquals(
                    View.GONE,
                    activity.findViewById<View>(R.id.badgeScam).visibility
                )
                assertEquals(
                    View.GONE,
                    activity.findViewById<View>(R.id.textRemoteOffline).visibility
                )
                assertTrue(activity.findViewById<TextView>(R.id.textRemoteBattery).text.isNotEmpty())
            }
        }
    }

    @Test
    fun home_showsScamBadgeWhenRemoteRisk() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                StatusHub.remoteStatus = DeviceStatus(
                    deviceId = "remote1", deviceName = "爸爸的手机",
                    battery = 60, hasLocation = false,
                    callState = "idle",
                    scamRisk = 1, scamReason = "短信平台号段（106…），谨防验证码类诈骗",
                    ts = System.currentTimeMillis()
                )
                StatusHub.notifyChanged()
                assertEquals(
                    View.VISIBLE,
                    activity.findViewById<View>(R.id.badgeScam).visibility
                )
                val badge = activity.findViewById<TextView>(R.id.badgeScam)
                assertTrue(badge.text.toString().isNotEmpty())
            }
        }
    }
}
