package com.limanphotos.limandoc.di

import com.limanphotos.limandoc.data.repository.AnalysisCacheRepository
import com.limanphotos.limandoc.data.repository.CollectionsRepository
import com.limanphotos.limandoc.data.repository.ImageRecognitionRepositoryImpl
import com.limanphotos.limandoc.data.repository.PhotoRepositoryImpl
import com.limanphotos.limandoc.data.source.ImageRecognitionDataSource
import com.limanphotos.limandoc.data.source.LocalPhotoDataSource
import com.limanphotos.limandoc.domain.repository.ImageRecognitionRepository
import com.limanphotos.limandoc.domain.repository.PhotoRepository
import com.limanphotos.limandoc.domain.repository.PhotoSearchRepository
import com.limanphotos.limandoc.domain.usecase.AnalyzeImageUseCase
import com.limanphotos.limandoc.domain.usecase.GetPhotosUseCase
import com.limanphotos.limandoc.domain.usecase.SearchPhotosUseCase
import com.limanphotos.limandoc.domain.usecase.SearchPhotosWithLuceneUseCase
import com.limanphotos.limandoc.presentation.collections.CollectionsViewModel
import com.limanphotos.limandoc.presentation.components.ImageAnalysisViewModel
import com.limanphotos.limandoc.presentation.gallery.PhotoGalleryViewModel
import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository
import com.limanphotos.limandoc.presentation.settings.SettingsViewModel
import com.limanphotos.limandoc.utils.PlatformUtils

expect fun createFolderSelectionRepository(): FolderSelectionRepository
expect fun createPhotoSearchRepository(): PhotoSearchRepository
expect fun createAnalysisCacheRepository(): AnalysisCacheRepository

object AppModule {

    private val localPhotoDataSource: LocalPhotoDataSource by lazy {
        LocalPhotoDataSource(folderSelectionRepository)
    }

    private val photoRepository: PhotoRepository by lazy {
        PhotoRepositoryImpl(localPhotoDataSource)
    }

    private val getPhotosUseCase: GetPhotosUseCase by lazy {
        GetPhotosUseCase(photoRepository)
    }

    private val searchPhotosUseCase: SearchPhotosUseCase by lazy {
        SearchPhotosUseCase(photoRepository)
    }

    private val searchPhotosWithLuceneUseCase: SearchPhotosWithLuceneUseCase by lazy {
        SearchPhotosWithLuceneUseCase(
            photoSearchRepository,
            analysisCacheRepository,
            photoRepository
        )
    }

    private val imageRecognitionDataSource: ImageRecognitionDataSource by lazy {
        ImageRecognitionDataSource()
    }

    private val imageRecognitionRepository: ImageRecognitionRepository by lazy {
        ImageRecognitionRepositoryImpl(imageRecognitionDataSource, photoSearchRepository)
    }

    private val analyzeImageUseCase: AnalyzeImageUseCase by lazy {
        AnalyzeImageUseCase(imageRecognitionRepository)
    }

    private val folderSelectionRepository: FolderSelectionRepository by lazy {
        createFolderSelectionRepository()
    }

    private val photoSearchRepository: PhotoSearchRepository by lazy {
        createPhotoSearchRepository()
    }

    private val analysisCacheRepository: AnalysisCacheRepository by lazy {
        createAnalysisCacheRepository()
    }

    private val collectionsRepository: CollectionsRepository by lazy {
        CollectionsRepository(analysisCacheRepository, localPhotoDataSource)
    }

    fun providePhotoGalleryViewModel(): PhotoGalleryViewModel {
        return PhotoGalleryViewModel(
            getPhotosUseCase = getPhotosUseCase,
            searchPhotosUseCase = searchPhotosUseCase,
            searchPhotosWithLuceneUseCase = searchPhotosWithLuceneUseCase
        )
    }

    fun provideImageAnalysisViewModel(): ImageAnalysisViewModel {
        return ImageAnalysisViewModel(analyzeImageUseCase)
    }

    fun providePhotoSearchRepository(): PhotoSearchRepository {
        return photoSearchRepository
    }

    fun provideSettingsViewModel(): SettingsViewModel {
        return SettingsViewModel(
            folderSelectionRepository = folderSelectionRepository,
            photoRepository = photoRepository,
            photoSearchRepository = photoSearchRepository,
            analysisCacheRepository = analysisCacheRepository,
            analyzeImageUseCase = analyzeImageUseCase,
            platformUtils = PlatformUtils
        )
    }

    fun provideCollectionsViewModel(): CollectionsViewModel {
        return CollectionsViewModel(collectionsRepository)
    }
}