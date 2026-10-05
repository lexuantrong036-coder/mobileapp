package com.screensolver.ai.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.screensolver.ai.data.local.PreferencesManager
import com.screensolver.ai.data.model.AiConfig
import com.screensolver.ai.data.remote.AiSolverRepository
import com.screensolver.ai.service.FloatingOverlayService
import com.screensolver.ai.ui.theme.ScreenAiSolverTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    private val aiRepository = AiSolverRepository()

    private var hasOverlayPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)
    private var isTestingConnection by mutableStateOf(false)
    private var testConnectionResult by mutableStateOf<String?>(null)
    private var currentConfig by mutableStateOf(AiConfig())

    // Launcher xin quyền MediaProjection (chụp màn hình)
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startOverlayService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Bạn cần cho phép chụp màn hình để AI có thể đọc câu hỏi", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher xin quyền Vẽ đè lên ứng dụng khác (SYSTEM_ALERT_WINDOW)
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = PreferencesManager(this)
        currentConfig = prefsManager.getAiConfig()

        checkPermissions()

        setContent {
            ScreenAiSolverTheme {
                SettingsScreen(
                    config = currentConfig,
                    hasOverlayPermission = hasOverlayPermission,
                    isServiceRunning = isServiceRunning,
                    isTestingConnection = isTestingConnection,
                    testConnectionResult = testConnectionResult,
                    onSaveConfig = { newConfig ->
                        currentConfig = newConfig
                        prefsManager.saveAiConfig(newConfig)
                    },
                    onTestConnection = { configToTest ->
                        testApiConnection(configToTest)
                    },
                    onRequestOverlayPermission = {
                        requestOverlayPermission()
                    },
                    onToggleService = {
                        toggleService()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        isServiceRunning = FloatingOverlayService.isRunning
    }

    private fun checkPermissions() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun toggleService() {
        if (FloatingOverlayService.isRunning) {
            val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_STOP
            }
            startService(stopIntent)
            isServiceRunning = false
            Toast.makeText(this, "Đã tắt trợ lý nổi", Toast.LENGTH_SHORT).show()
        } else {
            if (!hasOverlayPermission) {
                Toast.makeText(this, "Vui lòng cấp quyền Hiển thị đè lên ứng dụng khác trước", Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return
            }

            if (currentConfig.apiKey.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập API Key 9router trước khi bật trợ lý", Toast.LENGTH_LONG).show()
                return
            }

            // Bắt đầu xin quyền MediaProjection
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    private fun startOverlayService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_START
            putExtra(FloatingOverlayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(FloatingOverlayService.EXTRA_RESULT_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        isServiceRunning = true
        Toast.makeText(this, "Trợ lý nổi đã khởi động! Bạn có thể chuyển sang app bài thi", Toast.LENGTH_LONG).show()
    }

    private fun testApiConnection(config: AiConfig) {
        isTestingConnection = true
        testConnectionResult = null

        lifecycleScope.launch {
            val result = aiRepository.testConnection(config)
            isTestingConnection = false
            result.onSuccess { msg ->
                testConnectionResult = msg
            }.onFailure { err ->
                testConnectionResult = "Lỗi: ${err.message}"
            }
        }
    }
}
