package com.limanphotos.limandoc.presentation.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limanphotos.limandoc.presentation.components.FilterDialog
import com.limanphotos.limandoc.presentation.components.FullScreenPhotoViewer
import com.limanphotos.limandoc.presentation.components.ImageAnalysisDialog
import com.limanphotos.limandoc.presentation.components.ImageAnalysisViewModel
import com.limanphotos.limandoc.presentation.components.PhotoFilters
import com.limanphotos.limandoc.presentation.components.SearchBubbleBar
import com.limanphotos.limandoc.presentation.components.SearchBubbleState
import com.limanphotos.limandoc.presentation.settings.SettingsState
import com.limanphotos.limandoc.presentation.settings.SettingsViewModel
import com.limanphotos.limandoc.ui.RightClickMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    viewModel: PhotoGalleryViewModel,
    imageAnalysisViewModel: ImageAnalysisViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToSettings: () -> Unit,
    initialSearchState: SearchBubbleState? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val analysisState by imageAnalysisViewModel.analysisState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

    // Dialog states
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    var fullScreenPhoto by remember {
        mutableStateOf<com.limanphotos.limandoc.domain.model.Photo?>(
            null
        )
    }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Bubble search state - initialize with passed state if available
    var bubbleSearchState by remember(initialSearchState) {
        mutableStateOf(initialSearchState ?: SearchBubbleState())
    }

    // Update search query when initial search state is provided
    // But skip if we're already showing specific photos (like from collections)
    LaunchedEffect(initialSearchState) {
        initialSearchState?.let { searchState ->
            if (searchState.bubbles.isNotEmpty() && uiState.photos == null) {
                // Only trigger search if we don't already have specific photos loaded
                val query = searchState.getCompleteQuery()
                viewModel.updateSearchQuery(query)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with search and filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search bar
            SearchBubbleBar(
                state = bubbleSearchState,
                onStateChange = { newState ->
                    bubbleSearchState = newState
                },
                onSearch = { query ->
                    if (query.isBlank()) {
                        viewModel.resetToAllPhotos()
                    } else {
                        viewModel.updateSearchQuery(query)
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = "Search photos... (try \"red umbrella\" or umbrella)"
            )

            // Filter controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter button
                IconButton(
                    onClick = { showFilterDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (bubbleSearchState.hasActiveFilters()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Clear filters button
                IconButton(
                    onClick = {
                        val clearedState = SearchBubbleState()
                        bubbleSearchState = clearedState
                        viewModel.resetToAllPhotos()
                        viewModel.applyFilters(PhotoFilters.EMPTY)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Filters",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        when {
            uiState.isLoading || uiState.photos == null -> {
                PhotosLoadingScreen(
                    searchQuery = searchQuery,
                    settingsState = settingsState,
                    loadingMessage = uiState.loadingMessage
                )
            }

            uiState.error != null -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::clearError
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            uiState.photos!!.isEmpty() -> {
                println("uiState.photos.isEmpty() true")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No photos found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (settingsState.selectedFolders.isEmpty()) {
                            // No folders selected
                            Row {
                                Text(
                                    text = "You currently selected no folder - ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "go to Settings",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onNavigateToSettings() }
                                )
                            }
                        } else {
                            // Folders selected but no photos found
                            Text(
                                text = "No photos found in selected folders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            else -> {
                val gridState = rememberLazyStaggeredGridState()

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Adaptive(minSize = 280.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.photos ?: emptyList()) { photo ->
                            RightClickMenu(photo.path) {
                                PhotoItem(
                                    photo = photo,
                                    onClick = {
                                        selectedImagePath = photo.path
                                        imageAnalysisViewModel.analyzeImage(photo.path)
                                    },
                                    onFullScreenView = {
                                        fullScreenPhoto = photo
                                        // Also trigger analysis for full-screen view
                                        imageAnalysisViewModel.analyzeImage(photo.path)
                                    },
                                    imageScale = settingsState.imageScale
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show image analysis dialog (legacy - for long press)
    selectedImagePath?.let { imagePath ->
        ImageAnalysisDialog(
            imagePath = imagePath,
            analysis = analysisState,
            onDismiss = {
                selectedImagePath = null
                imageAnalysisViewModel.clearAnalysis()
            }
        )
    }

    // Show full-screen photo viewer (for left click)
    fullScreenPhoto?.let { photo ->
        FullScreenPhotoViewer(
            photo = photo,
            analysis = analysisState,
            onDismiss = {
                fullScreenPhoto = null
                imageAnalysisViewModel.clearAnalysis()
            }
        )
    }

    // Show filter dialog
    if (showFilterDialog) {
        FilterDialog(
            state = bubbleSearchState,
            onStateChange = { newState ->
                bubbleSearchState = newState
            },
            onApplyFilters = { newState ->
                bubbleSearchState = newState
                viewModel.applyFilters(newState.filters)
            },
            onClearAll = {
                val clearedState = SearchBubbleState()
                bubbleSearchState = clearedState
                viewModel.resetToAllPhotos()
                viewModel.applyFilters(PhotoFilters.EMPTY)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun PhotosLoadingScreen(
    searchQuery: String,
    settingsState: SettingsState,
    loadingMessage: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )

            // Show custom loading message if available, otherwise default messages
            val primaryMessage = if (loadingMessage.isNotBlank()) {
                loadingMessage
            } else if (searchQuery.isNotBlank()) {
                "Searching photos..."
            } else {
                "Loading photos..."
            }

            Text(
                text = primaryMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (searchQuery.isNotBlank() && loadingMessage.isBlank()) {
                Text(
                    text = "Query: \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (searchQuery.isBlank()) {
                if (settingsState.selectedFolders.isNotEmpty()) {
                    Text(
                        text = "Scanning ${settingsState.selectedFolders.size} folder${if (settingsState.selectedFolders.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show folder names if not too many
                    if (settingsState.selectedFolders.size <= 3) {
                        settingsState.selectedFolders.forEach { folder ->
                            Text(
                                text = "• ${folder.split("/").lastOrNull() ?: folder}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Please configure folders in Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Additional helpful text for first-time users
            if (settingsState.selectedFolders.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Large folders may take a few moments to scan initially",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}