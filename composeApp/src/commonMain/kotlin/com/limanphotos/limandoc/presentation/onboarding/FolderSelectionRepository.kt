package com.limanphotos.limandoc.presentation.onboarding

import kotlinx.serialization.Serializable

data class DefaultFolders(
    val downloads: String,
    val documents: String,
    val desktop: String
)

@Serializable
data class SavedFolderSelection(
    val selectedFolders: Set<String>,
    val customFolders: List<String>
) {
    /**
     * Get all folders (both from onboarding and added via "Add Folder")
     */
    fun getAllFolders(): Set<String> = selectedFolders + customFolders.toSet()
}

expect class FolderSelectionRepository {
    suspend fun getDefaultFolders(): DefaultFolders
    suspend fun pickFolder(): String?
    suspend fun saveFolderSelection(selectedFolders: Set<String>, customFolders: List<String>)
    suspend fun getSavedFolderSelection(): SavedFolderSelection
    suspend fun clearAllPreferences()
    suspend fun saveImageScale(scale: Float)
    suspend fun getImageScale(): Float
    suspend fun saveCollectionsImageScale(scale: Float)
    suspend fun getCollectionsImageScale(): Float
}