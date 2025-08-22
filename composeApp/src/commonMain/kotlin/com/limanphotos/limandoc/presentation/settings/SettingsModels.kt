package com.limanphotos.limandoc.presentation.settings

import androidx.compose.runtime.Immutable

/**
 * Settings screen state
 */
@Immutable
data class SettingsState(
    val selectedFolders: List<String> = emptyList(),
    val aiStatus: AIStatus = AIStatus.OLLAMA_NEEDED,
    val analysisProgress: AnalysisProgress? = null,
    val currentlyAnalyzingImage: String? = null,
    val memoryUsage: MemoryUsage = MemoryUsage(0.0),
    val isLoading: Boolean = false,
    val imageScale: Float = 0.4f, // Default scale for image thumbnails (0.1 to 1.0)
    val collectionsImageScale: Float = 0.4f // Default scale for collections preview images (0.1 to 1.0)
)

/**
 * AI installation and analysis status
 */
enum class AIStatus {
    OLLAMA_NEEDED,          // Ollama not installed
    LLAVA_NEEDED,           // Ollama installed, LLaVA not downloaded
    READY_FOR_ANALYSIS,     // Both installed, ready to analyze
    ANALYZING,              // Currently analyzing images
    ANALYSIS_COMPLETE       // All images analyzed
}

/**
 * Analysis progress information
 */
@Immutable
data class AnalysisProgress(
    val completed: Int,
    val total: Int
) {
    val percentage: Float get() = if (total > 0) completed.toFloat() / total else 0f
}

/**
 * Memory usage information
 */
@Immutable
data class MemoryUsage(
    val aiTextSizeMB: Double
)

/**
 * Settings actions
 */
sealed class SettingsAction {
    // Folder management
    data object AddFolder : SettingsAction()
    data class RemoveFolder(val folderPath: String) : SettingsAction()

    // AI status management
    data object OpenOllamaWebsite : SettingsAction()
    data object VerifyOllamaInstallation : SettingsAction()
    data object VerifyLlavaInstallation : SettingsAction()
    data object StartBatchAnalysis : SettingsAction()
    data object StopBatchAnalysis : SettingsAction()
    data object ClearAllAIData : SettingsAction()

    // Display settings
    data class UpdateImageScale(val scale: Float) : SettingsAction()
    data class UpdateCollectionsImageScale(val scale: Float) : SettingsAction()

    // Debug management
    data object ClearAllPreferences : SettingsAction()
}