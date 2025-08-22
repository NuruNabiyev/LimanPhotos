package com.limanphotos.limandoc.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.limanphotos.limandoc.ui.theme.Spacing

/**
 * Settings dialog for compact UI presentation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .widthIn(min = 400.dp, max = 600.dp)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content in scrollable column
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Folder Selection Section
                    item {
                        SettingsSectionHeader(title = "Selected Folders")
                    }

                    item {
                        FolderSelectionSection(
                            selectedFolders = state.selectedFolders,
                            onAddFolder = { onAction(SettingsAction.AddFolder) },
                            onRemoveFolder = { folder -> onAction(SettingsAction.RemoveFolder(folder)) }
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // AI Search Section
                    item {
                        SettingsSectionHeader(title = "AI Search")
                    }

                    item {
                        AiAndDataSection(
                            aiStatus = state.aiStatus,
                            analysisProgress = state.analysisProgress,
                            currentlyAnalyzingImage = state.currentlyAnalyzingImage,
                            memoryUsage = state.memoryUsage,
                            onAction = onAction
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Display Settings Section
                    item {
                        SettingsSectionHeader(title = "Display Settings")
                    }

                    item {
                        ImageScaleSection(
                            currentScale = state.imageScale,
                            onScaleChange = { onAction(SettingsAction.UpdateImageScale(it)) }
                        )
                    }

                    item {
                        CollectionsImageScaleSection(
                            currentScale = state.collectionsImageScale,
                            onScaleChange = { onAction(SettingsAction.UpdateCollectionsImageScale(it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
private fun FolderSelectionSection(
    selectedFolders: List<String>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 500.dp)
    ) {
        Text(
            text = "The photos in these folders will be shown in \"All Photos\" section and can be analyzed by Ollama & LLaVA.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            selectedFolders.forEach { folder ->
                FolderItem(
                    folderPath = folder,
                    onRemove = { onRemoveFolder(folder) }
                )
            }

            if (selectedFolders.isEmpty()) {
                Text(
                    text = "No folders selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text("Add Folder")
            }
        }
    }
}

@Composable
private fun FolderItem(
    folderPath: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = folderPath,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove folder",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AiAndDataSection(
    aiStatus: AIStatus,
    analysisProgress: AnalysisProgress?,
    currentlyAnalyzingImage: String?,
    memoryUsage: MemoryUsage,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (aiStatus) {
                AIStatus.OLLAMA_NEEDED -> {
                    OllamaNeededStatus(onAction)
                }

                AIStatus.LLAVA_NEEDED -> {
                    LlavaNeededStatus(onAction)
                }

                AIStatus.READY_FOR_ANALYSIS -> {
                    ReadyForAnalysisStatus(analysisProgress, onAction)
                }

                AIStatus.ANALYZING -> {
                    AnalyzingStatus(analysisProgress, currentlyAnalyzingImage, onAction)
                }

                AIStatus.ANALYSIS_COMPLETE -> {
                    AnalysisCompleteStatus(analysisProgress)
                }
            }

            Divider()

            // Memory Usage Display
            MemoryUsageDisplay(memoryUsage)

            Divider()

            // Clear AI Data Button
            ClearAIDataButton(onAction)
        }
    }
}

@Composable
private fun OllamaNeededStatus(onAction: (SettingsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ollama installation needed.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            onClick = { onAction(SettingsAction.OpenOllamaWebsite) }
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Download Ollama")
        }

        Button(
            onClick = { onAction(SettingsAction.VerifyOllamaInstallation) }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Verify Installation")
        }
    }
}

@Composable
private fun LlavaNeededStatus(onAction: (SettingsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ollama installed! Next step: run \"ollama pull llava\" in your terminal.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = { onAction(SettingsAction.VerifyLlavaInstallation) }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Verify LLaVA Installation")
        }
    }
}

@Composable
private fun ReadyForAnalysisStatus(
    progress: AnalysisProgress?,
    onAction: (SettingsAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ollama and LLaVA are ready! Let's analyze images from your selected folders.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Show progress if available
        if (progress != null && progress.total > 0) {
            Text(
                text = "${progress.completed} of ${progress.total} images already analyzed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (progress.completed < progress.total) {
                Text(
                    text = "${progress.total - progress.completed} images need analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Button(
            onClick = { onAction(SettingsAction.StartBatchAnalysis) }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Start Analysis")
        }
    }
}

@Composable
private fun AnalyzingStatus(
    progress: AnalysisProgress?,
    currentImage: String?,
    onAction: (SettingsAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (progress != null) {
            Text(
                text = "${progress.completed}/${progress.total} images were analyzed so far",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            LinearProgressIndicator(
                progress = progress.completed.toFloat() / progress.total,
                modifier = Modifier.fillMaxWidth()
            )
        }

        currentImage?.let { imagePath ->
            Text(
                text = "Currently analyzing: $imagePath",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "If you cancel, you can continue this process later too in the settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = { onAction(SettingsAction.StopBatchAnalysis) }
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Stop Analysis")
        }
    }
}

@Composable
private fun AnalysisCompleteStatus(progress: AnalysisProgress?) {
    Text(
        text = if (progress != null) {
            "All ${progress.total} images in selected folders were analyzed with LLaVA."
        } else {
            "All images in selected folders were analyzed with LLaVA."
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun MemoryUsageDisplay(
    memoryUsage: MemoryUsage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Memory Usage",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "AI-generated text: ${memoryUsage.aiTextSizeMB} MB",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ClearAIDataButton(
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "Remove all AI analysis data and descriptions. This will free up storage space but you'll need to re-analyze images.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = { onAction(SettingsAction.ClearAllAIData) },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Clear All AI Data")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Reset all application preferences including folder selections, settings, and cached data. Use this for debugging or to start fresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = { onAction(SettingsAction.ClearAllPreferences) },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text("Clear LimanPhotos Preferences")
        }
    }
}

@Composable
private fun ImageScaleSection(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Image Thumbnail Scale",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Adjust the size of image thumbnails in the All Photos section. Smaller values show more images at once, larger values show more detail.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "0.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = currentScale,
                    onValueChange = onScaleChange,
                    valueRange = 0.1f..1.0f,
                    steps = 8, // 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Current: ${String.format("%.1f", currentScale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CollectionsImageScaleSection(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Collections Thumbnail Scale",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Adjust the size of preview images in Collections cards. Smaller values load faster but show less detail.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "0.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = currentScale,
                    onValueChange = onScaleChange,
                    valueRange = 0.1f..1.0f,
                    steps = 8, // 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Current: ${String.format("%.1f", currentScale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}