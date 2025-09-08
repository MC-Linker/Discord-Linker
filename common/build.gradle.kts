plugins {
    java
}

group = "me.lianecx.discordlinker.spigot"
version = "1.0-SNAPSHOT"
description = "Common library for the Discord-Linker project."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("io.socket:socket.io-client:2.1.2")
    implementation("com.google.code.gson:gson:2.13.1")
}
