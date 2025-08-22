@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.source

import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository
import com.limanphotos.limandoc.utils.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.time.Instant

actual class LocalPhotoDataSource actual constructor(
    private val folderSelectionRepository: FolderSelectionRepository
) {

    private val photoExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "raw")

    actual suspend fun getAllPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        println("📁 Scanning all selected folders for photos...")

        val selectedFolders = getSelectedFolders()
        val photos = mutableListOf<Photo>()

        selectedFolders.forEach { folderPath ->
            val dir = File(folderPath)
            if (dir.exists() && dir.isDirectory) {
                println("📁 Scanning folder: $folderPath")
                scanDirectory(dir, photos)
            } else {
                println("⚠️ Folder does not exist: $folderPath")
            }
        }

        println("📁 Found ${photos.size} total photos")
        photos.sortedByDescending { it.creationTime }
    }

    actual suspend fun searchPhotos(query: String): List<Photo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }

        val allPhotos = getAllPhotos()

        allPhotos.filter { photo ->
            photo.name.contains(query, ignoreCase = true)
        }.sortedByDescending { it.creationTime }
    }


    private fun scanDirectory(directory: File, photos: MutableList<Photo>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile && isImageFile(file) -> {
                        val photo = createPhotoFromFile(file)
                        photo?.let { photos.add(it) }
                    }

                    file.isDirectory -> {
                        scanDirectory(file, photos)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error scanning directory ${directory.absolutePath}: ${e.message}")
        }
    }

    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in photoExtensions
    }

    private fun createPhotoFromFile(file: File): Photo? {
        return try {
            val fileSize = file.length()

            // Filter out images smaller than 50KB
            if (fileSize < 50 * 1024) {
                return null
            }

            val path = file.toPath()
            val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
            val creationTime = Instant.fromEpochMilliseconds(attributes.creationTime().toMillis())

            // Read image dimensions efficiently
            val dimensions = PlatformUtils.getImageDimensions(file)
            val (width, height) = dimensions ?: (0 to 0)

            Photo(
                id = UUID.randomUUID().toString(),
                path = file.absolutePath,
                name = file.nameWithoutExtension,
                creationTime = creationTime,
                size = fileSize,
                extension = file.extension.lowercase(),
                width = width,
                height = height
            )
        } catch (e: Exception) {
            println("Error creating photo from file ${file.absolutePath}: ${e.message}")
            null
        }
    }

    private suspend fun getSelectedFolders(): Set<String> {
        return try {
            val savedSelection = folderSelectionRepository.getSavedFolderSelection()
            val allFolders = savedSelection.getAllFolders()
            println("📂 Using folders: ${allFolders.joinToString(", ")}")
            allFolders
        } catch (e: Exception) {
            // Fallback: return empty set when no folder selection exists
            // This way the UI will show "No photos" instead of using default folders the user didn't choose
            println("⚠️ No folder selection found: ${e.message}")
            emptySet()
        }
    }

}