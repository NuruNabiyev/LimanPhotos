@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.source.LocalPhotoDataSource
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.model.TagCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class CollectionsRepository(
    private val analysisCacheRepository: AnalysisCacheRepository,
    private val localPhotoDataSource: LocalPhotoDataSource
) {

    /**
     * Get all collections based on most frequent keywords from Ollama descriptions
     */
    suspend fun getAllCollections(): List<com.limanphotos.limandoc.domain.model.Collection> =
        withContext(Dispatchers.IO) {
            try {
                println("🏷️ Starting keyword-based collections analysis...")

                // Get all photos to match with cached analyses
                val allPhotos = localPhotoDataSource.getAllPhotos()
                val photoMap = allPhotos.associateBy { it.path }
                println("🏷️ Found ${allPhotos.size} total photos")

                // Get all cached image paths
                val cachedPaths = analysisCacheRepository.getAllCachedImagePaths()
                println("🏷️ Found ${cachedPaths.size} cached analyses")

                // Extract keywords from descriptions and count frequency
                val keywordToPhotos = mutableMapOf<String, MutableList<Photo>>()
                val keywordFrequency = mutableMapOf<String, Int>()
                var processedCount = 0

                cachedPaths.forEach { imagePath ->
                    processedCount++
                    val photo = photoMap[imagePath]
                    if (photo != null) {
                        val cachedAnalysis = analysisCacheRepository.getCachedAnalysis(imagePath)
                        if (cachedAnalysis != null && cachedAnalysis.description.isNotBlank()) {
                            // Extract meaningful keywords from the actual Ollama description
                            val keywords =
                                extractKeywordsFromDescription(cachedAnalysis.description)

                            // Add photo to each keyword and count frequency
                            keywords.forEach { keyword ->
                                keywordToPhotos.getOrPut(keyword) { mutableListOf() }.add(photo)
                                keywordFrequency[keyword] = (keywordFrequency[keyword] ?: 0) + 1
                            }
                        }
                    }
                }

                println("🏷️ Processed $processedCount cached paths")
                println("🏷️ Found ${keywordFrequency.size} unique keywords")

                // Create collections from keywords with at least 2 photos, sorted by frequency
                val collections = keywordToPhotos
                    .filter { (_, photos) -> photos.size >= 2 }
                    .map { (keyword, photos) ->
                        com.limanphotos.limandoc.domain.model.Collection(
                            tag = keyword,
                            category = TagCategory.OBJECTS, // Use a default category since we're not categorizing
                            photos = photos.sortedByDescending { it.creationTime }
                        )
                    }
                    .sortedByDescending { keywordFrequency[it.tag] ?: 0 }
                    .take(30) // Limit to top 20 most frequent keywords

                println("🏷️ Created ${collections.size} collections from keywords")
                collections.forEach { collection ->
                    println("🏷️ Collection: '${collection.tag}' (${collection.photoCount} photos)")
                }

                collections

            } catch (e: Exception) {
                println("Error getting collections: ${e.message}")
                emptyList()
            }
        }

    /**
     * Extract meaningful keywords from Ollama's natural language description
     * This extracts actual words from the description rather than predefined categories
     */
    private fun extractKeywordsFromDescription(description: String): List<String> {
        try {
            // Common stop words and category labels to filter out
            val stopWords = setOf(
                // Basic stop words
                "the",
                "a",
                "an",
                "and",
                "or",
                "but",
                "in",
                "on",
                "at",
                "to",
                "for",
                "of",
                "with",
                "by",
                "this",
                "that",
                "these",
                "those",
                "is",
                "are",
                "was",
                "were",
                "be",
                "been",
                "have",
                "has",
                "had",
                "do",
                "does",
                "did",
                "will",
                "would",
                "could",
                "should",
                "may",
                "might",
                "can",
                "must",
                "i",
                "you",
                "he",
                "she",
                "it",
                "we",
                "they",
                "me",
                "him",
                "her",
                "us",
                "them",
                "my",
                "your",
                "his",
                "her",
                "its",
                "our",
                "their",
                "mine",
                "yours",
                "hers",
                "ours",
                "theirs",
                "very",
                "much",
                "many",
                "most",
                "more",
                "some",
                "any",
                "all",
                "each",
                "every",
                "no",
                "not",
                "up",
                "down",
                "out",
                "off",
                "over",
                "under",
                "again",
                "further",
                "then",
                "once",
                // Old category labels that should not become collections
                "objects",
                "people",
                "actions",
                "emotions",
                "settings",
                "person",
                "action",
                "emotion",
                "setting",
                // AI-generated "none" values and similar unhelpful terms
                "none",
                "nothing",
                "unknown",
                "unclear",
                "unspecified",
                "n/a",
                "na",
                "null",
                "empty"
            )

            // Extract words from description
            val words = description.lowercase()
                .replace(Regex("[^a-zA-Z0-9\\s]"), " ") // Remove punctuation
                .split(Regex("\\s+")) // Split on whitespace
                .filter { word ->
                    word.length >= 3 && // At least 3 characters
                            word !in stopWords && // Not a stop word
                            word.matches(Regex("[a-zA-Z]+")) // Only letters (no numbers)
                }
                .distinct()

            // Return top keywords (most meaningful ones will appear in multiple descriptions)
            return words.take(10)

        } catch (e: Exception) {
            println("Error extracting keywords: ${e.message}")
            return emptyList()
        }
    }
}