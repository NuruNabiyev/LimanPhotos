package com.limanphotos.limandoc.di

import com.limanphotos.limandoc.data.repository.PhotoSearchRepositoryStub
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository
import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository

actual fun createFolderSelectionRepository(): FolderSelectionRepository {
    return FolderSelectionRepository()
}

actual fun createPhotoSearchRepository(): PhotoSearchRepository {
    // Return stub implementation for Android (not implemented yet)
    return PhotoSearchRepositoryStub()
}