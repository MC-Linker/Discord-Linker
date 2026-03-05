import org.gradle.api.Project

class Env(project: Project, val compare: (String, String) -> Int) {
    val archivesBaseName = project.property("archives_base_name").toString()
    val mcVersion = project.versionProperty("deps.core.mc.version_range")
    val loader = project.property("loom.platform").toString()
    val isFabric = loader == "fabric"
    val isForge = loader == "forge"
    val isNeo = loader == "neoforge"
    val javaVer = when {
        atMost("1.16.5") -> 8
        atMost("1.20.4") -> 17
        else -> 21
    }
    val neoforgeVersion = project.versionProperty("deps.core.neoforge.version_range")
    val neoforgeLoaderVersion = project.versionProperty("deps.core.neoforge.loader.version_range")
    val fabricLoaderVersion = project.versionProperty("deps.core.fabric.loader.version_range")
    val forgeMavenVersion = project.versionProperty("deps.core.forge.version_range")
    val forgeVersion = VersionRange(extractForgeVer(forgeMavenVersion.min), extractForgeVer(forgeMavenVersion.max))
    private val fgl =
        if (isForge) forgeMavenVersion.min.substring(forgeMavenVersion.min.lastIndexOf("-"))
        else ""
    val forgeLanguageVersion = VersionRange(if (isForge) fgl.substring(0, fgl.indexOf(".")) else "", "")
    val forgelikeLoaderVersion =
        if (isForge) forgeLanguageVersion.asForgelike()
        else neoforgeLoaderVersion.asForgelike()
    val forgelikeAPIVersion =
        if (isForge) forgeVersion.asForgelike()
        else neoforgeVersion.asForgelike()

    val group = project.property("group").toString()

    val fabricServerEntry =
        if (isFabric) "${group}.${archivesBaseName}.fabric.${project.property("mod.fabric.entry.server")}"
        else ""

    fun atLeast(version: String) = compare(mcVersion.min, version) >= 0
    fun atMost(version: String) = compare(mcVersion.min, version) <= 0

    private fun extractForgeVer(str: String): String {
        val split = str.split("-")
        return when {
            split.size == 1 -> split[0]
            split.size > 1 -> split[1]
            else -> ""
        }
    }

    fun resourceMap(mod: ModProperties, env: Env) = mapOf(
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
}