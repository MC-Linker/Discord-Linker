plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("dev.architectury.loom") version "1.17.491" apply false
    id("dev.architectury.loom-no-remap") version "1.17.491" apply false
    id("architectury-plugin") version "3.4.161" apply false
}

val modPublish = ModPublish(project)

publishMods {
    displayName = "${property("mod.display_name")} v${modPublish.version}"
    version = modPublish.version
    changelog = modPublish.getChangelog(modPublish.version)
    type = STABLE
    dryRun = modPublish.dryRunMode

    github {
        accessToken = modPublish.githubToken
        repository = modPublish.githubRepository
        commitish = "main"
        tagName = "Discord-Linker-${modPublish.version}"

        allowEmptyFiles = true
    }
}

stonecutter active "1.20+1-fabric"

