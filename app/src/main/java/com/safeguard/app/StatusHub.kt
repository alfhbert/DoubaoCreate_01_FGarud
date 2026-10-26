package com.safeguard.app

import com.safeguard.app.net.DeviceStatus
import java.util.concurrent.CopyOnWriteArrayList

object StatusHub {
    @Volatile var myStatus: DeviceStatus? = null
    @Volatile var remoteStatus: DeviceStatus? = null
    @Volatile var connected: Boolean = false
    @Volatile var broker: String = ""

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(l: () -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    fun notifyChanged() {
        listeners.forEach { runCatching { it() } }
    }
}
