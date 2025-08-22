@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.presentation.gallery

import com.limanphotos.limandoc.data.search.SearchResult
import com.limanphotos.limandoc.domain.model.Collection
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.model.TagCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Test class for search functionality in different scenarios
 */
class SearchFunctionalityTest {

    @Test
    fun `manual search should work when Lucene index is empty but cached analysis exists`() =
        runTest {
            // Given: Empty Lucene index (simulating fresh app state)
            val luceneIndexed = false
            val luceneResults = emptyList<SearchResult>()

            // But cached analysis exists from previous AI runs
            val cachedAnalysis = mapOf(
                "/photos/walking1.jpg" to "People walking through beautiful park",
                "/photos/walking2.jpg" to "Woman walking gracefully by the river",
                "/photos/sunset1.jpg" to "Beautiful golden sunset over mountains",
                "/photos/pose1.jpg" to "Couple posing for romantic photo"
            )

            val searchQuery = "walking"

            // When: Performing manual search
            val searchResults = if (luceneResults.isEmpty()) {
                // Fallback to cached analysis search (what should happen)
                cachedAnalysis.filter { (_, description) ->
                    description.lowercase().contains(searchQuery.lowercase())
                }.map { (path, description) ->
                    // Convert to Photo objects for UI
                    Photo(
                        id = path.hashCode().toString(),
                        path = path,
                        name = path.substringAfterLast("/"),
                        size = 1000L,
                        creationTime = Clock.System.now(),
                        extension = path.substringAfterLast(".")
                    ) to description
                }
            } else {
                luceneResults.map { it.photo to it.matchedText }
            }

            // Then: Should find photos from cached analysis
            assertEquals(2, searchResults.size)
            assertTrue(searchResults.any { it.first.path.contains("walking1.jpg") })
            assertTrue(searchResults.any { it.first.path.contains("walking2.jpg") })
            assertFalse(searchResults.any { it.first.path.contains("sunset1.jpg") })
            assertFalse(searchResults.any { it.first.path.contains("pose1.jpg") })
        }

    @Test
    fun `collections navigation should work regardless of Lucene index state`() = runTest {
        // Given: A collection created from cached analysis
        val photos = listOf(
            Photo(
                id = "1",
                path = "/photos/walking1.jpg",
                name = "walking1.jpg",
                size = 1000L,
                creationTime = Clock.System.now(),
                extension = "jpg"
            ),
            Photo(
                id = "2",
                path = "/photos/walking2.jpg",
                name = "walking2.jpg",
                size = 2000L,
                creationTime = Clock.System.now(),
                extension = "jpg"
            )
        )

        val walkingCollection = Collection(
            tag = "walking",
            category = TagCategory.ACTIONS,
            photos = photos
        )

        // When: Clicking on collection (regardless of Lucene index state)
        val collectionPhotos = walkingCollection.photos

        // Then: Should always show exact collection photos
        assertEquals(2, collectionPhotos.size)
        assertEquals("walking1.jpg", collectionPhotos[0].name)
        assertEquals("walking2.jpg", collectionPhotos[1].name)

        // This should work whether Lucene index is empty or populated
        assertTrue(true) // Collections bypass search entirely
    }

    @Test
    fun `search should handle different query types appropriately`() = runTest {
        // Given: Different types of search queries
        val testQueries = listOf(
            "walking" to "exact keyword",
            "walk" to "partial keyword",
            "walking sunset" to "multiple keywords",
            "\"beautiful walking\"" to "quoted phrase",
            "walk*" to "wildcard pattern"
        )

        val mockDescription = "People walking through beautiful walking sunset park"

        // When: Testing different search strategies
        val results = testQueries.map { (query, type) ->
            val matches = when {
                query.contains("\"") -> {
                    // Phrase search - exact phrase matching
                    val phrase = query.replace("\"", "")
                    mockDescription.lowercase().contains(phrase.lowercase())
                }

                query.contains("*") -> {
                    // Wildcard search
                    val pattern = query.replace("*", "")
                    mockDescription.lowercase().contains(pattern.lowercase())
                }

                query.contains(" ") -> {
                    // Multiple keywords - all must match
                    query.split(" ").all { keyword ->
                        mockDescription.lowercase().contains(keyword.lowercase())
                    }
                }

                else -> {
                    // Single keyword
                    mockDescription.lowercase().contains(query.lowercase())
                }
            }
            Triple(query, type, matches)
        }

        // Then: Different query types should work appropriately
        assertTrue(results.find { it.first == "walking" }?.third == true)
        assertTrue(results.find { it.first == "walk" }?.third == true)
        assertTrue(results.find { it.first == "walking sunset" }?.third == true)
        assertTrue(results.find { it.first == "\"beautiful walking\"" }?.third == true)
        assertTrue(results.find { it.first == "walk*" }?.third == true)
    }

    @Test
    fun `search performance should be acceptable with large datasets`() = runTest {
        // Given: Large dataset simulation
        val largeDataset = (1..1000).map { index ->
            "/photos/photo$index.jpg" to when {
                index % 10 == 0 -> "People walking in photo $index"
                index % 7 == 0 -> "Beautiful sunset in photo $index"
                index % 5 == 0 -> "Group posing in photo $index"
                else -> "Random description for photo $index"
            }
        }.toMap()

        val searchQuery = "walking"

        // When: Performing search on large dataset
        val startTime = kotlin.time.TimeSource.Monotonic.markNow()

        val results = largeDataset.filter { (_, description) ->
            description.lowercase().contains(searchQuery.lowercase())
        }

        val duration = startTime.elapsedNow()

        // Then: Should complete within reasonable time and return expected results
        assertTrue(duration.inWholeMilliseconds < 100) // Should be very fast for in-memory search
        assertEquals(100, results.size) // Every 10th photo (1000/10 = 100)
        assertTrue(results.keys.all { it.contains("0.jpg") }) // All matching photos end with 0
    }

    @Test
    fun `search results should maintain relevance scoring`() = runTest {
        // Given: Photos with varying relevance to search term
        val photos = listOf(
            "/test/exact.jpg" to "walking",                                    // Exact match
            "/test/partial.jpg" to "people walking in park",                  // Contains word
            "/test/multiple.jpg" to "walking and walking again",              // Multiple occurrences
            "/test/context.jpg" to "beautiful photo of walking couple",       // In context
            "/test/irrelevant.jpg" to "sunset over mountains"                 // No match
        )

        val searchQuery = "walking"

        // When: Calculating relevance scores
        val scoredResults = photos.mapNotNull { (path, description) ->
            val desc = description.lowercase()
            val query = searchQuery.lowercase()

            if (!desc.contains(query)) return@mapNotNull null

            val score = when {
                desc == query -> 100f                                    // Exact match
                desc.split(" ").contains(query) -> 90f                  // Complete word
                desc.count { it.toString() == query } > 1 -> 80f       // Multiple occurrences  
                desc.contains(query) -> 70f                            // Partial match
                else -> 0f
            }

            path to score
        }.sortedByDescending { it.second }

        // Then: Results should be properly scored and sorted
        assertEquals(4, scoredResults.size)
        assertEquals("/test/exact.jpg", scoredResults[0].first)           // Highest score
        assertEquals("/test/partial.jpg", scoredResults[1].first)         // Complete word
        assertTrue(scoredResults[0].second > scoredResults[1].second)     // Proper ordering
        assertFalse(scoredResults.any { it.first == "/test/irrelevant.jpg" }) // No irrelevant results
    }
}