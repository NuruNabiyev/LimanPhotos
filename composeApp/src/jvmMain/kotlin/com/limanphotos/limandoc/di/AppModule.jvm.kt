package com.limanphotos.limandoc.di

import com.limanphotos.limandoc.data.repository.AnalysisCacheRepository
import com.limanphotos.limandoc.data.repository.PhotoSearchRepositoryImpl
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository
import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

actual fun createFolderSelectionRepository(): FolderSelectionRepository {
    return FolderSelectionRepository()
}

actual fun createPhotoSearchRepository(): PhotoSearchRepository {
    return PhotoSearchRepositoryImpl()
}

actual fun createAnalysisCacheRepository(): AnalysisCacheRepository {
    val settings =
        PreferencesSettings(Preferences.userNodeForPackage(AnalysisCacheRepository::class.java))
    return AnalysisCacheRepository(settings)
}