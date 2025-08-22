package com.limanphotos.limandoc.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect suspend fun loadImageBitmap(imagePath: String): ImageBitmap?

expect fun ImageBitmap.scale(scaleFactor: Float): ImageBitmap

@Composable
expect fun RightClickMenu(filepath: String, content: @Composable (() -> Unit))