package com.safeguard.app

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.safeguard.app.net.DeviceStatus
import com.safeguard.app.net.MqttManager
import com.safeguard.app.util.DeviceInfo
import com.safeguard.app.util.EventsStore
import com.safeguard.app.util.LocationUtils
import com.safeguard.app.util.Prefs
import com.safeguard.app.util.ScamChecker

class SafeguardService : Service(), MqttManager.Listener {

    companion object {
        const val ACTION_START = "com.safeguard.app.START"
        const val ACTION_STOP = "com.safeguard.app.STOP"
        const val ACTION_SOS = "com.safeguard.app.SOS"
        private const val NOTIF_ID = 1
        private const val MIN_PUBLISH_MS = 5_000L
        private const val ALERT_INTERVAL_MS = 60_000L

        fun start(context: Context) {
            val i = Intent(context, SafeguardService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, SafeguardService::class.java).setAction(ACTION_STOP))
        }

        fun sendSos(context: Context) {
            context.startService(Intent(context, SafeguardService::class.java).setAction(ACTION_SOS))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mqtt: MqttManager
    private lateinit var locationManager: LocationManager
    private var telephonyManager: TelephonyManager? = null
    private var phoneListener: PhoneStateListener? = null
    private val lastAlertTime = HashMap<String, Long>()

    private var lastBattery = -1
    private var lastCharging = false
    private var lastCallState = "idle"
    private var lastNumber: String? = null
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastAccuracy = 0f
    private var hasLocation = false
    private var lastPublishTime = 0L
    private var isRunning = false

    private val heartbeat = object : Runnable {
        override fun run() {
            if (isRunning) {
                publishNow()
                handler.postDelayed(this, 20_000L)
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastBattery = DeviceInfo.batteryLevel(context)
            lastCharging = DeviceInfo.isCharging(context)
            checkOwnLowBattery()
            publishThrottled()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLat = location.latitude
            lastLng = location.longitude
            lastAccuracy = location.accuracy
            hasLocation = true
            checkOwnSafeZone()
            publishThrottled()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mqtt = MqttManager()
        mqtt.setListener(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SOS -> {
                startEverything()
                sendSos()
                return START_STICKY
            }
            else -> startEverything()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------- 启动与停止 ----------

    private fun startEverything() {
        if (isRunning) return
        isRunning = true
        startForegroundCompat()

        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )

        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 10_000L, 0f, locationListener, Looper.getMainLooper()
                )
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 15_000L, 0f, locationListener, Looper.getMainLooper()
                )
            } catch (_: Exception) {
            }
        }

        if (hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            try {
                telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                phoneListener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                        when (state) {
                            TelephonyManager.CALL_STATE_RINGING -> {
                                lastCallState = "ringing"
                                if (!incomingNumber.isNullOrBlank()) {
                                    lastNumber = incomingNumber
                                }
                                checkScamCall()
                                publishNow()
                            }
                            TelephonyManager.CALL_STATE_OFFHOOK -> {
                                lastCallState = "offhook"
                                publishNow()
                            }
                            else -> {
                                lastCallState = "idle"
                                publishNow()
                            }
                        }
                    }
                }
                telephonyManager?.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE)
            } catch (_: Exception) {
            }
        }

        lastBattery = DeviceInfo.batteryLevel(this)
        lastCharging = DeviceInfo.isCharging(this)

        if (Prefs.isPaired) {
            mqtt.connect(Prefs.pairCode, Prefs.deviceId)
        }
        publishNow()
        handler.post(heartbeat)
        updateForegroundNotif()
    }

    private fun stopEverything() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        try {
            mqtt.disconnect()
        } catch (_: Exception) {
        }
        try {
            telephonyManager?.listen(phoneListener, PhoneStateListener.LISTEN_NONE)
        } catch (_: Exception) {
        }
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: Exception) {
        }
        StatusHub.connected = false
        StatusHub.notifyChanged()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startForegroundCompat() {
        val notif = buildForegroundNotif()
        try {
            if (Build.VERSION.SDK_INT >= 29 &&
                (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
            ) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            // 前台服务启动被系统拒绝（如权限不足）：降级为先提示，不再启动
            isRunning = false
            stopSelf()
        }
    }

    private fun buildForegroundNotif(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = when {
            !Prefs.isPaired -> getString(R.string.service_state_unpaired)
            StatusHub.connected -> getString(
                R.string.service_state,
                Prefs.deviceName
            )
            else -> getString(R.string.service_state_connecting)
        }
        return NotificationCompat.Builder(this, App.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_guard)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun updateForegroundNotif() {
        try {
            if (hasPermission(Manifest.permission.POST_NOTIFICATIONS) ||
                Build.VERSION.SDK_INT < 33
            ) {
                NotificationManagerCompat.from(this).notify(NOTIF_ID, buildForegroundNotif())
            }
        } catch (_: Exception) {
        }
    }

    // ---------- 状态发布 ----------

    private fun currentStatus(type: String = "status"): DeviceStatus {
        val number = lastNumber
        val contact = if (number.isNullOrBlank()) null else contactName(number)
        return DeviceStatus(
            type = type,
            deviceId = Prefs.deviceId,
            deviceName = Prefs.deviceName,
            battery = lastBattery,
            charging = lastCharging,
            lat = lastLat,
            lng = lastLng,
            accuracy = lastAccuracy,
            hasLocation = hasLocation,
            callState = lastCallState,
            callNumber = if (number.isNullOrBlank()) null else ScamChecker.normalize(number),
            callContact = contact,
            scamRisk = scamRiskNow(),
            scamReason = scamReasonNow(),
            ts = System.currentTimeMillis()
        )
    }

    private fun scamRiskNow(): Int {
        if (lastCallState == "idle" || lastNumber.isNullOrBlank()) return 0
        val inContacts = contactName(lastNumber) != null
        return ScamChecker.check(lastNumber, inContacts, Prefs.blacklistList().toSet()).risk
    }

    private fun scamReasonNow(): String {
        if (lastCallState == "idle" || lastNumber.isNullOrBlank()) return ""
        val inContacts = contactName(lastNumber) != null
        return ScamChecker.check(lastNumber, inContacts, Prefs.blacklistList().toSet()).reason
    }

    private fun publishNow() {
        if (!Prefs.isPaired) {
            StatusHub.myStatus = currentStatus()
            StatusHub.notifyChanged()
            return
        }
        val s = currentStatus()
        mqtt.publish(s)
        lastPublishTime = System.currentTimeMillis()
        StatusHub.myStatus = s
        StatusHub.notifyChanged()
        updateForegroundNotif()
    }

    private fun publishThrottled() {
        if (System.currentTimeMillis() - lastPublishTime >= MIN_PUBLISH_MS) {
            publishNow()
        }
    }

    // ---------- 对方状态处理 ----------

    override fun onRemoteStatus(s: DeviceStatus) {
        StatusHub.remoteStatus = s
        StatusHub.notifyChanged()
        evaluateRemote(s)
    }

    override fun onRemoteSos(s: DeviceStatus) {
        StatusHub.remoteStatus = s
        StatusHub.notifyChanged()
        val loc = if (s.hasLocation) {
            "位置：${String.format(java.util.Locale.CHINA, "%.4f, %.4f", s.lat, s.lng)}"
        } else {
            "位置：对方暂未开启定位"
        }
        notify(App.CH_SOS, "【SOS】${s.deviceName}发出紧急求助！", loc)
        EventsStore.add(this, "sos", "收到 ${s.deviceName} 的 SOS 求助", loc)
    }

    override fun onConnectionChanged(connected: Boolean, broker: String) {
        StatusHub.connected = connected
        StatusHub.broker = broker
        StatusHub.notifyChanged()
        updateForegroundNotif()
    }

    private fun evaluateRemote(s: DeviceStatus) {
        val threshold = Prefs.lowBatteryThreshold
        if (s.battery in 0..100 && s.battery <= threshold) {
            alert(
                "battery",
                "battery",
                "${s.deviceName}电量低",
                "当前 ${s.battery}%，请提醒尽快充电"
            )
        }
        if (Prefs.safeZoneEnabled && s.hasLocation &&
            (Prefs.safeZoneLat != 0.0 || Prefs.safeZoneLng != 0.0)
        ) {
            val dist = LocationUtils.distanceMeters(
                s.lat, s.lng, Prefs.safeZoneLat, Prefs.safeZoneLng
            )
            if (dist > Prefs.safeZoneRadius) {
                alert(
                    "geofence",
                    "geofence",
                    "${s.deviceName}离开安全区域",
                    "距安全中心约 ${LocationUtils.formatDistance(dist)}"
                )
            }
        }
        if (s.scamRisk > 0 && (s.callState == "ringing" || s.callState == "offhook")) {
            alert(
                "scam",
                "scam",
                "${s.deviceName}可能正接听可疑电话",
                "${s.scamReason} · 号码 ${s.callNumber ?: "未知"}"
            )
        }
    }

    private fun alert(key: String, kind: String, title: String, detail: String) {
        val now = System.currentTimeMillis()
        val last = lastAlertTime[key] ?: 0L
        if (now - last < ALERT_INTERVAL_MS) return
        lastAlertTime[key] = now
        notify(App.CH_ALERTS, title, detail)
        EventsStore.add(this, kind, title, detail)
    }

    // ---------- 本机检查 ----------

    private fun checkOwnLowBattery() {
        if (lastBattery in 0..100 && lastBattery <= Prefs.lowBatteryThreshold) {
            alert(
                "own_battery",
                "battery",
                "本机电量低",
                "当前 ${lastBattery}%，请注意充电"
            )
        }
    }

    private fun checkOwnSafeZone() {
        if (Prefs.safeZoneEnabled && hasLocation &&
            (Prefs.safeZoneLat != 0.0 || Prefs.safeZoneLng != 0.0)
        ) {
            val dist = LocationUtils.distanceMeters(
                lastLat, lastLng, Prefs.safeZoneLat, Prefs.safeZoneLng
            )
            if (dist > Prefs.safeZoneRadius) {
                alert(
                    "own_geofence",
                    "geofence",
                    "本机离开安全区域",
                    "距安全中心约 ${LocationUtils.formatDistance(dist)}"
                )
            }
        }
    }

    private fun checkScamCall() {
        if (lastNumber.isNullOrBlank()) return
        val contact = contactName(lastNumber)
        val verdict = ScamChecker.check(
            lastNumber, contact != null, Prefs.blacklistList().toSet()
        )
        if (verdict.risk > 0) {
            notify(
                App.CH_SCAM,
                "疑似诈骗来电：${lastNumber}",
                "${verdict.reason} · 请勿转账或提供验证码，与家人核实"
            )
            EventsStore.add(
                this, "scam",
                "可疑来电 ${lastNumber}",
                verdict.reason
            )
        }
    }

    // ---------- SOS ----------

    private fun sendSos() {
        if (!Prefs.isPaired) {
            notify(App.CH_ALERTS, "尚未配对", "请先完成守护配对后再使用 SOS")
            return
        }
        val s = currentStatus("sos")
        mqtt.publish(s)
        val loc = if (s.hasLocation) {
            "位置：${String.format(java.util.Locale.CHINA, "%.4f, %.4f", s.lat, s.lng)}"
        } else {
            "位置：暂未获取到定位"
        }
        notify(App.CH_SOS, "SOS 求助已发送", "$loc · 对方手机将收到提醒")
        EventsStore.add(this, "sos", "本机发出 SOS 求助", loc)
    }

    // ---------- 通知与工具 ----------

    private fun notify(channel: String, title: String, text: String) {
        try {
            if (hasPermission(Manifest.permission.POST_NOTIFICATIONS) ||
                Build.VERSION.SDK_INT < 33
            ) {
                val pi = PendingIntent.getActivity(
                    this, channel.hashCode(),
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
                val n = NotificationCompat.Builder(this, channel)
                    .setSmallIcon(R.drawable.ic_stat_guard)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setPriority(
                        if (channel == App.CH_ALERTS || channel == App.CH_SOS || channel == App.CH_SCAM)
                            NotificationCompat.PRIORITY_HIGH
                        else NotificationCompat.PRIORITY_LOW
                    )
                    .build()
                NotificationManagerCompat.from(this).notify(channel.hashCode(), n)
            }
        } catch (_: Exception) {
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun contactName(number: String?): String? {
        if (number.isNullOrBlank()) return null
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return null
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                arrayOf("%${ScamChecker.normalize(number)}%"),
                null
            ) ?: return null
            cursor.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
