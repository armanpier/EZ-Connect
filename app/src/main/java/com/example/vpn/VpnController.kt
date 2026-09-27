package com.example.vpn

import android.content.Context
import android.content.Intent
import com.example.data.models.ActiveVpnProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnController {

    private val _status = MutableStateFlow<VpnStatus>(VpnStatus.Disconnected)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _metrics = MutableStateFlow(VpnMetrics())
    val metrics: StateFlow<VpnMetrics> = _metrics.asStateFlow()

    private val _activeProfile = MutableStateFlow<ActiveVpnProfile?>(null)
    val activeProfile: StateFlow<ActiveVpnProfile?> = _activeProfile.asStateFlow()

    private val _settings = MutableStateFlow(VpnSettings())
    val settings: StateFlow<VpnSettings> = _settings.asStateFlow()

    fun updateStatus(newStatus: VpnStatus) {
        _status.value = newStatus
    }

    fun updateMetrics(newMetrics: VpnMetrics) {
        _metrics.value = newMetrics
    }

    fun setActiveProfile(profile: ActiveVpnProfile?) {
        _activeProfile.value = profile
    }

    fun updateSettings(newSettings: VpnSettings) {
        _settings.value = newSettings
    }

    fun startVpn(context: Context, profile: ActiveVpnProfile) {
        _activeProfile.value = profile
        _status.value = VpnStatus.Connecting

        val intent = Intent(context, Simple3xuiVpnService::class.java).apply {
            action = Simple3xuiVpnService.ACTION_CONNECT
            putExtra(Simple3xuiVpnService.EXTRA_REMARK, profile.remark)
            putExtra(Simple3xuiVpnService.EXTRA_HOST, profile.host)
            putExtra(Simple3xuiVpnService.EXTRA_PORT, profile.port)
            putExtra(Simple3xuiVpnService.EXTRA_PROTOCOL, profile.protocol)
            putExtra(Simple3xuiVpnService.EXTRA_LINK, profile.proxyLink)
        }
        context.startService(intent)
    }

    fun stopVpn(context: Context) {
        _status.value = VpnStatus.Disconnecting
        val intent = Intent(context, Simple3xuiVpnService::class.java).apply {
            action = Simple3xuiVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
