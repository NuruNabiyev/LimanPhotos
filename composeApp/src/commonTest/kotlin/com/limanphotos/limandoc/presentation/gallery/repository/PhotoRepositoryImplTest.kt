package com.limanphotos.limandoc.data.repository

import com.limanphotos.limandoc.data.source.LocalPhotoDataSource
import com.limanphotos.limandoc.domain.model.Photo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class PhotoRepositoryImplTest {

    private val mockDataSource = mockk<LocalPhotoDataSource>()
    private val repository = PhotoRepositoryImpl(mockDataSource)

    private val testPhoto = Photo(
        id = "1",
        path = "/test/path.jpg",
        name = "test",
        creationTime = Instant.fromEpochMilliseconds(1000),
        size = 100000,
        extension = "jpg"
    )

    @Test
    fun `getAllPhotos returns photos from data source`() = runTest {
        val expectedPhotos = listOf(testPhoto)
        coEvery { mockDataSource.getAllPhotos() } returns expectedPhotos

        val result = repository.getAllPhotos()

        assertEquals(expectedPhotos, result)
    }

    @Test
    fun `searchPhotos returns search results from data source`() = runTest {
        val query = "test"
        val expectedPhotos = listOf(testPhoto)
        coEvery { mockDataSource.searchPhotos(query) } returns expectedPhotos

        val result = repository.searchPhotos(query)

        assertEquals(expectedPhotos, result)
    }

    @Test
    fun `searchPhotos with empty query returns results from data source`() = runTest {
        val query = ""
        val expectedPhotos = emptyList<Photo>()
        coEvery { mockDataSource.searchPhotos(query) } returns expectedPhotos

        val result = repository.searchPhotos(query)

        assertEquals(expectedPhotos, result)
    }
}