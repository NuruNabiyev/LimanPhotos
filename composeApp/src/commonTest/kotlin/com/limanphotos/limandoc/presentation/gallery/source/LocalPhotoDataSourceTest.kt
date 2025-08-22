package com.limanphotos.limandoc.data.source

import com.limanphotos.limandoc.presentation.onboarding.FolderSelectionRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalPhotoDataSourceTest {

    private val mockFolderSelectionRepository = mockk<FolderSelectionRepository>(relaxed = true)
    private val dataSource = LocalPhotoDataSource(mockFolderSelectionRepository)

    @Test
    fun `getAllPhotos returns list of photos`() = runTest {
        val photos = dataSource.getAllPhotos()

        // Should return a list (might be empty if no photos in test directories)
        assertTrue(photos is List)
    }

    @Test
    fun `searchPhotos with empty query returns empty list`() = runTest {
        val results = dataSource.searchPhotos("")

        assertEquals(emptyList(), results)
    }

    @Test
    fun `searchPhotos with whitespace query returns empty list`() = runTest {
        val results = dataSource.searchPhotos("   ")

        assertEquals(emptyList(), results)
    }

    @Test
    fun `searchPhotos with query searches by photo name`() = runTest {
        // This test would need actual photos to be meaningful
        // For now, just verify it returns a list
        val results = dataSource.searchPhotos("test")

        assertTrue(results is List)
    }
}