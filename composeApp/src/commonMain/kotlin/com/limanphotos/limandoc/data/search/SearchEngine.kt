@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.search

import com.limanphotos.limandoc.domain.model.Photo

/**
 * Search document representing a photo with its analysis data for indexing
 */
data class SearchDocument(
    val photo: Photo,
    val description: String,
    val tags: List<String>,
    val indexedAt: kotlinx.datetime.Instant
)

/**
 * Search result containing the photo and relevance score
 */
data class SearchResult(
    val photo: Photo,
    val score: Float,
    val matchedText: String? = null
)

/**
 * Cross-platform search engine interface for full-text search
 */
expect class SearchEngine {

    /**
     * Initialize the search engine with an index directory
     */
    suspend fun initialize(indexDirectory: String)

    /**
     * Index a photo with its analysis data
     */
    suspend fun indexPhoto(document: SearchDocument)

    /**
     * Search for photos by query text
     */
    suspend fun search(query: String, limit: Int = 50): List<SearchResult>

    /**
     * Remove a photo from the index
     */
    suspend fun removePhoto(photoPath: String)

    /**
     * Clear all indexed data
     */
    suspend fun clearIndex()

    /**
     * Get index statistics
     */
    suspend fun getIndexStats(): IndexStats

    /**
     * Close the search engine and free resources
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