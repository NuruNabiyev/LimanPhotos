@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.domain.repository

import com.limanphotos.limandoc.data.search.SearchResult
import com.limanphotos.limandoc.domain.model.Photo

/**
 * Repository for managing photo search operations with full-text search capabilities
 */
interface PhotoSearchRepository {

    /**
     * Initialize the search repository with index directory
     */
    suspend fun initialize()

    /**
     * Index a photo with its AI analysis data for full-text search
     */
    suspend fun indexPhoto(photo: Photo, description: String, tags: List<String>)

    /**
     * Search for photos using full-text search across descriptions and tags
     */
    suspend fun searchPhotos(query: String, limit: Int = 50): List<SearchResult>

    /**
     * Remove a photo from the search index
     */
    suspend fun removePhoto(photoPath: String)

    /**
     * Remove all photos from a specific folder path from the search index
     */
    suspend fun removePhotosFromFolder(folderPath: String)

    /**
     * Clear all indexed data
     */
    suspend fun clearIndex()

    /**
     * Get search index statistics
     */
    suspend fun getIndexStats(): IndexStats

    /**
     * Close the search repository and free resources
     */
    suspend fun close()
}

/**
 * Statistics about the search index
 */
data class IndexStats(
    val totalDocuments: Int,
    val indexSizeBytes: Long,
    val lastUpdated: kotlinx.datetime.Instant?
)