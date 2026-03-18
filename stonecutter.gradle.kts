plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
    id("dev.architectury.loom") version "1.13-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
}

val modPublish = ModPublish(project)

publishMods {
    github {
        accessToken = modPublish.githubToken
        repository = modPublish.githubRepository
        commitish = "main"
        tagName = "Discord-Linker-${modPublish.version}"

        allowEmptyFiles = true
    }
}

stonecutter active "1.20+1-fabric"

