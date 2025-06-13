plugins {
    kotlin("jvm")
    application
}

val mcpVersion = "0.4.0"
val ktorVersion = "3.1.1"

dependencies {
    implementation(project(":common"))

    implementation("ai.koog:koog-agents:0.2.1")

    implementation("io.modelcontextprotocol:kotlin-sdk:${mcpVersion}")
    implementation("io.ktor:ktor-server-core-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktorVersion}")

}

application {
    mainClass.set("com.example.serveralpha.MainAlphaKt")
}