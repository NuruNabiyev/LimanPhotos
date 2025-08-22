@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.search.SearchDocument
import com.limanphotos.limandoc.data.search.SearchEngine
import com.limanphotos.limandoc.data.search.SearchResult
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.repository.IndexStats
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Instant

/**
 * JVM implementation of PhotoSearchRepository using Apache Lucene
 */
class PhotoSearchRepositoryImpl : PhotoSearchRepository {

    private val searchEngine = SearchEngine()
    private var isInitialized = false

    companion object {
        private const val INDEX_DIRECTORY_NAME = "search_index"
    }

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            // Create index directory in user's app data directory
            val userHome = System.getProperty("user.home")
            val appDataDir = File(userHome, ".photo-gallery")
            val indexDir = File(appDataDir, INDEX_DIRECTORY_NAME)

            // Ensure directories exist
            if (!indexDir.exists()) {
                indexDir.mkdirs()
            }

            searchEngine.initialize(indexDir.absolutePath)
            isInitialized = true

            println("📁 PhotoSearchRepository initialized with index: ${indexDir.absolutePath}")

        } catch (e: Exception) {
            println("❌ Failed to initialize PhotoSearchRepository: ${e.message}")
            throw RuntimeException("Failed to initialize search repository", e)
        }
    }

    override suspend fun indexPhoto(photo: Photo, description: String, tags: List<String>) {
        if (!isInitialized) {
            println("❌ Failed to index photo ${photo.path}: PhotoSearchRepository not initialized. Call initialize() first.")
            return
        }

        try {
            val searchDocument = SearchDocument(
                photo = photo,
                description = description,
                tags = tags,
                indexedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )

            searchEngine.indexPhoto(searchDocument)

        } catch (e: Exception) {
            println("❌ Failed to index photo ${photo.path}: ${e.message}")
            // Don't throw here to avoid breaking the UI flow
        }
    }

    override suspend fun searchPhotos(query: String, limit: Int): List<SearchResult> {
        if (!isInitialized) {
            println("❌ Search failed for query '$query': PhotoSearchRepository not initialized. Call initialize() first.")
            return emptyList()
        }

        return try {
            searchEngine.search(query, limit)
        } catch (e: Exception) {
            println("❌ Search failed for query '$query': ${e.message}")
            emptyList()
        }
    }

    override suspend fun removePhoto(photoPath: String) {
        ensureInitialized()

        try {
            searchEngine.removePhoto(photoPath)
        } catch (e: Exception) {
            println("❌ Failed to remove photo from index: ${e.message}")
        }
    }

    override suspend fun removePhotosFromFolder(folderPath: String) {
        ensureInitialized()

        try {
            searchEngine.removePhotosFromFolder(folderPath)
            println("🧹 Removed photos from folder: $folderPath")
        } catch (e: Exception) {
            println("❌ Failed to remove photos from folder: ${e.message}")
        }
    }

    override suspend fun clearIndex() {
        ensureInitialized()

        try {
            searchEngine.clearIndex()
            println("🧹 Search index cleared")
        } catch (e: Exception) {
            println("❌ Failed to clear search index: ${e.message}")
        }
    }

    override suspend fun getIndexStats(): IndexStats {
        if (!isInitialized) {
            println("❌ Failed to get index stats: PhotoSearchRepository not initialized. Call initialize() first.")
            return IndexStats(
                totalDocuments = 0,
                indexSizeBytes = 0L,
                lastUpdated = null
            )
        }

        return try {
            val luceneStats = searchEngine.getIndexStats()
            IndexStats(
                totalDocuments = luceneStats.totalDocuments,
                indexSizeBytes = luceneStats.indexSizeBytes,
                lastUpdated = luceneStats.lastUpdated
            )
        } catch (e: Exception) {
            println("❌ Failed to get index stats: ${e.message}")
            IndexStats(
                totalDocuments = 0,
                indexSizeBytes = 0L,
                lastUpdated = null
            )
        }
    }

    override suspend fun close() {
        if (isInitialized) {
            try {
                searchEngine.close()
                isInitialized = false
                println("🔒 PhotoSearchRepository closed")
            } catch (e: Exception) {
                println("❌ Error closing PhotoSearchRepository: ${e.message}")
            }
        }
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("PhotoSearchRepository not initialized. Call initialize() first.")
        }
    }
}