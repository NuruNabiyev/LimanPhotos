package com.limanphotos.limandoc.data.source

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageRecognitionDataSourceTest {

    private val dataSource = ImageRecognitionDataSource()

    // Path to test image from com.limanphotos.limandoc root
    private val testImagePath = "${System.getProperty("user.dir")}/testphoto/img.png"

    @Test
    fun `analyzeImage returns error for non-existent file`() = runTest {
        val nonExistentPath = "/path/that/does/not/exist/image.jpg"

        val result = dataSource.analyzeImage(nonExistentPath)

        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Image file not found"))
        assertEquals("", result.description)
    }

    @Test
    fun `analyzeImage handles empty path`() = runTest {
        val emptyPath = ""

        val result = dataSource.analyzeImage(emptyPath)

        assertNotNull(result.error)
        assertEquals("", result.description)
    }

    @Test
    fun `analyzeImage handles invalid file format gracefully`() = runTest {
        // This will test the error handling for invalid files
        val invalidPath = "/invalid/path/file.txt"

        val result = dataSource.analyzeImage(invalidPath)

        assertNotNull(result.error)
        assertEquals("", result.description)
    }

    @Test
    fun `analyzeImage with real test image when LLaVA available`() = runTest {
        val testImageFile = File(testImagePath)

        if (!testImageFile.exists()) {
            // Skip test if image doesn't exist
            println("Test image not found at $testImagePath - skipping LLaVA test")
            return@runTest
        }

        val result = dataSource.analyzeImage(testImagePath)

        // The result should either be successful analysis or a helpful error
        assertTrue(
            (result.description.isNotBlank() && result.error == null) ||
                    (result.error != null && result.description.isEmpty()),
            "Result should have either description OR error, not both"
        )

        if (result.error != null) {
            // If there's an error, it should be informative
            val error = result.error!!
            assertTrue(
                error.contains("Ollama") ||
                        error.contains("model") ||
                        error.contains("llava") ||
                        error.contains("Connection refused"),
                "Error should provide helpful information about Ollama/model status: $error"
            )
        } else {
            // If successful, should have meaningful content
            assertTrue(result.description.length > 10, "Description should be meaningful")
            assertTrue(
                result.tags.isNotEmpty() || result.description.contains("image"),
                "Should have tags or mention image content"
            )
        }
    }

    @Test
    fun `analyzeImage validates image format correctly`() = runTest {
        val validFormats = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff")
        val invalidFormats = listOf("txt", "pdf", "doc", "mp4")

        validFormats.forEach { format ->
            val path = "/test/valid/image.$format"
            val result = dataSource.analyzeImage(path)

            // Should not fail due to format (will fail due to file not found)
            if (result.error != null) {
                assertFalse(
                    result.error!!.contains("Unsupported image format"),
                    "Format $format should be supported"
                )
            }
        }

        invalidFormats.forEach { format ->
            val path = "/test/invalid/file.$format"
            val result = dataSource.analyzeImage(path)

            // Should fail due to format if file exists, or file not found if it doesn't
            assertNotNull(result.error)
        }
    }

    @Test
    fun `analyzeImage handles different Ollama states gracefully`() = runTest {
        // This test validates our error handling for different Ollama conditions
        val result = dataSource.analyzeImage(testImagePath)

        // Should always return a valid ImageAnalysis object
        assertNotNull(result)
        assertNotNull(result.description) // Should be non-null string (may be empty)
        assertNotNull(result.tags) // Should be non-null list (may be empty)

        if (result.error != null) {
            // Error should be informative and actionable
            val error = result.error!!
            assertTrue(error.isNotBlank(), "Error message should not be blank")

            // Check for expected error patterns
            val hasExpectedError = error.contains("Ollama service not running") ||
                    error.contains("Vision model not found") ||
                    error.contains("Image file not found") ||
                    error.contains("Ollama connection failed") ||
                    error.contains("Unsupported image format")

            assertTrue(hasExpectedError, "Error should be one of the expected types: $error")
        }

        // Ensure mutually exclusive states
        val hasDescription = result.description.isNotBlank()
        val hasError = result.error != null

        if (hasDescription && hasError) {
            // This should not happen - either success or error, not both
            assertTrue(false, "Should not have both description and error")
        }
    }
}