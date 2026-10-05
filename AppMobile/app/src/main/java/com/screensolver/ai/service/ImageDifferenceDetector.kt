package com.screensolver.ai.service

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Thuật toán tính Difference Hash (dHash) 64-bit siêu nhanh để phát hiện màn hình thay đổi.
 * Tiết kiệm tối đa CPU/pin khi chạy vòng lặp kiểm tra tự động (Auto-loop).
 */
object ImageDifferenceDetector {

    /**
     * Tính mã băm dHash 64-bit từ Bitmap.
     * Thu nhỏ ảnh về 9x8, chuyển sang grayscale và so sánh cặp pixel liền kề.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L

        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftPixel = scaled.getPixel(x, y)
                val rightPixel = scaled.getPixel(x + 1, y)

                val leftGray = (Color.red(leftPixel) * 299 + Color.green(leftPixel) * 587 + Color.blue(leftPixel) * 114) / 1000
                val rightGray = (Color.red(rightPixel) * 299 + Color.green(rightPixel) * 587 + Color.blue(rightPixel) * 114) / 1000

                if (leftGray > rightGray) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return hash
    }

    /**
     * Tính khoảng cách Hamming giữa 2 mã hash (số bit khác biệt từ 0 đến 64).
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    /**
     * Kiểm tra xem màn hình có sự thay đổi đáng kể (chuyển câu hỏi mới) hay không.
     * @param threshold số bit khác nhau tối thiểu để coi là màn hình mới (mặc định 10/64 bit ~ 15%)
     */
    fun hasScreenChanged(previousHash: Long, currentHash: Long, threshold: Int = 10): Boolean {
        val distance = hammingDistance(previousHash, currentHash)
        return distance >= threshold
    }
}
