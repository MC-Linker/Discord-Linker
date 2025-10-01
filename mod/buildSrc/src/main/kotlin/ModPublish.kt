import org.gradle.api.Project

// Publishing-Konfiguration
class ModPublish(project: Project, mcVersion: VersionRange) {
    val mcTargets = arrayListOf<String>()
    val modrinthProjectToken = project.property("publish.token.modrinth").toString()
    val curseforgeProjectToken = project.property("publish.token.curseforge").toString()
    val githubToken = project.property("publish.token.github").toString()
    val dryRunMode = project.boolProperty("publish.dry_run")

    init {
        mcTargets.add(mcVersion.min)
        if (mcVersion.min != mcVersion.max) {
            mcTargets.add(mcVersion.max)
        }
    }
}