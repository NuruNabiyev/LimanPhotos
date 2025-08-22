package com.limanphotos.limandoc.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limanphotos.limandoc.presentation.collections.CollectionsScreen
import com.limanphotos.limandoc.presentation.collections.CollectionsViewModel
import com.limanphotos.limandoc.presentation.components.ImageAnalysisViewModel
import com.limanphotos.limandoc.presentation.components.SearchBubble
import com.limanphotos.limandoc.presentation.components.SearchBubbleState
import com.limanphotos.limandoc.presentation.gallery.PhotoGalleryScreen
import com.limanphotos.limandoc.presentation.gallery.PhotoGalleryViewModel
import com.limanphotos.limandoc.presentation.settings.SettingsDialog
import com.limanphotos.limandoc.presentation.settings.SettingsViewModel

/**
 * Navigation destinations for bottom navigation
 */
enum class NavigationDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ALL_PHOTOS("All Photos", Icons.Filled.Photo, Icons.Outlined.Photo),
    COLLECTIONS("Collections", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * Main navigation screen with bottom navigation bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    photoGalleryViewModel: PhotoGalleryViewModel,
    imageAnalysisViewModel: ImageAnalysisViewModel,
    settingsViewModel: SettingsViewModel,
    collectionsViewModel: CollectionsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedDestination by remember { mutableStateOf(NavigationDestination.ALL_PHOTOS) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var initialSearchState by remember { mutableStateOf<SearchBubbleState?>(null) }
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

    // Set up callback to refresh photos when folders change
    LaunchedEffect(Unit) {
        settingsViewModel.setOnFoldersChangedCallback {
            photoGalleryViewModel.refreshPhotos()
            collectionsViewModel.refreshCollections()
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedDestination == destination) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(destination.title)
                        },
                        selected = selectedDestination == destination && destination != NavigationDestination.SETTINGS,
                        onClick = {
                            if (destination == NavigationDestination.SETTINGS) {
                                showSettingsDialog = true
                            } else {
                                selectedDestination = destination
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedDestination) {
                NavigationDestination.ALL_PHOTOS -> {
                    PhotoGalleryScreen(
                        viewModel = photoGalleryViewModel,
                        imageAnalysisViewModel = imageAnalysisViewModel,
                        settingsViewModel = settingsViewModel,
                        initialSearchState = initialSearchState,
                        onNavigateToSettings = {
                            showSettingsDialog = true
                        }
                    )
                }

                NavigationDestination.COLLECTIONS -> {
                    CollectionsScreen(
                        viewModel = collectionsViewModel,
                        collectionsImageScale = settingsState.collectionsImageScale,
                        onNavigateToPhotosWithCollection = { collection ->
                            // Create search state with a bubble for the tag (for UI display only)
                            val searchBubble = SearchBubble.word(collection.tag)
                            initialSearchState = SearchBubbleState(
                                bubbles = listOf(searchBubble)
                            )

                            // Navigate to All Photos showing exact collection photos
                            selectedDestination = NavigationDestination.ALL_PHOTOS
                            photoGalleryViewModel.showCollectionPhotos(
                                collection.photos,
                                collection.tag
                            )
                        }
                    )
                }

                NavigationDestination.SETTINGS -> {
                    // Settings is handled by dialog, should never reach here
                    PhotoGalleryScreen(
                        viewModel = photoGalleryViewModel,
                        imageAnalysisViewModel = imageAnalysisViewModel,
                        settingsViewModel = settingsViewModel,
                        initialSearchState = initialSearchState,
                        onNavigateToSettings = {
                            showSettingsDialog = true
                        }
                    )
                }
            }
        }
    }

    // Show settings dialog
    if (showSettingsDialog) {
        SettingsDialog(
            state = settingsState,
            onAction = settingsViewModel::handleAction,
            onDismiss = { showSettingsDialog = false }
        )
    }
}