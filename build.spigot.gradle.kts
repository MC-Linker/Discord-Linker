plugins {
    kotlin("jvm")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.3.1"
    id("me.modmuss50.mod-publish-plugin")
    id("io.papermc.hangar-publish-plugin")
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

val mod = ModProperties(project)
val modPublish = ModPublish(project)

val spigotVersion = versionProperty("deps.core.spigot.version_range")
val properties = mapOf(
    "name" to mod.displayName,
    "description" to mod.pluginDescription,
    "author" to mod.authors,
    "website" to mod.generalWebsite,
    "version" to mod.version,
    "main" to "${property("group")}.${property("archives_base_name")}.spigot.${property("mod.spigot.main")}",
    "spigot_api_version" to spigotVersion.min
)

val shadowLib by configurations.creating

configurations.implementation {
    extendsFrom(shadowLib)
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:${spigotVersion.min}-R0.1-SNAPSHOT")
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

        archiveClassifier.set("spigot")
        archiveVersion.set(mod.version)
        archiveBaseName.set(project.property("archives_base_name").toString())

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

publishMods {
    file = tasks.shadowJar.flatMap { it.archiveFile }
    displayName = "${mod.displayName} v${modPublish.version}"
    version = modPublish.version
    changelog = modPublish.getChangelog(modPublish.version)
    type = STABLE
    modLoaders.addAll("spigot", "paper", "bukkit", "purpur")
    dryRun = modPublish.dryRunMode

    modrinth {
        projectId = modPublish.modrinthProjectId
        accessToken = modPublish.modrinthToken

        minecraftVersionRange {
            start = modPublish.mcVersionRange.min
            end = modPublish.mcVersionRange.max
        }

        optional("LuckPerms")
    }

    github {
        accessToken = modPublish.githubToken

        parent(rootProject.tasks.named("publishGithub"))
    }
}

hangarPublish {
    publications.register("plugin") {
        version = modPublish.version
        id = modPublish.hangarSlug
        channel = "Release"
        apiKey = modPublish.hangarToken
        changelog = modPublish.getChangelog(modPublish.version)

        platforms {
            paper {
                jar = tasks.shadowJar.flatMap { it.archiveFile }
                platformVersions = listOf("${modPublish.mcVersionRange.min}-${modPublish.mcVersionRange.max}")

                dependencies {
                    hangar("LuckPerms") { required.set(false) }
                }
            }
        }
    }
}
