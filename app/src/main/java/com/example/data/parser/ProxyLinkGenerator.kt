package com.example.data.parser

import android.util.Base64
import com.example.data.models.ActiveVpnProfile
import com.example.data.models.Inbound
import com.example.data.models.ParsedInboundClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ProxyLinkGenerator {

    fun parseClients(inbound: Inbound): List<ParsedInboundClient> {
        val result = mutableListOf<ParsedInboundClient>()
        try {
            val settingsJson = JSONObject(inbound.settings)
            val clientsArray: JSONArray? = when {
                settingsJson.has("clients") -> settingsJson.getJSONArray("clients")
                settingsJson.has("accounts") -> settingsJson.getJSONArray("accounts")
                else -> null
            }

            if (clientsArray != null) {
                for (i in 0 until clientsArray.length()) {
                    val clientObj = clientsArray.getJSONObject(i)
                    val email = clientObj.optString("email", "client-$i")
                    val id = clientObj.optString("id", clientObj.optString("password", ""))
                    val flow = clientObj.optString("flow", "")
                    val alterId = clientObj.optInt("alterId", 0)
                    val security = clientObj.optString("security", "auto")
                    val subId = clientObj.optString("subId", "")
                    val limitIp = clientObj.optInt("limitIp", 0)
                    val totalGB = clientObj.optLong("totalGB", 0L)
                    val expiryTime = clientObj.optLong("expiryTime", 0L)
                    val enable = clientObj.optBoolean("enable", true)

                    val matchingStat = inbound.clientStats?.find { it.email.equals(email, ignoreCase = true) }
                    val up = matchingStat?.up ?: 0L
                    val down = matchingStat?.down ?: 0L

                    result.add(
                        ParsedInboundClient(
                            email = email,
                            id = id,
                            flow = flow,
                            alterId = alterId,
                            security = security,
                            subId = subId,
                            limitIp = limitIp,
                            totalGB = totalGB,
                            expiryTime = expiryTime,
                            enable = enable,
                            upBytes = up,
                            downBytes = down
                        )
                    )
                }
            } else if (inbound.protocol.equals("shadowsocks", ignoreCase = true)) {
                // Shadowsocks inbound settings often have password and method
                val method = settingsJson.optString("method", "aes-256-gcm")
                val password = settingsJson.optString("password", "")
                result.add(
                    ParsedInboundClient(
                        email = "default",
                        id = password,
                        security = method,
                        enable = true
                    )
                )
            }
        } catch (e: Exception) {
            // fallback if JSON parsing fails
            result.add(
                ParsedInboundClient(
                    email = "default",
                    id = "client-default",
                    enable = true
                )
            )
        }
        return result
    }

    fun buildProxyLink(
        serverHost: String,
        inbound: Inbound,
        client: ParsedInboundClient
    ): String {
        val protocol = inbound.protocol.lowercase()
        val port = inbound.port
        val tagOrRemark = if (inbound.remark.isNotBlank()) inbound.remark else "Inbound-${inbound.port}"
        val encodedRemark = URLEncoder.encode("$tagOrRemark (${client.email})", StandardCharsets.UTF_8.name())

        var network = "tcp"
        var security = "none"
        var sni = ""
        var path = ""
        var hostHeader = ""
        var pbk = ""
        var sid = ""
        var fp = ""

        try {
            val streamJson = JSONObject(inbound.streamSettings)
            network = streamJson.optString("network", "tcp")
            security = streamJson.optString("security", "none")

            if (security.equals("tls", ignoreCase = true)) {
                val tlsSettings = streamJson.optJSONObject("tlsSettings")
                sni = tlsSettings?.optString("serverName", "") ?: ""
            } else if (security.equals("reality", ignoreCase = true)) {
                val realitySettings = streamJson.optJSONObject("realitySettings")
                if (realitySettings != null) {
                    val serverNames = realitySettings.optJSONArray("serverNames")
                    sni = if (serverNames != null && serverNames.length() > 0) serverNames.getString(0) else ""
                    val shortIds = realitySettings.optJSONArray("shortIds")
                    sid = if (shortIds != null && shortIds.length() > 0) shortIds.getString(0) else ""
                    val settingsObj = realitySettings.optJSONObject("settings")
                    pbk = realitySettings.optString("publicKey", settingsObj?.optString("publicKey", ""))
                    fp = realitySettings.optString("fingerprint", "chrome")
                }
            }

            when (network.lowercase()) {
                "ws" -> {
                    val wsSettings = streamJson.optJSONObject("wsSettings")
                    path = wsSettings?.optString("path", "/") ?: "/"
                    val headers = wsSettings?.optJSONObject("headers")
                    hostHeader = headers?.optString("Host", "") ?: ""
                }
                "grpc" -> {
                    val grpcSettings = streamJson.optJSONObject("grpcSettings")
                    path = grpcSettings?.optString("serviceName", "") ?: ""
                }
                "http", "h2" -> {
                    val httpSettings = streamJson.optJSONObject("httpSettings")
                    path = httpSettings?.optString("path", "/") ?: "/"
                }
            }
        } catch (_: Exception) {}

        return when (protocol) {
            "vless" -> {
                val queryParams = mutableListOf<String>()
                queryParams.add("type=$network")
                queryParams.add("security=$security")
                if (client.flow.isNotBlank()) queryParams.add("flow=${client.flow}")
                if (sni.isNotBlank()) queryParams.add("sni=$sni")
                if (pbk.isNotBlank()) queryParams.add("pbk=$pbk")
                if (sid.isNotBlank()) queryParams.add("sid=$sid")
                if (fp.isNotBlank()) queryParams.add("fp=$fp")
                if (path.isNotBlank()) queryParams.add("path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.name()))
                if (hostHeader.isNotBlank()) queryParams.add("host=" + URLEncoder.encode(hostHeader, StandardCharsets.UTF_8.name()))

                "vless://${client.id}@$serverHost:$port?${queryParams.joinToString("&")}#$encodedRemark"
            }
            "vmess" -> {
                val vmessJson = JSONObject().apply {
                    put("v", "2")
                    put("ps", "$tagOrRemark (${client.email})")
                    put("add", serverHost)
                    put("port", port)
                    put("id", client.id)
                    put("aid", client.alterId)
                    put("scy", client.security.ifEmpty { "auto" })
                    put("net", network)
                    put("type", "none")
                    put("host", hostHeader)
                    put("path", path)
                    put("tls", if (security.equals("tls", ignoreCase = true)) "tls" else "")
                    put("sni", sni)
                }
                val encoded = Base64.encodeToString(
                    vmessJson.toString().toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )
                "vmess://$encoded"
            }
            "trojan" -> {
                val queryParams = mutableListOf<String>()
                queryParams.add("security=${if (security == "none") "tls" else security}")
                if (sni.isNotBlank()) queryParams.add("sni=$sni")
                queryParams.add("type=$network")
                if (path.isNotBlank()) queryParams.add("path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.name()))

                "trojan://${client.id}@$serverHost:$port?${queryParams.joinToString("&")}#$encodedRemark"
            }
            "shadowsocks" -> {
                val method = if (client.security.isNotBlank()) client.security else "aes-256-gcm"
                val userInfo = "$method:${client.id}"
                val encodedUserInfo = Base64.encodeToString(
                    userInfo.toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )
                "ss://$encodedUserInfo@$serverHost:$port#$encodedRemark"
            }
            else -> {
                // Generic fallback
                "${protocol}://${client.id}@$serverHost:$port#$encodedRemark"
            }
        }
    }

    fun parseRawConfig(raw: String): ActiveVpnProfile? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // If it's a base64 subscription, decode it first
        val configStr = if (!trimmed.startsWith("vless://") &&
            !trimmed.startsWith("vmess://") &&
            !trimmed.startsWith("trojan://") &&
            !trimmed.startsWith("ss://")
        ) {
            try {
                val decodedBytes = Base64.decode(trimmed, Base64.DEFAULT)
                val decodedStr = String(decodedBytes, StandardCharsets.UTF_8).trim()
                if (decodedStr.isNotEmpty()) decodedStr.lines().firstOrNull { it.isNotBlank() } ?: trimmed
                else trimmed
            } catch (_: Exception) {
                trimmed
            }
        } else {
            trimmed.lines().firstOrNull { it.isNotBlank() } ?: trimmed
        }

        return when {
            configStr.startsWith("vless://") -> parseVless(configStr)
            configStr.startsWith("vmess://") -> parseVmess(configStr)
            configStr.startsWith("trojan://") -> parseTrojan(configStr)
            configStr.startsWith("ss://") -> parseShadowsocks(configStr)
            else -> null
        }
    }

    private fun parseVless(link: String): ActiveVpnProfile? {
        try {
            val withoutPrefix = link.removePrefix("vless://")
            val hashSplit = withoutPrefix.split("#", limit = 2)
            val remark = if (hashSplit.size > 1) {
                java.net.URLDecoder.decode(hashSplit[1], StandardCharsets.UTF_8.name())
            } else "VLESS Node"

            val mainPart = hashSplit[0]
            val atSplit = mainPart.split("@", limit = 2)
            if (atSplit.size < 2) return null
            val uuid = atSplit[0]

            val afterAt = atSplit[1]
            val querySplit = afterAt.split("?", limit = 2)
            val hostPort = querySplit[0].split(":")
            val host = hostPort[0]
            val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 443 else 443

            var sni = ""
            var network = "tcp"
            var security = "reality"
            var flow = ""

            if (querySplit.size > 1) {
                val params = querySplit[1].split("&")
                for (p in params) {
                    val kv = p.split("=", limit = 2)
                    if (kv.size == 2) {
                        when (kv[0].lowercase()) {
                            "sni" -> sni = kv[1]
                            "type" -> network = kv[1]
                            "security" -> security = kv[1]
                            "flow" -> flow = kv[1]
                        }
                    }
                }
            }

            return ActiveVpnProfile(
                id = uuid,
                panelName = "3X-UI Server",
                remark = remark,
                host = host,
                port = port,
                protocol = "vless",
                clientEmail = remark,
                clientUuid = uuid,
                proxyLink = link,
                network = network,
                security = security,
                sni = sni,
                flow = flow
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseVmess(link: String): ActiveVpnProfile? {
        try {
            val base64Data = link.removePrefix("vmess://").trim()
            val decoded = String(Base64.decode(base64Data, Base64.DEFAULT), StandardCharsets.UTF_8)
            val json = JSONObject(decoded)

            val host = json.optString("add", "127.0.0.1")
            val port = json.optInt("port", 443)
            val id = json.optString("id", "")
            val remark = json.optString("ps", "VMess Node")
            val network = json.optString("net", "tcp")
            val sni = json.optString("sni", "")

            return ActiveVpnProfile(
                id = id,
                panelName = "3X-UI Server",
                remark = remark,
                host = host,
                port = port,
                protocol = "vmess",
                clientEmail = remark,
                clientUuid = id,
                proxyLink = link,
                network = network,
                sni = sni
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseTrojan(link: String): ActiveVpnProfile? {
        try {
            val withoutPrefix = link.removePrefix("trojan://")
            val hashSplit = withoutPrefix.split("#", limit = 2)
            val remark = if (hashSplit.size > 1) {
                java.net.URLDecoder.decode(hashSplit[1], StandardCharsets.UTF_8.name())
            } else "Trojan Node"

            val mainPart = hashSplit[0]
            val atSplit = mainPart.split("@", limit = 2)
            val pass = atSplit[0]
            val afterAt = atSplit.getOrNull(1) ?: return null
            val querySplit = afterAt.split("?", limit = 2)
            val hostPort = querySplit[0].split(":")
            val host = hostPort[0]
            val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 443 else 443

            return ActiveVpnProfile(
                id = pass,
                panelName = "3X-UI Server",
                remark = remark,
                host = host,
                port = port,
                protocol = "trojan",
                clientEmail = remark,
                clientUuid = pass,
                proxyLink = link
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseShadowsocks(link: String): ActiveVpnProfile? {
        try {
            val withoutPrefix = link.removePrefix("ss://")
            val hashSplit = withoutPrefix.split("#", limit = 2)
            val remark = if (hashSplit.size > 1) {
                java.net.URLDecoder.decode(hashSplit[1], StandardCharsets.UTF_8.name())
            } else "Shadowsocks Node"

            val mainPart = hashSplit[0]
            val atSplit = mainPart.split("@", limit = 2)
            val hostPort = atSplit.getOrNull(1)?.split(":") ?: return null
            val host = hostPort[0]
            val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 8388 else 8388

            return ActiveVpnProfile(
                id = "ss-${port}",
                panelName = "3X-UI Server",
                remark = remark,
                host = host,
                port = port,
                protocol = "shadowsocks",
                clientEmail = remark,
                clientUuid = "ss",
                proxyLink = link
            )
        } catch (_: Exception) {
            return null
        }
    }
}
