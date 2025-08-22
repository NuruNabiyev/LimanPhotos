package com.limanphotos.limandoc.presentation.components

import kotlinx.serialization.Serializable

/**
 * File size range filter with dual handles
 */
@Serializable
data class FileSizeFilter(
    val minSizeBytes: Long? = null, // null means no minimum limit
    val maxSizeBytes: Long? = null  // null means no maximum limit
) {
    fun isEmpty(): Boolean = minSizeBytes == null && maxSizeBytes == null

    fun matches(fileSizeBytes: Long): Boolean {
        val minOk = minSizeBytes?.let { fileSizeBytes >= it } ?: true
        val maxOk = maxSizeBytes?.let { fileSizeBytes <= it } ?: true
        return minOk && maxOk
    }

    companion object {
        val EMPTY = FileSizeFilter()

        fun fromMB(minMB: Float?, maxMB: Float?): FileSizeFilter {
            return FileSizeFilter(
                minSizeBytes = minMB?.let { (it * 1024 * 1024).toLong() },
                maxSizeBytes = maxMB?.let { (it * 1024 * 1024).toLong() }
            )
        }

        fun toMB(sizeBytes: Long?): Float? {
            return sizeBytes?.let { it / (1024f * 1024f) }
        }
    }
}

/**
 * File type filter
 */
@Serializable
data class FileTypeFilter(
    val allowedExtensions: Set<String> = emptySet()
) {
    fun isEmpty(): Boolean = allowedExtensions.isEmpty()

    fun matches(fileExtension: String): Boolean {
        if (isEmpty()) return true
        return allowedExtensions.contains(fileExtension.lowercase())
    }

    companion object {
        val EMPTY = FileTypeFilter()
        val ALL_IMAGES = FileTypeFilter(setOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp"))
        val RAW_FORMATS = FileTypeFilter(setOf("raw", "cr2", "nef", "arw", "dng", "orf", "rw2"))
        val COMMON_FORMATS = FileTypeFilter(setOf("jpg", "jpeg", "png"))

        // Available file type options for checkboxes
        val AVAILABLE_TYPES = listOf(
            "jpg" to "JPEG",
            "jpeg" to "JPEG (alt)",
            "png" to "PNG",
            "gif" to "GIF",
            "bmp" to "BMP",
            "tiff" to "TIFF",
            "webp" to "WebP",
            "raw" to "RAW",
            "cr2" to "Canon RAW",
            "nef" to "Nikon RAW",
            "arw" to "Sony RAW",
            "dng" to "Adobe DNG"
        )
    }
}

/**
 * Combined filter state
 */
@Serializable
data class PhotoFilters(
    val fileSizeFilter: FileSizeFilter = FileSizeFilter.EMPTY,
    val fileTypeFilter: FileTypeFilter = FileTypeFilter.EMPTY
) {
    fun isEmpty(): Boolean = fileSizeFilter.isEmpty() && fileTypeFilter.isEmpty()

    fun matches(photo: com.limanphotos.limandoc.domain.model.Photo): Boolean {
        return fileSizeFilter.matches(photo.size) && fileTypeFilter.matches(photo.extension)
    }

    companion object {
        val EMPTY = PhotoFilters()
    }
}

/**
 * UI state for filter controls
 */
data class FilterUIState(
    val minSizeMB: String = "", // Min size as text input
    val maxSizeMB: String = "", // Max size as text input
    val selectedFileTypes: Set<String> = setOf("jpg", "jpeg", "png"), // Default selected types
    val isExpanded: Boolean = false // Whether filter section is expanded
)