package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.HiddifySettingsDialog
import com.example.ui.screens.HiddifyMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.vpn.VpnController
import com.example.vpn.VpnStatus

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainHiddifyAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainHiddifyAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val status by viewModel.vpnStatus.collectAsStateWithLifecycle()
    val metrics by viewModel.vpnMetrics.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val currentPanel by viewModel.currentPanel.collectAsStateWithLifecycle()
    val savedHintWord by viewModel.savedHintWord.collectAsStateWithLifecycle()
    val isFetchingConfig by viewModel.isFetchingConfig.collectAsStateWithLifecycle()
    val accountStats by viewModel.accountStats.collectAsStateWithLifecycle()
    val middlewareUrl by viewModel.middlewareUrl.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    // Activity result launcher for Android VpnService authorization
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val profile = activeProfile
            if (profile != null) {
                VpnController.startVpn(context, profile)
            }
        } else {
            Toast.makeText(context, "مجوز VPN برای اتصال الزامی است", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Android 13+ notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show SnackBars on user messages
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    fun startVpnWithProfile(profile: com.example.data.models.ActiveVpnProfile) {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            VpnController.startVpn(context, profile)
        }
    }

    fun handleConnectClick() {
        if (status is VpnStatus.Connected || status is VpnStatus.Connecting) {
            // Stop VPN
            VpnController.stopVpn(context)
        } else {
            val profile = activeProfile
            if (profile != null) {
                startVpnWithProfile(profile)
            } else if (!savedHintWord.isNullOrEmpty()) {
                viewModel.fetchAndConnect(savedHintWord!!) { fetchedProfile ->
                    startVpnWithProfile(fetchedProfile)
                }
            } else {
                Toast.makeText(context, "لطفاً کلمه اختصاصی را وارد کنید", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HiddifyMainScreen(
                status = status,
                metrics = metrics,
                activeProfile = activeProfile,
                savedHintWord = savedHintWord,
                accountStats = accountStats,
                isFetchingConfig = isFetchingConfig,
                onConnectWithHint = { word ->
                    viewModel.fetchAndConnect(word) { fetchedProfile ->
                        startVpnWithProfile(fetchedProfile)
                    }
                },
                onToggleVpn = { handleConnectClick() },
                onResetHintWord = {
                    if (status is VpnStatus.Connected || status is VpnStatus.Connecting) {
                        VpnController.stopVpn(context)
                    }
                    viewModel.resetHintWord()
                },
                onOpenSettings = { showSettingsDialog = true }
            )

            if (showSettingsDialog) {
                HiddifySettingsDialog(
                    currentPanel = currentPanel,
                    middlewareUrl = middlewareUrl,
                    onSaveSettings = { updatedPanel, workerUrl ->
                        viewModel.savePanel(updatedPanel)
                        viewModel.updateMiddlewareUrl(workerUrl)
                    },
                    onTestPing = { viewModel.testPing() },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}
