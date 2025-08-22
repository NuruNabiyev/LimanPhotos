package com.limanphotos.limandoc.data.source

import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository

expect class LocalPhotoDataSource(folderSelectionRepository: FolderSelectionRepository) {
    suspend fun getAllPhotos(): List<Photo>
    suspend fun searchPhotos(query: String): List<Photo>
}