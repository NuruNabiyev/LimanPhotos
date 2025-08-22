package com.limanphotos.limandoc.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.usecase.GetPhotosUseCase
import com.limanphotos.limandoc.domain.usecase.SearchPhotosUseCase
import com.limanphotos.limandoc.domain.usecase.SearchPhotosWithLuceneUseCase
import com.limanphotos.limandoc.presentation.components.PhotoFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoGalleryUiState(
    val photos: List<Photo>? = null,
    val allPhotos: List<Photo> = emptyList(), // Keep original list for filtering
    val searchQuery: String = "",
    val activeFilters: PhotoFilters = PhotoFilters.EMPTY,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val error: String? = null,
    val searchMode: SearchMode = SearchMode.FILENAME
)

enum class SearchMode {
    FILENAME,   // Search by filename only (fallback)
    LUCENE      // Search using Lucene full-text search
}

class PhotoGalleryViewModel(
    private val getPhotosUseCase: GetPhotosUseCase,
    private val searchPhotosUseCase: SearchPhotosUseCase,
    private val searchPhotosWithLuceneUseCase: SearchPhotosWithLuceneUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoGalleryUiState())
    val uiState: StateFlow<PhotoGalleryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilters = MutableStateFlow(PhotoFilters.EMPTY)

    init {
        loadPhotos()
        observeSearchQuery()
        observeFilters()
    }

    val isLoading: Boolean
        get() = _uiState.value.isLoading

    private fun loadPhotos() {
        viewModelScope.launch(Dispatchers.IO) {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Scanning folders for photos..."
                )
            }
            try {
                val photos = getPhotosUseCase()
                _uiState.update {
                    it.copy(
                        photos = applyFilters(photos, _activeFilters.value),
                        allPhotos = photos,
                        isLoading = false,
                        loadingMessage = "",
                        error = null
                    )
                }
                println("📷 Loaded ${photos.size} photos from selected folders")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }


    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    if (isLoading) {
                        println("LOADING_PHOTOS: observeSearchQuery")
                        return@collect
                    }

                    // Always handle the query change, whether blank or not
                    if (query.isNotBlank()) {
                        searchPhotos(query)
                    } else {
                        // Reset to show all photos with filters applied
                        val currentPhotos = _uiState.value.allPhotos
                        val filteredPhotos = applyFilters(currentPhotos, _activeFilters.value)
                        _uiState.update {
                            it.copy(
                                photos = filteredPhotos,
                                searchQuery = query,
                                searchMode = SearchMode.FILENAME,
                                isLoading = false,
                                loadingMessage = ""
                            )
                        }
                        println("🔄 Reset to show all ${filteredPhotos.size} photos (cleared search)")
                    }
                }
        }
    }

    private fun searchPhotos(query: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = if (query.isBlank()) "Loading all photos..." else "Searching for \"$query\"..."
                )
            }
            try {
                if (query.isBlank()) {
                    // When no search query, just apply filters to all photos
                    val currentPhotos = _uiState.value.allPhotos
                    val filteredPhotos = applyFilters(currentPhotos, _activeFilters.value)
                    _uiState.update {
                        it.copy(
                            photos = filteredPhotos,
                            searchQuery = query,
                            isLoading = false,
                            loadingMessage = ""
                        )
                    }
                } else {
                    // Try Lucene search first, fallback to filename search
                    val luceneResults = try {
                        searchPhotosWithLuceneUseCase(query)
                    } catch (e: Exception) {
                        println("⚠️ Lucene search failed, falling back to filename search: ${e.message}")
                        emptyList()
                    }

                    if (luceneResults.isNotEmpty()) {
                        // Use Lucene search results and apply filters
                        val photos = luceneResults.map { it.photo }
                        val filteredPhotos = applyFilters(photos, _activeFilters.value)
                        _uiState.update {
                            it.copy(
                                photos = filteredPhotos,
                                searchQuery = query,
                                searchMode = SearchMode.LUCENE,
                                isLoading = false,
                                loadingMessage = ""
                            )
                        }
                        println("🔍 Lucene search returned ${luceneResults.size} results, filtered to ${filteredPhotos.size} for '$query'")
                    } else {
                        // Fallback to filename search and apply filters
                        val searchResults = searchPhotosUseCase(query)
                        val filteredPhotos = applyFilters(searchResults, _activeFilters.value)
                        _uiState.update {
                            it.copy(
                                photos = filteredPhotos,
                                searchQuery = query,
                                searchMode = SearchMode.FILENAME,
                                isLoading = false,
                                loadingMessage = ""
                            )
                        }
                        println("📝 Filename search returned ${searchResults.size} results, filtered to ${filteredPhotos.size} for '$query'")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        error = e.message ?: "Search failed"
                    )
                }
            }
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            _activeFilters.collect { filters ->
                if (isLoading) {
                    println("LOADING_PHOTOS: observeFilters")
                    return@collect
                }

                // Re-apply current search with new filters
                val currentQuery = _searchQuery.value
                val currentPhotos = if (currentQuery.isBlank()) {
                    _uiState.value.allPhotos
                } else {
                    _uiState.value.photos // Already searched photos
                } ?: emptyList<Photo>()


                val filteredPhotos = applyFilters(currentPhotos, filters)
                _uiState.update {
                    it.copy(
                        photos = filteredPhotos,
                        activeFilters = filters
                    )
                }
            }
        }
    }

    private fun applyFilters(photos: List<Photo>, filters: PhotoFilters): List<Photo> {
        if (filters.isEmpty()) {
            return photos
        }

        return photos.filter { photo ->
            filters.matches(photo)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun applyFilters(filters: PhotoFilters) {
        _activeFilters.value = filters
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refreshPhotos() {
        loadPhotos()
    }

    /**
     * Explicitly reset to show all photos with current filters applied
     */
    fun resetToAllPhotos() {
        viewModelScope.launch {
            _searchQuery.value = ""  // This will trigger observeSearchQuery to reset
        }
    }

    /**
     * Show specific photos from a collection (bypasses search to ensure exact match)
     */
    fun showCollectionPhotos(collectionPhotos: List<Photo>, searchQuery: String) {
        viewModelScope.launch {
            val filteredPhotos = applyFilters(collectionPhotos, _activeFilters.value)

            // Update UI state with collection photos
            _uiState.update {
                it.copy(
                    photos = filteredPhotos,
                    searchQuery = searchQuery, // Set in UI state for display purposes
                    searchMode = SearchMode.LUCENE, // Mark as collection view
                    isLoading = false,
                    loadingMessage = "",
                    error = null
                )
            }

            // Important: Also update _searchQuery to keep it in sync with UI state
            // This ensures that clearing the search will properly trigger reset
            _searchQuery.value = searchQuery

            println("📊 Showing ${filteredPhotos.size} photos from collection '$searchQuery' (${collectionPhotos.size} total, filtered to ${filteredPhotos.size})")
        }
    }
}