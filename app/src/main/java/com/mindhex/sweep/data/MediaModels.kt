package com.mindhex.sweep.data

import android.net.Uri

/** The buckets Sweep can clean. */
enum class Category(val label: String) {
    DUPLICATES("Duplicates"),
    SIMILAR("Similar photos"),
    SCREENSHOTS("Screenshots"),
    BLURRY("Blurry photos"),
    LARGE("Large files")
}

/** One image or video on the device. */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val isVideo: Boolean,
    val bucket: String?
)

/**
 * A set of related items. For DUPLICATES/SIMILAR this holds the copies;
 * for SCREENSHOTS/BLURRY/LARGE each group holds a single item.
 */
data class MediaGroup(
    val category: Category,
    val key: String,
    val items: List<MediaItem>
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
}

/** Everything found for one category, plus the space that could be reclaimed. */
data class CategoryResult(
    val category: Category,
    val groups: List<MediaGroup>
) {
    val items: List<MediaItem> get() = groups.flatMap { it.items }
    val itemCount: Int get() = items.size
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }

    /**
     * For duplicate/similar sets we can only delete all but one keeper,
     * so reclaimable space is total minus the largest item in each group.
     * For single-item categories the whole file is reclaimable.
     */
    val reclaimableBytes: Long
        get() = when (category) {
            Category.LARGE, Category.SCREENSHOTS, Category.BLURRY -> totalBytes
            else -> groups.sumOf { g ->
                if (g.items.size > 1) g.totalBytes - (g.items.maxOfOrNull { it.sizeBytes } ?: 0L) else 0L
            }
        }
}

/** The full result of one scan. */
data class ScanResult(val categories: List<CategoryResult>)
