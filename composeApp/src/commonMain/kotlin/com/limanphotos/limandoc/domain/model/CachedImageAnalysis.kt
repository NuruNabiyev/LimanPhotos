@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Cached analysis result for an image that persists across app sessions
 */
@Serializable
data class CachedImageAnalysis(
    val imagePath: String,
    val lastPhotoEditTime: Instant,
    val description: String,
    val analysisTime: Instant
) {
    /**
     * Checks if this cached analysis is outdated based on the current file modification time
     */
    fun isOutdated(currentFileModTime: Instant): Boolean {
        return currentFileModTime > lastPhotoEditTime || analysisTime < lastPhotoEditTime
    }

    /**
     * Converts this cached analysis to an ImageAnalysis for UI consumption
     */
    fun toImageAnalysis(): ImageAnalysis {
        return ImageAnalysis(
            description = description,
            isLoading = false,
            error = null
        )
    }
}