package com.safeguard.app.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 模块测试：加密载荷模块（PayloadCodec）
 * 覆盖 AES-256-GCM 加解密往返、篡改检测、错误守护码、垃圾输入。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PayloadCodecTest {

    private fun sample() = DeviceStatus(
        type = "status",
        deviceId = "abc123def456",
        deviceName = "妈妈的手机",
        battery = 67,
        charging = true,
        lat = 45.8038,
        lng = 126.5349,
        accuracy = 12f,
        hasLocation = true,
        callState = "ringing",
        callNumber = "1069070066",
        callContact = null,
        scamRisk = 3,
        scamReason = "疑似短信平台号段（106）来电",
        ts = 1_700_000_000_000L
    )

    @Test
    fun encryptDecode_roundTrip_preservesAllFields() {
        val s = sample()
        val cipher = PayloadCodec.encrypt(PayloadCodec.toJson(s), "123456")
        val decoded = PayloadCodec.decode(cipher, "123456")
        assertNotNull(decoded)
        assertEquals(s.deviceId, decoded!!.deviceId)
        assertEquals(s.deviceName, decoded.deviceName)
        assertEquals(s.battery, decoded.battery)
        assertEquals(s.charging, decoded.charging)
        assertEquals(s.lat, decoded.lat, 0.0)
        assertEquals(s.lng, decoded.lng, 0.0)
        assertEquals(s.accuracy, decoded.accuracy, 0.0f)
        assertEquals(s.hasLocation, decoded.hasLocation)
        assertEquals(s.callState, decoded.callState)
        assertEquals(s.callNumber, decoded.callNumber)
        assertEquals(s.callContact, decoded.callContact)
        assertEquals(s.scamRisk, decoded.scamRisk)
        assertEquals(s.scamReason, decoded.scamReason)
        assertEquals(s.ts, decoded.ts)
    }

    @Test
    fun wrongCode_failsToDecode() {
        val cipher = PayloadCodec.encrypt(PayloadCodec.toJson(sample()), "123456")
        assertNull(PayloadCodec.decode(cipher, "654321"))
    }

    @Test
    fun tamperedCipher_failsToDecode() {
        val cipher = PayloadCodec.encrypt(PayloadCodec.toJson(sample()), "123456")
        val bytes = android.util.Base64.decode(cipher, android.util.Base64.NO_WRAP)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()
        val tampered = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        assertNull(PayloadCodec.decode(tampered, "123456"))
    }

    @Test
    fun garbageInput_failsGracefully() {
        assertNull(PayloadCodec.decode("", "123456"))
        assertNull(PayloadCodec.decode("AAAA", "123456")) // 有效载荷 < IV 长度
        assertNull(PayloadCodec.decode("not-base64!!!", "123456"))
        assertNull(PayloadCodec.decode("aGVsbG8=", "123456")) // 短于 IV
    }

    @Test
    fun encryptedPayload_doesNotLeakPlaintext() {
        val plain = PayloadCodec.toJson(sample())
        val cipher = PayloadCodec.encrypt(plain, "123456")
        assertFalse(cipher.contains("deviceName"))
        assertFalse(cipher.contains("妈妈的手机"))
        assertFalse(cipher.contains("1069070066"))
    }

    @Test
    fun jsonRoundTrip_viaGson() {
        val s = sample()
        val json = PayloadCodec.toJson(s)
        assertTrue(json.contains("\"type\":\"status\""))
        assertTrue(json.contains("battery"))
    }
}
