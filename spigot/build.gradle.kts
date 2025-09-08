plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.lianecx"
version = "3.6-SNAPSHOT"
description = "Official plugin for the MC Linker Discord Bot."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.12-R0.1-SNAPSHOT")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("io.socket:socket.io-client:2.1.2")
//    implementation("io.vacco.java-express:java-express:0.2.1")
    implementation("io.github.bananapuncher714:nbteditor:7.19.10")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    compileOnly("net.luckperms:api:5.4")
}

// Shadow plugin configuration
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    minimize()
    relocate("org.bstats", "me.lianecx")
}

// Make the shadow JAR the default artifact
tasks.build {
    dependsOn(tasks.shadowJar)
}
