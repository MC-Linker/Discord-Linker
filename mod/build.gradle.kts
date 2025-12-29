plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("dev.architectury.loom")
    id("architectury-plugin")
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

val mod = ModProperties(project)
val metaExclude = MetadataExcludes(env)

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

    apis.forEach { it.applyDependency(this, env) }

    implementation("io.socket:socket.io-client:2.1.2")
    compileOnly("net.luckperms:api:5.4")
    implementation("org.yaml:snakeyaml:2.4")
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
    val (fabricDeps, forgeDeps) = dependencyJsonOrToml(apis)

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
                if (src.type.optional)
                    src.modInfo.rinthSlug?.let { optional { slug = it; version = ver.min } }
                else
                    src.modInfo.rinthSlug?.let { requires { slug = it; version = ver.min } }
            }
        }
    }

    curseforge {
        projectId = modPublish.curseforgeProjectToken
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(modPublish.mcTargets)
        apis.forEach { src ->
            if (src.enabled) src.versionRange.ifPresent { ver ->
                if (src.type.optional)
                    src.modInfo.curseSlug?.let { optional { slug = it; version = ver.min } }
                else
                    src.modInfo.curseSlug?.let { requires { slug = it; version = ver.min } }
            }
        }
    }
}