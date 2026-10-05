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
    private val backgroundHandler = Handler(Looper.getMainLooper())

    init {
        setupVirtualDisplay()
    }

    private fun setupVirtualDisplay() {
        try {
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

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
            Log.d(TAG, "Đã khởi tạo VirtualDisplay (${screenWidth}x${screenHeight} @ ${screenDensity}dpi)")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo VirtualDisplay", e)
        }
    }

    /**
     * Chụp frame màn hình hiện tại thành đối tượng Bitmap.
     * Xử lý cẩn thận row padding của ImageReader để tránh biến dạng hình ảnh.
     */
    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.Default) {
        val reader = imageReader ?: return@withContext null

        // Chờ 1 nhịp ngắn để ImageReader sẵn sàng có frame mới
        var image: Image? = null
        for (attempt in 0..3) {
            image = reader.acquireLatestImage()
            if (image != null) break
            delay(40)
        }

        if (image == null) {
            Log.w(TAG, "Không thể lấy frame từ ImageReader (image == null)")
            return@withContext null
        }

        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            // Tạo bitmap tạm bao gồm cả padding
            val fullBitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            fullBitmap.copyPixelsFromBuffer(buffer)

            // Cắt phần ảnh chuẩn (loại bỏ padding)
            val cleanBitmap = if (rowPadding > 0) {
                val cropped = Bitmap.createBitmap(fullBitmap, 0, 0, screenWidth, screenHeight)
                fullBitmap.recycle()
                cropped
            } else {
                fullBitmap
            }

            cleanBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trích xuất Bitmap từ ImageReader", e)
            null
        } finally {
            image.close()
        }
    }

    /**
     * Nén Bitmap thành chuỗi Base64 JPEG để gửi lên 9router Vision API.
     * Tự động resize ảnh về maxDimension (1280px) để tối ưu dung lượng và tốc độ mạng.
     */
    fun compressAndEncodeBase64(
        bitmap: Bitmap,
        maxDimension: Int = 1280,
        quality: Int = 75
    ): String {
        var processedBitmap = bitmap

        // Resize nếu kích thước vượt quá maxDimension
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
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            Log.d(TAG, "Đã giải phóng VirtualDisplay và ImageReader")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi giải phóng ScreenCaptureManager", e)
        }
    }
}
