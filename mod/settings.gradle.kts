pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/maven")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
    }
    plugins {
        kotlin("jvm") version "2.2.20"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun loaderVersion(version: String, vararg loaders: String) {
            for (it in loaders) version("$version-$it", version)
        }

        version("1.12.2-spigot", "1.12.2").buildscript("build.spigot.gradle.kts")
        // mc("1.12.2", "forge")

        // mc("1.14.4", "fabric", "forge")

        loaderVersion("1.16.5", "fabric", "forge")

        loaderVersion("1.18.2", "fabric", "forge")
        loaderVersion("1.19.2", "fabric", "forge")

        version("1.20+1-fabric", "1.20")
        version("1.20+1-forge", "1.20")

        version("1.21+1-fabric", "1.21")
        version("1.21+1-neoforge", "1.21")

        version("1.21.2+3-fabric", "1.21.2")
        version("1.21.2+3-neoforge", "1.21.2")

        vcsVersion = "1.20+1-fabric"
    }
}

rootProject.name = "discordlinker"