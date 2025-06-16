package com.example.serveralpha

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool as KTool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import com.example.common.EchoRequest
import com.example.common.EchoResponse
import com.example.common.McpAgentServer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import ai.koog.prompt.executor.model.PromptExecutor
import io.ktor.server.cio.CIO
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*


class AlphaToolSet : ToolSet {
    @KTool
    @LLMDescription("Echoes back the provided message, prepended by Agent Alpha's signature.")
    fun simpleEchoAlpha(
        @LLMDescription("Message text") message: EchoRequest
    ): EchoResponse {
        println("Alpha: Tool called")
        return EchoResponse("Agent Alpha echoes: ${message.message}", "Alpha")
    }
}

class ServerAlpha(
    override val port: Int,
    private val host: String = "0.0.0.0",
    private val grazieExecutor: PromptExecutor
) : McpAgentServer {
    val mcpServer = Server(
        Implementation(
            name = "echo",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    init {
        mcpServer.addTool(
            name = "get_echo",
            description = """
            Echoes back the provided message, prepended by Agent Alpha's signature.
        """.trimIndent(),
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("message") {
                        put("type", "string")
                        put("description", "A message to be echoed")
                    }
                },
                required = listOf("message")
            )
        ) { request ->
            val message = request.arguments["message"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
                content = listOf(TextContent("The 'message' parameter is required.")),
                isError = true
            )
            println(message)
            val result = agent.runAndGetResult("Echo the following message: $message")
            initializeAgent() // Since runAndGetResult is a one-time use for agents FIXME

            println(result)
            CallToolResult(
                content = listOf(if (result != null) TextContent(result) else TextContent("")),
                isError = result == null
            )
        }
    }

    private var agent: AIAgent
    private val toolRegistry = ToolRegistry {
        AlphaToolSet().asTools()
    }

    fun initializeAgent() {
        agent = AIAgent(
            executor = grazieExecutor,
            systemPrompt = "You are Agent Alpha. You are a helpful assistant and can use the simpleEchoAlpha as a tool. If asked to echo, use your tool and do not print anything else except tool's invocation result.",
            llmModel = OpenAIModels.Chat.GPT4o,
            toolRegistry = toolRegistry
        )
        println("[AgentAlpha] Initialized with tools: ${toolRegistry.tools.joinToString { it.name }}")
    }

    init {
        agent = AIAgent(
            executor = grazieExecutor,
            systemPrompt = "You are Agent Alpha. You are a helpful assistant and can use the simpleEchoAlpha as a tool. If asked to echo, use your tool and do not print anything else except tool's invocation result.",
            llmModel = OpenAIModels.Chat.GPT4o,
            toolRegistry = toolRegistry
        )
        println("[AgentAlpha] Initialized with tools: ${toolRegistry.tools.joinToString { it.name }}")
    }

    override suspend fun start(wait: Boolean) {
        println("[AgentAlpha] Starting MCP server on port $port...")

        embeddedServer(CIO, host = host, port = port) {
            mcp {
                return@mcp mcpServer
            }
        }.startSuspend(wait = false)
        println("[AgentAlpha] MCP server started. Agent is ready.")
    }

    override suspend fun stop() {
        println("[AgentAlpha] Idk what to do here")
    }
}

class ServerAlphaBuilder(
    port: Int,
    host: String = "0.0.0.0",
    grazieExecutor: PromptExecutor
) : com.example.common.AbstractServerBuilder(port, host, grazieExecutor) {
    override suspend fun buildAndStart(): McpAgentServer {
        val server = ServerAlpha(port, host, grazieExecutor)
        server.start(wait = false)
        return server
    }
}