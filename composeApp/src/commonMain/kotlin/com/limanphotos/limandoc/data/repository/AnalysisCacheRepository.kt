@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.domain.model.CachedImageAnalysis
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Platform-specific file modification time getter
 */
expect fun getFileModificationTimeImpl(imagePath: String): Instant?

/**
 * Repository for caching image analysis results persistently using Multiplatform Settings
 */
class AnalysisCacheRepository(
    private val settings: Settings
) {
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    /**
     * Retrieves cached analysis for an image if it exists and is not outdated
     */
    fun getCachedAnalysis(imagePath: String): CachedImageAnalysis? {
        return try {
            val cachedJson = settings.getStringOrNull(imagePath) ?: return null
            val cached = json.decodeFromString<CachedImageAnalysis>(cachedJson)

            // Check if the cached analysis is outdated
            val currentFileModTime = getFileModificationTime(imagePath)
            if (currentFileModTime != null && cached.isOutdated(currentFileModTime)) {
                // Remove outdated cache
                settings.remove(imagePath)
                null
            } else {
                cached
            }
        } catch (e: Exception) {
            // If there's any error reading/parsing cache, remove it
            settings.remove(imagePath)
            null
        }
    }

    /**
     * Stores analysis results in cache
     */
    fun cacheAnalysis(
        imagePath: String,
        description: String,
        analysisTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    ) {
        try {
            val fileModTime = getFileModificationTime(imagePath) ?: return

            val cachedAnalysis = CachedImageAnalysis(
                imagePath = imagePath,
                lastPhotoEditTime = fileModTime,
                description = description,
                analysisTime = analysisTime
            )

            val jsonString = json.encodeToString(cachedAnalysis)
            settings.putString(imagePath, jsonString)
        } catch (e: Exception) {
            // If we can't cache, that's not critical - just log and continue
            println("Failed to cache analysis for $imagePath: ${e.message}")
        }
    }

    /**
     * Checks if analysis is cached and up-to-date for given image path
     */
    fun hasValidCachedAnalysis(imagePath: String): Boolean {
        return getCachedAnalysis(imagePath) != null
    }

    /**
     * Removes cached analysis for specific image
     */
    fun removeCachedAnalysis(imagePath: String) {
        settings.remove(imagePath)
    }

    /**
     * Clears all cached analyses
     */
    fun clearAllCache() {
        settings.clear()
    }

    /**
     * Gets all cached image paths
     */
    fun getAllCachedImagePaths(): Set<String> {
        return settings.keys
    }

    /**
     * Gets file modification time as Instant
     */
    private fun getFileModificationTime(imagePath: String): Instant? {
        return getFileModificationTimeImpl(imagePath)
    }

    /**
     * Gets cache statistics for debugging
     */
    fun getCacheStats(): CacheStats {
        val allKeys = settings.keys
        val validEntries = allKeys.count { hasValidCachedAnalysis(it) }
        val outdatedEntries = allKeys.size - validEntries

        return CacheStats(
            totalEntries = allKeys.size,
            validEntries = validEntries,
            outdatedEntries = outdatedEntries
        )
    }
}

/**
 * Statistics about the analysis cache
 */
data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val outdatedEntries: Int
)