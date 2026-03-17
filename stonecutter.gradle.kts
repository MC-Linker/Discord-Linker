plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
    id("dev.architectury.loom") version "1.13-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
}

val modPublish = ModPublish(project, versionProperty("deps.core.version_range"))

publishMods {
    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = property("publish.github.repository").toString()
        commitish = "main"
        tagName = "Discord-Linker-${property("version")}"

        allowEmptyFiles = true
    }
}

stonecutter active "1.20+1-fabric"

