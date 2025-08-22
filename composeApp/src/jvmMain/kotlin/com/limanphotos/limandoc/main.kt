package com.limanphotos.limandoc

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LimanPhotos",
        state = WindowState(
            position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center),
            size = DpSize(1400.dp, 900.dp)
        )
    ) {
        App()
    }
}