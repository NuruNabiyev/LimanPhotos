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
class SearchPhotosUseCaseTest {

    private val mockRepository = mockk<PhotoRepository>()
    private val useCase = SearchPhotosUseCase(mockRepository)

    private val testPhoto = Photo(
        id = "1",
        path = "/test/path.jpg",
        name = "test",
        creationTime = Instant.fromEpochMilliseconds(1000),
        size = 100000,
        extension = "jpg"
    )

    @Test
    fun `invoke with query returns search results from repository`() = runTest {
        val query = "test"
        val expectedPhotos = listOf(testPhoto)
        coEvery { mockRepository.searchPhotos(query) } returns expectedPhotos

        val result = useCase(query)

        assertEquals(expectedPhotos, result)
    }

    @Test
    fun `invoke with blank query returns empty list`() = runTest {
        val result = useCase("")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `invoke with whitespace query returns empty list`() = runTest {
        val result = useCase("   ")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `invoke with null whitespace query returns empty list`() = runTest {
        val result = useCase("  \t  ")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `invoke with valid query calls repository`() = runTest {
        val query = "photo"
        val expectedPhotos = listOf(testPhoto)
        coEvery { mockRepository.searchPhotos(query) } returns expectedPhotos

        val result = useCase(query)

        assertEquals(expectedPhotos, result)
    }
}