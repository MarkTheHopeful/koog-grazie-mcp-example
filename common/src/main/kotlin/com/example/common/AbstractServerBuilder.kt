package com.example.common

import ai.koog.prompt.executor.model.PromptExecutor

abstract class AbstractServerBuilder(
    protected val port: Int,
    protected val host: String = "0.0.0.0",
    protected val grazieExecutor: PromptExecutor
) {
    abstract suspend fun buildAndStart(): McpAgentServer
}