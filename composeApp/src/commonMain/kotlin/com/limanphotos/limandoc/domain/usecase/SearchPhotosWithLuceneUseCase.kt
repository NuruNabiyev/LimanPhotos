package com.limanphotos.limandoc.domain.usecase

import com.limanphotos.limandoc.data.repository.AnalysisCacheRepository
import com.limanphotos.limandoc.data.search.SearchResult
import com.limanphotos.limandoc.domain.repository.PhotoRepository
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository

/**
 * Use case for full-text search using Lucene search engine with cached analysis fallback
 */
class SearchPhotosWithLuceneUseCase(
    private val photoSearchRepository: PhotoSearchRepository,
    private val analysisCacheRepository: AnalysisCacheRepository,
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 50): List<SearchResult> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            try {
                // First try Lucene search
                val luceneResults = photoSearchRepository.searchPhotos(query, limit)

                if (luceneResults.isNotEmpty()) {
                    println("🔍 Lucene search returned ${luceneResults.size} results for '$query'")
                    luceneResults
                } else {
                    // Fallback to cached analysis search
                    println("🔄 Lucene search returned no results, falling back to cached analysis search for '$query'")
                    searchCachedAnalysis(query, limit)
                }
            } catch (e: Exception) {
                println("❌ Lucene search failed: ${e.message}")
                // Fallback to cached analysis search when Lucene fails
                println("🔄 Falling back to cached analysis search for '$query'")
                try {
                    searchCachedAnalysis(query, limit)
                } catch (fallbackError: Exception) {
                    println("❌ Cached analysis search also failed: ${fallbackError.message}")
                    emptyList()
                }
            }
        }
    }

    /**
     * Search through cached analysis descriptions when Lucene index is empty or fails
     */
    private suspend fun searchCachedAnalysis(query: String, limit: Int): List<SearchResult> {
        try {
            // Get all photos to match against cached analysis
            val allPhotos = photoRepository.getAllPhotos()
            val queryLower = query.lowercase()

            // Search through cached analysis
            val matchingResults = mutableListOf<SearchResult>()

            for (photo in allPhotos) {
                val cachedAnalysis = analysisCacheRepository.getCachedAnalysis(photo.path)
                if (cachedAnalysis != null) {
                    val description = cachedAnalysis.description.lowercase()

                    // Check if query matches the description
                    if (description.contains(queryLower)) {
                        // Calculate relevance score based on match quality
                        val score = calculateRelevanceScore(description, queryLower)

                        matchingResults.add(
                            SearchResult(
                                photo = photo,
                                score = score,
                                matchedText = cachedAnalysis.description
                            )
                        )
                    }
                }

                if (matchingResults.size >= limit) break
            }

            // Sort by relevance score and return
            val sortedResults = matchingResults.sortedByDescending { it.score }
            println("💾 Cached analysis search found ${sortedResults.size} results for '$query'")
            return sortedResults

        } catch (e: Exception) {
            println("❌ Cached analysis search failed: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Calculate relevance score for cached analysis search results
     */
    private fun calculateRelevanceScore(description: String, query: String): Float {
        val desc = description.lowercase()
        val queryTerms = query.split(" ").filter { it.isNotBlank() }

        if (queryTerms.isEmpty()) return 0f

        var score = 0f

        for (term in queryTerms) {
            when {
                // Exact word match (highest score)
                desc.split(" ").contains(term) -> score += 100f
                // Exact phrase match
                desc == term -> score += 90f
                // Multiple occurrences
                desc.split(term).size > 2 -> score += 80f
                // Contains term (partial match)
                desc.contains(term) -> score += 70f
            }
        }

        // Normalize by number of query terms
        return score / queryTerms.size
    }
}