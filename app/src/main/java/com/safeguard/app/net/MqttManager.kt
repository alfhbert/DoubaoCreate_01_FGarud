package com.safeguard.app.net

import android.os.Handler
import android.os.Looper
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlin.math.min

class MqttManager {

    interface Listener {
        fun onRemoteStatus(s: DeviceStatus)
        fun onRemoteSos(s: DeviceStatus)
        fun onConnectionChanged(connected: Boolean, broker: String)
    }

    private val brokerList = listOf(
        "tcp://broker.emqx.io:1883",
        "tcp://broker.hivemq.com:1883",
        "tcp://test.mosquitto.org:1883"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var client: MqttAsyncClient? = null
    private var listener: Listener? = null
    private var connected = false
    private var shouldRun = false
    private var connecting = false
    private var brokerIdx = 0
    private var reconnectDelayMs = 3000L
    private var code = ""
    private var deviceId = ""
    @Volatile
    private var lastSent: DeviceStatus? = null

    fun setListener(l: Listener?) {
        listener = l
    }

    fun currentBroker(): String = brokerList[brokerIdx]

    fun connect(code: String, deviceId: String) {
        this.code = code
        this.deviceId = deviceId
        shouldRun = true
        handler.post { doConnect() }
    }

    fun disconnect() {
        shouldRun = false
        handler.removeCallbacksAndMessages(null)
        connected = false
        connecting = false
        try {
            client?.disconnect()
        } catch (_: Exception) {
        }
        try {
            client?.close()
        } catch (_: Exception) {
        }
        client = null
    }

    fun publish(s: DeviceStatus) {
        lastSent = s
        if (!connected || code.isBlank()) return
        try {
            val topic = "safeguard/$code/dev/$deviceId"
            val payload = PayloadCodec.encrypt(PayloadCodec.toJson(s), code)
            client?.publish(topic, payload.toByteArray(Charsets.UTF_8), 1, false)
        } catch (_: Exception) {
        }
    }

    private fun doConnect() {
        if (!shouldRun || connecting || connected) return
        connecting = true
        try {
            val opts = MqttConnectOptions().apply {
                isCleanSession = true
                keepAliveInterval = 30
                connectionTimeout = 10
                isAutomaticReconnect = false
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
            }
            val c = MqttAsyncClient(brokerList[brokerIdx], "sg-${deviceId.take(20)}", MemoryPersistence())
            client = c
            c.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    connected = false
                    connecting = false
                    handler.post {
                        listener?.onConnectionChanged(false, currentBroker())
                        scheduleReconnect()
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage) {
                    val text = String(message.payload, Charsets.UTF_8)
                    handler.post { handleIncoming(text) }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })
            c.connect(opts, null, object : IMqttActionListener {
                override fun onSuccess(token: IMqttToken?) {
                    connecting = false
                    connected = true
                    reconnectDelayMs = 3000L
                    handler.post {
                        listener?.onConnectionChanged(true, currentBroker())
                        subscribe()
                        lastSent?.let { publish(it) }
                    }
                }

                override fun onFailure(token: IMqttToken?, exception: Throwable?) {
                    connecting = false
                    connected = false
                    handler.post { scheduleReconnect() }
                }
            })
        } catch (e: Exception) {
            connecting = false
            handler.post { scheduleReconnect() }
        }
    }

    private fun subscribe() {
        if (code.isBlank()) return
        try {
            client?.subscribe("safeguard/$code/dev/+", 1)
        } catch (_: Exception) {
        }
    }

    private fun scheduleReconnect() {
        if (!shouldRun) return
        handler.postDelayed({
            if (!connected && !connecting && shouldRun) {
                brokerIdx = (brokerIdx + 1) % brokerList.size
                reconnectDelayMs = min(reconnectDelayMs * 2, 60_000L)
                doConnect()
            }
        }, reconnectDelayMs)
    }

    private fun handleIncoming(text: String) {
        if (code.isBlank()) return
        val s = PayloadCodec.decode(text, code) ?: return
        if (s.deviceId == deviceId) return
        if (s.type == "sos") {
            listener?.onRemoteSos(s)
        } else {
            listener?.onRemoteStatus(s)
        }
    }
}
