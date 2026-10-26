package com.safeguard.app.net

import android.util.Base64
import com.google.gson.Gson
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class DeviceStatus(
    val type: String = "status",
    val deviceId: String = "",
    val deviceName: String = "",
    val battery: Int = -1,
    val charging: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Float = 0f,
    val hasLocation: Boolean = false,
    val callState: String = "idle",
    val callNumber: String? = null,
    val callContact: String? = null,
    val scamRisk: Int = 0,
    val scamReason: String = "",
    val ts: Long = System.currentTimeMillis()
)

object PayloadCodec {
    private val gson = Gson()

    fun toJson(s: DeviceStatus): String = gson.toJson(s)

    fun encrypt(plain: String, code: String): String {
        val key = deriveKey(code)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    fun decode(b64: String, code: String): DeviceStatus? {
        return try {
            val raw = Base64.decode(b64, Base64.NO_WRAP)
            if (raw.size <= 12) {
                null
            } else {
                val iv = raw.copyOfRange(0, 12)
                val ct = raw.copyOfRange(12, raw.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(code), GCMParameterSpec(128, iv))
                val plain = String(cipher.doFinal(ct), Charsets.UTF_8)
                gson.fromJson(plain, DeviceStatus::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun deriveKey(code: String): SecretKeySpec {
        val md = MessageDigest.getInstance("SHA-256")
        var bytes = md.digest("safeguard:v1:$code".toByteArray(Charsets.UTF_8))
        // 迭代散列（10,000 轮），提高暴力猜测 6 位守护码的成本
        repeat(10_000) {
            bytes = md.digest(bytes)
        }
        return SecretKeySpec(bytes, "AES")
    }
}
