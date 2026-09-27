package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.InboundCacheEntity
import com.example.data.db.PanelEntity
import com.example.data.models.ActiveVpnProfile
import com.example.data.repository.ThreeXuiRepository
import com.example.vpn.VpnController
import com.example.vpn.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ThreeXuiRepository(application)

    val currentPanel: StateFlow<PanelEntity?> = repository.getCurrentPanel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPanels: StateFlow<List<PanelEntity>> = repository.getAllPanels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawInbounds: StateFlow<List<InboundCacheEntity>> = repository.getAllInbounds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vpnStatus = VpnController.status
    val vpnMetrics = VpnController.metrics
    val activeProfile = VpnController.activeProfile
    val vpnSettings = VpnController.settings

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _protocolFilter = MutableStateFlow("ALL")
    val protocolFilter = _protocolFilter.asStateFlow()

    val filteredInbounds: StateFlow<List<InboundCacheEntity>> = combine(
        rawInbounds,
        _searchQuery,
        _protocolFilter
    ) { inbounds, query, protocol ->
        inbounds.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.remark.contains(query, ignoreCase = true) ||
                    item.clientEmail.contains(query, ignoreCase = true) ||
                    item.port.toString().contains(query)
            val matchesProtocol = protocol == "ALL" || item.protocol.equals(protocol, ignoreCase = true)
            matchesQuery && matchesProtocol
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _savedHintWord = MutableStateFlow(repository.getSavedHintWord())
    val savedHintWord = _savedHintWord.asStateFlow()

    private val _isFetchingConfig = MutableStateFlow(false)
    val isFetchingConfig = _isFetchingConfig.asStateFlow()

    private val _middlewareUrl = MutableStateFlow(repository.getMiddlewareUrl())
    val middlewareUrl = _middlewareUrl.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult = _testConnectionResult.asStateFlow()

    private val _qrCodeData = MutableStateFlow<String?>(null)
    val qrCodeData = _qrCodeData.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSampleDataIfEmpty()
            val savedWord = repository.getSavedHintWord()
            _savedHintWord.value = savedWord
            val savedProfile = repository.getSavedProfile()
            if (savedProfile != null && VpnController.activeProfile.value == null) {
                VpnController.setActiveProfile(savedProfile)
            }
        }
    }

    fun fetchAndConnect(hintWord: String, onReadyToStartVpn: (ActiveVpnProfile) -> Unit) {
        val word = hintWord.trim()
        if (word.isEmpty()) {
            _userMessage.value = "لطفاً کلمه اختصاصی را وارد کنید"
            return
        }

        viewModelScope.launch {
            _isFetchingConfig.value = true
            _userMessage.value = "در حال دریافت تنظیمات..."

            val result = repository.fetchClientByRemark(word)
            _isFetchingConfig.value = false

            result.onSuccess { profile ->
                _savedHintWord.value = word
                VpnController.setActiveProfile(profile)
                _userMessage.value = "حساب با موفقیت تنظیم شد"
                onReadyToStartVpn(profile)
            }.onFailure { err ->
                _userMessage.value = err.message ?: "خطا در دریافت حساب از سرور"
            }
        }
    }

    fun resetHintWord() {
        repository.clearSavedHintWord()
        _savedHintWord.value = null
        VpnController.setActiveProfile(null)
        _userMessage.value = "کلمه اختصاصی حذف شد. می‌توانید کلمه جدیدی وارد کنید."
    }

    fun updateMiddlewareUrl(url: String?) {
        repository.setMiddlewareUrl(url)
        _middlewareUrl.value = repository.getMiddlewareUrl()
        _userMessage.value = "آدرس میدل‌ویر ذخیره شد"
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProtocolFilter(protocol: String) {
        _protocolFilter.value = protocol
    }

    fun showQrCode(link: String) {
        _qrCodeData.value = link
    }

    fun dismissQrCode() {
        _qrCodeData.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearTestResult() {
        _testConnectionResult.value = null
    }

    fun toggleFavorite(inboundId: Long) {
        viewModelScope.launch {
            repository.toggleFavoriteInbound(inboundId)
        }
    }

    fun setActiveInboundProfile(inbound: InboundCacheEntity) {
        val panel = currentPanel.value
        val host = panel?.host ?: "127.0.0.1"
        val profile = ActiveVpnProfile(
            id = inbound.id.toString(),
            panelName = panel?.name ?: "3X-UI Panel",
            remark = inbound.remark,
            host = host,
            port = inbound.port,
            protocol = inbound.protocol,
            clientEmail = inbound.clientEmail,
            clientUuid = inbound.clientUuid,
            proxyLink = inbound.proxyLink,
            network = inbound.network,
            security = inbound.security,
            sni = inbound.sni
        )
        VpnController.setActiveProfile(profile)
        _userMessage.value = "Selected profile: ${inbound.remark}"
    }

    fun syncCurrentPanel() {
        val panel = currentPanel.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncInbounds(panel)
            _isSyncing.value = false

            result.onSuccess { list ->
                _userMessage.value = "Synced ${list.size} inbounds from ${panel.name}"
            }.onFailure { err ->
                _userMessage.value = "Sync failed: ${err.localizedMessage}"
            }
        }
    }

    fun testPanelConnection(panel: PanelEntity) {
        viewModelScope.launch {
            _testConnectionResult.value = "Testing connection to ${panel.host}:${panel.port}..."
            val result = repository.testConnection(panel)
            result.onSuccess { msg ->
                _testConnectionResult.value = msg
            }.onFailure { err ->
                _testConnectionResult.value = "Connection error: ${err.localizedMessage}"
            }
        }
    }

    fun savePanel(panel: PanelEntity) {
        viewModelScope.launch {
            val id = repository.savePanel(panel)
            repository.setActivePanel(id)
            _userMessage.value = "Saved panel: ${panel.name}"
            // Automatically trigger sync
            val savedPanel = panel.copy(id = id)
            repository.syncInbounds(savedPanel)
        }
    }

    fun switchPanel(panelId: Long) {
        viewModelScope.launch {
            repository.setActivePanel(panelId)
            _userMessage.value = "Switched active panel"
        }
    }

    fun deletePanel(panelId: Long) {
        viewModelScope.launch {
            repository.deletePanel(panelId)
            _userMessage.value = "Deleted panel"
        }
    }

    fun testPing() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            val latency = repository.pingHost(profile.host, profile.port)
            if (latency >= 0) {
                VpnController.updateMetrics(vpnMetrics.value.copy(currentPingMs = latency))
                _userMessage.value = "Ping: $latency ms"
            } else {
                _userMessage.value = "Host unreachable"
            }
        }
    }
}
