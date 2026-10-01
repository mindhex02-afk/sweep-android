package com.mindhex.sweep.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Reads the device's media through MediaStore and builds the cleanup buckets.
 * All work happens on a background dispatcher.
 */
class MediaRepository(private val context: Context) {

    private val imagesUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val videoUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    /** How many images we run the expensive pixel analysis on, per scan. */
    private val analysisCap = 600

    /** Files larger than this land in the "Large files" bucket. */
    private val largeFileThreshold = 5L * 1024 * 1024

    suspend fun scan(onProgress: (String) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val images = queryMedia(imagesUri, isVideo = false)
        val videos = queryMedia(videoUri, isVideo = true)

        onProgress("Finding duplicates…")
        val duplicates = findExactDuplicates(images)

        onProgress("Finding similar photos…")
        val similar = findSimilar(images)

        onProgress("Checking for blurry photos…")
        val blurry = findBlurry(images)

        onProgress("Collecting screenshots…")
        val screenshots = images
            .filter { isScreenshot(it) }
            .map { MediaGroup(Category.SCREENSHOTS, it.uri.toString(), listOf(it)) }

        onProgress("Finding large files…")
        val large = (images + videos)
            .filter { it.sizeBytes >= largeFileThreshold }
            .sortedByDescending { it.sizeBytes }
            .map { MediaGroup(Category.LARGE, it.uri.toString(), listOf(it)) }

        ScanResult(
            listOf(
                CategoryResult(Category.DUPLICATES, duplicates),
                CategoryResult(Category.SIMILAR, similar),
                CategoryResult(Category.SCREENSHOTS, screenshots),
                CategoryResult(Category.BLURRY, blurry),
                CategoryResult(Category.LARGE, large)
            )
        )
    }

    private fun isScreenshot(item: MediaItem): Boolean {
        val bucketHit = item.bucket?.contains("Screenshot", ignoreCase = true) == true
        val nameHit = item.name.contains("screenshot", ignoreCase = true) ||
            item.name.startsWith("Screenshot_", ignoreCase = true)
        return bucketHit || nameHit
    }

    private fun queryMedia(uri: Uri, isVideo: Boolean): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val out = mutableListOf<MediaItem>()
        context.contentResolver.query(
            uri, projection, null, null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketCol = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(uri, id),
                    name = c.getString(nameCol) ?: "",
                    sizeBytes = c.getLong(sizeCol),
                    dateAdded = c.getLong(dateCol),
                    isVideo = isVideo,
                    bucket = if (bucketCol >= 0) c.getString(bucketCol) else null
                )
            }
        }
        return out
    }

    /** Exact duplicates: same byte size, then same content hash. */
    private fun findExactDuplicates(items: List<MediaItem>): List<MediaGroup> {
        val groups = mutableListOf<MediaGroup>()
        val sameSize = items
            .filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }

        for ((_, candidates) in sameSize) {
            val byHash = candidates.groupBy { hashOf(it.uri) }.filter { it.value.size > 1 }
            for ((hash, dupes) in byHash) {
                groups += MediaGroup(Category.DUPLICATES, hash, dupes)
            }
        }
        return groups
    }

    /**
     * SHA-256 over the first 2 MB of the file. Cheap enough to run across a
     * library and reliable for exact-copy detection (size already matched).
     */
    private fun hashOf(uri: Uri): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buf = ByteArray(64 * 1024)
                var read = input.read(buf)
                var total = 0
                while (read > 0 && total < 2 * 1024 * 1024) {
                    md.update(buf, 0, read)
                    total += read
                    read = input.read(buf)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            uri.toString()
        }
    }

    /** Similar photos: cluster by dHash within a small Hamming distance. */
    private fun findSimilar(items: List<MediaItem>): List<MediaGroup> {
        val hashes = mutableListOf<Pair<MediaItem, Long>>()
        for (item in items.take(analysisCap)) {
            val bmp = ImageAnalysis.loadDownscaled(context.contentResolver, item.uri, 128) ?: continue
            hashes += item to ImageAnalysis.perceptualHash(bmp)
            bmp.recycle()
        }

        val used = BooleanArray(hashes.size)
        val groups = mutableListOf<MediaGroup>()
        for (i in hashes.indices) {
            if (used[i]) continue
            val cluster = mutableListOf(hashes[i].first)
            used[i] = true
            for (j in i + 1 until hashes.size) {
                if (used[j]) continue
                if (ImageAnalysis.hamming(hashes[i].second, hashes[j].second) <= 10) {
                    used[j] = true
                    cluster += hashes[j].first
                }
            }
            if (cluster.size > 1) {
                groups += MediaGroup(Category.SIMILAR, "sim-$i", cluster)
            }
        }
        return groups
    }

    /** Blurry photos: variance of Laplacian below a threshold. */
    private fun findBlurry(items: List<MediaItem>): List<MediaGroup> {
        val groups = mutableListOf<MediaGroup>()
        for (item in items.take(analysisCap)) {
            val bmp = ImageAnalysis.loadDownscaled(context.contentResolver, item.uri, 256) ?: continue
            val score = ImageAnalysis.blurScore(bmp)
            bmp.recycle()
            if (score < 60.0) {
                groups += MediaGroup(Category.BLURRY, "blur-${item.id}", listOf(item))
            }
        }
        return groups
    }
}
