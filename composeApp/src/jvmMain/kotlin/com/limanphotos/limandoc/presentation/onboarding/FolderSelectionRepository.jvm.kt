package com.limanphotos.limandoc.presentation.onboarding

import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser

actual class FolderSelectionRepository {

    private val settings = PreferencesSettings(
        Preferences.userNodeForPackage(FolderSelectionRepository::class.java)
    )

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    actual suspend fun getDefaultFolders(): DefaultFolders = withContext(Dispatchers.IO) {
        val userHome = System.getProperty("user.home")

        DefaultFolders(
            downloads = getOrCreatePath("$userHome/Downloads"),
            documents = getOrCreatePath("$userHome/Documents"),
            desktop = getOrCreatePath("$userHome/Desktop")
        )
    }

    actual suspend fun pickFolder(): String? {
        return try {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            fileChooser.dialogTitle = "Select Folder for Photo Analysis"
            fileChooser.currentDirectory = File(System.getProperty("user.home"))

            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error picking folder: ${e.message}")
            null
        }
    }

    actual suspend fun saveFolderSelection(
        selectedFolders: Set<String>,
        customFolders: List<String>
    ) = withContext(Dispatchers.IO) {
        try {
            val selection = SavedFolderSelection(selectedFolders, customFolders)
            val jsonString = json.encodeToString(selection)
            settings.putString(KEY_FOLDER_SELECTION, jsonString)
            settings.putBoolean(KEY_ONBOARDING_COMPLETED, true)
        } catch (e: Exception) {
            throw RuntimeException("Failed to save folder selection", e)
        }
    }

    actual suspend fun getSavedFolderSelection(): SavedFolderSelection =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = settings.getStringOrNull(KEY_FOLDER_SELECTION)
                if (jsonString == null) {
                    // Return empty selection if no saved selection exists
                    println("🔧 No saved folder selection found, returning empty selection")
                    return@withContext SavedFolderSelection(emptySet(), emptyList())
                }

                json.decodeFromString<SavedFolderSelection>(jsonString)
            } catch (e: Exception) {
                println("⚠️ Failed to parse saved folder selection: ${e.message}, returning empty selection")
                // Return empty selection if parsing fails
                SavedFolderSelection(emptySet(), emptyList())
            }
        }

    private fun getOrCreatePath(path: String): String {
        val file = File(path)
        return if (file.exists() && file.isDirectory) {
            file.absolutePath
        } else {
            // Fallback to user home if the common folder doesn't exist
            System.getProperty("user.home")
        }
    }

    /**
     * DEBUG FUNCTION: Clear all preferences stored by this repository
     */
    actual suspend fun clearAllPreferences() = withContext(Dispatchers.IO) {
        try {
            println("🧹 Clearing FolderSelectionRepository preferences...")
            settings.remove(KEY_FOLDER_SELECTION)
            settings.remove(KEY_ONBOARDING_COMPLETED)
            println("✅ Cleared folder selection preferences")
        } catch (e: Exception) {
            println("❌ Error clearing folder selection preferences: ${e.message}")
        }
    }

    /**
     * Save image scale preference
     */
    actual suspend fun saveImageScale(scale: Float) = withContext(Dispatchers.IO) {
        try {
            settings.putFloat(KEY_IMAGE_SCALE, scale)
        } catch (e: Exception) {
            println("❌ Error saving image scale: ${e.message}")
        }
    }

    /**
     * Get saved image scale preference
     */
    actual suspend fun getImageScale(): Float = withContext(Dispatchers.IO) {
        try {
            settings.getFloat(KEY_IMAGE_SCALE, 0.4f) // Default to 0.4
        } catch (e: Exception) {
            println("❌ Error getting image scale: ${e.message}")
            0.4f // Default fallback
        }
    }

    /**
     * Save collections image scale preference
     */
    actual suspend fun saveCollectionsImageScale(scale: Float) = withContext(Dispatchers.IO) {
        try {
            settings.putFloat(KEY_COLLECTIONS_IMAGE_SCALE, scale)
        } catch (e: Exception) {
            println("❌ Error saving collections image scale: ${e.message}")
        }
    }

    /**
     * Get saved collections image scale preference
     */
    actual suspend fun getCollectionsImageScale(): Float = withContext(Dispatchers.IO) {
        try {
            settings.getFloat(KEY_COLLECTIONS_IMAGE_SCALE, 0.1f) // Default to 0.1
        } catch (e: Exception) {
            println("❌ Error getting collections image scale: ${e.message}")
            0.1f // Default fallback
        }
    }

    companion object {
        private const val KEY_FOLDER_SELECTION = "folder_selection"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_IMAGE_SCALE = "image_scale"
        private const val KEY_COLLECTIONS_IMAGE_SCALE = "collections_image_scale"
    }
}