package com.safeguard.app.util

object ScamChecker {

    data class Verdict(val risk: Int, val reason: String)

    fun check(raw: String?, inContacts: Boolean, blacklist: Set<String>): Verdict {
        if (raw.isNullOrBlank()) return Verdict(0, "")
        val trimmed = raw.replace(Regex("[\\s\\-()（）]"), "")
        if (blacklist.contains(trimmed)) return Verdict(2, "该号码已在您的黑名单中")
        if (inContacts) return Verdict(0, "")

        var n = trimmed
        var isOverseas = false
        if (n.startsWith("+")) {
            isOverseas = true
            n = n.substring(1)
        } else if (n.startsWith("00")) {
            isOverseas = true
            n = n.substring(2)
        }

        val digits = n.filter { it.isDigit() }

        if (isOverseas) {
            if (digits.startsWith("86") && digits.length <= 13) {
                return checkDomestic(digits.substring(2))
            }
            return Verdict(2, "境外来电，谨防跨国/冒充客服诈骗")
        }
        return checkDomestic(digits)
    }

    private fun checkDomestic(digits: String): Verdict {
        return when {
            digits.isEmpty() -> Verdict(0, "")
            digits.startsWith("400") -> Verdict(1, "400热线号码，谨防冒充客服/售后诈骗")
            digits.startsWith("800") -> Verdict(1, "800号段号码，谨防免费热线诈骗")
            digits.startsWith("95") && digits.length in 5..8 -> Verdict(1, "95号段营销/客服号码，陌生来电需警惕")
            digits.startsWith("106") -> Verdict(1, "短信平台号段（106…），谨防验证码类诈骗")
            digits.length > 12 -> Verdict(1, "号码异常偏长，谨防改号诈骗")
            digits.startsWith("110") || digits.startsWith("120") ||
                digits.startsWith("119") || digits.startsWith("122") -> Verdict(0, "")
            else -> Verdict(0, "")
        }
    }

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.replace(Regex("[\\s\\-()（）]"), "")
    }
}
