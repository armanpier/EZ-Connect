package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PanelEntity
import com.example.data.repository.ThreeXuiRepository

@Composable
fun HiddifySettingsDialog(
    currentPanel: PanelEntity?,
    middlewareUrl: String?,
    onSaveSettings: (panel: PanelEntity, workerUrl: String?) -> Unit,
    onTestPing: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultWorker = ThreeXuiRepository.DEFAULT_MIDDLEWARE_URL
    var workerUrlInput by remember { mutableStateOf(middlewareUrl ?: defaultWorker) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF00D2FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنظیمات گیت‌وی ورکر",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "آدرس درگاه امن کلودفلر (Cloudflare Worker):",
                    fontSize = 13.sp,
                    color = Color(0xFFB0BEC5),
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = workerUrlInput,
                    onValueChange = { workerUrlInput = it },
                    placeholder = { Text(defaultWorker, fontSize = 11.sp, color = Color(0xFF616161)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { workerUrlInput = defaultWorker }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = Color(0xFF00D2FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "بازنشانی به پیش‌فرض",
                            fontSize = 12.sp,
                            color = Color(0xFF00D2FF)
                        )
                    }
                }

                Text(
                    text = "امنیت بالا: تمام اطلاعات پنل و پسوردها درون متغیرهای ابری کلودفلر نگهداری می‌شوند و هیچ اطلاعات حساسی روی گوشی کاربر ذخیره نمی‌شود. برنامه فقط با درخواست کلمه اختصاصی، کانفیگ اختصاصی شما را استخراج و متصل می‌کند.",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C),
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedPanel = currentPanel ?: PanelEntity(
                        name = "Cloudflare Gateway",
                        host = "gateway",
                        port = 443,
                        isCurrent = true
                    )
                    onSaveSettings(updatedPanel, workerUrlInput.ifBlank { defaultWorker })
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_hiddify_settings_button")
            ) {
                Text("ذخیره تنظیمات", color = Color(0xFF121212), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = Color(0xFF90A4AE))
            }
        }
    )
}
