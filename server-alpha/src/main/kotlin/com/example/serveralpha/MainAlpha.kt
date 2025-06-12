package com.example.serveralpha

import com.example.common.configureGrazieExecutor
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val grazieApiKey = System.getenv("GRAZIE_API_KEY")
    if (grazieApiKey.isNullOrBlank()) {
        System.err.println("GRAZIE_API_KEY environment variable is not set.")
        return@runBlocking
    }

    val grazieExecutor = configureGrazieExecutor(grazieApiKey)
    val serverAlphaPort = 8080

    println("--- Initializing Server Alpha ---")
    val serverAlphaBuilder = ServerAlphaBuilder(
        port = serverAlphaPort,
        grazieExecutor = grazieExecutor
    )
    val serverAlpha = serverAlphaBuilder.buildAndStart()
    println("Server Alpha started on port ${serverAlpha.port}. Press Ctrl+C to exit.")

    try {
        while (true) {
            kotlinx.coroutines.delay(60000)
        }
    } finally {
        serverAlpha.stop()
    }
}