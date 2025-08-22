package com.limanphotos.limandoc.data.search

import com.limanphotos.limandoc.domain.model.Photo
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SearchEngineTest {

    private lateinit var searchEngine: SearchEngine
    private lateinit var testIndexDir: File

    @BeforeTest
    fun setup() {
        searchEngine = SearchEngine()
        // Create a temporary directory for test index
        testIndexDir = createTempDir("test_search_index")
    }

    @AfterTest
    fun cleanup() {
        runTest {
            searchEngine.close()
        }
        // Clean up test directory
        testIndexDir.deleteRecursively()
    }

    @Test
    fun `test search engine initialization`() = runTest {
        // Initialize the search engine
        searchEngine.initialize(testIndexDir.absolutePath)

        // Get index stats
        val stats = searchEngine.getIndexStats()
        assertEquals(0, stats.totalDocuments)
        assertNotNull(stats.lastUpdated)
    }

    @Test
    fun `test photo indexing and search`() = runTest {
        // Initialize the search engine
        searchEngine.initialize(testIndexDir.absolutePath)

        // Create a test photo
        val testPhoto = Photo(
            id = "test_photo_1",
            path = "/test/path/golden_sunset.jpg",
            name = "golden_sunset.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        // Create a search document with LLaVA analysis
        val searchDocument = SearchDocument(
            photo = testPhoto,
            description = "A beautiful golden sunset over mountains with warm colors",
            tags = listOf("sunset", "golden", "mountains", "warm", "beautiful"),
            indexedAt = kotlin.time.Clock.System.now()
        )

        // Index the photo
        searchEngine.indexPhoto(searchDocument)

        // Verify index stats
        val stats = searchEngine.getIndexStats()
        assertEquals(1, stats.totalDocuments)

        // Test search for "golden"
        val goldenResults = searchEngine.search("golden", 10)
        assertEquals(1, goldenResults.size)
        assertEquals(testPhoto.path, goldenResults[0].photo.path)
        assertTrue(goldenResults[0].score > 0.0f)

        // Test search for "sunset"
        val sunsetResults = searchEngine.search("sunset", 10)
        assertEquals(1, sunsetResults.size)

        // Test search for "mountains"
        val mountainResults = searchEngine.search("mountains", 10)
        assertEquals(1, mountainResults.size)

        // Test search for non-existent term
        val noResults = searchEngine.search("ocean", 10)
        assertEquals(0, noResults.size)

        // Test empty query
        val emptyResults = searchEngine.search("", 10)
        assertEquals(0, emptyResults.size)
    }

    @Test
    fun `test partial word matching with stemming`() = runTest {
        // Initialize the search engine
        searchEngine.initialize(testIndexDir.absolutePath)

        // Create a test photo with golden description
        val goldenPhoto = Photo(
            id = "golden_photo",
            path = "/test/path/golden_sunset.jpg",
            name = "golden_sunset.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        // Index with description containing "golden"
        val goldenDoc = SearchDocument(
            photo = goldenPhoto,
            description = "A beautiful golden sunset with warm golden light",
            tags = listOf("sunset", "golden", "warm"),
            indexedAt = kotlin.time.Clock.System.now()
        )

        searchEngine.indexPhoto(goldenDoc)

        // Test 1: "gold" should find "golden" (stemming)
        val goldResults = searchEngine.search("gold", 10)
        assertEquals(1, goldResults.size, "Searching 'gold' should find photos with 'golden'")
        assertEquals(goldenPhoto.path, goldResults[0].photo.path)

        // Test 2: "golden" should also work (exact match after stemming)
        val goldenResults = searchEngine.search("golden", 10)
        assertEquals(1, goldenResults.size, "Searching 'golden' should find photos with 'golden'")

        // Test 3: Partial word "gol" should work via wildcard
        val golPartialResults = searchEngine.search("gol", 10)
        assertEquals(
            1,
            golPartialResults.size,
            "Searching 'gol' should find photos with 'golden' via wildcard"
        )
    }

    @Test
    fun `test stemming with different word forms`() = runTest {
        searchEngine.initialize(testIndexDir.absolutePath)

        // Create photos with different word forms
        val photoPhoto = Photo(
            id = "photo_1",
            path = "/test/photograph.jpg",
            name = "photograph.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        val runningPhoto = Photo(
            id = "photo_2",
            path = "/test/running.jpg",
            name = "running.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        // Index with various word forms
        searchEngine.indexPhoto(
            SearchDocument(
                photo = photoPhoto,
                description = "A beautiful photograph showing photography techniques",
                tags = listOf("photography", "photograph"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        searchEngine.indexPhoto(
            SearchDocument(
                photo = runningPhoto,
                description = "A runner running through the park",
                tags = listOf("running", "runner", "sport"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        // Test stemming works for various forms

        // "photo" should find "photograph", "photography"
        val photoResults = searchEngine.search("photo", 10)
        assertEquals(1, photoResults.size, "'photo' should find 'photograph' and 'photography'")

        // "photograph" should find the same
        val photographResults = searchEngine.search("photograph", 10)
        assertEquals(1, photographResults.size)

        // "photography" should find the same  
        val photographyResults = searchEngine.search("photography", 10)
        assertEquals(1, photographyResults.size)

        // "run" should find "running", "runner"
        val runResults = searchEngine.search("run", 10)
        assertEquals(1, runResults.size, "'run' should find 'running' and 'runner'")

        // "running" should find the same
        val runningResults = searchEngine.search("running", 10)
        assertEquals(1, runningResults.size)

        // "runner" should find the same
        val runnerResults = searchEngine.search("runner", 10)
        assertEquals(1, runnerResults.size)
    }

    @Test
    fun `test wildcard search fallback`() = runTest {
        searchEngine.initialize(testIndexDir.absolutePath)

        val testPhoto = Photo(
            id = "wildcard_test",
            path = "/test/specialized.jpg",
            name = "specialized.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        searchEngine.indexPhoto(
            SearchDocument(
                photo = testPhoto,
                description = "A specialized equipment for professional use",
                tags = listOf("specialized", "professional"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        // Test wildcard fallback for partial words
        val specResults = searchEngine.search("spec", 10)
        assertTrue(specResults.isNotEmpty(), "'spec' should find 'specialized' via wildcard")
        assertEquals(testPhoto.path, specResults[0].photo.path)

        val proResults = searchEngine.search("pro", 10)
        assertTrue(proResults.isNotEmpty(), "'pro' should find 'professional' via wildcard")
    }

    @Test
    fun `test phrase search with quotes`() = runTest {
        searchEngine.initialize(testIndexDir.absolutePath)

        // Create photos with specific phrases
        val photo1 = Photo(
            id = "phrase_test_1",
            path = "/test/two_women.jpg",
            name = "two_women.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        val photo2 = Photo(
            id = "phrase_test_2",
            path = "/test/women_two.jpg",
            name = "women_two.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        // Index photos with different phrase arrangements
        searchEngine.indexPhoto(
            SearchDocument(
                photo = photo1,
                description = "Two women standing together in formal dress",
                tags = listOf("people", "formal", "women"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        searchEngine.indexPhoto(
            SearchDocument(
                photo = photo2,
                description = "A group where women number two in the lineup",
                tags = listOf("group", "lineup"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        // Test phrase search vs boolean search

        // 1. Quoted phrase should find exact phrase match
        val phraseResults = searchEngine.search("\"two women\"", 10)
        assertEquals(
            1,
            phraseResults.size,
            "Phrase search '\"two women\"' should find exact phrase only"
        )
        assertEquals(photo1.path, phraseResults[0].photo.path)

        // 2. Unquoted should find both (boolean search)
        val booleanResults = searchEngine.search("two women", 10)
        assertEquals(2, booleanResults.size, "Boolean search 'two women' should find both photos")

        // 3. Test single quotes
        val singleQuoteResults = searchEngine.search("'formal dress'", 10)
        assertEquals(1, singleQuoteResults.size, "Single quote phrase search should work")

        // 4. Test mixed query: phrase + boolean
        val mixedResults = searchEngine.search("\"two women\" group", 10)
        assertTrue(mixedResults.isNotEmpty(), "Mixed query should work")
    }

    @Test
    fun `test enhanced fuzzy search for longer phrases`() = runTest {
        searchEngine.initialize(testIndexDir.absolutePath)

        val testPhoto = Photo(
            id = "fuzzy_long_test",
            path = "/test/beautiful_landscape.jpg",
            name = "beautiful_landscape.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        searchEngine.indexPhoto(
            SearchDocument(
                photo = testPhoto,
                description = "A beautiful mountain landscape with stunning photography techniques",
                tags = listOf("landscape", "mountain", "photography", "beautiful"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        // Test enhanced fuzzy search for longer phrases with multiple typos

        // 1. Single typo in short phrase
        val singleTypoResults = searchEngine.search("beatiful mountain", 10)
        assertTrue(
            singleTypoResults.isNotEmpty(),
            "Should find 'beautiful mountain' from 'beatiful mountain'"
        )

        // 2. Multiple typos in longer phrase
        val multiTypoResults = searchEngine.search("beatiful mountan landscap", 10)
        assertTrue(multiTypoResults.isNotEmpty(), "Should handle multiple typos in longer phrases")

        // 3. Very long phrase with typos
        val longTypoResults =
            searchEngine.search("beatiful mountan landscap photographi tecniques", 10)
        assertTrue(longTypoResults.isNotEmpty(), "Should handle many typos in very long phrases")

        // 4. Test tolerance levels
        val mediumWordResults = searchEngine.search("photographi", 10) // 1 edit distance
        assertTrue(mediumWordResults.isNotEmpty(), "Medium words should use 1 edit distance")

        val longWordResults = searchEngine.search("photographyy", 10) // 2 edit distance possible
        assertTrue(longWordResults.isNotEmpty(), "Long words should use 2 edit distance")
    }

    @Test
    fun `test complex mixed queries`() = runTest {
        searchEngine.initialize(testIndexDir.absolutePath)

        val testPhoto = Photo(
            id = "complex_test",
            path = "/test/golden_hour_portrait.jpg",
            name = "golden_hour_portrait.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        searchEngine.indexPhoto(
            SearchDocument(
                photo = testPhoto,
                description = "Golden hour portrait of two women in beautiful formal dress outdoors",
                tags = listOf("portrait", "golden hour", "formal", "outdoors", "women"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        // Test various complex query combinations

        // 1. Multiple phrases
        val multiplePhrases = searchEngine.search("\"two women\" \"formal dress\"", 10)
        assertTrue(multiplePhrases.isNotEmpty(), "Should handle multiple phrases")

        // 2. Phrase + fuzzy
        val phraseFuzzy = searchEngine.search("\"golden hour\" beatiful", 10)
        assertTrue(phraseFuzzy.isNotEmpty(), "Should handle phrase + fuzzy combination")

        // 3. Mixed quotes
        val mixedQuotes = searchEngine.search("\"two women\" 'golden hour' portrait", 10)
        assertTrue(mixedQuotes.isNotEmpty(), "Should handle mixed quote styles")

        // 4. Phrase + wildcard (fallback)
        val phraseWildcard = searchEngine.search("\"two women\" port", 10)
        assertTrue(phraseWildcard.isNotEmpty(), "Should handle phrase + wildcard fallback")
    }

    @Test
    fun `test fuzzy search for typos`() = runTest {
        searchEngine.initialize(testIndexDir.absolutePath)

        val testPhoto = Photo(
            id = "fuzzy_test",
            path = "/test/beautiful.jpg",
            name = "beautiful.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        searchEngine.indexPhoto(
            SearchDocument(
                photo = testPhoto,
                description = "A beautiful landscape with amazing colors",
                tags = listOf("beautiful", "landscape", "amazing"),
                indexedAt = kotlin.time.Clock.System.now()
            )
        )

        // Test fuzzy search catches typos
        val typoResults = searchEngine.search("beatiful", 10) // Missing 'u'
        assertTrue(typoResults.isNotEmpty(), "Fuzzy search should find 'beautiful' from 'beatiful'")

        val typoResults2 = searchEngine.search("amazng", 10) // Missing 'i'
        assertTrue(typoResults2.isNotEmpty(), "Fuzzy search should find 'amazing' from 'amazng'")
    }

    @Test
    fun `test multiple photos indexing and relevance`() = runTest {
        // Initialize the search engine
        searchEngine.initialize(testIndexDir.absolutePath)

        // Create multiple test photos
        val photo1 = Photo(
            id = "photo_1",
            path = "/test/golden_sunset.jpg",
            name = "golden_sunset.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        val photo2 = Photo(
            id = "photo_2",
            path = "/test/mountain_landscape.jpg",
            name = "mountain_landscape.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 2048L,
            extension = "jpg"
        )

        // Index photo 1 with golden sunset description
        val doc1 = SearchDocument(
            photo = photo1,
            description = "A stunning golden sunset with warm golden light illuminating the landscape",
            tags = listOf("sunset", "golden", "warm", "light"),
            indexedAt = kotlin.time.Clock.System.now()
        )
        searchEngine.indexPhoto(doc1)

        // Index photo 2 with mountain description (mentions golden once)
        val doc2 = SearchDocument(
            photo = photo2,
            description = "A mountain landscape with some golden highlights in the rocks",
            tags = listOf("mountain", "landscape", "rocks"),
            indexedAt = kotlin.time.Clock.System.now()
        )
        searchEngine.indexPhoto(doc2)

        // Verify both photos are indexed
        val stats = searchEngine.getIndexStats()
        assertEquals(2, stats.totalDocuments)

        // Search for "golden" - should return both but photo1 should have higher score
        val goldenResults = searchEngine.search("golden", 10)
        assertEquals(2, goldenResults.size)

        // The first result should have a higher score (more relevant)
        assertTrue(goldenResults[0].score >= goldenResults[1].score)

        // Search for "mountain" - should return only photo2
        val mountainResults = searchEngine.search("mountain", 10)
        assertEquals(1, mountainResults.size)
        assertEquals(photo2.path, mountainResults[0].photo.path)
    }

    @Test
    fun `test photo removal from index`() = runTest {
        // Initialize and add a photo
        searchEngine.initialize(testIndexDir.absolutePath)

        val testPhoto = Photo(
            id = "test_photo",
            path = "/test/to_remove.jpg",
            name = "to_remove.jpg",
            creationTime = kotlin.time.Clock.System.now(),
            size = 1024L,
            extension = "jpg"
        )

        val doc = SearchDocument(
            photo = testPhoto,
            description = "A photo to be removed",
            tags = listOf("test", "remove"),
            indexedAt = kotlin.time.Clock.System.now()
        )

        searchEngine.indexPhoto(doc)

        // Verify it's indexed
        assertEquals(1, searchEngine.getIndexStats().totalDocuments)
        assertEquals(1, searchEngine.search("remove", 10).size)

        // Remove the photo
        searchEngine.removePhoto(testPhoto.path)

        // Verify it's removed
        assertEquals(0, searchEngine.getIndexStats().totalDocuments)
        assertEquals(0, searchEngine.search("remove", 10).size)
    }
}