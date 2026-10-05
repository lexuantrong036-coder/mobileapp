package com.screensolver.ai.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

class ScreenCaptureManager(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val screenDensity: Int
) {

    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenSolverVirtualDisplay"
    }

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    @Volatile
    private var latestBitmap: Bitmap? = null
    private val bitmapLock = Any()

    init {
        initBackgroundThread()
        setupVirtualDisplay()
    }

    private fun initBackgroundThread() {
        handlerThread = HandlerThread("ScreenCaptureThread").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun setupVirtualDisplay() {
        try {
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection đã dừng")
                    release()
                }
            }, backgroundHandler ?: Handler(Looper.getMainLooper()))

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    processImageToBitmap(image)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi trong OnImageAvailableListener", e)
                }
            }, backgroundHandler)

            virtualDisplay = mediaProjection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                backgroundHandler
            )
            Log.d(TAG, "Đã khởi tạo VirtualDisplay thành công (${screenWidth}x${screenHeight} @ ${screenDensity}dpi)")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi nghiêm trọng khi tạo VirtualDisplay", e)
        }
    }

    private fun processImageToBitmap(image: Image) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val fullBitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            fullBitmap.copyPixelsFromBuffer(buffer)

            val cleanBitmap = if (rowPadding > 0) {
                val cropped = Bitmap.createBitmap(fullBitmap, 0, 0, screenWidth, screenHeight)
                fullBitmap.recycle()
                cropped
            } else {
                fullBitmap
            }

            synchronized(bitmapLock) {
                val old = latestBitmap
                latestBitmap = cleanBitmap
                old?.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trích xuất Bitmap từ Image", e)
        } finally {
            image.close()
        }
    }

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.Default) {
        for (i in 0..15) {
            synchronized(bitmapLock) {
                latestBitmap?.let { bmp ->
                    if (!bmp.isRecycled) {
                        return@withContext bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false)
                    }
                }
            }
            delay(100)
        }

        val reader = imageReader ?: return@withContext null
        try {
            val image = reader.acquireLatestImage()
            if (image != null) {
                processImageToBitmap(image)
                synchronized(bitmapLock) {
                    latestBitmap?.let { bmp ->
                        if (!bmp.isRecycled) {
                            return@withContext bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback acquireLatestImage thất bại", e)
        }

        Log.w(TAG, "Không thể lấy frame màn hình (timeout sau 1.5s)")
        null
    }

    fun compressAndEncodeBase64(
        bitmap: Bitmap,
        maxDimension: Int = 1280,
        quality: Int = 75
    ): String {
        var processedBitmap = bitmap

        val currentMax = max(bitmap.width, bitmap.height)
        if (currentMax > maxDimension) {
            val scale = maxDimension.toFloat() / currentMax
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            processedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        val outputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()

        if (processedBitmap != bitmap) {
            processedBitmap.recycle()
        }

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun release() {
        try {
            synchronized(bitmapLock) {
                latestBitmap?.recycle()
                latestBitmap = null
            }
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null

            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null

            Log.d(TAG, "Đã giải phóng toàn bộ tài nguyên ScreenCaptureManager")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi giải phóng ScreenCaptureManager", e)
        }
    }
}
