package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "panels")
data class PanelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 2053,
    val basePath: String = "/",
    val useHttps: Boolean = false,
    val username: String = "",
    val password: String = "",
    val apiToken: String = "",
    val isCurrent: Boolean = false,
    val lastSyncTime: Long = 0L
) {
    fun getBaseUrl(): String {
        val scheme = if (useHttps) "https" else "http"
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val path = if (basePath.startsWith("/")) basePath else "/$basePath"
        val cleanPath = if (path.endsWith("/")) path else "$path/"
        return "$scheme://$cleanHost:$port$cleanPath"
    }
}

@Entity(tableName = "inbounds")
data class InboundCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panelId: Long,
    val inboundId: Int,
    val remark: String,
    val protocol: String,
    val port: Int,
    val clientEmail: String,
    val clientUuid: String,
    val proxyLink: String,
    val upTraffic: Long = 0L,
    val downTraffic: Long = 0L,
    val totalTraffic: Long = 0L,
    val enable: Boolean = true,
    val network: String = "tcp",
    val security: String = "none",
    val sni: String = "",
    val isFavorite: Boolean = false
)

@Entity(tableName = "vpn_logs")
data class VpnLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val profileRemark: String,
    val serverHost: String,
    val protocol: String,
    val durationSeconds: Long,
    val bytesSent: Long,
    val bytesReceived: Long
)
