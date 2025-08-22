package com.limanphotos.limandoc.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onApplyFilters: (SearchBubbleState) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .widthIn(min = 350.dp, max = 500.dp)
                .heightIn(max = 600.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Filters",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Content
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // File size inputs
                    CompactFileSizeInputs(
                        state = state,
                        onStateChange = onStateChange,
                        onApplyFilters = onApplyFilters
                    )

                    // File type checkboxes
                    CompactFileTypeCheckboxes(
                        state = state,
                        onStateChange = onStateChange,
                        onApplyFilters = onApplyFilters
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearAll,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear All")
                        }

                        Button(
                            onClick = { onApplyFilters(state) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFileSizeInputs(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onApplyFilters: (SearchBubbleState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "File Size (MB)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    val finalState = updatedState.copy(
                        filters = updatedState.filters.copy(
                            fileSizeFilter = FileSizeFilter.fromMB(
                                minMB = newMin.toFloatOrNull(),
                                maxMB = updatedState.filterUIState.maxSizeMB.toFloatOrNull()
                            )
                        )
                    )
                    onApplyFilters(finalState)
                },
                label = { Text("Min") },
                placeholder = { Text("0") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Text(
                text = "to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Max size input
            OutlinedTextField(
                value = state.filterUIState.maxSizeMB,
                onValueChange = { newMax ->
                    val updatedState = state.copy(
                        filterUIState = state.filterUIState.copy(maxSizeMB = newMax)
                    )
                    updateFileSizeFilter(updatedState, onStateChange)
                    val finalState = updatedState.copy(
                        filters = updatedState.filters.copy(
                            fileSizeFilter = FileSizeFilter.fromMB(
                                minMB = updatedState.filterUIState.minSizeMB.toFloatOrNull(),
                                maxMB = newMax.toFloatOrNull()
                            )
                        )
                    )
                    onApplyFilters(finalState)
                },
                label = { Text("Max") },
                placeholder = { Text("∞") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

@Composable
private fun CompactFileTypeCheckboxes(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onApplyFilters: (SearchBubbleState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "File Types",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Checkboxes in a compact grid layout
        val availableTypes = FileTypeFilter.AVAILABLE_TYPES
        val selectedTypes = state.filterUIState.selectedFileTypes

        availableTypes.chunked(2).forEach { rowTypes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTypes.forEach { (extension, displayName) ->
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(extension),
                            onCheckedChange = { isChecked ->
                                val newSelectedTypes = if (isChecked) {
                                    selectedTypes + extension
                                } else {
                                    selectedTypes - extension
                                }

                                val newFileTypeFilter = FileTypeFilter(newSelectedTypes)

                                val newState = state.copy(
                                    filters = state.filters.copy(fileTypeFilter = newFileTypeFilter),
                                    filterUIState = state.filterUIState.copy(selectedFileTypes = newSelectedTypes)
                                )

                                onStateChange(newState)
                                // Apply filters immediately without waiting for Apply button
                                onApplyFilters(newState)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Fill remaining space if row has less than 2 items
                if (rowTypes.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
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

    val newState = state.copy(
        filters = state.filters.copy(fileSizeFilter = newFileSizeFilter)
    )

    onStateChange(newState)
}