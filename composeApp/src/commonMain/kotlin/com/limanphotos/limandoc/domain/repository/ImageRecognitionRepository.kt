package com.limanphotos.limandoc.domain.repository

import com.limanphotos.limandoc.domain.model.ImageAnalysis

interface ImageRecognitionRepository {
    suspend fun analyzeImage(imagePath: String): ImageAnalysis
}