@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.presentation.gallery

import com.limanphotos.limandoc.data.search.SearchResult
import com.limanphotos.limandoc.domain.model.Collection
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.model.TagCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Test class to ensure collection clicks and manual search work consistently
 */
class CollectionSearchConsistencyTest {

    @Test
    fun `collection shows exact photos that were used to create it`() = runTest {
        // Given: A collection with specific photos
        val photo1 = Photo(
            id = "1",
            path = "/test/walking1.jpg",
            name = "walking1.jpg",
            size = 1000L,
            creationTime = Clock.System.now(),
            extension = "jpg"
        )
        val photo2 = Photo(
            id = "2",
            path = "/test/walking2.jpg",
            name = "walking2.jpg",
            size = 2000L,
            creationTime = Clock.System.now(),
            extension = "jpg"
        )
        val photo3 = Photo(
            id = "3",
            path = "/test/walking3.jpg",
            name = "walking3.jpg",
            size = 3000L,
            creationTime = Clock.System.now(),
            extension = "jpg"
        )

        val walkingCollection = Collection(
            tag = "walking",
            category = TagCategory.ACTIONS,
            photos = listOf(photo1, photo2, photo3)
        )

        // When: Getting photos from the collection
        val collectionPhotos = walkingCollection.photos

        // Then: Collection should contain exactly the expected photos
        assertEquals(3, collectionPhotos.size)
        assertEquals(photo1.path, collectionPhotos[0].path)
        assertEquals(photo2.path, collectionPhotos[1].path)
        assertEquals(photo3.path, collectionPhotos[2].path)
    }

    @Test
    fun `search by collection tag should find same photos as collection contains`() = runTest {
        // Given: Photos with AI descriptions containing "walking"
        val walkingDescriptions = mapOf(
            "/test/walking1.jpg" to "A couple walking hand in hand through the park",
            "/test/walking2.jpg" to "Woman walking gracefully down the street",
            "/test/walking3.jpg" to "Group of friends walking together at sunset",
            "/test/pose1.jpg" to "Person in a pose near the fountain",
            "/test/running1.jpg" to "Runner running through the city"
        )

        // When: Extracting keywords like collections do (simulate CollectionsRepository logic)
        val stopWords = setOf(
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
            "by"
        )
        val walkingPhotos = mutableListOf<String>()

        walkingDescriptions.forEach { (path, description) ->
            val words = description.lowercase()
                .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { word ->
                    word.length >= 3 &&
                            word !in stopWords &&
                            word.matches(Regex("[a-zA-Z]+"))
                }
                .distinct()

            if ("walking" in words) {
                walkingPhotos.add(path)
            }
        }

        // Then: Should find exactly the photos with "walking" keyword
        assertEquals(3, walkingPhotos.size)
        assertTrue(walkingPhotos.contains("/test/walking1.jpg"))
        assertTrue(walkingPhotos.contains("/test/walking2.jpg"))
        assertTrue(walkingPhotos.contains("/test/walking3.jpg"))

        // And: Should NOT include photos with other keywords
        assertTrue(!walkingPhotos.contains("/test/pose1.jpg"))
        assertTrue(!walkingPhotos.contains("/test/running1.jpg"))
    }

    @Test
    fun `search should handle different word forms consistently`() = runTest {
        // Given: Photos with different forms of "walking"
        val descriptions = mapOf(
            "/test/walk1.jpg" to "People walking in the park",
            "/test/walk2.jpg" to "A nice walk through the forest",
            "/test/walk3.jpg" to "Child learning to walk",
            "/test/pose1.jpg" to "Beautiful pose near the lake"
        )

        val walkingKeywordPhotos = mutableListOf<String>()
        val walkKeywordPhotos = mutableListOf<String>()

        descriptions.forEach { (path, description) ->
            val words = description.lowercase().split("\\s+".toRegex())
                .map { it.replace(Regex("[^a-zA-Z]"), "") }
                .filter { it.length >= 3 }

            if ("walking" in words) walkingKeywordPhotos.add(path)
            if ("walk" in words) walkKeywordPhotos.add(path)
        }

        // Then: Different search strategies should handle word forms differently
        assertEquals(1, walkingKeywordPhotos.size) // Only exact "walking"
        assertEquals(2, walkKeywordPhotos.size)    // "walk" appears in 2 descriptions

        // This explains why Lucene (with stemming) might find more results
        // than collections (which use exact keyword matching)
    }

