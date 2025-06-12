import kotlinx.coroutines.runBlocking
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GrazieLLMClient

import ai.grazie.api.gateway.client.DefaultUrlResolver
import ai.grazie.api.gateway.client.PlatformConfigurationUrl
import ai.grazie.api.gateway.client.ResolutionResult
import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GraziePromptExecutor

suspend fun configureClient(userToken: String): SuspendableAPIGatewayClient {
    // Configure the HTTP client for sending requests
    val httpClient = SuspendableHTTPClient.WithV5(
        GrazieKtorHTTPClient.Client.Default,
        // Provide the authentication token and the application name and version
        authData = AuthData(userToken, grazieAgent = GrazieAgent("koog-testing", "dev"))
    )
    // Parse the configuration file
    val resolutionResult = DefaultUrlResolver(PlatformConfigurationUrl.Production.GLOBAL, httpClient).resolve()
    val serverUrl = when (resolutionResult) {
        is ResolutionResult.Failure -> {
            // Throw exception because we cannot download the configuration file
            throw resolutionResult.problems.first()
        }

        is ResolutionResult.FallbackUrl -> {
            // Log a warning that for some reason the default URL is not accessible
            println(resolutionResult.problems)
            // Use a fallback URL instead of the default one
            resolutionResult.url
        }

        is ResolutionResult.Success -> resolutionResult.url
    }
    // Create an instance of the client
    return SuspendableAPIGatewayClient(
        // Provide the server URL
        serverUrl = serverUrl,
        // Set the User authentication type (optional)
        authType = AuthType.User,
        // Specify the HTTP client
        httpClient = httpClient,
    )
}

fun main() = runBlocking {
    // Before you run the example, assign a corresponding API key as an environment variable.
    val apiKey = System.getenv("GRAZIE_API_KEY")

    val grazieClient = configureClient(apiKey)

    val executor = GraziePromptExecutor(GrazieLLMClient(grazieClient))


    val agent = AIAgent(
        executor = executor,
        systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
        llmModel = OpenAIModels.Chat.GPT4o
    )

    val result = agent.runAndGetResult("Hello! How can you help me?")
    println(result)
}