import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm") version "2.2.21" apply false
    java
    id("me.modmuss50.mod-publish-plugin")
    id("com.gradleup.shadow") version "9.6.1"
}

val env = Env(project, stonecutter::compare)

// Only obfuscated versions (< 26) use the normal
// remapping toolchain (mappings + remapJar + mod* configs); unobfuscated ones have to use the
// loom-no-remap plugin. See architectury-loom#328.
val isObfus = !env.atLeast("26")
apply(plugin = if(isObfus) "dev.architectury.loom" else "dev.architectury.loom-no-remap")
apply(plugin = "architectury-plugin")

val loomExt = the<LoomGradleExtensionAPI>()

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

val archVersion = versionProperty("deps.api.architectury")
val fabricApiVersion = versionProperty("deps.api.fabric")
val lpVersion = versionProperty("deps.api.luckperms")

val deps = arrayListOf(
    Dependency(
        ModInfo("architectury", "architectury-api"),
        archVersion,
        side = "SERVER"
    ),
    Dependency(
        ModInfo("luckperms"),
        lpVersion,
        optional = true,
        side = "SERVER",
        ordering = "BEFORE"
    ),
    Dependency(
        ModInfo("fabricloader"),
        env.fabricLoaderVersion,
        enabled = env.isFabric,
        publish = false
    ),
    Dependency(
        ModInfo("minecraft"),
        env.mcVersion,
        enabled = env.isFabric,
        publish = false
    ),
    Dependency(
        ModInfo(if(env.atMost("1.18")) "fabric" else "fabric-api", "fabric-api"),
        fabricApiVersion,
        enabled = env.isFabric,
        side = "SERVER"
    )
)

val modPublish = ModPublish(project)

val mod = ModProperties(project)
val metaExclude = MetadataExcludes(env)

//dependencies.forEachAfter { mid, ver -> stonecutter { dependencies[mid] = ver.min } }
deps.forEach { dep ->
    dep.modInfo.modid?.let {
        stonecutter { constants[it] = dep.enabled }
        if(dep.enabled) stonecutter { dependencies[it] = dep.versionRange.min }
    }
}
stonecutter {
    constants["fabric"] = env.isFabric
    constants["forge"] = env.isForge
    constants["neoforge"] = env.isNeo
    constants["spigot"] = false
    constants["mod"] = true

    replacements.string(env.isNeo) {
        replace("net.minecraftforge.fml", "net.neoforged.fml")
        replace("net.minecraftforge.common", "net.neoforged.neoforge.common")
        replace("net.minecraftforge.event", "net.neoforged.neoforge.event")
    }

    replacements.string(env.atMost("1.18.0")) {
        replace("dev.architectury", "me.shedaniel.architectury")
    }
}

// Resolved here (Project receiver) to avoid RunConfigSettings' deprecated getProject().
val serverRunDir = project.file("../../run")

configure<LoomGradleExtensionAPI> {
    if(isObfus) silentMojangMappingsLicense()
    if(env.isForge && env.atMost("1.16.5")) {
        forge {
            mixinConfig("discordlinker.forge.legacy.mixins.json")
        }
    }

    runConfigs.all {
        if(runtimeEnvironment.get() == "server") {
            generateRunConfig.set(false)
            runDirectory.set(serverRunDir)
        }
    }
}

base {
    archivesName.set(env.archivesBaseName)
}

val shadowLib by configurations.creating

configurations.implementation {
    extendsFrom(shadowLib)
}

// Unobfuscated versions have no remapping, so mod* configurations don't exist there;
// use plain implementation/compileOnly instead.
val implCfg = if(isObfus) "modImplementation" else "implementation"
val apiCfg = if(isObfus) "modApi" else "implementation"
val compileOnlyCfg = if(isObfus) "modCompileOnly" else "compileOnly"

dependencies {
    "minecraft"("com.mojang:minecraft:${env.mcVersion.min}")

    if(isObfus) "mappings"(loomExt.officialMojangMappings())
    if(env.isFabric) implCfg("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
    if(env.isFabric) implCfg("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion.min}")
    if(env.isForge) "forge"("net.minecraftforge:forge:${env.forgeMavenVersion.min}")
    if(env.isNeo) "neoForge"("net.neoforged:neoforge:${env.neoforgeVersion.min}")

    val archMaven = "${if(env.atLeast("1.18.0")) "dev.architectury" else "me.shedaniel"}:architectury-${env.loader}"
    apiCfg("$archMaven:${archVersion.min}")

    compileOnlyCfg("net.luckperms:api:${lpVersion.min}")

    shadowLib("org.yaml:snakeyaml:2.5")
    shadowLib("io.socket:socket.io-client:2.1.2")

    if((env.isForge || env.isNeo) && isObfus) {
        "forgeRuntimeLibrary"("org.yaml:snakeyaml:2.5")
        "forgeRuntimeLibrary"("io.socket:socket.io-client:2.1.2")
    }
}

configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
    }
}

