package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThreeXuiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "obj") val obj: T? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "loginSecret") val loginSecret: String? = null
)

@JsonClass(generateAdapter = true)
data class Inbound(
    @Json(name = "id") val id: Int,
    @Json(name = "up") val up: Long = 0L,
    @Json(name = "down") val down: Long = 0L,
    @Json(name = "total") val total: Long = 0L,
    @Json(name = "remark") val remark: String = "",
    @Json(name = "enable") val enable: Boolean = true,
    @Json(name = "expiryTime") val expiryTime: Long = 0L,
    @Json(name = "listen") val listen: String = "",
    @Json(name = "port") val port: Int = 0,
    @Json(name = "protocol") val protocol: String = "",
    @Json(name = "settings") val settings: String = "{}",
    @Json(name = "streamSettings") val streamSettings: String = "{}",
    @Json(name = "tag") val tag: String = "",
    @Json(name = "sniffing") val sniffing: String? = null,
    @Json(name = "clientStats") val clientStats: List<ClientStat>? = null
)

@JsonClass(generateAdapter = true)
data class ClientStat(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "inboundId") val inboundId: Int = 0,
    @Json(name = "enable") val enable: Boolean = true,
    @Json(name = "email") val email: String = "",
    @Json(name = "up") val up: Long = 0L,
    @Json(name = "down") val down: Long = 0L,
    @Json(name = "expiryTime") val expiryTime: Long = 0L,
    @Json(name = "total") val total: Long = 0L,
    @Json(name = "reset") val reset: Int = 0
)

data class ParsedInboundClient(
    val email: String,
    val id: String, // UUID or password
    val flow: String = "",
    val alterId: Int = 0,
    val security: String = "auto",
    val subId: String = "",
    val limitIp: Int = 0,
    val totalGB: Long = 0L,
    val expiryTime: Long = 0L,
    val enable: Boolean = true,
    val upBytes: Long = 0L,
    val downBytes: Long = 0L
)

data class ActiveVpnProfile(
    val id: String,
    val panelName: String,
    val remark: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val clientEmail: String,
    val clientUuid: String,
    val proxyLink: String,
    val network: String = "tcp",
    val security: String = "none",
    val sni: String = "",
    val flow: String = ""
)

data class AccountStats(
    val remainingDays: String = "نامحدود",
    val remainingGB: String = "نامحدود",
    val totalGB: String = "نامحدود",
    val isExpired: Boolean = false,
    val isTrafficExhausted: Boolean = false
) {
    fun getFormattedPersianText(): String {
        return when {
            isExpired -> "اعتبار زمانی حساب به پایان رسیده است"
            isTrafficExhausted -> "حجم ترافیک حساب تمام شده است"
            else -> {
                val daysText = if (remainingDays.equals("نامحدود", true) || remainingDays == "-1") "زمان: نامحدود" else "اعتبار: $remainingDays روز"
                val gbText = if (remainingGB.equals("نامحدود", true) || remainingGB == "-1") "حجم: نامحدود" else "باقیمانده: $remainingGB گیگابایت"
                "$daysText  •  $gbText"
            }
        }
    }
}
