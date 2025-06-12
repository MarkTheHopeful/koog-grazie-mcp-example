package com.example.common

import ai.grazie.api.gateway.client.DefaultUrlResolver
import ai.grazie.api.gateway.client.PlatformConfigurationUrl
import ai.grazie.api.gateway.client.ResolutionResult
import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GrazieLLMClient
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GraziePromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor

suspend fun configureGrazieExecutor(userToken: String): PromptExecutor {
    // Configure the HTTP client for sending requests
    val httpClient = SuspendableHTTPClient.WithV5(
        GrazieKtorHTTPClient.Client.Default,
        // Provide the authentication token and the application name and version
        authData = AuthData(userToken, grazieAgent = GrazieAgent("koog-mcp-demo-app", "0.1.0"))
    )

    // Parse the configuration file
    val resolutionResult = DefaultUrlResolver(PlatformConfigurationUrl.Production.GLOBAL, httpClient).resolve()
    val serverUrl = when (resolutionResult) {
        is ResolutionResult.Failure -> {
            // Throw exception because we cannot download the configuration file
            System.err.println("Failed to resolve Grazie server URL: ${resolutionResult.problems.first()}")
            throw resolutionResult.problems.first()
        }
        is ResolutionResult.FallbackUrl -> {
            // Log a warning that for some reason the default URL is not accessible
            println("Warning: Using fallback Grazie server URL. Problems: ${resolutionResult.problems}")
            // Use a fallback URL instead of the default one
            resolutionResult.url
        }
        is ResolutionResult.Success -> resolutionResult.url
    }

    // Create an instance of the client
    val gatewayClient = SuspendableAPIGatewayClient(
        serverUrl = serverUrl,
        authType = AuthType.User,
        httpClient = httpClient,
    )

    return GraziePromptExecutor(GrazieLLMClient(gatewayClient))
}