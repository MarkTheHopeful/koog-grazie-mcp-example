package com.example.serverbeta

import com.example.common.configureGrazieExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Scanner

fun main() = runBlocking {
    val grazieApiKey = System.getenv("GRAZIE_API_KEY")
    if (grazieApiKey.isNullOrBlank()) {
        System.err.println("GRAZIE_API_KEY environment variable is not set.")
        return@runBlocking
    }

    val grazieExecutor = configureGrazieExecutor(grazieApiKey)
    val serverBetaPort = 8081
    val agentAlphaMcpUrl = "http://localhost:8080"

    println("--- Initializing Server Beta ---")
    val serverBetaBuilder = ServerBetaBuilder(
        port = serverBetaPort,
        grazieExecutor = grazieExecutor,
        agentAlphaMcpUrl = agentAlphaMcpUrl
    )
    val serverBeta = serverBetaBuilder.buildAndStart() as ServerBeta

    println("Server Beta initialized. Agent Beta is ready to take commands.")
    println("Agent Beta has access to tools: ${serverBeta.agent.toolRegistry.tools.joinToString { it.name }}")
    println("Type your requests for Agent Beta (e.g., 'Ask Agent Alpha to echo \"hello from Beta\"') or 'exit' to quit:")

    val scanner = Scanner(System.`in`)
    launch(Dispatchers.IO) {
        while (true) {
            print("> ")
            val inputLine = scanner.nextLine()
            if (inputLine.equals("exit", ignoreCase = true)) {
                break
            }
            if (inputLine.isNotBlank()) {
                withContext(Dispatchers.Default) {
                    println(" Sending to AgentBeta: '$inputLine'")
                    val result = serverBeta.agent.runAndGetResult(inputLine)
                    println(" << $result")
                }
            }
        }
        serverBeta.stop()
        kotlin.system.exitProcess(0)
    }.join()
}