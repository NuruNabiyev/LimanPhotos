@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.source.ImageRecognitionDataSource
import com.limanphotos.limandoc.domain.model.ImageAnalysis
import com.limanphotos.limandoc.domain.repository.ImageRecognitionRepository
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository

class ImageRecognitionRepositoryImpl(
    private val dataSource: ImageRecognitionDataSource,
    private val photoSearchRepository: PhotoSearchRepository? = null
) : ImageRecognitionRepository {

    override suspend fun analyzeImage(imagePath: String): ImageAnalysis {
        val analysis = dataSource.analyzeImage(imagePath)

        // Note: Photo indexing is handled by SettingsViewModel during batch analysis
        // Individual photo analysis doesn't need immediate indexing

        return analysis
    }

}