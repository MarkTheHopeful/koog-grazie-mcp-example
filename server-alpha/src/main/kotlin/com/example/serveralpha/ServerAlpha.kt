package com.example.serveralpha

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
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


class AlphaToolSet : ToolSet {
    @Tool
    @LLMDescription("Echoes back the provided message, prepended by Agent Alpha's signature.")
    fun simpleEchoAlpha(
        @LLMDescription("Message text") message: EchoRequest
    ): EchoResponse {
        return EchoResponse("Agent Alpha echoes: ${message.message}", "Alpha")
    }
}

// --- MCP Server and Agent Holder for Alpha ---
class ServerAlpha(
    override val port: Int,
    private val host: String = "0.0.0.0",
    private val grazieExecutor: PromptExecutor
) : McpAgentServer {

    private var agent: AIAgent
    private val toolRegistry = ToolRegistry {
        AlphaToolSet().asTools()
    }

    private val ktorServer = embeddedServer(Netty, port = port, host = host, module = {
        serverAlphaModule(this@ServerAlpha)
    })

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
        ktorServer.start(wait = wait)
        println("[AgentAlpha] MCP server started. Agent is ready.")
    }

    override suspend fun stop() {
        println("[AgentAlpha] Stopping MCP server...")
        ktorServer.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        println("[AgentAlpha] MCP server stopped.")
    }
}


fun Application.serverAlphaModule(serverInstance: ServerAlpha) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    routing {
        get("/health") {
            call.respondText("AgentAlpha Server is healthy.")
        }
    }
}

class ServerAlphaBuilder(
    port: Int,
    host: String = "0.0.0.0",
    grazieExecutor: PromptExecutor
) : com.example.common.AbstractServerBuilder(port, host, grazieExecutor) {
    override suspend fun buildAndStart(): McpAgentServer {
        val server = ServerAlpha(port, host, grazieExecutor)
        server.start(wait = false) // Start non-blocking
        return server
    }
}