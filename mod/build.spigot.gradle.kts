plugins {
    kotlin("jvm")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.3.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

base {
    archivesName.set(property("archives_base_name").toString())
}

val properties = mapOf(
    "name" to property("mod.display_name"),
    "description" to property("mod.description"),
    "author" to property("mod.authors"),
    "website" to property("mod.general_website"),
    "version" to property("version"),
    "main" to "${property("group")}.${property("archives_base_name")}.spigot.${property("mod.spigot.main")}",
    "spigot_version_range" to property("deps.core.spigot.version_range")
)

val shadowLib by configurations.creating

configurations.implementation {
    extendsFrom(shadowLib)
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:${properties["spigot_version_range"]}-R0.1-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")

    shadowLib("io.socket:socket.io-client:2.1.2")
    shadowLib("org.bstats:bstats-bukkit:3.0.0")
    shadowLib("org.yaml:snakeyaml:2.5")
    compileOnly("net.luckperms:api:5.4")
}

// Relocation and Shadowing
tasks {
    shadowJar {
        configurations = listOf(shadowLib)

        archiveClassifier.set("")

        relocate("org.yaml.snakeyaml", "me.lianecx.snakeyaml")
        relocate("org.bstats", "me.lianecx.bstats")
        relocate("io.socket", "me.lianecx.iosocket")
        relocate("okio", "me.lianecx.okio")
        relocate("okhttp3", "me.lianecx.okhttp3")
        relocate("org.json", "me.lianecx.json")
    }

    jar {
        enabled = false
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(properties)
    }
}

sourceSets.main {
    java {
        exclude(
            listOf("**/fabric/**", "**/forge/**", "**/architectury/**")
        )
    }

    resources {
        exclude(
            listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")
        )
    }
}

stonecutter {
    constants["fabric"] = false
    constants["forge"] = false
    constants["neoforge"] = false
    constants["spigot"] = true
    constants["mod"] = false
}
