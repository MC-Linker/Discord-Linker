import io.github.cdimascio.dotenv.Dotenv
import org.gradle.api.Project
import org.gradle.api.provider.Provider

// Publishing-Konfiguration
class ModPublish(private val project: Project) {
    private fun changelogSection(allChangelogs: String, version: String): String? {
        val escapedVersion = Regex.escape(version)
        val sectionRegex = Regex("""(?ms)^#{1,2}\s+\[?$escapedVersion\]?(?:\s*-\s*[^\r\n]*)?\R?(.*?)(?=^#{1,2}\s+|\z)""")
        return sectionRegex.find(allChangelogs)?.groupValues?.get(1)?.trim()
    }

    fun getChangelog(version: String): String {
        val changelogFile = project.rootProject.file("CHANGELOG.md")
        val allChangelogs = changelogFile.takeIf { it.exists() }?.readText()
        // Support both "1.0.0" and "1.0.0-SNAPSHOT" style version headers
        val versionsToTry = listOf(version, version.substringBefore("-")).distinct()

        val changelog = allChangelogs?.let { text ->
            versionsToTry.asSequence()
                .mapNotNull { tryVersion -> changelogSection(text, tryVersion) }
                .firstOrNull { it.isNotBlank() }
        }
        if (!changelog.isNullOrBlank()) return changelog

        if (dryRunMode) return "No changelog details provided."

        if (allChangelogs == null) throw IllegalStateException("CHANGELOG.md file not found in the project root.")
        throw IllegalStateException("Changelog section for version '$version' not found in CHANGELOG.md.")
    }

    private val dotEnv: Dotenv by lazy {
        Dotenv.configure()
            .directory(project.rootProject.projectDir.absolutePath)
            .filename(".env")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load()
    }

    private fun requiredDotEnvValue(key: String): String {
        val value = dotEnv[key]
        if (!value.isNullOrBlank()) return value

        val envPath = project.rootProject.file(".env").absolutePath
        throw IllegalStateException("Missing required key '$key' in $envPath")
    }

    private fun tokenProvider(key: String): Provider<String> {
        return project.providers.provider { requiredDotEnvValue(key) }
    }

    val mcVersionRange = project.versionProperty("deps.core.mc.version_range")
    val version = project.property("version").toString()
    val modrinthProjectId = project.optionalStrProperty("publish.project_id.modrinth").orElse("UNSET")
    val curseforgeProjectId = project.optionalStrProperty("publish.project_id.curseforge").orElse("UNSET")
    val hangarSlug = project.optionalStrProperty("publish.project_id.hangar").orElse("UNSET")
    val githubRepository = project.optionalStrProperty("publish.github.repository").orElse("UNSET")
    val modrinthToken = tokenProvider("MODRINTH_TOKEN")
    val curseforgeToken = tokenProvider("CURSEFORGE_TOKEN")
    val githubToken = tokenProvider("GITHUB_TOKEN")
    val hangarToken = tokenProvider("HANGAR_TOKEN")
    val dryRunMode = project.boolProperty("publish.dry_run")
}
