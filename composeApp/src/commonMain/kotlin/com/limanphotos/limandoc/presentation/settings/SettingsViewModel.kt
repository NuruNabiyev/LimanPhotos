package com.limanphotos.limandoc.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limanphotos.limandoc.data.repository.AnalysisCacheRepository
import com.limanphotos.limandoc.domain.repository.PhotoRepository
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository
import com.limanphotos.limandoc.domain.usecase.AnalyzeImageUseCase
import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository
import com.limanphotos.limandoc.utils.PlatformUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SettingsViewModel(
    private val folderSelectionRepository: FolderSelectionRepository,
    private val photoRepository: PhotoRepository,
    private val photoSearchRepository: PhotoSearchRepository,
    private val analysisCacheRepository: AnalysisCacheRepository,
    private val analyzeImageUseCase: AnalyzeImageUseCase,
    private val platformUtils: PlatformUtils
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var batchAnalysisJob: kotlinx.coroutines.Job? = null
    private var onFoldersChangedCallback: (() -> Unit)? = null

    init {
        loadInitialData()
    }

    fun setOnFoldersChangedCallback(callback: () -> Unit) {
        onFoldersChangedCallback = callback
    }

    fun handleAction(action: SettingsAction) {
        when (action) {
            SettingsAction.AddFolder -> addFolder()
            is SettingsAction.RemoveFolder -> removeFolder(action.folderPath)
            SettingsAction.OpenOllamaWebsite -> openOllamaWebsite()
            SettingsAction.VerifyOllamaInstallation -> verifyOllamaInstallation()
            SettingsAction.VerifyLlavaInstallation -> verifyLlavaInstallation()
            SettingsAction.StartBatchAnalysis -> startBatchAnalysis()
            SettingsAction.StopBatchAnalysis -> stopBatchAnalysis()
            SettingsAction.ClearAllAIData -> clearAllAIData()
            SettingsAction.ClearAllPreferences -> clearAllPreferences()
            is SettingsAction.UpdateImageScale -> updateImageScale(action.scale)
            is SettingsAction.UpdateCollectionsImageScale -> updateCollectionsImageScale(action.scale)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Load selected folders
                val savedSelection = folderSelectionRepository.getSavedFolderSelection()
                val folders = savedSelection.getAllFolders().toList()

                // Load saved image scale
                val imageScale = folderSelectionRepository.getImageScale()

                // Load saved collections image scale
                val collectionsImageScale = folderSelectionRepository.getCollectionsImageScale()

                // Check AI status
                val aiStatus = determineAIStatus()

                // Calculate memory usage
                val memoryUsage = calculateMemoryUsage()

                // Check if analysis was in progress
                val analysisProgress = getAnalysisProgress()

                _state.value = _state.value.copy(
                    selectedFolders = folders,
                    imageScale = imageScale,
                    collectionsImageScale = collectionsImageScale,
                    aiStatus = aiStatus,
                    analysisProgress = analysisProgress,
                    memoryUsage = memoryUsage
                )
            } catch (e: Exception) {
                // Handle error
                println("Error loading settings: ${e.message}")
            }
        }
    }

    private suspend fun determineAIStatus(): AIStatus {
        return try {
            // Check if analysis is currently running first
            if (batchAnalysisJob?.isActive == true) {
                return AIStatus.ANALYZING
            }

            // Check if Ollama is installed by running ollama --version
            val ollamaInstalled = checkOllamaInstallation()
            if (!ollamaInstalled) {
                return AIStatus.OLLAMA_NEEDED
            }

            // Check if LLaVA is available by running ollama list
            val llavaInstalled = checkLlavaInstallation()
            if (!llavaInstalled) {
                return AIStatus.LLAVA_NEEDED
            }

            // Check if analysis is complete
            val analysisProgress = getAnalysisProgress()
            if (analysisProgress != null && analysisProgress.total > 0 && analysisProgress.completed >= analysisProgress.total) {
                return AIStatus.ANALYSIS_COMPLETE
            }

            AIStatus.READY_FOR_ANALYSIS
        } catch (e: Exception) {
            println("Error determining AI status: ${e.message}")
            AIStatus.OLLAMA_NEEDED
        }
    }

    private suspend fun checkOllamaInstallation(): Boolean {
        return try {
            val result = platformUtils.executeCommand("ollama --version")
            result.isSuccess && result.output.contains("ollama version")
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkLlavaInstallation(): Boolean {
        return try {
            val result = platformUtils.executeCommand("ollama list")
            result.isSuccess && result.output.lowercase().contains("llava")
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getAnalysisProgress(): AnalysisProgress? {
        return try {
            println("📊 getAnalysisProgress: Starting photo count...")

            val allPhotos = photoRepository.getAllPhotos()
            println("📊 getAnalysisProgress: Found ${allPhotos.size} total photos")

            if (allPhotos.isEmpty()) {
                println("📊 No photos found, returning null progress")
                return null
            }

            // Create set of current photo paths for comparison
            val currentPhotoPaths = allPhotos.map { photo -> photo.path }.toSet()
            println("📊 Current photos in selected folders: ${currentPhotoPaths.size}")

            // Get cached analyses (primary source of truth for completed analyses)
            val allCachedPaths = try {
                analysisCacheRepository.getAllCachedImagePaths().also { paths ->
                    println("📊 Found ${paths.size} cached analyses")
                }
            } catch (e: Exception) {
                println("❌ Failed to get cached analyses: ${e.message}")
                emptySet<String>()
            }

            // Filter cached analyses to only include those that still exist in current folders
            val relevantCachedPhotos = allCachedPaths.filter { cachedPath ->
                currentPhotoPaths.contains(cachedPath)
            }.toSet()

            println("📊 Relevant cached analyses (in current folders): ${relevantCachedPhotos.size}")

            // Also check search repository as a secondary confirmation
            val allIndexedResults = try {
                photoSearchRepository.searchPhotos("").also { results ->
                    println("📊 Found ${results.size} indexed photos in search repository")
                }
            } catch (e: Exception) {
                println("❌ Failed to get indexed photos: ${e.message}")
                emptyList()
            }

            val relevantIndexedPhotos = allIndexedResults.filter { result ->
                currentPhotoPaths.contains(result.photo.path)
            }.map { searchResult -> searchResult.photo.path }.toSet()

            println("📊 Relevant indexed photos (in current folders): ${relevantIndexedPhotos.size}")

            // Use the maximum of cached and indexed to handle any inconsistencies
            val analyzedPhotos = relevantCachedPhotos.union(relevantIndexedPhotos)
            val analyzedCount = analyzedPhotos.size

            println("📊 Final analysis progress: $analyzedCount/${allPhotos.size} photos analyzed")
            println("📊 Analyzed photos: ${analyzedPhotos.take(5)}...")

            AnalysisProgress(
                completed = analyzedCount,
                total = allPhotos.size
            )
        } catch (e: Exception) {
            println("❌ Failed to get analysis progress: ${e.message}")
            null
        }
    }

    /**
     * Completely restart AI status system when folders change
     * This cancels any running jobs and recalculates everything from scratch
     */
    private suspend fun restartAIStatusSystem() {
        println("🔄 Completely restarting AI status system...")

        // 1. Cancel any running batch analysis
        if (batchAnalysisJob?.isActive == true) {
            println("🛑 Cancelling active batch analysis job")
            batchAnalysisJob?.cancel()
            batchAnalysisJob = null
        }

        // 2. Reset state to loading while we recalculate everything
        _state.value = _state.value.copy(
            aiStatus = AIStatus.OLLAMA_NEEDED, // Temporarily reset
            analysisProgress = null,
            currentlyAnalyzingImage = null
        )

        // 3. Cache invalidation is now complete

        // 4. Recalculate everything from scratch
        val progress = getAnalysisProgress()
        val aiStatus = determineAIStatus()
        val memoryUsage = calculateMemoryUsage()

        println("🔄 AI status system restart complete:")
        println("   - AI Status: $aiStatus")
        println("   - Progress: ${progress?.completed}/${progress?.total}")
        println("   - Memory: ${memoryUsage.aiTextSizeMB} MB")

        // 5. Update final state
        _state.value = _state.value.copy(
            aiStatus = aiStatus,
            analysisProgress = progress,
            memoryUsage = memoryUsage
        )
    }

    private suspend fun calculateMemoryUsage(): MemoryUsage {
        return try {
            // Check if still active before making repository call
            currentCoroutineContext().ensureActive()

            // Get index stats to calculate memory usage of AI-generated text
            val indexStats = photoSearchRepository.getIndexStats()

            // Rough estimation: index size includes overhead but gives us an idea of storage
            val sizeInMB = indexStats.indexSizeBytes / (1024.0 * 1024.0)
            MemoryUsage(aiTextSizeMB = kotlin.math.round(sizeInMB * 100) / 100.0)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                // Don't log cancellation as an error
                throw e
            }
            println("⚠️ Failed to calculate memory usage: ${e.message}")
            MemoryUsage(0.0)
        }
    }

    private fun addFolder() {
        viewModelScope.launch {
            try {
                val selectedFolder = folderSelectionRepository.pickFolder()
                selectedFolder?.let { folder ->
                    val currentSelection = folderSelectionRepository.getSavedFolderSelection()
                    val currentFolders = _state.value.selectedFolders.toMutableList()
                    if (!currentFolders.contains(folder)) {
                        currentFolders.add(folder)

                        // Update the saved selection
                        val newCustomFolders = (currentSelection.customFolders + folder).distinct()
                        folderSelectionRepository.saveFolderSelection(
                            currentSelection.selectedFolders,
                            newCustomFolders
                        )

                        _state.value = _state.value.copy(
                            selectedFolders = currentFolders
                        )


                        // Completely restart AI status system to ensure accuracy
                        restartAIStatusSystem()

                        // Notify that folders have changed
                        onFoldersChangedCallback?.invoke()
                    }
                }
            } catch (e: Exception) {
                println("Error adding folder: ${e.message}")
            }
        }
    }

    private fun removeFolder(folderPath: String) {
        viewModelScope.launch {
            try {
                val currentSelection = folderSelectionRepository.getSavedFolderSelection()
                val currentFolders = _state.value.selectedFolders.toMutableList()
                currentFolders.remove(folderPath)

                // Update the saved selection
                val newSelectedFolders = currentSelection.selectedFolders - folderPath
                val newCustomFolders = currentSelection.customFolders.filter { it != folderPath }

                folderSelectionRepository.saveFolderSelection(
                    newSelectedFolders,
                    newCustomFolders
                )

                _state.value = _state.value.copy(
                    selectedFolders = currentFolders
                )

                // Remove AI generated data from the removed folder
                photoSearchRepository.removePhotosFromFolder(folderPath)

                // Completely restart AI status system to ensure accuracy
                restartAIStatusSystem()

                // Notify that folders have changed
                onFoldersChangedCallback?.invoke()
            } catch (e: Exception) {
                println("Error removing folder: ${e.message}")
            }
        }
    }

    private fun openOllamaWebsite() {
        viewModelScope.launch {
            try {
                platformUtils.openUrl("https://ollama.ai")
            } catch (e: Exception) {
                println("Error opening Ollama website: ${e.message}")
            }
        }
    }

    private fun verifyOllamaInstallation() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val aiStatus = determineAIStatus()
                _state.value = _state.value.copy(
                    aiStatus = aiStatus,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                println("Error verifying Ollama: ${e.message}")
            }
        }
    }

    private fun verifyLlavaInstallation() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val aiStatus = determineAIStatus()
                _state.value = _state.value.copy(
                    aiStatus = aiStatus,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                println("Error verifying LLaVA: ${e.message}")
            }
        }
    }

    private fun startBatchAnalysis() {
        // Prevent starting multiple jobs
        if (batchAnalysisJob?.isActive == true) {
            println("⚠️ Batch analysis already running, ignoring start request")
            return
        }

        // Cancel any existing job first
        batchAnalysisJob?.cancel()

        batchAnalysisJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(aiStatus = AIStatus.ANALYZING)
                println("🚀 Starting batch analysis")

                // Get all photos from repository
                val allPhotos = photoRepository.getAllPhotos()

                if (allPhotos.isEmpty()) {
                    println("📂 No photos found, analysis complete")
                    _state.value = _state.value.copy(aiStatus = AIStatus.ANALYSIS_COMPLETE)
                    return@launch
                }

                val totalPhotos = allPhotos.size
                println("📊 Total photos to process: $totalPhotos")

                // Get list of already analyzed photos from search repository
                val indexedPhotos = try {
                    val searchResults = photoSearchRepository.searchPhotos("")
                    val indexed = searchResults.map { it.photo.path }.toSet()
                    println("🔍 Found ${indexed.size} already indexed photos")
                    indexed
                } catch (e: Exception) {
                    println("⚠️ Failed to get indexed photos: ${e.message}")
                    // If we can't get indexed photos, assume none are indexed to avoid skipping
                    emptySet<String>()
                }

                // Filter out already analyzed photos
                val unanalyzedPhotos = allPhotos.filter { photo ->
                    !indexedPhotos.contains(photo.path)
                }

                val alreadyCompleted = totalPhotos - unanalyzedPhotos.size
                var currentCompleted = alreadyCompleted

                println("📈 Progress: $alreadyCompleted/$totalPhotos already analyzed, ${unanalyzedPhotos.size} remaining")

                // Update initial progress
                _state.value = _state.value.copy(
                    analysisProgress = AnalysisProgress(currentCompleted, totalPhotos)
                )

                // Check if all photos are already analyzed
                if (unanalyzedPhotos.isEmpty()) {
                    println("✅ All photos already analyzed, marking complete")
                    _state.value = _state.value.copy(
                        aiStatus = AIStatus.ANALYSIS_COMPLETE,
                        analysisProgress = AnalysisProgress(totalPhotos, totalPhotos)
                    )
                    return@launch
                }

                // Analyze only unanalyzed photos
                for ((index, photo) in unanalyzedPhotos.withIndex()) {
                    // Check if the coroutine is still active (less frequently)
                    if (index % 5 == 0) {
                        ensureActive()
                    }

                    // Update current image being analyzed
                    _state.value = _state.value.copy(
                        currentlyAnalyzingImage = photo.path,
                        analysisProgress = AnalysisProgress(currentCompleted, totalPhotos)
                    )

                    try {
                        println("🔍 Analyzing image ${index + 1}/${unanalyzedPhotos.size}: ${photo.path}")

                        // Analyze the image
                        val analysis = analyzeImageUseCase(photo.path)

                        if (analysis.error == null && analysis.description.isNotBlank()) {
                            // Cache the analysis for Collections feature
                            analysisCacheRepository.cacheAnalysis(photo.path, analysis.description)

                            // Index the photo with AI description (if search repository is available)
                            try {
                                photoSearchRepository.indexPhoto(
                                    photo,
                                    analysis.description,
                                    analysis.tags
                                )
                                println("📇 Indexed photo for search: ${photo.name}")
                            } catch (e: Exception) {
                                // Log but don't fail the batch analysis
                                println("⚠️ Failed to index photo for search: ${e.message}")
                            }

                            currentCompleted++
                            println("📝 Successfully cached: ${photo.path}")
                        } else {
                            println("⚠️ Skipped indexing (no description): ${photo.path}")
                        }

                        // Update progress
                        _state.value = _state.value.copy(
                            analysisProgress = AnalysisProgress(currentCompleted, totalPhotos)
                        )

                        // Update memory usage every 10 photos
                        if (currentCompleted % 10 == 0) {
                            try {
                                val memoryUsage = calculateMemoryUsage()
                                _state.value = _state.value.copy(memoryUsage = memoryUsage)
                            } catch (e: Exception) {
                                println("⚠️ Failed to update memory usage: ${e.message}")
                            }
                        }

                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) {
                            println("🛑 Batch analysis cancelled")
                            break
                        }
                        println("❌ Error analyzing image ${photo.path}: ${e.message}")
                        // Continue with next image
                    }
                }

                // Analysis complete
                println("✅ Batch analysis completed: $currentCompleted/$totalPhotos")
                _state.value = _state.value.copy(
                    aiStatus = AIStatus.ANALYSIS_COMPLETE,
                    currentlyAnalyzingImage = null,
                    analysisProgress = AnalysisProgress(currentCompleted, totalPhotos),
                    memoryUsage = calculateMemoryUsage()
                )

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    println("🛑 Batch analysis was cancelled")
                } else {
                    println("❌ Error in batch analysis: ${e.message}")
                }
                _state.value = _state.value.copy(
                    aiStatus = AIStatus.READY_FOR_ANALYSIS,
                    currentlyAnalyzingImage = null
                )
            }
        }
    }

    private fun stopBatchAnalysis() {
        batchAnalysisJob?.cancel()
        batchAnalysisJob = null

        // Just update the status and clear current image - keep existing progress
        _state.value = _state.value.copy(
            aiStatus = AIStatus.READY_FOR_ANALYSIS,
            currentlyAnalyzingImage = null
            // Keep the existing analysisProgress as-is
        )
    }

    private fun clearAllAIData() {
        viewModelScope.launch {
            try {
                // Stop any running batch analysis first
                if (batchAnalysisJob?.isActive == true) {
                    batchAnalysisJob?.cancel()
                    batchAnalysisJob = null
                }

                // Clear the search index (this removes all AI analysis data)
                photoSearchRepository.clearIndex()

                // Update UI state to reflect that no analysis is complete
                val progress = getAnalysisProgress()
                val aiStatus = determineAIStatus()
                val memoryUsage = calculateMemoryUsage()

                _state.value = _state.value.copy(
                    aiStatus = aiStatus,
                    analysisProgress = progress,
                    memoryUsage = memoryUsage,
                    currentlyAnalyzingImage = null
                )

                println("🧹 All AI analysis data has been cleared")
            } catch (e: Exception) {
                println("❌ Error clearing AI data: ${e.message}")
            }
        }
    }

    /**
     * DEBUG FUNCTION: Clear all preferences/settings data
     * This clears:
     * - Folder selection data
     * - Onboarding status
     * - Analysis cache
     * - Search index
     * - All preference stores
     */
    fun clearAllPreferences() {
        viewModelScope.launch {
            try {
                println("🧹 DEBUG: Starting to clear ALL preferences and data...")

                // Stop any running batch analysis first
                if (batchAnalysisJob?.isActive == true) {
                    batchAnalysisJob?.cancel()
                    batchAnalysisJob = null
                }

                // Clear analysis cache repository (all cached analyses)
                analysisCacheRepository.clearAllCache()
                println("🧹 Cleared analysis cache")

                // Clear search index
                photoSearchRepository.clearIndex()
                println("🧹 Cleared search index")

                // Clear folder selection and other platform preferences
                folderSelectionRepository.clearAllPreferences()
                println("🧹 Cleared platform preferences")

                // File system operations are complete

                // Reset UI state to defaults
                _state.value = SettingsState()
                println("🧹 Reset UI state")

                // Notify that folders have changed (to trigger re-scan)
                onFoldersChangedCallback?.invoke()

                println("✅ DEBUG: Successfully cleared ALL preferences and data")
                println("📝 You may need to restart the app for complete reset")

            } catch (e: Exception) {
                println("❌ Error clearing preferences: ${e.message}")
            }
        }
    }

    private fun updateImageScale(scale: Float) {
        viewModelScope.launch {
            try {
                // Clamp scale to valid range
                val clampedScale = scale.coerceIn(0.1f, 1.0f)

                // Save to preferences
                folderSelectionRepository.saveImageScale(clampedScale)

                // Update state
                _state.value = _state.value.copy(imageScale = clampedScale)

            } catch (e: Exception) {
                println("Error updating image scale: ${e.message}")
            }
        }
    }

    private fun updateCollectionsImageScale(scale: Float) {
        viewModelScope.launch {
            try {
                // Clamp scale to valid range
                val clampedScale = scale.coerceIn(0.1f, 1.0f)

                // Save to preferences
                folderSelectionRepository.saveCollectionsImageScale(clampedScale)

                // Update state
                _state.value = _state.value.copy(collectionsImageScale = clampedScale)

            } catch (e: Exception) {
                println("Error updating collections image scale: ${e.message}")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        batchAnalysisJob?.cancel()
    }
}