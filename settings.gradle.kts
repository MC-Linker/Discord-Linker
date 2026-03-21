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
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    plugins {
        kotlin("jvm") version "2.2.20"
        id("io.papermc.hangar-publish-plugin") version "0.1.2"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun loaderVersion(version: String, vararg loaders: String) {
            for (it in loaders) version("$version-$it", version)
        }

        version("1.8-spigot", "1.8").buildscript("build.spigot.gradle.kts")
        version("1.12.2-spigot", "1.12.2").buildscript("build.spigot.gradle.kts")
        // loaderVersion("1.12.2", "forge")

        // loaderVersion("1.14.4", "fabric", "forge")

        loaderVersion("1.16.5", "fabric", "forge")

        loaderVersion("1.18.2", "fabric", "forge")
        loaderVersion("1.19.2", "fabric", "forge")

        loaderVersion("1.20+1", "fabric", "forge")

        loaderVersion("1.21.11", "fabric", "neoforge")

        vcsVersion = "1.20+1-fabric"
    }
}

rootProject.name = "discordlinker"