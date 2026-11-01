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

// A dependency with only a min (single value, or max UNSET) is open-ended; otherwise it is bounded by min and max (min == max pins an exact version).
private fun Dependency.openOrBounded(): VersionRange =
    if (versionRange.minOnly || versionRange.max.isEmpty()) VersionRange(versionRange.min, "") else versionRange

fun dependencyJsonOrToml(deps: List<Dependency>, modId: String, isNeo: Boolean): Pair<String, String> {
    val enabledDeps = deps.filter { it.enabled }

    // JSON for fabric
    val jsonDeps = enabledDeps.filter { !it.optional }
        .joinToString(",\n") { "    \"${it.modInfo.modid}\": \"${it.openOrBounded().asFabric()}\"" }
    val fabricJsonDeps = "{\n$jsonDeps\n}"

    val jsonRecommends = enabledDeps.filter { it.optional }
        .joinToString(",\n") { "    \"${it.modInfo.modid}\": \"${it.openOrBounded().asFabric()}\"" }
    val fabricJsonRecommends = if (jsonRecommends.isNotBlank()) "{\n$jsonRecommends\n}" else null

    val fabricJson =
        if (fabricJsonRecommends != null)
            "\"depends\": $fabricJsonDeps,\n\"recommends\": $fabricJsonRecommends"
        else fabricJsonDeps

    // TOML for forge/neoforge. Both read dependencies from a [[dependencies.<modid>]] table;
    // NeoForge replaced the `mandatory` boolean with a `type` field.
    val tomlDeps = enabledDeps.joinToString("\n") {
        val requirement =
            if (isNeo) "type = \"${if (it.optional) "optional" else "required"}\""
            else "mandatory = ${!it.optional}"
        """
        [[dependencies.$modId]]
        modId = "${it.modInfo.modid}"
        $requirement
        versionRange = "${it.openOrBounded().asForgelike()}"
        side = "${it.side}"
        ordering = "${it.ordering}"
        """.trimIndent()
    }
    return fabricJson.prependIndent() to tomlDeps
}
