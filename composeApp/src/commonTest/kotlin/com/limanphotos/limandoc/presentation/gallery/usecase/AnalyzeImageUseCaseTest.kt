package com.limanphotos.limandoc.domain.usecase

import com.limanphotos.limandoc.domain.model.ImageAnalysis
import com.limanphotos.limandoc.domain.repository.ImageRecognitionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyzeImageUseCaseTest {

    private val mockRepository = mockk<ImageRecognitionRepository>()
    private val useCase = AnalyzeImageUseCase(mockRepository)

    private val testImagePath = "/test/path/image.jpg"
    private val testAnalysis = ImageAnalysis(
        description = "A beautiful landscape with mountains and trees",
        tags = listOf("landscape", "mountains", "trees", "nature")
    )

    @Test
    fun `invoke returns analysis from repository when successful`() = runTest {
        coEvery { mockRepository.analyzeImage(testImagePath) } returns testAnalysis

        val result = useCase(testImagePath)

        assertEquals(testAnalysis, result)
    }

    @Test
    fun `invoke returns error analysis when repository throws exception`() = runTest {
        val errorMessage = "Network error"
        coEvery { mockRepository.analyzeImage(testImagePath) } throws RuntimeException(errorMessage)

        val result = useCase(testImagePath)

        assertEquals("", result.description)
        assertEquals(errorMessage, result.error)
        assertEquals(emptyList(), result.tags)
    }

    @Test
    fun `invoke returns generic error when repository throws exception without message`() =
        runTest {
            coEvery { mockRepository.analyzeImage(testImagePath) } throws RuntimeException()

            val result = useCase(testImagePath)

            assertEquals("", result.description)
            assertEquals("Failed to analyze image", result.error)
        }

    @Test
    fun `invoke handles empty image path`() = runTest {
        val emptyPath = ""
        val expectedAnalysis = ImageAnalysis(description = "No content found")
        coEvery { mockRepository.analyzeImage(emptyPath) } returns expectedAnalysis

        val result = useCase(emptyPath)

        assertEquals(expectedAnalysis, result)
    }
}