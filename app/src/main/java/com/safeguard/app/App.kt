package com.safeguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.safeguard.app.util.Prefs
import org.osmdroid.config.Configuration

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        createChannels()
        Configuration.getInstance().userAgentValue = "SafetyGuard/1.0"
    }

    private fun createChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CH_SERVICE,
                "守护服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "后台守护服务运行状态" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_ALERTS,
                "守护提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "电量、位置、可疑来电等提醒"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_SOS,
                "紧急求助",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "对方发出的 SOS 紧急求助"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_SCAM,
                "防诈骗提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "疑似诈骗来电提醒"
                enableVibration(true)
            }
        )
    }

    companion object {
        const val CH_SERVICE = "service"
        const val CH_ALERTS = "alerts"
        const val CH_SOS = "sos"
        const val CH_SCAM = "scam"
    }
}
