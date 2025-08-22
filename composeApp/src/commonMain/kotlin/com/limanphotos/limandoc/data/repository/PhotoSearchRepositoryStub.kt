package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.search.SearchResult
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.repository.IndexStats
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository

/**
 * Stub implementation of PhotoSearchRepository for platforms where Lucene is not available
 */
class PhotoSearchRepositoryStub : PhotoSearchRepository {

    override suspend fun initialize() {
        // No-op for stub implementation
    }

    override suspend fun indexPhoto(photo: Photo, description: String, tags: List<String>) {
        // No-op for stub implementation
    }

    override suspend fun searchPhotos(query: String, limit: Int): List<SearchResult> {
        // Return empty results for stub implementation
        return emptyList()
    }

    override suspend fun removePhoto(photoPath: String) {
        // No-op for stub implementation
    }

    override suspend fun removePhotosFromFolder(folderPath: String) {
        // No-op for stub implementation
    }

    override suspend fun clearIndex() {
        // No-op for stub implementation
    }

    override suspend fun getIndexStats(): IndexStats {
        return IndexStats(
            totalDocuments = 0,
            indexSizeBytes = 0L,
            lastUpdated = null
        )
    }

    override suspend fun close() {
        // No-op for stub implementation
    }
}