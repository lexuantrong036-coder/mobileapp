package com.screensolver.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screensolver.ai.data.model.AiConfig
import com.screensolver.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: AiConfig,
    hasOverlayPermission: Boolean,
    isServiceRunning: Boolean,
    isTestingConnection: Boolean,
    testConnectionResult: String?,
    onSaveConfig: (AiConfig) -> Unit,
    onTestConnection: (AiConfig) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onToggleService: () -> Unit
) {
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var selectedModel by remember(config.model) { mutableStateOf(config.model) }
    var customModel by remember { mutableStateOf("") }
    var isAutoLoop by remember(config.isAutoLoopEnabled) { mutableStateOf(config.isAutoLoopEnabled) }
    var autoInterval by remember(config.autoIntervalSeconds) { mutableStateOf(config.autoIntervalSeconds) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    val presetModels = listOf(
        "gemini-1.5-flash" to "Nhanh nhất (Gợi ý)",
        "gpt-4o-mini" to "Thông minh & Ổn định",
        "claude-3-5-sonnet" to "Logic phức tạp"
    )

    fun currentConfig() = AiConfig(
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = selectedModel.ifBlank { "gemini-1.5-flash" },
        isAutoLoopEnabled = isAutoLoop,
        autoIntervalSeconds = autoInterval
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Screen Solver",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonGreen
                        )
                        Text(
                            text = "Trợ lý giải trắc nghiệm nổi qua 9router",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        onSaveConfig(currentConfig())
                        onToggleService()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) ErrorRed else NeonGreen,
                        contentColor = if (isServiceRunning) Color.White else DarkBackground
                    )
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "DỪNG TRỢ LÝ NỔI" else "BẬT TRỢ LÝ NỔI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // CARD 1: CẤU HÌNH 9ROUTER
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cấu hình 9router AI",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Base URL
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            onSaveConfig(currentConfig())
                        },
                        label = { Text("Base URL (OpenAI-compatible)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // API Key
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            onSaveConfig(currentConfig())
                        },
                        label = { Text("9router API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Model Selection
                    Text(
                        text = "Chọn Model:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetModels.forEach { (modelId, desc) ->
                            val isSelected = selectedModel == modelId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonGreen else BorderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedModel = modelId
                                        onSaveConfig(currentConfig())
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = modelId,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonGreen else TextPrimary
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Test Connection Button
                    Button(
                        onClick = { onTestConnection(currentConfig()) },
                        enabled = !isTestingConnection && apiKey.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonGreen,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang kiểm tra...")
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kiểm tra kết nối API", color = TextPrimary)
                        }
                    }

                    testConnectionResult?.let { msg ->
                        Text(
                            text = msg,
                            color = if (msg.contains("thành công", ignoreCase = true)) NeonGreen else ErrorRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // CARD 2: CHẾ ĐỘ GIẢI
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chế độ hoạt động",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "• Bấm nút nổi: Nhấp bong bóng để quét màn hình và hiện đáp án ngay.\n• Giữ nút nổi: Bật/tắt nhanh chế độ tự động.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tự động liên tục (Auto-loop)",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tự nhận diện khi có câu hỏi mới",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Switch(
                            checked = isAutoLoop,
                            onCheckedChange = {
                                isAutoLoop = it
                                onSaveConfig(currentConfig())
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    if (isAutoLoop) {
                        Text(
                            text = "Tần suất quét: ${"%.1f".format(autoInterval)} giây/lần",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = autoInterval,
                            onValueChange = {
                                autoInterval = it
                                onSaveConfig(currentConfig())
                            },
                            valueRange = 1.5f..5.0f,
                            steps = 6,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonGreen,
                                activeTrackColor = NeonGreen
                            )
                        )
                    }
                }
            }

            // CARD 3: QUYỀN HỆ THỐNG
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quyền hệ thống",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hiển thị đè lên ứng dụng khác",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasOverlayPermission) "Đã cấp quyền" else "Cần cấp quyền để vẽ bong bóng nổi",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasOverlayPermission) NeonGreen else ErrorRed
                            )
                        }
                        if (!hasOverlayPermission) {
                            Button(
                                onClick = onRequestOverlayPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text("Cấp quyền", color = DarkBackground)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
