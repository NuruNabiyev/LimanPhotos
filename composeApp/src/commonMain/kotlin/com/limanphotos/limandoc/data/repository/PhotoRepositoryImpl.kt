package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.source.LocalPhotoDataSource
import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.repository.PhotoRepository

class PhotoRepositoryImpl(
    private val localDataSource: LocalPhotoDataSource
) : PhotoRepository {

    override suspend fun getAllPhotos(): List<Photo> {
        return localDataSource.getAllPhotos()
    }

    override suspend fun searchPhotos(query: String): List<Photo> {
        return localDataSource.searchPhotos(query)
    }
}