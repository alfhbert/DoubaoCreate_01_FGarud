package com.safeguard.app.util

import kotlin.text.Regex

data class Verdict(val risk: Int, val reason: String)

object ScamChecker {

    private val OVERSEAS = Regex("^(00|\\+|86)1\\d{10}$")
    private val HOTLINE = Regex("^4(00|01|08)\\d{7}$")
    private val SERVICE_95 = Regex("^95\\d{4,5}$")
    private val SMS_SENDER = Regex("^106\\d{6,14}$")
    private val LONG_NUMBER = Regex("^\\d{12,20}$")
    private val SHORT_MAINLAND = Regex("^1[3-9]\\d{9}$")

    fun normalize(number: String): String {
        var n = number.replace(" ", "").replace("-", "")
        if (n.startsWith("+86")) n = n.removePrefix("+86")
        if (n.startsWith("0086")) n = n.removePrefix("0086")
        if (n.startsWith("86") && n.length > 11 && SHORT_MAINLAND.matches(n.removePrefix("86"))) {
            n = n.removePrefix("86")
        }
        return n
    }

    fun check(number: String, inContacts: Boolean, blacklist: Set<String>): Verdict {
        val n = normalize(number)
        if (n.isBlank()) return Verdict(0, "")

        if (blacklist.contains(n)) {
            return Verdict(4, "号码在黑名单中")
        }
        if (inContacts) {
            return Verdict(0, "")
        }
        if (OVERSEAS.matches(n)) {
            return Verdict(3, "疑似境外来电（00/+/86 开头）")
        }
        if (SMS_SENDER.matches(n)) {
            return Verdict(3, "疑似短信平台号段（106）来电")
        }
        if (LONG_NUMBER.matches(n)) {
            return Verdict(3, "号码异常过长")
        }
        if (SERVICE_95.matches(n)) {
            return Verdict(2, "95 号段服务热线（注意核实）")
        }
        if (HOTLINE.matches(n)) {
            return Verdict(2, "400 客服热线（注意核实）")
        }
        if (!SHORT_MAINLAND.matches(n)) {
            return Verdict(2, "号码格式异常，请注意核实")
        }
        return Verdict(0, "")
    }
}
