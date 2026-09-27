package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PanelEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun PanelSettingsScreen(
    panels: List<PanelEntity>,
    currentPanel: PanelEntity?,
    testConnectionResult: String?,
    onSavePanel: (PanelEntity) -> Unit,
    onSwitchPanel: (Long) -> Unit,
    onDeletePanel: (Long) -> Unit,
    onTestConnection: (PanelEntity) -> Unit,
    onClearTestResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "3X-UI PANELS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manage your connected proxy management servers",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Panel", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (panels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No panels configured yet. Tap 'Add Panel' to connect to your 3X-UI server.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(panels, key = { it.id }) { panel ->
                    val isCurrent = currentPanel?.id == panel.id

                    PanelListItem(
                        panel = panel,
                        isCurrent = isCurrent,
                        onSelect = { onSwitchPanel(panel.id) },
                        onDelete = { onDeletePanel(panel.id) },
                        onTest = { onTestConnection(panel) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Info Box on 3x-ui API
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About 3X-UI Panel Integration",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Supports standard 3X-UI / X-UI panels. Connects via session cookies (/login) or API Token. Inbounds (VLESS, VMess, Trojan, Shadowsocks) and client credentials are synchronized automatically into your local secure tunnel engine.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPanelDialog(
                testResult = testConnectionResult,
                onDismiss = {
                    showAddDialog = false
                    onClearTestResult()
                },
                onSave = { panel ->
                    onSavePanel(panel)
                    showAddDialog = false
                    onClearTestResult()
                },
                onTest = onTestConnection
            )
        }
    }
}

@Composable
private fun PanelListItem(
    panel: PanelEntity,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(
                if (isCurrent) 1.5.dp else 1.dp,
                if (isCurrent) NeonGreen else DarkCardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .padding(16.dp)
            .testTag("panel_item_${panel.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) NeonGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = panel.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Panel",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${if (panel.useHttps) "https://" else "http://"}${panel.host}:${panel.port}${panel.basePath}",
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (panel.username.isNotBlank()) "User: ${panel.username}" else "API Token Auth",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                OutlinedButton(
                    onClick = onTest,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Test Connection", fontSize = 11.sp, color = CyberCyan)
                }
            }
        }
    }
}

@Composable
private fun AddPanelDialog(
    testResult: String?,
    onDismiss: () -> Unit,
    onSave: (PanelEntity) -> Unit,
    onTest: (PanelEntity) -> Unit
) {
    var name by remember { mutableStateOf("My 3X-UI VPS") }
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("2053") }
    var basePath by remember { mutableStateOf("/") }
    var useHttps by remember { mutableStateOf(false) }
    var authTab by remember { mutableIntStateOf(0) } // 0: Password, 1: API Token
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var apiToken by remember { mutableStateOf("") }

    val currentPanel = PanelEntity(
        name = name.ifBlank { "3X-UI Panel" },
        host = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/'),
        port = portText.toIntOrNull() ?: 2053,
        basePath = basePath.trim(),
        useHttps = useHttps,
        username = if (authTab == 0) username.trim() else "",
        password = if (authTab == 0) password else "",
        apiToken = if (authTab == 1) apiToken.trim() else "",
        isCurrent = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = "Connect 3X-UI Panel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Panel Name / Tag", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host or IP", fontSize = 12.sp) },
                        placeholder = { Text("vps.example.com or 192.168.1.10") },
                        singleLine = true,
                        modifier = Modifier.weight(2f).testTag("panel_host_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("panel_port_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Use HTTPS (SSL)", fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = useHttps,
                        onCheckedChange = { useHttps = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                    )
                }

                OutlinedTextField(
                    value = basePath,
                    onValueChange = { basePath = it },
                    label = { Text("Web Base Path (default '/')", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Auth Mode Selector
                TabRow(
                    selectedTabIndex = authTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = ElectricBlue
                ) {
                    Tab(
                        selected = authTab == 0,
                        onClick = { authTab = 0 },
                        text = { Text("Username & Password", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = authTab == 1,
                        onClick = { authTab = 1 },
                        text = { Text("API Token", fontSize = 11.sp) }
                    )
                }

                if (authTab == 0) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("panel_user_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = TextMuted
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("panel_pass_input")
                    )
                } else {
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("API Token (Settings > Security > API Token)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (testResult != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = testResult,
                            fontSize = 11.sp,
                            color = if (testResult.startsWith("Connection error")) ErrorRed else NeonGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentPanel) },
                enabled = host.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier.testTag("save_panel_button")
            ) {
                Text("Save & Sync", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onTest(currentPanel) },
                    enabled = host.isNotBlank()
                ) {
                    Text("Test", color = CyberCyan)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}
