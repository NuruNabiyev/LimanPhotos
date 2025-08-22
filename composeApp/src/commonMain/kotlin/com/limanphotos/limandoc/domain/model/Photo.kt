@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Photo(
    val id: String,
    val path: String,
    val name: String,
    val creationTime: Instant,
    val size: Long,
    val extension: String,
    val width: Int = 0,  // Default to 0 if dimensions not available
    val height: Int = 0
) {
}