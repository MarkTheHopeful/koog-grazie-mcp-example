package com.example.serverbeta

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import com.example.common.McpAgentServer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport

class ServerBeta(
    override val port: Int,
    private val host: String = "0.0.0.0",
    private val grazieExecutor: PromptExecutor,
    private val agentAlphaMcpUrl: String,
    private val scope: CoroutineScope
) : McpAgentServer {

    lateinit var agent: AIAgent

    lateinit var toolMcpRegistry: ToolRegistry

    private lateinit var toolRegistry: ToolRegistry

    init {
        toolMcpRegistry = runBlocking {
            McpToolRegistryProvider.fromTransport(
                transport = McpToolRegistryProvider.defaultSseTransport(agentAlphaMcpUrl)
            )
        }
    }


    suspend fun initializeAgent() {
        toolRegistry = ToolRegistry {

        }

        agent = AIAgent(
            executor = grazieExecutor,
            systemPrompt = "You are Agent Beta. You can use tools provided by Agent Alpha. Your ID is AgentBeta. When using Alpha's echo tool, provide your ID.",
            llmModel = OpenAIModels.Chat.GPT4o,
            toolRegistry = toolMcpRegistry
        )
        println(" Initialized with tools: ${toolRegistry.tools.joinToString { it.name }}")
    }

    override suspend fun start(wait: Boolean) {
        println("Not doing anything, actually")
    }

    override suspend fun stop() {
        println("Not doing anything here also")
    }
}

class ServerBetaBuilder(
    port: Int,
    host: String = "0.0.0.0",
    grazieExecutor: PromptExecutor,
    private val agentAlphaMcpUrl: String
) : com.example.common.AbstractServerBuilder(port, host, grazieExecutor) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var serverBetaInstance: ServerBeta

    override suspend fun buildAndStart(): McpAgentServer {
        serverBetaInstance = ServerBeta(
            port,
            host,
            grazieExecutor,
            agentAlphaMcpUrl,
            coroutineScope
        )
        serverBetaInstance.initializeAgent()
        serverBetaInstance.start(wait = false)
        return serverBetaInstance
    }
}