package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.source.ImageRecognitionDataSource
import com.limanphotos.limandoc.domain.model.ImageAnalysis
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageRecognitionRepositoryImplTest {

    private val mockDataSource = mockk<ImageRecognitionDataSource>()
    private val repository = ImageRecognitionRepositoryImpl(mockDataSource)

    private val testImagePath = "/test/path/image.jpg"
    private val testAnalysis = ImageAnalysis(
        description = "A cat sitting on a windowsill",
        tags = listOf("cat", "animal", "pet", "window")
    )

    @Test
    fun `analyzeImage returns analysis from data source`() = runTest {
        coEvery { mockDataSource.analyzeImage(testImagePath) } returns testAnalysis

        val result = repository.analyzeImage(testImagePath)

        assertEquals(testAnalysis, result)
    }

    @Test
    fun `analyzeImage propagates exception from data source`() = runTest {
        val errorMessage = "Ollama service unavailable"
        coEvery { mockDataSource.analyzeImage(testImagePath) } throws RuntimeException(errorMessage)

        try {
            repository.analyzeImage(testImagePath)
            kotlin.test.fail("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals(errorMessage, e.message)
        }
    }

    @Test
    fun `analyzeImage handles different image formats`() = runTest {
        val pngImagePath = "/test/path/image.png"
        val pngAnalysis = ImageAnalysis(description = "PNG image content")
        coEvery { mockDataSource.analyzeImage(pngImagePath) } returns pngAnalysis

        val result = repository.analyzeImage(pngImagePath)

        assertEquals(pngAnalysis, result)
    }
}