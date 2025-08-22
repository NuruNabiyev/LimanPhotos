package com.limanphotos.limandoc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.limanphotos.limandoc.di.AppModule
import com.limanphotos.limandoc.presentation.MainNavigationScreen
import com.limanphotos.limandoc.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        var isInitialized by remember { mutableStateOf(false) }

        // Initialize search repository first
        LaunchedEffect(Unit) {
            try {
                val photoSearchRepository = AppModule.providePhotoSearchRepository()
                photoSearchRepository.initialize()
                println("🔍 Photo search repository initialized")
                isInitialized = true
            } catch (e: Exception) {
                println("⚠️ Failed to initialize search repository: ${e.message}")
                // Still allow app to continue with limited functionality
                isInitialized = true
            }
        }

        if (isInitialized) {
            // Create persistent ViewModels after search repository is initialized
            val settingsViewModel = remember { AppModule.provideSettingsViewModel() }
            val photoGalleryViewModel = remember { AppModule.providePhotoGalleryViewModel() }
            val imageAnalysisViewModel = remember { AppModule.provideImageAnalysisViewModel() }
            val collectionsViewModel = remember { AppModule.provideCollectionsViewModel() }

            MainNavigationScreen(
                photoGalleryViewModel = photoGalleryViewModel,
                imageAnalysisViewModel = imageAnalysisViewModel,
                settingsViewModel = settingsViewModel,
                collectionsViewModel = collectionsViewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show app initialization loading screen
            AppLoadingScreen()
        }
    }
}

@Composable
private fun AppLoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )

            Text(
                text = "LimanPhotos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Initializing search engine...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}