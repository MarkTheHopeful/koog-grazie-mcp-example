package com.example.common

import kotlinx.serialization.Serializable

@Serializable
data class EchoRequest(val message: String, val senderAgentId: String)

@Serializable
data class EchoResponse(val reply: String, val respondingAgentId: String) {
    override fun toString(): String {
        return "[$respondingAgentId] $reply"
    }
}
