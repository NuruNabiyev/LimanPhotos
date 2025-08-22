package com.limanphotos.limandoc.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limanphotos.limandoc.domain.model.ImageAnalysis
import com.limanphotos.limandoc.domain.usecase.AnalyzeImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageAnalysisViewModel(
    private val analyzeImageUseCase: AnalyzeImageUseCase
) : ViewModel() {

    private val _analysisState = MutableStateFlow(
        ImageAnalysis(description = "", isLoading = false)
    )
    val analysisState: StateFlow<ImageAnalysis> = _analysisState.asStateFlow()

    fun analyzeImage(imagePath: String) {
        viewModelScope.launch {
            _analysisState.value = ImageAnalysis(description = "", isLoading = true)

            try {
                val result = analyzeImageUseCase(imagePath)
                _analysisState.value = result
            } catch (e: Exception) {
                _analysisState.value = ImageAnalysis(
                    description = "",
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun clearAnalysis() {
        _analysisState.value = ImageAnalysis(description = "", isLoading = false)
    }
}