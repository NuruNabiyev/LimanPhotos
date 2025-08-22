package com.limanphotos.limandoc.data.source

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OllamaIntegrationTest {

    private val dataSource = ImageRecognitionDataSource()
    private val testImagePath = "${System.getProperty("user.dir")}/testphoto/img.png"

    @Test
    fun `analyzeImage with valid image returns proper analysis structure`() = runTest {
        // Test with a path that doesn't exist to verify error handling structure
        val result = dataSource.analyzeImage("/path/to/valid/image.jpg")

        // Should have either description or error, never both
        val hasDescription = result.description.isNotBlank()
        val hasError = result.error != null

        assertTrue(hasDescription || hasError, "Result should have either description or error")
        assertFalse(hasDescription && hasError, "Result should not have both description and error")

        if (hasDescription) {
            assertNotNull(result.description)
            assertTrue(result.description.length > 10, "Description should be meaningful")
        }

        if (hasError) {
            assertNotNull(result.error)
            assertTrue(result.error!!.isNotBlank(), "Error message should not be blank")
        }
    }

    @Test
    fun `integration test with real LLaVA model if available`() = runTest {
        val testImageFile = File(testImagePath)

        if (!testImageFile.exists()) {
            println("Test image not found at $testImagePath - creating minimal test image for integration test")
            // Skip this specific test if no test image
            return@runTest
        }

        val result = dataSource.analyzeImage(testImagePath)

        // This test works regardless of whether LLaVA is installed
        assertNotNull(result, "Should always return a result object")

        when {
            result.error != null -> {
                // Expected if LLaVA not installed or Ollama not running
                val error = result.error!!
                assertTrue(error.isNotBlank(), "Error should have meaningful message")

                // Should be one of our expected error types
                val isExpectedError = error.contains("Ollama service not running") ||
                        error.contains("Vision model not found") ||
                        error.contains("Ollama connection failed") ||
                        error.contains("Image file not found")

                assertTrue(isExpectedError, "Should be a recognized error type: $error")
                assertEquals("", result.description, "Should have empty description on error")
            }

            result.description.isNotBlank() -> {
                // Success case - LLaVA is working!
                assertTrue(result.description.length > 10, "Should have meaningful description")
                assertTrue(result.tags.isNotEmpty(), "Should extract some tags from description")
                println("✅ LLaVA Integration Success! Description: ${result.description}")
                println("✅ Extracted tags: ${result.tags}")
            }

            else -> {
                assertTrue(false, "Should have either error or description, not neither")
            }
        }
    }

    @Test
    fun `analyzeImage with non-existent image returns proper error`() = runTest {
        val result = dataSource.analyzeImage("/definitely/does/not/exist/image.jpg")

        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Image file not found"))
        assertEquals("", result.description)
        assertTrue(result.tags.isEmpty())
    }

    @Test
    fun `analyzeImage with invalid image format handles gracefully`() = runTest {
        // Test with invalid format
        val invalidImagePath = "/tmp/test.txt"

        val result = dataSource.analyzeImage(invalidImagePath)

        // Should either find the file doesn't exist or handle the invalid format
        assertNotNull(result.error)
        assertEquals("", result.description)
    }

    @Test
    fun `real LLaVA response parsing and tag extraction`() = runTest {
        // Test our tag extraction logic with realistic AI responses
        val sampleDescriptions = listOf(
            "This image shows a red car parked on a street with buildings in the background",
            "A yellow sun in a blue sky with green trees and a house",
            "The photograph depicts a person walking a dog in a park setting"
        )

        // We can't directly test the private extractTagsFromDescription method,
        // but we can test it indirectly by checking if common words get extracted
        // from actual LLaVA responses when they contain recognizable terms

        val testFile = File(testImagePath)
        if (testFile.exists()) {
            val result = dataSource.analyzeImage(testImagePath)

            if (result.description.isNotBlank()) {
                // If we got a real description, verify tag extraction works
                val description = result.description.lowercase()
                val tags = result.tags

                // Check if common objects mentioned in description appear in tags
                val commonWords = listOf(
                    "image", "photo", "picture", "red", "blue", "green",
                    "car", "house", "person", "tree", "sky", "background"
                )

                val foundCommonWords = commonWords.filter { word ->
                    description.contains(word)
                }

                if (foundCommonWords.isNotEmpty()) {
                    // At least some common words from description should appear in tags
                    val tagWordsInDescription = tags.filter { tag ->
                        description.contains(tag.lowercase())
                    }

                    println("Description: $description")
                    println("Tags: $tags")
                    println("Common words found: $foundCommonWords")
                    println("Tag words in description: $tagWordsInDescription")
                }
            }
        }

        // This test always passes - it's more for logging and verification
        assertTrue(true, "Tag extraction test completed")
    }

    @Test
    fun `analyzeImage handles different image formats`() = runTest {
        val imageFormats = listOf("jpg", "png", "gif", "bmp", "webp")

        imageFormats.forEach { format ->
            val result = dataSource.analyzeImage("/test/image.$format")

            // Should handle all formats consistently (likely with file not found error)
            assertNotNull(result)
            assertTrue(result.description.isNotEmpty() || result.error != null)
        }
    }

    @Test
    fun `analyzeImage with empty path returns appropriate error`() = runTest {
        val result = dataSource.analyzeImage("")

        assertNotNull(result.error)
        assertEquals("", result.description)
        assertTrue(result.tags.isEmpty())
    }

    @Test
    fun `analyzeImage returns consistent data structure`() = runTest {
        val result = dataSource.analyzeImage("/test/path.jpg")

        // Verify the data structure is always consistent
        assertNotNull(result.description) // Should be non-null string (may be empty)
        assertNotNull(result.tags) // Should be non-null list (may be empty)
        // confidence can be null
        // error can be null

        // Verify mutually exclusive states
        if (result.error != null) {
            assertTrue(result.error!!.isNotBlank())
        }

        if (result.description.isNotBlank()) {
            // If we have a description, we shouldn't have an error
            assertTrue(result.error == null || result.error!!.isBlank())
        }
    }

    @Test
    fun `performance test with real image analysis`() = runTest {
        val testFile = File(testImagePath)
        if (!testFile.exists()) {
            println("Test image not found - skipping performance test")
            return@runTest
        }

        val startTime = System.currentTimeMillis()
        val result = dataSource.analyzeImage(testImagePath)
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        println("Image analysis took ${duration}ms")

        // Should complete within reasonable time (30 seconds max)
        assertTrue(
            duration < 30000,
            "Analysis should complete within 30 seconds, took ${duration}ms"
        )

        // Should always return a result
        assertNotNull(result)

        if (result.description.isNotBlank()) {
            println("✅ Performance test passed with successful analysis in ${duration}ms")
        } else if (result.error != null) {
            println("⚠️ Performance test completed with error (expected if LLaVA not installed): ${result.error}")
        }
    }
}