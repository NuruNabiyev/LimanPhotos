package com.limanphotos.limandoc.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun FileSizeInputs(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "File Size (MB)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Min size input
            OutlinedTextField(
                value = state.filterUIState.minSizeMB,
                onValueChange = { newMin ->
                    val updatedState = state.copy(
                        filterUIState = state.filterUIState.copy(minSizeMB = newMin)
                    )
                    updateFileSizeFilter(updatedState, onStateChange)
                },
                label = { Text("Min") },
                placeholder = { Text("No min") },
                modifier = Modifier
                    .weight(1f),
                singleLine = true
            )

            Text(
                text = "to",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Max size input
            OutlinedTextField(
                value = state.filterUIState.maxSizeMB,
                onValueChange = { newMax ->
                    val updatedState = state.copy(
                        filterUIState = state.filterUIState.copy(maxSizeMB = newMax)
                    )
                    updateFileSizeFilter(updatedState, onStateChange)
                },
                label = { Text("Max") },
                placeholder = { Text("No max") },
                modifier = Modifier
                    .weight(1f),
                singleLine = true
            )
        }
    }
}

private fun updateFileSizeFilter(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit
) {
    val minMB = state.filterUIState.minSizeMB.toFloatOrNull()
    val maxMB = state.filterUIState.maxSizeMB.toFloatOrNull()

    val newFileSizeFilter = FileSizeFilter.fromMB(
        minMB = minMB,
        maxMB = maxMB
    )

    onStateChange(
        state.copy(
            filters = state.filters.copy(fileSizeFilter = newFileSizeFilter)
        )
    )
}

@Composable
private fun FileTypeCheckboxes(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "File Types",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Checkboxes in a grid layout
        val availableTypes = FileTypeFilter.AVAILABLE_TYPES
        val selectedTypes = state.filterUIState.selectedFileTypes

        availableTypes.chunked(3).forEach { rowTypes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowTypes.forEach { (extension, displayName) ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val newSelectedTypes = if (selectedTypes.contains(extension)) {
                                    selectedTypes - extension
                                } else {
                                    selectedTypes + extension
                                }

                                val newFileTypeFilter = FileTypeFilter(newSelectedTypes)

                                onStateChange(
                                    state.copy(
                                        filters = state.filters.copy(fileTypeFilter = newFileTypeFilter),
                                        filterUIState = state.filterUIState.copy(selectedFileTypes = newSelectedTypes)
                                    )
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(extension),
                            onCheckedChange = null // Handled by row click
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                        )
                    }
                }

                // Fill remaining space if row has less than 3 items
                repeat(3 - rowTypes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

