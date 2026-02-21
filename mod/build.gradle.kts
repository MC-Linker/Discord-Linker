plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("me.modmuss50.mod-publish-plugin")
    id("com.gradleup.shadow") version "9.3.1"
}

// Repositories
repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://www.cursemaven.com") { name = "CurseForge" }
        }
        filter { includeGroup("curse.maven") }
    }
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") { name = "Modrinth" }
        }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev/")
    maven("https://modmaven.dev/")
    maven("https://panel.ryuutech.com/nexus/repository/maven-releases/")
}

val env = Env(project, stonecutter::compare)

val archVersion = versionProperty("deps.api.architectury")
val lpVersion = versionProperty("deps.api.luckperms")

val deps = arrayListOf(
    Dependency(
        ModInfo("architectury", "architectury-api"),
        archVersion,
        side = "SERVER"
    ),
    Dependency(
        ModInfo("luckperms", "luckperms"),
        lpVersion,
        optional = true,
        side = "SERVER",
        ordering = "BEFORE"
    ),
    Dependency(
        ModInfo("fabricloader", "fabricloader"),
        env.fabricLoaderVersion,
        enabled = env.isFabric,
        publish = false
    ),
    Dependency(
        ModInfo("minecraft", "minecraft"),
        env.mcVersion,
        enabled = env.isFabric,
        publish = false
    )
)

val modPublish = ModPublish(project, env.mcVersion)

val mod = ModProperties(project)
val metaExclude = MetadataExcludes(env)

version = "${mod.version}+${env.mcVersion.min}+${env.loader}"

//dependencies.forEachAfter { mid, ver -> stonecutter { dependencies[mid] = ver.min } }
deps.forEach { dep ->
    dep.modInfo.modid?.let {
        stonecutter { constants[it] = dep.enabled }
        stonecutter { dependencies[it] = dep.versionRange.min }
    }
}
stonecutter {
    constants["fabric"] = env.isFabric
    constants["forge"] = env.isForge
    constants["neoforge"] = env.isNeo
    constants["spigot"] = false
    constants["mod"] = true

    replacements.string(env.isNeo) {
        replace("net.minecraftforge", "net.neoforged")
    }

    replacements.string(env.atMost("1.18.0")) {
        replace("dev.architectury", "me.shedaniel.architectury")
    }
}

loom {
    silentMojangMappingsLicense()
    runConfigs.all {
        ideConfigGenerated(stonecutter.current.isActive)
        runDir = "../../run"
    }
}

base {
    archivesName.set(env.archivesBaseName)
}

val shadowLib by configurations.creating

configurations.implementation {
    extendsFrom(shadowLib)
}

dependencies {
    minecraft("com.mojang:minecraft:${env.mcVersion.min}")
    mappings(loom.officialMojangMappings())
    if (env.isFabric) modImplementation("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
    if (env.isForge) "forge"("net.minecraftforge:forge:${env.forgeMavenVersion.min}")
    if (env.isNeo) "neoForge"("net.neoforged:neoforge:${env.neoforgeVersion.min}")

    val archMaven = "${if (env.atLeast("1.18.0")) "dev.architectury" else "me.shedaniel"}:architectury-${env.loader}"
    modApi("$archMaven:${archVersion.min}")

    modCompileOnly("net.luckperms:api:${lpVersion.min}")

    shadowLib("org.yaml:snakeyaml:2.5")
    shadowLib("io.socket:socket.io-client:2.1.2")

    if (env.isForge || env.isNeo) {
        "forgeRuntimeLibrary"("org.yaml:snakeyaml:2.5")
        "forgeRuntimeLibrary"("io.socket:socket.io-client:2.1.2")
    }
}

configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
    }
}

tasks {
    shadowJar {
        configurations = listOf(shadowLib)

//        archiveClassifier.set("")

        relocate("org.yaml.snakeyaml", "me.lianecx.snakeyaml")
        relocate("io.socket", "me.lianecx.iosocket")
        relocate("okio", "me.lianecx.okio")
        relocate("okhttp3", "me.lianecx.okhttp3")
        relocate("org.json", "me.lianecx.json")

        destinationDirectory = layout.buildDirectory.dir("devlibs")
    }

    remapJar {
        inputFile = shadowJar.flatMap { it.archiveFile }
    }

    jar {
        enabled = false
    }
}

java {
    val java = when (env.javaVer) {
        8 -> JavaVersion.VERSION_1_8
        17 -> JavaVersion.VERSION_17
        else -> JavaVersion.VERSION_21
    }

    sourceCompatibility = JavaVersion.VERSION_1_8 // Keep codebase in java 8 for compatibility
    targetCompatibility = java
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(env.javaVer))
    }
}

tasks.processResources {
    val (fabricDeps, forgeDeps) = dependencyJsonOrToml(deps)

    val map = env.resourceMap(mod, env) + mapOf(
        "dependencies_json" to fabricDeps,
        "dependencies_field" to forgeDeps
    )

    map.forEach { (key, value) -> inputs.property(key, value) }

    metaExclude.files.forEach { file -> exclude(file) }
    filesMatching("fabric.mod.json") { expand(map) }
    filesMatching("META-INF/mods.toml") { expand(map) }
    filesMatching("META-INF/neoforge.mods.toml") { expand(map) }
}

sourceSets.main {
    java {
        val spigot = listOf("**/spigot/**")
        exclude(
            spigot + when {
                env.isFabric -> listOf("**/forge/**")
                env.isForge -> listOf("**/fabric/**")
                env.isNeo -> listOf("**/fabric/**")
                else -> throw IllegalStateException("No valid mod environment detected")
            }
        )
    }

    resources {
        val spigot = listOf("plugin.yml")
        exclude(
            spigot + when {
                env.isFabric -> listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
                env.isForge -> listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")
                env.isNeo -> listOf("fabric.mod.json", "META-INF/mods.toml")
                else -> throw IllegalStateException("No valid mod environment detected")
            }
        )
    }
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    displayName = "${mod.displayName} ${mod.version} for ${env.mcVersion.min}"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add(env.loader)
    dryRun = modPublish.dryRunMode

    modrinth {
        projectId = modPublish.modrinthProjectToken
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(modPublish.mcTargets)
        deps.forEach { dep ->
            if (dep.enabled && dep.publish) {
                if (dep.optional)
                    dep.modInfo.rinthSlug?.let { optional { slug = it; version = dep.versionRange.min } }
                else
                    dep.modInfo.rinthSlug?.let { requires { slug = it; version = dep.versionRange.min } }
            }
        }
    }

    curseforge {
        projectId = modPublish.curseforgeProjectToken
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(modPublish.mcTargets)
        deps.forEach { dep ->
            if (dep.enabled && dep.publish) {
                if (dep.optional)
                    dep.modInfo.curseSlug?.let { optional { slug = it; version = dep.versionRange.min } }
                else
                    dep.modInfo.curseSlug?.let { requires { slug = it; version = dep.versionRange.min } }
            }
        }
    }
}