// Relocation and Shadowing
tasks {
    shadowJar {
        configurations = listOf(shadowLib)

        relocate("org.yaml.snakeyaml", "me.lianecx.snakeyaml")
        relocate("io.socket", "me.lianecx.iosocket")
        relocate("okio", "me.lianecx.okio")
        relocate("okhttp3", "me.lianecx.okhttp3")
        relocate("org.json", "me.lianecx.json")

        if(isObfus) destinationDirectory = layout.buildDirectory.dir("devlibs")
        else {
            // No remap step: shadowJar is the final artifact.
            archiveClassifier.set(env.loader)
            archiveVersion.set("${mod.version}-${env.mcVersion.min}")
            archiveBaseName.set(env.archivesBaseName)
        }
    }

    if(isObfus) {
        named<RemapJarTask>("remapJar") {
            inputFile.set(shadowJar.flatMap { it.archiveFile })

            archiveClassifier.set(env.loader)
            archiveVersion.set("${mod.version}-${env.mcVersion.min}")
            archiveBaseName.set(env.archivesBaseName)
        }

        jar {
            enabled = false
        }
    }
    else {
        // Give the plain jar a distinct name so it doesn't clash with the shadowJar artifact.
        jar {
            archiveClassifier.set("raw")
        }
        assemble {
            dependsOn(shadowJar)
        }
    }
}

java {
    val java = when(env.javaVer) {
        8    -> JavaVersion.VERSION_1_8
        17   -> JavaVersion.VERSION_17
        25   -> JavaVersion.VERSION_25
        else -> JavaVersion.VERSION_21
    }

    sourceCompatibility = JavaVersion.VERSION_1_8 // Keep codebase in java 8 for compatibility
    targetCompatibility = java
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(env.javaVer))
    }
}

// Add JBR for advanced hotreloading
val jbrLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(env.javaVer))
    @Suppress("UnstableApiUsage")
    if(env.javaVer > 8) vendor.set(JvmVendorSpec.JETBRAINS)
}

tasks.withType<JavaExec>().configureEach {
    if(name == "runServer" && env.javaVer > 8) {
        javaLauncher.set(jbrLauncher)
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")
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
        val legacyForgeMixin = if(env.isForge && env.atMost("1.16.5")) emptyList() else listOf("**/forge/mixin/**")
        exclude(
            spigot + legacyForgeMixin + when {
                env.isFabric -> listOf("**/forge/**")
                env.isForge  -> listOf("**/fabric/**")
                env.isNeo    -> listOf("**/fabric/**")
                else         -> throw IllegalStateException("No valid mod environment detected")
            }
        )
    }

    resources {
        val spigot = listOf("plugin.yml")
        val legacyForgeMixin =
            if(env.isForge && env.atMost("1.16.5")) emptyList() else listOf("discordlinker.forge.legacy.mixins.json")
        exclude(
            spigot + legacyForgeMixin + when {
                env.isFabric -> listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
                env.isForge  -> listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")
                env.isNeo    -> listOf("fabric.mod.json", "META-INF/mods.toml")
                else         -> throw IllegalStateException("No valid mod environment detected")
            }
        )
    }
}

publishMods {
    file = if(isObfus) tasks.named<RemapJarTask>("remapJar").flatMap { it.archiveFile }
    else tasks.shadowJar.flatMap { it.archiveFile }
    displayName = "${mod.displayName} v${modPublish.version}"
    version = modPublish.version
    changelog = modPublish.getChangelog(modPublish.version)
    type = STABLE
    modLoaders.add(env.loader)
    dryRun = modPublish.dryRunMode

    modrinth {
        projectId = modPublish.modrinthProjectId
        accessToken = modPublish.modrinthToken

        minecraftVersionRange {
            start = modPublish.mcVersionRange.min
            end = modPublish.mcVersionRange.max
        }

        deps.forEach { dep ->
            if(dep.enabled && dep.publish) {
                if(dep.optional) dep.modInfo.rinthSlug?.let { optional(it) }
                else dep.modInfo.rinthSlug?.let { requires(it) }
            }
        }
    }

    curseforge {
        projectId = modPublish.curseforgeProjectId
        accessToken = modPublish.curseforgeToken

        client = false
        server = true

        minecraftVersionRange {
            start = modPublish.mcVersionRange.min
            end = modPublish.mcVersionRange.max
        }

        deps.forEach { dep ->
            if(dep.enabled && dep.publish) {
                if(dep.optional) dep.modInfo.curseSlug?.let { optional(it) }
                else dep.modInfo.curseSlug?.let { requires(it) }
            }
        }
    }

    github {
        accessToken = modPublish.githubToken

        parent(rootProject.tasks.named("publishGithub"))
    }
}

