package com.limanphotos.limandoc.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A search bar that displays search terms as interactive bubbles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBubbleBar(
    state: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search photos..."
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bubbles display
            if (state.bubbles.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(state.bubbles, key = { it.id }) { bubble ->
                        SearchBubbleChip(
                            bubble = bubble,
                            onEdit = { bubbleId ->
                                onStateChange(state.copy(editingBubbleId = bubbleId))
                            },
                            onRemove = { bubbleId ->
                                val newBubbles = state.bubbles.filter { it.id != bubbleId }
                                val newState = state.copy(bubbles = newBubbles)
                                onStateChange(newState)

                                // Trigger search with updated query
                                val updatedQuery = newState.getCompleteQuery()
                                onSearch(updatedQuery)
                            }
                        )
                    }
                }
            }

            // Text input field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = state.currentInput,
                    onValueChange = { newInput ->
                        handleInputChange(newInput, state, onStateChange, onSearch)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            handleKeyEvent(keyEvent, state, onStateChange, onSearch)
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val query = state.getCompleteQuery()
                            if (query.isNotBlank()) {
                                onSearch(query)
                            }
                            focusManager.clearFocus()
                        }
                    )
                )

                // Placeholder
                if (state.currentInput.isEmpty() && state.bubbles.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Individual bubble chip component
 */
@Composable
private fun SearchBubbleChip(
    bubble: SearchBubble,
    onEdit: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (bubble.isPhrase) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = if (bubble.isPhrase) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onEdit(bubble.id) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = bubble.getDisplayText(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "×",
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.7f),
            modifier = Modifier
                .clickable { onRemove(bubble.id) }
                .padding(2.dp)
        )
    }
}

/**
 * Handle input changes and bubble creation
 */
private fun handleInputChange(
    newInput: String,
    currentState: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onSearch: (String) -> Unit
) {
    // Parse the input to extract any new bubbles
    val (newBubbles, remainingInput) = SearchBubbleParser.parseInput(newInput)

    if (newBubbles.isNotEmpty()) {
        // New bubbles were created
        val updatedBubbles = currentState.bubbles + newBubbles
        val newState = currentState.copy(
            bubbles = updatedBubbles,
            currentInput = remainingInput
        )
        onStateChange(newState)

        // Trigger search with the updated query
        val completeQuery = newState.getCompleteQuery()
        if (completeQuery.isNotBlank()) {
            onSearch(completeQuery)
        }
    } else {
        // No new bubbles, just update the current input
        val newState = currentState.copy(currentInput = newInput)
        onStateChange(newState)

        // Trigger search with current complete query
        val completeQuery = newState.getCompleteQuery()
        if (completeQuery.isNotBlank()) {
            onSearch(completeQuery)
        }
    }
}

/**
 * Handle special key events like backspace for bubble editing
 */
private fun handleKeyEvent(
    keyEvent: KeyEvent,
    currentState: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onSearch: (String) -> Unit
): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) {
        return false
    }

    when (keyEvent.key) {
        Key.Backspace -> {
            return handleBackspace(currentState, onStateChange, onSearch)
        }

        Key.DirectionLeft -> {
            return handleLeftArrow(currentState, onStateChange)
        }

        Key.DirectionRight -> {
            return handleRightArrow(currentState, onStateChange)
        }

        Key.Enter -> {
            val query = currentState.getCompleteQuery()
            if (query.isNotBlank()) {
                onSearch(query)
            }
            return true
        }

        else -> return false
    }
}

/**
 * Handle backspace key for bubble editing and removal
 */
private fun handleBackspace(
    currentState: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit,
    onSearch: (String) -> Unit
): Boolean {
    // If there's current input, let normal backspace behavior happen
    if (currentState.currentInput.isNotEmpty()) {
        return false
    }

    // If no current input but there are bubbles, edit the last bubble
    if (currentState.bubbles.isNotEmpty()) {
        val lastBubble = currentState.bubbles.last()
        val newBubbles = currentState.bubbles.dropLast(1)
        val newState = currentState.copy(
            bubbles = newBubbles,
            currentInput = lastBubble.content,
            editingBubbleId = null
        )
        onStateChange(newState)
        return true
    }

    return false
}

/**
 * Handle left arrow key for bubble navigation and editing
 */
private fun handleLeftArrow(
    currentState: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit
): Boolean {
    // If there's current input and cursor is at the beginning, move to edit the last bubble
    if (currentState.currentInput.isNotEmpty()) {
        return false // Let normal cursor movement handle it
    }

    // If no current input and there are bubbles, edit the last bubble
    if (currentState.bubbles.isNotEmpty()) {
        val lastBubble = currentState.bubbles.last()
        val newBubbles = currentState.bubbles.dropLast(1)
        val newState = currentState.copy(
            bubbles = newBubbles,
            currentInput = lastBubble.content,
            editingBubbleId = lastBubble.id
        )
        onStateChange(newState)
        return true
    }

    return false
}

/**
 * Handle right arrow key for bubble navigation and editing
 */
private fun handleRightArrow(
    currentState: SearchBubbleState,
    onStateChange: (SearchBubbleState) -> Unit
): Boolean {
    // If there's current input, let normal cursor movement handle it
    if (currentState.currentInput.isNotEmpty()) {
        return false
    }

    // If no current input and there are bubbles, edit the last bubble
    if (currentState.bubbles.isNotEmpty()) {
        val lastBubble = currentState.bubbles.last()
        val newBubbles = currentState.bubbles.dropLast(1)
        val newState = currentState.copy(
            bubbles = newBubbles,
            currentInput = lastBubble.content,
            editingBubbleId = lastBubble.id
        )
        onStateChange(newState)
        return true
    }

    return false
}