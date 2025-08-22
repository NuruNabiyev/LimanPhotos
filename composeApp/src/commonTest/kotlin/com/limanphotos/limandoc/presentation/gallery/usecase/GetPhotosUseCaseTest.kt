package com.limanphotos.limandoc.domain.usecase

import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.repository.PhotoRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GetPhotosUseCaseTest {

    private val mockRepository = mockk<PhotoRepository>()
    private val useCase = GetPhotosUseCase(mockRepository)

    private val testPhoto = Photo(
        id = "1",
        path = "/test/path.jpg",
        name = "test",
        creationTime = Instant.fromEpochMilliseconds(1000),
        size = 100000,
        extension = "jpg"
    )

    @Test
    fun `invoke returns photos from repository`() = runTest {
        val expectedPhotos = listOf(testPhoto)
        coEvery { mockRepository.getAllPhotos() } returns expectedPhotos

        val result = useCase()

        assertEquals(expectedPhotos, result)
    }

    @Test
    fun `invoke returns empty list when repository has no photos`() = runTest {
        val expectedPhotos = emptyList<Photo>()
        coEvery { mockRepository.getAllPhotos() } returns expectedPhotos

        val result = useCase()

        assertEquals(expectedPhotos, result)
    }
}