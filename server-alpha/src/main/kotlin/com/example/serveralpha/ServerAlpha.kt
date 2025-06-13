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
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

import kotlinx.io.asSink
import kotlinx.io.buffered


class AlphaToolSet : ToolSet {
    @KTool
    @LLMDescription("Echoes back the provided message, prepended by Agent Alpha's signature.")
    fun simpleEchoAlpha(
        @LLMDescription("Message text") message: EchoRequest
    ): EchoResponse {
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
                        put("description", "A message to be echoes")
                    }
                },
                required = listOf("message")
            )
        ) { request ->
            val message = request.arguments["message"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
                content = listOf(TextContent("The 'message' parameter is required."))
            )
            val result = agent.runAndGetResult("Echo the following message: $message")

            CallToolResult(
                content = listOf(if (result == null) TextContent(result) else TextContent("")),
                isError = result == null
            )
        }
    }

    private var agent: AIAgent
    private val toolRegistry = ToolRegistry {
        AlphaToolSet().asTools()
    }

    init {
        agent = AIAgent(
            executor = grazieExecutor,
            systemPrompt = "You are Agent Alpha. You are a helpful assistant and can use the SimpleEchoToolAlpha. If asked to echo, use your tool.",
            llmModel = OpenAIModels.Chat.GPT4o,
            toolRegistry = toolRegistry
        )
        println("[AgentAlpha] Initialized with tools: ${toolRegistry.tools.joinToString { it.name }}")
    }

    override suspend fun start(wait: Boolean) {
        println("[AgentAlpha] Starting MCP server on port $port...")
        val transport = StdioServerTransport(
            System.`in`.asInput(),
            System.out.asSink().buffered()
        )
        println("[AgentAlpha] MCP server started. Agent is ready.")
        runBlocking {
            mcpServer.connect(transport)
            val done = Job()
            mcpServer.onClose {
                done.complete()
            }
            done.join()
        }
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