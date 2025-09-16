plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

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

val properties = mapOf(
    "name" to property("mod.display_name").toString(),
    "description" to property("mod.description").toString(),
    "author" to property("mod.authors").toString(),
    "website" to property("mod.general_website").toString(),
    "version" to property("version").toString(),
    "main" to property("mod.spigot.main").toString(),
    "spigot_version_range" to property("deps.core.spigot.version_range").toString()
)

dependencies {
    compileOnly("org.spigotmc:spigot-api:${properties["spigot_version_range"]}-R0.1-SNAPSHOT")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("io.socket:socket.io-client:2.1.2")
//    implementation("io.vacco.java-express:java-express:0.2.1")
    implementation("io.github.bananapuncher714:nbteditor:7.19.10")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    compileOnly("net.luckperms:api:5.4")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(properties)
    }
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

stonecutter {
    constants["fabric"] = false
    constants["forge"] = false
    constants["neoforge"] = false
    constants["spigot"] = true
}
