package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber
import com.example.vpn.VpnStatus

@Composable
fun QuickConnectButton(
    status: VpnStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    val (buttonColor, glowColor, statusText) = when (status) {
        is VpnStatus.Connected -> Triple(NeonGreen, NeonGreen.copy(alpha = 0.25f), "CONNECTED")
        is VpnStatus.Connecting -> Triple(WarningAmber, WarningAmber.copy(alpha = 0.2f), "CONNECTING")
        is VpnStatus.Disconnecting -> Triple(WarningAmber, WarningAmber.copy(alpha = 0.2f), "DISCONNECTING")
        is VpnStatus.Error -> Triple(ErrorRed, ErrorRed.copy(alpha = 0.25f), "FAILED")
        is VpnStatus.Disconnected -> Triple(ElectricBlue, ElectricBlue.copy(alpha = 0.15f), "TAP TO CONNECT")
    }

    val animatedColor by animateColorAsState(targetValue = buttonColor, label = "color")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing background circle when connected
            if (status is VpnStatus.Connected || status is VpnStatus.Connecting) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(glowColor)
                )
            }

            // Outer decorative ring
            Canvas(modifier = Modifier.size(175.dp)) {
                drawCircle(
                    color = animatedColor.copy(alpha = 0.2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                if (status is VpnStatus.Connected) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(animatedColor.copy(alpha = 0.1f), animatedColor, animatedColor.copy(alpha = 0.1f))
                        ),
                        startAngle = rotateAngle,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Main clickable circle button
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColor.copy(alpha = 0.25f),
                                DarkCard
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 70.dp),
                        onClick = onClick
                    )
                    .testTag("vpn_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                if (status is VpnStatus.Connecting || status is VpnStatus.Disconnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(76.dp),
                        color = animatedColor,
                        strokeWidth = 3.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (status is VpnStatus.Connected) Icons.Default.Security else Icons.Default.PowerSettingsNew,
                        contentDescription = "VPN Power Switch",
                        tint = animatedColor,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = statusText,
            color = animatedColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.5.sp
        )
    }
}
