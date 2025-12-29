import org.gradle.api.artifacts.dsl.DependencyHandler
import java.util.*
import java.util.function.Predicate

enum class DepType(
    val optional: Boolean = false,
    val listInDeps: Boolean = true,
) {
    API,
    API_OPTIONAL(optional = true),
    IMPL,
    FRL(listInDeps = false),
    INCLUDE(listInDeps = false);
}

class APIModInfo(val modid: String?, val curseSlug: String?, val rinthSlug: String?) {
    constructor() : this(null, null, null)
    constructor(modid: String) : this(modid, modid, modid)
    constructor(modid: String, slug: String) : this(modid, slug, slug)
}

class APISource(
    val type: DepType,
    val modInfo: APIModInfo,
    val mavenLocation: String,
    val versionRange: Optional<VersionRange>,
    private val enableCondition: Predicate<APISource>
) {
    val enabled = this.enableCondition.test(this)
}

fun APISource.applyDependency(deps: DependencyHandler, env: Env) {
    if (!enabled || !versionRange.isPresent) return
    val coord = "$mavenLocation:${versionRange.get().min}"
    when (type) {
        DepType.API, DepType.API_OPTIONAL -> deps.add("modApi", coord)
        DepType.IMPL -> deps.add("modImplementation", coord)
        DepType.FRL -> if (env.isForge) deps.add("forgeRuntimeLibrary", coord)
        DepType.INCLUDE -> {
            deps.add("modImplementation", coord)
            deps.add("include", coord)
        }
    }
}