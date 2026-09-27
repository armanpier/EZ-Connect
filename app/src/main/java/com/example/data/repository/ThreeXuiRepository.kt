package com.example.data.repository

import android.content.Context
import com.example.data.api.ThreeXuiApiClient
import com.example.data.db.InboundCacheEntity
import com.example.data.db.PanelDao
import com.example.data.db.PanelEntity
import com.example.data.db.InboundDao
import com.example.data.db.VpnAppDatabase
import com.example.data.models.ActiveVpnProfile
import com.example.data.models.Inbound
import com.example.data.models.LoginRequest
import com.example.data.parser.ProxyLinkGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class ThreeXuiRepository(context: Context) {

    private val database = VpnAppDatabase.getDatabase(context)
    private val panelDao: PanelDao = database.panelDao()
    private val inboundDao: InboundDao = database.inboundDao()
    private val apiClient = ThreeXuiApiClient()

    companion object {
        const val DEFAULT_MIDDLEWARE_URL = "https://vpn-hint-gateway.hi-deploy.workers.dev/?remark="
    }

    private val prefs = context.getSharedPreferences("xui_vpn_prefs", Context.MODE_PRIVATE)

    fun getAllPanels(): Flow<List<PanelEntity>> = panelDao.getAllPanels()
    fun getCurrentPanel(): Flow<PanelEntity?> = panelDao.getCurrentPanel()
    fun getAllInbounds(): Flow<List<InboundCacheEntity>> = inboundDao.getAllInbounds()
    fun getInboundsByPanel(panelId: Long): Flow<List<InboundCacheEntity>> = inboundDao.getInboundsByPanel(panelId)

    fun getSavedHintWord(): String? = prefs.getString("saved_hint_word", null)

    fun saveHintWord(word: String) {
        prefs.edit().putString("saved_hint_word", word).apply()
    }

    fun clearSavedHintWord() {
        prefs.edit()
            .remove("saved_hint_word")
            .remove("saved_profile_link")
            .remove("saved_profile_remark")
            .remove("saved_profile_host")
            .remove("saved_profile_port")
            .remove("saved_profile_protocol")
            .remove("saved_profile_uuid")
            .remove("stats_remaining_days")
            .remove("stats_remaining_gb")
            .remove("stats_total_gb")
            .remove("stats_is_expired")
            .remove("stats_is_traffic_exhausted")
            .apply()
    }

    fun getSavedAccountStats(): com.example.data.models.AccountStats? {
        val days = prefs.getString("stats_remaining_days", null) ?: return null
        val remainingGB = prefs.getString("stats_remaining_gb", "نامحدود") ?: "نامحدود"
        val totalGB = prefs.getString("stats_total_gb", "نامحدود") ?: "نامحدود"
        val isExpired = prefs.getBoolean("stats_is_expired", false)
        val isTrafficExhausted = prefs.getBoolean("stats_is_traffic_exhausted", false)

        return com.example.data.models.AccountStats(
            remainingDays = days,
            remainingGB = remainingGB,
            totalGB = totalGB,
            isExpired = isExpired,
            isTrafficExhausted = isTrafficExhausted
        )
    }

    fun saveAccountStats(stats: com.example.data.models.AccountStats) {
        prefs.edit()
            .putString("stats_remaining_days", stats.remainingDays)
            .putString("stats_remaining_gb", stats.remainingGB)
            .putString("stats_total_gb", stats.totalGB)
            .putBoolean("stats_is_expired", stats.isExpired)
            .putBoolean("stats_is_traffic_exhausted", stats.isTrafficExhausted)
            .apply()
    }

    fun getSavedProfile(): ActiveVpnProfile? {
        val link = prefs.getString("saved_profile_link", null) ?: return null
        val remark = prefs.getString("saved_profile_remark", "Secure Node") ?: "Secure Node"
        val host = prefs.getString("saved_profile_host", "127.0.0.1") ?: "127.0.0.1"
        val port = prefs.getInt("saved_profile_port", 443)
        val protocol = prefs.getString("saved_profile_protocol", "vless") ?: "vless"
        val uuid = prefs.getString("saved_profile_uuid", "client") ?: "client"

        return ProxyLinkGenerator.parseRawConfig(link) ?: ActiveVpnProfile(
            id = uuid,
            panelName = "VPN Gateway",
            remark = remark,
            host = host,
            port = port,
            protocol = protocol,
            clientEmail = remark,
            clientUuid = uuid,
            proxyLink = link
        )
    }

    fun saveProfile(profile: ActiveVpnProfile) {
        prefs.edit()
            .putString("saved_profile_link", profile.proxyLink)
            .putString("saved_profile_remark", profile.remark)
            .putString("saved_profile_host", profile.host)
            .putInt("saved_profile_port", profile.port)
            .putString("saved_profile_protocol", profile.protocol)
            .putString("saved_profile_uuid", profile.clientUuid)
            .apply()
    }

    fun getMiddlewareUrl(): String {
        return prefs.getString("middleware_url", null) ?: DEFAULT_MIDDLEWARE_URL
    }

    fun setMiddlewareUrl(url: String?) {
        if (url.isNullOrBlank() || url.trim() == DEFAULT_MIDDLEWARE_URL) {
            prefs.edit().remove("middleware_url").apply()
        } else {
            prefs.edit().putString("middleware_url", url.trim()).apply()
        }
    }

    suspend fun fetchClientByRemark(
        remark: String,
        customMiddlewareUrl: String? = null
    ): Result<ActiveVpnProfile> = withContext(Dispatchers.IO) {
        val cleanRemark = remark.trim()
        if (cleanRemark.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("لطفاً کلمه اختصاصی را وارد کنید"))
        }

        val effectiveMiddleware = customMiddlewareUrl ?: getMiddlewareUrl()

        // 1. Fetch from Cloudflare Worker gateway
        try {
            val fullUrl = if (effectiveMiddleware.endsWith("=") || effectiveMiddleware.endsWith("?remark=")) {
                "$effectiveMiddleware$cleanRemark"
            } else if (effectiveMiddleware.contains("?")) {
                "$effectiveMiddleware&remark=$cleanRemark"
            } else {
                "$effectiveMiddleware?remark=$cleanRemark"
            }

            val url = java.net.URL(fullUrl)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                setRequestProperty("Accept", "text/plain, application/json, */*")
                setRequestProperty("User-Agent", "v2rayNG/1.8.12 (Android)")
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val rawResponse = connection.inputStream.bufferedReader().use { it.readText() }
                var configPayload = rawResponse.trim()

                // Check if response is structured JSON with config and stats
                if (configPayload.startsWith("{")) {
                    try {
                        val json = org.json.JSONObject(configPayload)
                        if (json.has("config")) {
                            configPayload = json.getString("config").trim()
                        }
                        if (json.has("stats")) {
                            val statsObj = json.optJSONObject("stats")
                            if (statsObj != null) {
                                val parsedStats = com.example.data.models.AccountStats(
                                    remainingDays = statsObj.optString("remainingDays", "نامحدود"),
                                    remainingGB = statsObj.optString("remainingGB", "نامحدود"),
                                    totalGB = statsObj.optString("totalGB", "نامحدود"),
                                    isExpired = statsObj.optBoolean("isExpired", false),
                                    isTrafficExhausted = statsObj.optBoolean("isTrafficExhausted", false)
                                )
                                saveAccountStats(parsedStats)
                            }
                        }
                    } catch (_: Exception) {}
                }

                val parsed = ProxyLinkGenerator.parseRawConfig(configPayload)
                if (parsed != null) {
                    saveHintWord(cleanRemark)
                    saveProfile(parsed)
                    return@withContext Result.success(parsed)
                } else if (configPayload.isNotBlank()) {
                    // Try to construct fallback profile from link
                    val fallbackProfile = ActiveVpnProfile(
                        id = cleanRemark,
                        panelName = "Gateway Node",
                        remark = cleanRemark,
                        host = "gateway",
                        port = 443,
                        protocol = "vless",
                        clientEmail = cleanRemark,
                        clientUuid = cleanRemark,
                        proxyLink = configPayload
                    )
                    saveHintWord(cleanRemark)
                    saveProfile(fallbackProfile)
                    return@withContext Result.success(fallbackProfile)
                }
            } else if (responseCode == 404) {
                return@withContext Result.failure(Exception("حساب با کلمه '$cleanRemark' در سرور یافت نشد"))
            } else {
                return@withContext Result.failure(Exception("پاسخ ناموفق از سرور ورکر (کد $responseCode)"))
            }
        } catch (e: Exception) {
            // Check if local cached profile exists with this hint word
            val savedProfile = getSavedProfile()
            val savedWord = getSavedHintWord()
            if (savedProfile != null && savedWord.equals(cleanRemark, ignoreCase = true)) {
                return@withContext Result.success(savedProfile)
            }
            return@withContext Result.failure(Exception("خطا در اتصال به گیت‌وی ورکر: ${e.localizedMessage}"))
        }

        Result.failure(Exception("خطا در دریافت و پردازش کانفیگ"))
    }

    suspend fun pingHost(host: String, port: Int, timeoutMs: Int = 3000): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        }
    }

    suspend fun savePanel(panel: PanelEntity): Long = withContext(Dispatchers.IO) {
        val id = panelDao.insertPanel(panel)
        if (panel.isCurrent) {
            panelDao.setActivePanel(id)
        }
        id
    }

    suspend fun setActivePanel(panelId: Long) = withContext(Dispatchers.IO) {
        panelDao.setActivePanel(panelId)
    }

    suspend fun deletePanel(panelId: Long) = withContext(Dispatchers.IO) {
        panelDao.deletePanel(panelId)
        inboundDao.deleteByPanel(panelId)
    }

    suspend fun toggleFavoriteInbound(inboundId: Long) = withContext(Dispatchers.IO) {
        inboundDao.toggleFavorite(inboundId)
    }

    suspend fun testConnection(panel: PanelEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = apiClient.createService(panel.getBaseUrl(), panel.apiToken.ifBlank { null })

            if (panel.username.isNotBlank() && panel.password.isNotBlank()) {
                val loginResp = service.loginForm(panel.username, panel.password)
                if (!loginResp.isSuccessful || loginResp.body()?.success != true) {
                    val jsonResp = service.loginJson(LoginRequest(panel.username, panel.password))
                    if (!jsonResp.isSuccessful || jsonResp.body()?.success != true) {
                        return@withContext Result.failure(
                            Exception(loginResp.body()?.msg ?: "Login failed with HTTP ${loginResp.code()}")
                        )
                    }
                }
            }

            val inboundsResp = service.getInbounds()
            if (inboundsResp.isSuccessful && inboundsResp.body()?.success == true) {
                val count = inboundsResp.body()?.obj?.size ?: 0
                Result.success("Connection successful! Found $count inbounds.")
            } else {
                Result.failure(Exception(inboundsResp.body()?.msg ?: "Failed to retrieve inbounds (HTTP ${inboundsResp.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncInbounds(panel: PanelEntity): Result<List<InboundCacheEntity>> = withContext(Dispatchers.IO) {
        try {
            val service = apiClient.createService(panel.getBaseUrl(), panel.apiToken.ifBlank { null })

            if (panel.username.isNotBlank() && panel.password.isNotBlank()) {
                val loginResp = service.loginForm(panel.username, panel.password)
                if (!loginResp.isSuccessful || loginResp.body()?.success != true) {
                    service.loginJson(LoginRequest(panel.username, panel.password))
                }
            }

            val response = service.getInbounds()
            if (!response.isSuccessful || response.body()?.success != true) {
                return@withContext Result.failure(
                    Exception(response.body()?.msg ?: "Failed to fetch inbounds (HTTP ${response.code()})")
                )
            }

            val inbounds: List<Inbound> = response.body()?.obj ?: emptyList()
            val entities = mutableListOf<InboundCacheEntity>()

            for (inbound in inbounds) {
                val clients = ProxyLinkGenerator.parseClients(inbound)
                for (client in clients) {
                    val link = ProxyLinkGenerator.buildProxyLink(panel.host, inbound, client)
                    entities.add(
                        InboundCacheEntity(
                            panelId = panel.id,
                            inboundId = inbound.id,
                            remark = inbound.remark.ifBlank { "${inbound.protocol.uppercase()}-${inbound.port}" },
                            protocol = inbound.protocol.lowercase(),
                            port = inbound.port,
                            clientEmail = client.email,
                            clientUuid = client.id,
                            proxyLink = link,
                            upTraffic = if (client.upBytes > 0) client.upBytes else inbound.up,
                            downTraffic = if (client.downBytes > 0) client.downBytes else inbound.down,
                            totalTraffic = if (client.totalGB > 0) client.totalGB * 1024 * 1024 * 1024 else inbound.total,
                            enable = inbound.enable && client.enable
                        )
                    )
                }
            }

            inboundDao.deleteByPanel(panel.id)
            inboundDao.insertAll(entities)
            panelDao.updatePanel(panel.copy(lastSyncTime = System.currentTimeMillis()))

            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureDefaultSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // App starts clean, relying on the secure Cloudflare Worker gateway
    }
}
