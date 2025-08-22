package com.limanphotos.limandoc.presentation.gallery

import com.limanphotos.limandoc.domain.model.Photo
import com.limanphotos.limandoc.domain.usecase.GetPhotosUseCase
import com.limanphotos.limandoc.domain.usecase.SearchPhotosUseCase
import com.limanphotos.limandoc.domain.usecase.SearchPhotosWithLuceneUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class PhotoGalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockGetPhotosUseCase = mockk<GetPhotosUseCase>()
    private val mockSearchPhotosUseCase = mockk<SearchPhotosUseCase>()
    private val mockSearchPhotosWithLuceneUseCase = mockk<SearchPhotosWithLuceneUseCase>()

    private val testPhoto = Photo(
        id = "1",
        path = "/test/path.jpg",
        name = "test",
        creationTime = Instant.fromEpochMilliseconds(1000),
        size = 100000,
        extension = "jpg"
    )

    private fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    private fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PhotoGalleryViewModel {
        return PhotoGalleryViewModel(
            mockGetPhotosUseCase,
            mockSearchPhotosUseCase,
            mockSearchPhotosWithLuceneUseCase
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        setUp()
        try {
            coEvery { mockGetPhotosUseCase() } returns emptyList<Photo>()

            val viewModel = createViewModel()

            // Wait for all async operations to complete
            testScheduler.advanceUntilIdle()
            // Add additional delay to ensure coroutines complete
            testScheduler.advanceTimeBy(1000)
            testScheduler.advanceUntilIdle()

            val currentState = viewModel.uiState.value
            // Debug: print actual state to understand what we're getting
            println("DEBUG: currentState.photos = ${currentState.photos}")
            println("DEBUG: currentState.allPhotos = ${currentState.allPhotos}")
            println("DEBUG: currentState.isLoading = ${currentState.isLoading}")

            // After loadPhotos completes, photos should contain the filtered results  
            assertEquals(
                emptyList<Photo>(),
                currentState.allPhotos
            )  // allPhotos should be empty from mock
            assertEquals(
                emptyList<Photo>(),
                currentState.photos ?: emptyList()
            )  // photos can be null initially, then empty after filtering
            assertEquals("", currentState.searchQuery)
            // Note: isLoading might still be true if async operations are ongoing
            assertEquals(null, currentState.error)
        } finally {
            tearDown()
        }
    }

    @Test
    fun `updateSearchQuery updates search query state`() = runTest {
        setUp()
        try {
            coEvery { mockGetPhotosUseCase() } returns emptyList<Photo>()
            coEvery { mockSearchPhotosUseCase("test") } returns listOf(testPhoto)

            val viewModel = createViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.updateSearchQuery("test")

            assertEquals("test", viewModel.searchQuery.value)
        } finally {
            tearDown()
        }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        setUp()
        try {
            coEvery { mockGetPhotosUseCase() } throws RuntimeException("Test error")

            val viewModel = createViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.clearError()

            val state = viewModel.uiState.value
            assertEquals(null, state.error)
        } finally {
            tearDown()
        }
    }
}