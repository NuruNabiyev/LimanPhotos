package com.limanphotos.limandoc.domain.usecase

import com.limanphotos.limandoc.domain.model.ImageAnalysis
import com.limanphotos.limandoc.domain.repository.ImageRecognitionRepository

class AnalyzeImageUseCase(
    private val imageRecognitionRepository: ImageRecognitionRepository
) {
    suspend operator fun invoke(imagePath: String): ImageAnalysis {
        return try {
            imageRecognitionRepository.analyzeImage(imagePath)
        } catch (e: Exception) {
            ImageAnalysis(
                description = "",
                error = e.message ?: "Failed to analyze image"
            )
        }
    }
}