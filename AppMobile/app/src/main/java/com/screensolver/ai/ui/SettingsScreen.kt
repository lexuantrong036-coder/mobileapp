package com.screensolver.ai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var selectedModel by remember(config.model) { mutableStateOf(config.model) }
    var isAutoLoop by remember(config.isAutoLoopEnabled) { mutableStateOf(config.isAutoLoopEnabled) }
    var autoInterval by remember(config.autoIntervalSeconds) { mutableStateOf(config.autoIntervalSeconds) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    val presetModels = listOf(
        "gemini-1.5-flash" to "Google Gemini 1.5 Flash (Siêu nhanh, miễn phí)",
        "gemini-1.5-pro" to "Google Gemini 1.5 Pro (Tư duy cao cấp, giải đề khó)",
        "gemini-2.0-flash-exp" to "Google Gemini 2.0 Flash (Thế hệ mới nhất)",
        "gemini-3.8-flash-high" to "Gemini 3.8 Flash High (9router Gateway)",
        "gpt-4o-mini" to "OpenAI GPT-4o Mini (Thông minh & Ổn định)"
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
                            text = "Google Gemini & 9router Trợ lý màn hình",
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

            // CARD: HƯỚNG DẪN ĐĂNG NHẬP GOOGLE LẤY KEY MIỄN PHÍ
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đăng nhập Google lấy Key (Miễn phí 100%)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Bạn chỉ cần đăng nhập tài khoản Google trên trang Google AI Studio và bấm 'Create API key' để lấy key dùng miễn phí cho các model Gemini 1.5, 2.0 Pro/Flash.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://aistudio.google.com/app/apikey")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = DarkBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MỞ GOOGLE AI STUDIO LẤY KEY",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CARD: CẤU HÌNH API
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Khóa API & Máy chủ",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // API Key Input
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            // Tự động nhận diện nếu người dùng dán key Google (AIzaSy...)
                            if (it.trim().startsWith("AIzaSy") && (baseUrl.contains("9router") || baseUrl.isBlank())) {
                                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai"
                            }
                            onSaveConfig(currentConfig())
                        },
                        label = { Text("API Key (Google Gemini hoặc 9router)") },
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

                    // Phím chọn nhanh Base URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai"
                                onSaveConfig(currentConfig())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (baseUrl.contains("googleapis")) NeonGreen else BorderColor
                                )
                            )
                        ) {
                            Text("Google Official", fontSize = 11.sp, color = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                baseUrl = "https://api.9router.com/v1"
                                onSaveConfig(currentConfig())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (baseUrl.contains("9router")) NeonGreen else BorderColor
                                )
                            )
                        ) {
                            Text("9router Cloud", fontSize = 11.sp, color = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                baseUrl = "http://localhost:8080/v1"
                                onSaveConfig(currentConfig())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (baseUrl.contains("localhost")) NeonGreen else BorderColor
                                )
                            )
                        ) {
                            Text("Localhost", fontSize = 11.sp, color = TextPrimary)
                        }
                    }

                    // Base URL Input
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            onSaveConfig(currentConfig())
                        },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Chọn Model
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
                                Column(modifier = Modifier.weight(1f)) {
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

                    // Nhập Model tùy chỉnh
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {
                            selectedModel = it
                            onSaveConfig(currentConfig())
                        },
                        label = { Text("Hoặc nhập tên Model bất kỳ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Nút Kiểm tra kết nối
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
                            Text("Đang kiểm tra kết nối...")
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

            // CARD: CHẾ ĐỘ GIẢI
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tự động quét liên tục",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tự giải khi chuyển câu mới",
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

            // CARD: QUYỀN HỆ THỐNG
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
