package com.example.common

interface McpAgentServer {
    val port: Int
    suspend fun start(wait: Boolean = false)
    suspend fun stop()
}