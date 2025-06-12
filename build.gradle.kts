plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "1.9.23"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
    }
}

group = "org.example"
version = "1.0-SNAPSHOT"
//
//repositories {
//    mavenCentral()
//    maven(url = "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public/")
//}
//
//dependencies {
//    testImplementation(kotlin("test"))

//}
//
//tasks.test {
//    useJUnitPlatform()
//}
//kotlin {
//    jvmToolchain(23)
//}