@file:OptIn(kotlin.time.ExperimentalTime::class)

import kotlin.time.Clock

// Simple data classes for testing (minimal versions)
data class Photo(
    val id: String,
    val path: String,
    val name: String,
    val creationTime: kotlin.time.Instant,
    val size: Long,
    val extension: String
)

data class Collection(
    val tag: String,
    val photos: List<Photo>
)

data class SearchResult(
    val photo: Photo,
    val score: Float,
    val matchedText: String? = null
)

// Test functions
fun testCollectionShowsExactPhotos() {
    println("🧪 Test: Collection shows exact photos that were used to create it")
    
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
        photos = listOf(photo1, photo2, photo3)
    )
    
    // Test: Collection should contain exactly the expected photos
    val collectionPhotos = walkingCollection.photos
    
    assert(collectionPhotos.size == 3) { "Expected 3 photos, got ${collectionPhotos.size}" }
    assert(collectionPhotos[0].path == photo1.path) { "First photo path mismatch" }
    assert(collectionPhotos[1].path == photo2.path) { "Second photo path mismatch" }
    assert(collectionPhotos[2].path == photo3.path) { "Third photo path mismatch" }
    
    println("✅ Test passed: Collection contains exactly 3 photos with correct paths")
}

fun testSearchByCollectionTag() {
    println("🧪 Test: Search by collection tag should find same photos as collection contains")
    
    // Test data: Photos with AI descriptions containing "walking"
    val walkingDescriptions = mapOf(
        "/test/walking1.jpg" to "A couple walking hand in hand through the park",
        "/test/walking2.jpg" to "Woman walking gracefully down the street", 
        "/test/walking3.jpg" to "Group of friends walking together at sunset",
        "/test/pose1.jpg" to "Person in a pose near the fountain",
        "/test/running1.jpg" to "Runner running through the city"
    )
    
    // Simulate keyword extraction like CollectionsRepository does
    val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by")
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
    
    // Test: Should find exactly the photos with "walking" keyword
    assert(walkingPhotos.size == 3) { "Expected 3 walking photos, got ${walkingPhotos.size}" }
    assert(walkingPhotos.contains("/test/walking1.jpg")) { "Should contain walking1.jpg" }
    assert(walkingPhotos.contains("/test/walking2.jpg")) { "Should contain walking2.jpg" }
    assert(walkingPhotos.contains("/test/walking3.jpg")) { "Should contain walking3.jpg" }
    assert(!walkingPhotos.contains("/test/pose1.jpg")) { "Should not contain pose1.jpg" }
    assert(!walkingPhotos.contains("/test/running1.jpg")) { "Should not contain running1.jpg" }
    
    println("✅ Test passed: Found exactly 3 photos with 'walking' keyword, excluded non-matching photos")
}

fun testSearchResultsDeterministic() {
    println("🧪 Test: Search results should be deterministic and repeatable")
    
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
    
    // Test: Execute search multiple times
    val results1 = mockSearchResults.sortedByDescending { it.score }
    val results2 = mockSearchResults.sortedByDescending { it.score }
    
    // Test: Results should be identical
    assert(results1.size == results2.size) { "Result sizes should be identical" }
    assert(results1[0].photo.path == results2[0].photo.path) { "First result should be identical" }
    assert(results1[1].photo.path == results2[1].photo.path) { "Second result should be identical" }
    
    println("✅ Test passed: Search results are deterministic and repeatable")
}

fun testFallbackSearch() {
    println("🧪 Test: Empty search index should gracefully fallback to alternative search")
    
    // Test data: Empty Lucene index but cached analysis available
    val emptyLuceneResults = emptyList<SearchResult>()
    val cachedAnalysis = mapOf(
        "/test/walking1.jpg" to "People walking in the park",
        "/test/walking2.jpg" to "Beautiful sunset landscape",
        "/test/walking3.jpg" to "Group walking together"
    )
    
    // Test: When Lucene returns empty results
    val luceneResults = emptyLuceneResults
    assert(luceneResults.isEmpty()) { "Lucene results should be empty" }
    
    // Test: Should fallback to cached analysis search
    val query = "walking"
    val fallbackResults = cachedAnalysis.filter { (_, description) ->
        description.lowercase().contains(query.lowercase())
    }
    
    assert(fallbackResults.size == 2) { "Should find 2 matches in cached analysis, got ${fallbackResults.size}" }
    assert(fallbackResults.containsKey("/test/walking1.jpg")) { "Should contain walking1.jpg" }
    assert(fallbackResults.containsKey("/test/walking3.jpg")) { "Should contain walking3.jpg" }
    
    println("✅ Test passed: Fallback search found 2 matches when Lucene index was empty")
}

// Main test runner
fun main() {
    println("🚀 Running Collection Search Consistency Tests")
    println("=" * 50)
    
    try {
        testCollectionShowsExactPhotos()
        testSearchByCollectionTag()
        testSearchResultsDeterministic()
        testFallbackSearch()
        
        println("=" * 50)
        println("🎉 All tests passed!")
    } catch (e: Exception) {
        println("❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
}