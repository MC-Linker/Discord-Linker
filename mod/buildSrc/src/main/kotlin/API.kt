import java.util.*
import java.util.function.Predicate

enum class DepType {
    API,
    API_OPTIONAL {
        override fun isOptional() = true
    },
    IMPL,
    FRL {
        override fun includeInDepsList() = false
    },
    INCLUDE {
        override fun includeInDepsList() = false
    };

    open fun isOptional() = false
    open fun includeInDepsList() = true
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