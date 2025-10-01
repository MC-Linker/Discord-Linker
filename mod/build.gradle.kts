import kotlin.collections.*

plugins {
    kotlin("jvm")
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("dev.kikugie.j52j")
    id("me.modmuss50.mod-publish-plugin")
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

val apis = arrayListOf(
    APISource(
        DepType.API,
        APIModInfo("fabric", "fabric-api"),
        "net.fabricmc.fabric-api:fabric-api",
        optionalVersionProperty("deps.api.fabric")
    ) { src -> src.versionRange.isPresent && env.isFabric },
    APISource(
        DepType.API,
        APIModInfo("architectury", "architectury-api"),
        "${if (env.atLeast("1.18.0")) "dev.architectury" else "me.shedaniel"}:architectury-${env.loader}",
        optionalVersionProperty("deps.api.architectury")
    ) { src -> src.versionRange.isPresent }
)

val modPublish = ModPublish(project, env.mcVersion)

class SpecialMultiversionedConstants {
    private val mandatoryIndicator = if (env.isNeo) "required" else "mandatory"

    val excludes = excludes0()

    private fun excludes0(): List<String> {
        val out = arrayListOf<String>()
        if (!env.isForge) {
            if (!env.isNeo || !env.atLeast("1.20.6")) out.add("META-INF/mods.toml")
        }
        if (!env.isFabric) out.add("fabric.mod.json")
        if (!env.isNeo) out.add("META-INF/neoforge.mods.toml")
        return out
    }
}

class ModProperties {
    val id = property("mod.id").toString()
    val displayName = property("mod.display_name").toString()
    val version = property("version").toString()
    val description = optionalStrProperty("mod.description").orElse("")
    val authors = property("mod.authors").toString()
    val icon = property("mod.icon").toString()
    val issueTracker = optionalStrProperty("mod.issue_tracker").orElse("")
    val license = optionalStrProperty("mod.license").orElse("")
    val sourceUrl = optionalStrProperty("mod.source_url").orElse("")
    val generalWebsite = optionalStrProperty("mod.general_website").orElse(sourceUrl)
}
val mod = ModProperties()
val dynamics = SpecialMultiversionedConstants()

version = "${mod.version}+${env.mcVersion.min}+${env.loader}"

//dependencies.forEachAfter { mid, ver -> stonecutter { dependencies[mid] = ver.min } }
apis.forEach { src ->
    src.modInfo.modid?.let {
        stonecutter { constants[it] = src.enabled }
        src.versionRange.ifPresent { ver -> stonecutter { dependencies[it] = ver.min } }
    }
}
stonecutter {
    constants["fabric"] = env.isFabric
    constants["forge"] = env.isForge
    constants["neoforge"] = env.isNeo
    constants["spigot"] = false
    constants["mod"] = true
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

dependencies {
    minecraft("com.mojang:minecraft:${env.mcVersion.min}")
    mappings(loom.officialMojangMappings())
    if (env.isFabric) modImplementation("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
    if (env.isForge) "forge"("net.minecraftforge:forge:${env.forgeMavenVersion.min}")
    if (env.isNeo) "neoForge"("net.neoforged:neoforge:${env.neoforgeVersion.min}")

    apis.forEach { src ->
        if (src.enabled) {
            src.versionRange.ifPresent { ver ->
                when (src.type) {
                    DepType.API, DepType.API_OPTIONAL -> modApi("${src.mavenLocation}:${ver.min}")
                    DepType.IMPL -> modImplementation("${src.mavenLocation}:${ver.min}")
                    DepType.FRL -> if (env.isForge) "forgeRuntimeLibrary"("${src.mavenLocation}:${ver.min}")
                    DepType.INCLUDE -> {
                        modImplementation("${src.mavenLocation}:${ver.min}")
                        include("${src.mavenLocation}:${ver.min}")
                    }
                }
            }
        }
    }

    implementation("io.socket:socket.io-client:2.1.2")
    compileOnly("net.luckperms:api:5.4")
}

configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
    }
}

java {
    val java = when (env.javaVer) {
        8 -> JavaVersion.VERSION_1_8
        17 -> JavaVersion.VERSION_17
        else -> JavaVersion.VERSION_21
    }
    targetCompatibility = java
    sourceCompatibility = java
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(env.javaVer))
    }
}

tasks.processResources {
    val map = mapOf(
        "modid" to mod.id,
        "id" to mod.id,
        "name" to mod.displayName,
        "display_name" to mod.displayName,
        "version" to mod.version,
        "description" to mod.description,
        "authors" to mod.authors,
        "github_url" to mod.sourceUrl,
        "source_url" to mod.sourceUrl,
        "website" to mod.generalWebsite,
        "icon" to mod.icon,
        "fabric_server_entry" to env.fabricServerEntry,
        "mc_min" to env.mcVersion.min,
        "mc_max" to env.mcVersion.max,
        "issue_tracker" to mod.issueTracker,
        "java_ver" to env.javaVer.toString(),
        "forgelike_loader_ver" to env.forgelikeLoaderVersion,
        "forgelike_api_ver" to env.forgelikeAPIVersion,
        "loader_id" to env.loader,
        "license" to mod.license,
    )
    map.forEach { (key, value) -> inputs.property(key, value) }
//    dynamics.excludes.forEach { file -> exclude(file) }
    filesMatching("fabric.mod.json") { expand(map) }
    filesMatching("META-INF/mods.toml") { expand(map) }
    filesMatching("META-INF/neoforge.mods.toml") { expand(map) }
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
        apis.forEach { src ->
            if (src.enabled) src.versionRange.ifPresent { ver ->
                if (src.type.isOptional()) {
                    src.modInfo.rinthSlug?.let { optional { slug = it; version = ver.min } }
                } else {
                    src.modInfo.rinthSlug?.let { requires { slug = it; version = ver.min } }
                }
            }
        }
    }

    curseforge {
        projectId = modPublish.curseforgeProjectToken
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(modPublish.mcTargets)
        apis.forEach { src ->
            if (src.enabled) src.versionRange.ifPresent { ver ->
                if (src.type.isOptional()) {
                    src.modInfo.curseSlug?.let { optional { slug = it; version = ver.min } }
                } else {
                    src.modInfo.curseSlug?.let { requires { slug = it; version = ver.min } }
                }
            }
        }
    }
}