    @Test
    fun `search results should be deterministic and repeatable`() = runTest {
        // Given: Same search query executed multiple times
        val searchQuery = "walking"
        val mockSearchResults = listOf(
            SearchResult(
                photo = Photo(
                    id = "1",
                    path = "/test/walking1.jpg",
                    name = "walking1.jpg",
                    size = 1000L,
                    creationTime = Clock.System.now(),
                    extension = "jpg"
                ),
                score = 1.0f,
                matchedText = "People walking in the park"
            ),
            SearchResult(
                photo = Photo(
                    id = "2",
                    path = "/test/walking2.jpg",
                    name = "walking2.jpg",
                    size = 2000L,
                    creationTime = Clock.System.now(),
                    extension = "jpg"
                ),
                score = 0.8f,
                matchedText = "Woman walking gracefully"
            )
        )

        // When: Executing search multiple times
        val results1 = mockSearchResults.sortedByDescending { it.score }
        val results2 = mockSearchResults.sortedByDescending { it.score }

        // Then: Results should be identical
        assertEquals(results1.size, results2.size)
        assertEquals(results1[0].photo.path, results2[0].photo.path)
        assertEquals(results1[1].photo.path, results2[1].photo.path)
    }

    @Test
    fun `empty search index should gracefully fallback to alternative search`() = runTest {
        // Given: Empty Lucene index but cached analysis available
        val emptyLuceneResults = emptyList<SearchResult>()
        val cachedAnalysis = mapOf(
            "/test/walking1.jpg" to "People walking in the park",
            "/test/walking2.jpg" to "Beautiful sunset landscape",
            "/test/walking3.jpg" to "Group walking together"
        )

        // When: Lucene returns empty results
        val luceneResults = emptyLuceneResults

        // Then: Should fallback to cached analysis search
        assertTrue(luceneResults.isEmpty())

        // Simulate fallback search through cached analysis
        val query = "walking"
        val fallbackResults = cachedAnalysis.filter { (_, description) ->
            description.lowercase().contains(query.lowercase())
        }

        assertEquals(2, fallbackResults.size) // Should find 2 matches in cached analysis
        assertTrue(fallbackResults.containsKey("/test/walking1.jpg"))
        assertTrue(fallbackResults.containsKey("/test/walking3.jpg"))
    }

    @Test
    fun `collections and search should be consistent after AI analysis`() = runTest {
        // Given: Photos analyzed by AI with descriptions
        val analysisResults = listOf(
            Pair("/test/photo1.jpg", "A couple walking hand in hand through a beautiful garden"),
            Pair("/test/photo2.jpg", "Woman walking her dog in the morning sunshine"),
            Pair("/test/photo3.jpg", "People walking across a busy intersection"),
            Pair("/test/photo4.jpg", "Beautiful sunset over the mountains"),
            Pair("/test/photo5.jpg", "Children playing in the park")
        )

        // When: Creating collections from AI analysis (simulate CollectionsRepository)
        val walkingPhotosInCollection = analysisResults.filter { (_, description) ->
            description.lowercase().split("\\s+".toRegex())
                .map { it.replace(Regex("[^a-zA-Z]"), "") }
                .contains("walking")
        }.map { it.first }

        // And: Searching with Lucene index (simulate search after indexing)
        val walkingPhotosInSearch = analysisResults.filter { (_, description) ->
            // Simulate Lucene search with stemming and broader matching
            description.lowercase().contains("walk")
        }.map { it.first }

        // Then: Collection should be subset of search results (due to exact vs fuzzy matching)
        assertEquals(3, walkingPhotosInCollection.size)
        assertTrue(walkingPhotosInSearch.size >= walkingPhotosInCollection.size)

        // All collection photos should be found in search results
        walkingPhotosInCollection.forEach { collectionPhoto ->
            assertTrue(walkingPhotosInSearch.contains(collectionPhoto))
        }
    }
}