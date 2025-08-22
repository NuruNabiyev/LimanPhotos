package com.limanphotos.limandoc.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FolderSelectionUiState(
    val selectedFolders: Set<String> = emptySet(),
    val customFolders: List<String> = emptyList(),
    val downloadsPath: String = "",
    val documentsPath: String = "",
    val desktopPath: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class FolderSelectionViewModel(
    private val folderSelectionRepository: FolderSelectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderSelectionUiState())
    val uiState: StateFlow<FolderSelectionUiState> = _uiState.asStateFlow()

    init {
        initializeDefaultFolders()
        loadSavedFolders()
    }

    private fun initializeDefaultFolders() {
        viewModelScope.launch {
            try {
                val defaultFolders = folderSelectionRepository.getDefaultFolders()
                _uiState.value = _uiState.value.copy(
                    downloadsPath = defaultFolders.downloads,
                    documentsPath = defaultFolders.documents,
                    desktopPath = defaultFolders.desktop
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load default folders: ${e.message}"
                )
            }
        }
    }

    private fun loadSavedFolders() {
        viewModelScope.launch {
            try {
                val savedSelection = folderSelectionRepository.getSavedFolderSelection()
                _uiState.value = _uiState.value.copy(
                    selectedFolders = savedSelection.selectedFolders,
                    customFolders = savedSelection.customFolders
                )
            } catch (e: Exception) {
                // If no saved selection exists, use default Downloads folder
                _uiState.value = _uiState.value.copy(
                    selectedFolders = setOf(_uiState.value.downloadsPath)
                )
            }
        }
    }

    fun toggleFolder(folderPath: String) {
        val currentSelected = _uiState.value.selectedFolders
        val newSelected = if (currentSelected.contains(folderPath)) {
            currentSelected - folderPath
        } else {
            currentSelected + folderPath
        }

        _uiState.value = _uiState.value.copy(selectedFolders = newSelected)
    }

    fun addCustomFolder(folderPath: String) {
        val currentCustom = _uiState.value.customFolders
        if (!currentCustom.contains(folderPath)) {
            _uiState.value = _uiState.value.copy(
                customFolders = currentCustom + folderPath,
                selectedFolders = _uiState.value.selectedFolders + folderPath
            )
        }
    }

    fun removeCustomFolder(folderPath: String) {
        _uiState.value = _uiState.value.copy(
            customFolders = _uiState.value.customFolders - folderPath,
            selectedFolders = _uiState.value.selectedFolders - folderPath
        )
    }

    fun openFolderPicker() {
        viewModelScope.launch {
            try {
                val selectedFolder = folderSelectionRepository.pickFolder()
                if (selectedFolder != null) {
                    addCustomFolder(selectedFolder)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to pick folder: ${e.message}"
                )
            }
        }
    }

    fun saveFolderSelection() {
        viewModelScope.launch {
            try {
                folderSelectionRepository.saveFolderSelection(
                    selectedFolders = _uiState.value.selectedFolders,
                    customFolders = _uiState.value.customFolders
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save folder selection: ${e.message}"
                )
            }
        }
    }

}