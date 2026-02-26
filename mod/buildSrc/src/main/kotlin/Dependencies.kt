class ModInfo(val modid: String?, val curseSlug: String?, val rinthSlug: String?) {
    constructor() : this(null, null, null)
    constructor(modid: String) : this(modid, modid, modid)
    constructor(modid: String, slug: String) : this(modid, slug, slug)
}

class Dependency(
    val modInfo: ModInfo,
    val versionRange: VersionRange,
    val optional: Boolean = false,
    val side: String = "BOTH",
    val ordering: String = "NONE",
    val enabled: Boolean = true,
    val publish: Boolean = true,
)

fun dependencyJsonOrToml(deps: List<Dependency>): Pair<String, String> {
    val enabledDeps = deps.filter { it.enabled }

    // JSON for fabric
    val jsonDeps = enabledDeps.filter { !it.optional }
        .joinToString(",\n") { "    \"${it.modInfo.modid}\": \">=${it.versionRange.min}\"" }
    val fabricJsonDeps = "{\n$jsonDeps\n}"

    val jsonRecommends = enabledDeps.filter { it.optional }
        .joinToString(",\n") { "    \"${it.modInfo.modid}\": \">=${it.versionRange.min}\"" }
    val fabricJsonRecommends = if (jsonRecommends.isNotBlank()) "{\n$jsonRecommends\n}" else null

    val fabricJson =
        if (fabricJsonRecommends != null)
            "\"depends\": $fabricJsonDeps,\n\"recommends\": $fabricJsonRecommends"
        else fabricJsonDeps

    // TOML for forge/neoforge
    val tomlDeps = enabledDeps.joinToString("\n") {
        """
        [[mods.dependencies]]
        modId = "${it.modInfo.modid}"
        mandatory = ${!it.optional}
        versionRange = "[${it.versionRange.min},${it.versionRange.max}]"
        side = "${it.side}"
        ordering = "${it.ordering}"
        """.trimIndent()
    }
    return fabricJson.prependIndent() to tomlDeps
}
