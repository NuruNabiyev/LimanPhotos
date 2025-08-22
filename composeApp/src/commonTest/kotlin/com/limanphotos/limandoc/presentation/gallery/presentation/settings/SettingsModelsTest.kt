package com.limanphotos.limandoc.presentation.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsModelsTest {

    @Test
    fun `AnalysisProgress calculates percentage correctly`() {
        val progress = AnalysisProgress(completed = 25, total = 100)
        assertEquals(0.25f, progress.percentage)
    }

    @Test
    fun `AnalysisProgress handles zero total`() {
        val progress = AnalysisProgress(completed = 0, total = 0)
        assertEquals(0f, progress.percentage)
    }

    @Test
    fun `AnalysisProgress handles completed equals total`() {
        val progress = AnalysisProgress(completed = 50, total = 50)
        assertEquals(1.0f, progress.percentage)
    }

    @Test
    fun `MemoryUsage stores size correctly`() {
        val memoryUsage = MemoryUsage(aiTextSizeMB = 15.75)
        assertEquals(15.75, memoryUsage.aiTextSizeMB)
    }

    @Test
    fun `SettingsState has correct default values`() {
        val defaultState = SettingsState()

        assertTrue(defaultState.selectedFolders.isEmpty())
        assertEquals(AIStatus.OLLAMA_NEEDED, defaultState.aiStatus)
        assertEquals(null, defaultState.analysisProgress)
        assertEquals(null, defaultState.currentlyAnalyzingImage)
        assertEquals(0.0, defaultState.memoryUsage.aiTextSizeMB)
        assertFalse(defaultState.isLoading)
    }

    @Test
    fun `SettingsState can be updated with new values`() {
        val initialState = SettingsState()
        val updatedState = initialState.copy(
            selectedFolders = listOf("/path/to/folder"),
            aiStatus = AIStatus.READY_FOR_ANALYSIS,
            analysisProgress = AnalysisProgress(10, 100),
            currentlyAnalyzingImage = "/path/to/image.jpg",
            memoryUsage = MemoryUsage(25.5),
            isLoading = true
        )

        assertEquals(listOf("/path/to/folder"), updatedState.selectedFolders)
        assertEquals(AIStatus.READY_FOR_ANALYSIS, updatedState.aiStatus)
        assertEquals(10, updatedState.analysisProgress?.completed)
        assertEquals(100, updatedState.analysisProgress?.total)
        assertEquals("/path/to/image.jpg", updatedState.currentlyAnalyzingImage)
        assertEquals(25.5, updatedState.memoryUsage.aiTextSizeMB)
        assertTrue(updatedState.isLoading)
    }

    @Test
    fun `AIStatus enum has all expected values`() {
        val expectedStatuses = setOf(
            AIStatus.OLLAMA_NEEDED,
            AIStatus.LLAVA_NEEDED,
            AIStatus.READY_FOR_ANALYSIS,
            AIStatus.ANALYZING,
            AIStatus.ANALYSIS_COMPLETE
        )

        val actualStatuses = AIStatus.values().toSet()
        assertEquals(expectedStatuses, actualStatuses)
    }

    @Test
    fun `SettingsAction sealed class has all expected actions`() {
        // Test that we can create all action types
        val actions = listOf(
            SettingsAction.AddFolder,
            SettingsAction.RemoveFolder("/some/path"),
            SettingsAction.OpenOllamaWebsite,
            SettingsAction.VerifyOllamaInstallation,
            SettingsAction.VerifyLlavaInstallation,
            SettingsAction.StartBatchAnalysis,
            SettingsAction.StopBatchAnalysis
        )

        assertEquals(7, actions.size)

        // Test RemoveFolder action specifically
        val removeFolderAction = SettingsAction.RemoveFolder("/test/path")
        assertTrue(removeFolderAction is SettingsAction.RemoveFolder)
        assertEquals("/test/path", removeFolderAction.folderPath)
    }
}