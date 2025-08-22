package com.limanphotos.limandoc.domain.usecase

import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.repository.PhotoRepository

class GetPhotosUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(): List<Photo> {
        return photoRepository.getAllPhotos()
    }
}