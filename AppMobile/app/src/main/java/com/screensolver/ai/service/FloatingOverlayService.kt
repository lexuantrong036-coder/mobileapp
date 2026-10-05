package com.screensolver.ai.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.screensolver.ai.R
import com.screensolver.ai.data.local.PreferencesManager
import com.screensolver.ai.data.model.AiConfig
import com.screensolver.ai.data.model.SolverResult
import com.screensolver.ai.data.remote.AiSolverRepository
import com.screensolver.ai.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FloatingOverlayService : Service() {

    companion object {
        private const val TAG = "FloatingOverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ai_screen_solver_channel"

        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val ACTION_TOGGLE_AUTO = "action_toggle_auto"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefsManager: PreferencesManager
    private val aiRepository = AiSolverRepository()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoLoopJob: Job? = null
    private var pillDismissJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var captureManager: ScreenCaptureManager? = null

    // Views
    private var bubbleView: View? = null
    private var pillView: View? = null

    // LayoutParams
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var pillParams: WindowManager.LayoutParams

    private var isSolving = false
    private var lastScreenHash: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefsManager = PreferencesManager(this)

        createNotificationChannel()
        setupOverlayViews()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_AUTO -> {
                val current = prefsManager.isAutoLoopEnabled()
                prefsManager.setAutoLoopEnabled(!current)
                updateAutoBadge(!current)
                if (!current) {
                    startAutoLoop()
                } else {
                    stopAutoLoop()
                }
                return START_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

                if (resultCode != 0 && resultData != null && mediaProjection == null) {
                    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

                    initCaptureManager()
                }

                isRunning = true
                updateAutoBadge(prefsManager.isAutoLoopEnabled())

                if (prefsManager.isAutoLoopEnabled()) {
                    startAutoLoop()
                }
            }
        }

        return START_STICKY
    }

    private fun initCaptureManager() {
        val proj = mediaProjection ?: return

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        captureManager = ScreenCaptureManager(
            context = this,
            mediaProjection = proj,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            screenDensity = metrics.densityDpi
        )
        Log.d(TAG, "ScreenCaptureManager khởi tạo thành công")
    }

    private fun startForegroundWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trợ lý AI đang hoạt động")
            .setContentText("Chạm vào bong bóng trên màn hình để giải câu hỏi")
            .setSmallIcon(R.drawable.ic_sparkle)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "Tắt", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dịch vụ Trợ lý Nổi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Duy trì cửa sổ nổi và chụp màn hình nền"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupOverlayViews() {
        val layoutInflater = LayoutInflater.from(this)

        // 1. Setup Bubble View
        bubbleView = layoutInflater.inflate(R.layout.layout_floating_bubble, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        setupBubbleTouchListener()
        windowManager.addView(bubbleView, bubbleParams)

        // 2. Setup Pill View (Ẩn ban đầu)
        pillView = layoutInflater.inflate(R.layout.layout_floating_pill, null)

        pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        setupPillEvents()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleTouchListener() {
        val view = bubbleView ?: return

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }

                    bubbleParams.x = initialX + dx
                    bubbleParams.y = initialY + dy
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        onBubbleClicked()
                    } else {
                        snapBubbleToEdge()
                    }
                    true
                }
                else -> false
            }
        }

        // Long press để toggle nhanh chế độ Auto-Loop
        view.setOnLongClickListener {
            val currentAuto = prefsManager.isAutoLoopEnabled()
            val newAuto = !currentAuto
            prefsManager.setAutoLoopEnabled(newAuto)
            updateAutoBadge(newAuto)
            if (newAuto) {
                Toast.makeText(this, "Đã BẬT chế độ tự động giải", Toast.LENGTH_SHORT).show()
                startAutoLoop()
            } else {
                Toast.makeText(this, "Đã TẮT chế độ tự động giải", Toast.LENGTH_SHORT).show()
                stopAutoLoop()
            }
            true
        }
    }

    private fun snapBubbleToEdge() {
        val size = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(size)
        val screenWidth = size.x

        val targetX = if (bubbleParams.x < screenWidth / 2) 20 else screenWidth - 160
        bubbleParams.x = targetX
        windowManager.updateViewLayout(bubbleView, bubbleParams)
    }

    private fun setupPillEvents() {
        val pill = pillView ?: return

        val btnClose = pill.findViewById<ImageView>(R.id.btnClosePill)
        val btnExpand = pill.findViewById<ImageView>(R.id.btnExpand)
        val layoutExplanation = pill.findViewById<View>(R.id.layoutExplanation)

        btnClose.setOnClickListener {
            hidePillView()
        }

        btnExpand.setOnClickListener {
            if (layoutExplanation.visibility == View.VISIBLE) {
                layoutExplanation.visibility = View.GONE
            } else {
                layoutExplanation.visibility = View.VISIBLE
            }
        }
    }

    private fun onBubbleClicked() {
        if (isSolving) {
            Toast.makeText(this, "Đang xử lý câu hỏi...", Toast.LENGTH_SHORT).show()
            return
        }

        serviceScope.launch {
            triggerSolve()
        }
    }

    /**
     * Quy trình chụp màn hình và gọi AI giải câu hỏi
     */
    private suspend fun triggerSolve() {
        val capture = captureManager
        if (capture == null) {
            Toast.makeText(this, "Lỗi: Chưa cấp quyền chụp màn hình", Toast.LENGTH_SHORT).show()
            return
        }

        val config = prefsManager.getAiConfig()
        if (config.apiKey.isBlank()) {
            Toast.makeText(this, "Chưa nhập API Key. Mở app để cài đặt!", Toast.LENGTH_LONG).show()
            return
        }

        isSolving = true
        setBubbleLoading(true)

        try {
            // 1. Chụp màn hình
            val bitmap = capture.captureScreen()
            if (bitmap == null) {
                showPillResult(SolverResult(choice = "!", text = "Không thể chụp màn hình", explanation = "Hãy thử lại"))
                return
            }

            // 2. Nén và encode Base64
            val base64Image = withContext(Dispatchers.Default) {
                capture.compressAndEncodeBase64(bitmap, maxDimension = 1280, quality = 75)
            }
            bitmap.recycle()

            // 3. Gọi 9router Vision API
            val result = aiRepository.solveScreen(config, base64Image)

            result.onSuccess { solverResult ->
                Log.d(TAG, "Kết quả từ AI: [${solverResult.choice}] ${solverResult.text}")
                showPillResult(solverResult)
            }.onFailure { error ->
                Log.e(TAG, "Lỗi giải bài: ${error.message}")
                showPillResult(SolverResult(choice = "Lỗi", text = error.message ?: "Thất bại", explanation = "Kiểm tra mạng hoặc API Key"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ngoại lệ khi giải bài", e)
            showPillResult(SolverResult(choice = "Lỗi", text = "Lỗi xử lý", explanation = e.localizedMessage ?: ""))
        } finally {
            isSolving = false
            setBubbleLoading(false)
        }
    }

    private fun setBubbleLoading(isLoading: Boolean) {
        val bubble = bubbleView ?: return
        val icon = bubble.findViewById<ImageView>(R.id.bubbleIcon)
        val loading = bubble.findViewById<ProgressBar>(R.id.bubbleLoading)

        if (isLoading) {
            icon.visibility = View.GONE
            loading.visibility = View.VISIBLE
        } else {
            icon.visibility = View.VISIBLE
            loading.visibility = View.GONE
        }
    }

    private fun updateAutoBadge(isEnabled: Boolean) {
        val badge = bubbleView?.findViewById<TextView>(R.id.tvAutoBadge) ?: return
        badge.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun showPillResult(result: SolverResult) {
        serviceScope.launch(Dispatchers.Main) {
            val pill = pillView ?: return@launch
            val tvChoice = pill.findViewById<TextView>(R.id.tvChoiceBadge)
            val tvAnswer = pill.findViewById<TextView>(R.id.tvAnswerText)
            val tvExplanation = pill.findViewById<TextView>(R.id.tvExplanationText)
            val layoutExplanation = pill.findViewById<View>(R.id.layoutExplanation)

            tvChoice.text = "👉 [${result.choice}]"
            tvAnswer.text = result.text.ifBlank { "Đáp án tìm thấy" }
            tvExplanation.text = result.explanation.ifBlank { "Không có giải thích bổ sung." }
            layoutExplanation.visibility = View.GONE

            if (pill.windowToken == null) {
                windowManager.addView(pill, pillParams)
            } else {
                windowManager.updateViewLayout(pill, pillParams)
            }

            // Tự động thu gọn thanh pill sau 8 giây để không che màn hình
            pillDismissJob?.cancel()
            pillDismissJob = serviceScope.launch {
                delay(8000)
                hidePillView()
            }
        }
    }

    private fun hidePillView() {
        val pill = pillView ?: return
        if (pill.windowToken != null) {
            windowManager.removeView(pill)
        }
    }

    private fun startAutoLoop() {
        autoLoopJob?.cancel()
        autoLoopJob = serviceScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Bắt đầu luồng Auto-Loop")
            val config = prefsManager.getAiConfig()
            val intervalMs = (config.autoIntervalSeconds * 1000).toLong().coerceAtLeast(1500)

            while (isActive) {
                delay(intervalMs)

                if (isSolving) continue

                val capture = captureManager ?: continue
                val frame = capture.captureScreen() ?: continue

                val currentHash = ImageDifferenceDetector.computeDHash(frame)
                frame.recycle()

                if (lastScreenHash == 0L) {
                    lastScreenHash = currentHash
                    continue
                }

                // Nếu màn hình thay đổi đáng kể (> 15% bit)
                if (ImageDifferenceDetector.hasScreenChanged(lastScreenHash, currentHash, threshold = 10)) {
                    Log.d(TAG, "Phát hiện câu hỏi mới trên màn hình (dHash thay đổi). Chờ 400ms ổn định...")
                    lastScreenHash = currentHash
                    delay(400) // Debounce

                    withContext(Dispatchers.Main) {
                        triggerSolve()
                    }
                }
            }
        }
    }

    private fun stopAutoLoop() {
        autoLoopJob?.cancel()
        autoLoopJob = null
        lastScreenHash = 0L
        Log.d(TAG, "Đã dừng luồng Auto-Loop")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopAutoLoop()
        serviceScope.cancel()

        hidePillView()
        if (bubbleView?.windowToken != null) {
            windowManager.removeView(bubbleView)
        }

        captureManager?.release()
        captureManager = null
        mediaProjection?.stop()
        mediaProjection = null

        Log.d(TAG, "FloatingOverlayService đã hủy hoàn toàn")
    }
}
