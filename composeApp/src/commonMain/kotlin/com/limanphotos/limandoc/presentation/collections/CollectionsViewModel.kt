package com.limanphotos.limandoc.presentation.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limanphotos.limandoc.data.repository.CollectionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.limanphotos.limandoc.domain.model.Collection as PhotoCollection

/**
 * ViewModel for the Collections screen
 */
class CollectionsViewModel(
    private val collectionsRepository: CollectionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUIState())
    val uiState: StateFlow<CollectionsUIState> = _uiState.asStateFlow()

    init {
        loadCollections()
    }

    /**
     * Load collections from repository
     */
    fun loadCollections() {
        println("🎯 CollectionsViewModel: Starting to load collections...")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                println("🎯 CollectionsViewModel: Calling repository...")
                val collections = collectionsRepository.getAllCollections()
                println("🎯 CollectionsViewModel: Got ${collections.size} collections from repository")
                _uiState.value = _uiState.value.copy(
                    collections = collections,
                    isLoading = false,
                    error = null
                )
                println("🎯 CollectionsViewModel: Updated UI state with collections")
            } catch (e: Exception) {
                println("🎯 CollectionsViewModel: Error loading collections: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load collections: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh collections
     */
    fun refreshCollections() {
        loadCollections()
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the Collections screen
 */
data class CollectionsUIState(
    val collections: List<PhotoCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasCollections: Boolean
        get() = collections.isNotEmpty()
}