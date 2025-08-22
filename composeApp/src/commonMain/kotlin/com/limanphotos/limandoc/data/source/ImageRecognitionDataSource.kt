package com.limanphotos.limandoc.data.source

import com.limanphotos.limandoc.domain.model.ImageAnalysis

expect class ImageRecognitionDataSource() {
    suspend fun analyzeImage(imagePath: String): ImageAnalysis
}