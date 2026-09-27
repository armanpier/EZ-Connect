package com.example.vpn

sealed class VpnStatus {
    object Disconnected : VpnStatus()
    object Connecting : VpnStatus()
    object Connected : VpnStatus()
    object Disconnecting : VpnStatus()
    data class Error(val message: String) : VpnStatus()
}

data class VpnMetrics(
    val durationSeconds: Long = 0L,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val currentPingMs: Long = -1L
)

enum class DnsOption(val title: String, val primary: String, val secondary: String) {
    CLOUDFLARE("Cloudflare (Fast & Private)", "1.1.1.1", "1.0.0.1"),
    GOOGLE("Google Public DNS", "8.8.8.8", "8.8.4.4"),
    ADGUARD("AdGuard (Ad Blocking)", "94.140.14.14", "94.140.14.15"),
    QUAD9("Quad9 (Security Focused)", "9.9.9.9", "149.112.112.112"),
    CUSTOM("Custom DNS", "1.1.1.1", "8.8.8.8")
}

data class VpnSettings(
    val dnsOption: DnsOption = DnsOption.CLOUDFLARE,
    val customDns1: String = "1.1.1.1",
    val customDns2: String = "8.8.8.8",
    val mtu: Int = 1500,
    val bypassLan: Boolean = true,
    val autoReconnect: Boolean = true
)
