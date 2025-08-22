package com.limanphotos.limandoc.utils

data class CommandResult(
    val isSuccess: Boolean,
    val output: String,
    val error: String = ""
)

expect object PlatformUtils {
    suspend fun executeCommand(command: String): CommandResult
    fun openUrl(url: String)
}