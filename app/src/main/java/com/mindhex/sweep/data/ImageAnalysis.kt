package com.mindhex.sweep.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Cheap, on-device image analysis:
 *  - a 64-bit difference hash (dHash) for finding visually similar photos
 *  - a variance-of-Laplacian score for finding blurry photos
 *
 * No network, no ML models — everything runs locally.
 */
object ImageAnalysis {

    /** Decode a downscaled bitmap so analysis stays fast and memory-light. */
    fun loadDownscaled(cr: ContentResolver, uri: Uri, maxDim: Int = 256): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }

    /** 64-bit dHash: 1 bit per pixel comparing each pixel with its right neighbour. */
    fun perceptualHash(bmp: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bmp, 9, 8, true)
        val w = small.width
        val h = small.height
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)

        var hash = 0L
        var bit = 0
        for (y in 0 until h) {
            for (x in 0 until w - 1) {
                val left = luminance(px[y * w + x])
                val right = luminance(px[y * w + x + 1])
                if (left > right) hash = hash or (1L shl bit)
                bit++
            }
        }
        return hash
    }

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    /**
     * Variance of the Laplacian. Sharp photos have lots of high-frequency
     * edges and therefore a high score; blurry ones sit low.
     */
    fun blurScore(bmp: Bitmap): Double {
        val w = bmp.width
        val h = bmp.height
        if (w < 3 || h < 3) return Double.MAX_VALUE

        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h) { luminance(px[it]) }

        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val c = gray[y * w + x]
                val lap = 4 * c -
                    gray[(y - 1) * w + x] -
                    gray[(y + 1) * w + x] -
                    gray[y * w + x - 1] -
                    gray[y * w + x + 1]
                val d = lap.toDouble()
                sum += d
                sumSq += d * d
                n++
            }
        }
        if (n == 0) return Double.MAX_VALUE
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    private fun luminance(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
