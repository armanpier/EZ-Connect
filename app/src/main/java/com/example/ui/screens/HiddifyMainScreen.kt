package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AccountStats
import com.example.data.models.ActiveVpnProfile
import com.example.vpn.VpnMetrics
import com.example.vpn.VpnStatus

@Composable
fun HiddifyMainScreen(
    status: VpnStatus,
    metrics: VpnMetrics,
    activeProfile: ActiveVpnProfile?,
    savedHintWord: String?,
    accountStats: AccountStats?,
    isFetchingConfig: Boolean,
    onConnectWithHint: (String) -> Unit,
    onToggleVpn: () -> Unit,
    onResetHintWord: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var inputWord by remember { mutableStateOf("") }
    val isConfigured = !savedHintWord.isNullOrEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Colors matching Hiddify dark modern theme
    val (statusText, statusSubText, mainColor, glowColor) = when {
        isFetchingConfig -> Quadruple(
            "در حال دریافت تنظیمات...",
            "Fetching configuration...",
            Color(0xFF00D2FF),
            Color(0x3300D2FF)
        )
        status is VpnStatus.Connected -> Quadruple(
            "متصل شد",
            "Connected",
            Color(0xFF4CAF50),
            Color(0x334CAF50)
        )
        status is VpnStatus.Connecting -> Quadruple(
            "در حال اتصال...",
            "Connecting...",
            Color(0xFFFFB300),
            Color(0x33FFB300)
        )
        status is VpnStatus.Disconnecting -> Quadruple(
            "در حال قطع اتصال...",
            "Disconnecting...",
            Color(0xFFFFB300),
            Color(0x33FFB300)
        )
        status is VpnStatus.Error -> Quadruple(
            "خطا در اتصال",
            (status as VpnStatus.Error).message,
            Color(0xFFEF5350),
            Color(0x33EF5350)
        )
        else -> Quadruple(
            "قطع است",
            "Disconnected",
            Color(0xFF78909C),
            Color(0x2278909C)
        )
    }

    val animatedColor by animateColorAsState(targetValue = mainColor, label = "mainColor")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp)) // balance spacer

                    Text(
                        text = "اتصال امن",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFB0BEC5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Status text
                Text(
                    text = statusText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusSubText,
                    fontSize = 13.sp,
                    color = Color(0xFF78909C),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Central Circular Connect Button (Hiddify / v2rayNG Style)
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                if (status is VpnStatus.Connected || isFetchingConfig || status is VpnStatus.Connecting) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(glowColor)
                    )
                }

                // Decorative ring
                Canvas(modifier = Modifier.size(190.dp)) {
                    drawCircle(
                        color = animatedColor.copy(alpha = 0.25f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    if (status is VpnStatus.Connected) {
                        drawCircle(
                            color = animatedColor,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Main Button
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedColor.copy(alpha = 0.35f),
                                    Color(0xFF212121),
                                    Color(0xFF1E1E1E)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 75.dp),
                            onClick = {
                                if (!isConfigured) {
                                    val wordToUse = inputWord.ifBlank { "freedom" }
                                    onConnectWithHint(wordToUse)
                                } else {
                                    onToggleVpn()
                                }
                            }
                        )
                        .testTag("hiddify_connect_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFetchingConfig || status is VpnStatus.Connecting || status is VpnStatus.Disconnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(68.dp),
                            color = animatedColor,
                            strokeWidth = 4.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (status is VpnStatus.Connected) Icons.Default.Security else Icons.Default.PowerSettingsNew,
                            contentDescription = "اتصال",
                            tint = if (status is VpnStatus.Connected) Color(0xFF4CAF50) else Color(0xFFECEFF1),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // User Account Stats Display (Remaining Days & Traffic for Elderly Users)
            AnimatedVisibility(
                visible = accountStats != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                accountStats?.let { stats ->
                    val isWarning = stats.isExpired || stats.isTrafficExhausted
                    val badgeBg = if (isWarning) Color(0x33EF5350) else Color(0x2281C784)
                    val badgeBorder = if (isWarning) Color(0x66EF5350) else Color(0x5581C784)
                    val textColor = if (isWarning) Color(0xFFFF8A80) else Color(0xFF81C784)

                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                            .testTag("tvUserStats"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.getFormattedPersianText(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hint Word Input Section (Visible only when no hint word is saved)
            AnimatedVisibility(
                visible = !isConfigured,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "کلمه اختصاصی خود را وارد کنید:",
                        fontSize = 14.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputWord,
                        onValueChange = { inputWord = it },
                        placeholder = {
                            Text(
                                text = "مثال: freedom",
                                fontSize = 16.sp,
                                color = Color(0xFF616161),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF37474F),
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                val wordToUse = inputWord.ifBlank { "freedom" }
                                onConnectWithHint(wordToUse)
                            }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hint_word_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "کلمه متناظر با نام کاربری/ریمورک در پنل 3X-UI",
                        fontSize = 11.sp,
                        color = Color(0xFF546E7A),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Configured State Card (When hint word is saved)
            AnimatedVisibility(
                visible = isConfigured,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF00D2FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حساب فعال: ${savedHintWord ?: ""}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (metrics.currentPingMs > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x334CAF50))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${metrics.currentPingMs} ms",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    if (status is VpnStatus.Connected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatSpeed(metrics.downloadSpeedBps),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFF00D2FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatSpeed(metrics.uploadSpeedBps),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDuration(metrics.durationSeconds),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    accountStats?.let { stats ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141414))
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اعتبار: ${if (stats.remainingDays.equals("نامحدود", true)) "نامحدود" else "${stats.remainingDays} روز"}",
                                fontSize = 12.sp,
                                color = if (stats.isExpired) Color(0xFFEF5350) else Color(0xFF81C784),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "باقیمانده: ${stats.remainingGB} از ${stats.totalGB} گیگ",
                                fontSize = 12.sp,
                                color = if (stats.isTrafficExhausted) Color(0xFFEF5350) else Color(0xFF81C784),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Reset Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isConfigured) {
                    TextButton(
                        onClick = onResetHintWord,
                        modifier = Modifier.testTag("reset_hint_word_button")
                    ) {
                        Text(
                            text = "تغییر کلمه عبور / حساب جدید",
                            fontSize = 13.sp,
                            color = Color(0xFF90A4AE),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
        else -> "$bytesPerSec B/s"
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}
