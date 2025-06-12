plugins {
    kotlin("jvm") version "2.1.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public/")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("ai.koog:koog-agents:0.2.1")
    implementation("ai.jetbrains.code.prompt:code-prompt-executor-grazie-koog-jvm:1.0.0-beta.68+0.4.71")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}