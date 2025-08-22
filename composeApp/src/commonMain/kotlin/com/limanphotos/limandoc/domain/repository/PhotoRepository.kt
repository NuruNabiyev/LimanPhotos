package com.limanphotos.limandoc.domain.repository

import com.limanphotos.limandoc.domain.model.Photo

interface PhotoRepository {
    suspend fun getAllPhotos(): List<Photo>
    suspend fun searchPhotos(query: String): List<Photo>
}