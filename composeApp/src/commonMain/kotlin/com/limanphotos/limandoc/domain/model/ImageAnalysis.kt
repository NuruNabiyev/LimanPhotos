package com.limanphotos.limandoc.domain.model

data class ImageAnalysis(
    val description: String,
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)