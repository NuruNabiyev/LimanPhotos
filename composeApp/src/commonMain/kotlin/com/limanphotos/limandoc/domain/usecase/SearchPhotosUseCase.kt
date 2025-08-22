package com.limanphotos.limandoc.domain.usecase

import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.repository.PhotoRepository

class SearchPhotosUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(query: String): List<Photo> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            photoRepository.searchPhotos(query)
        }
    }
}