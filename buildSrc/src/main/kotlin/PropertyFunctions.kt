import org.gradle.api.Project
import java.util.*

fun Project.bool(str: String): Boolean {
    return str.lowercase().startsWith("t")
}

fun Project.boolProperty(key: String): Boolean {
    if (!hasProperty(key)) {
        return false
    }
    return bool(property(key).toString())
}

fun Project.listProperty(key: String): ArrayList<String> {
    if (!hasProperty(key)) {
        return arrayListOf()
    }
    val str = property(key).toString()
    if (str == "UNSET") {
        return arrayListOf()
    }
    return ArrayList(str.split(" "))
}

fun Project.optionalStrProperty(key: String): Optional<String> {
    if (!hasProperty(key)) {
        return Optional.empty()
    }
    val str = property(key).toString()
    if (str == "UNSET") {
        return Optional.empty()
    }
    return Optional.of(str)
}

class VersionRange(val min: String, val max: String) {
    fun asForgelike(): String {
        return "${if (min.isEmpty()) "(" else "["}${min},${max}${if (max.isEmpty()) ")" else "]"}"
    }

    fun asFabric(): String {
        var out = ""
        if (min.isNotEmpty()) {
            out += ">=$min"
        }
        if (max.isNotEmpty()) {
            if (out.isNotEmpty()) {
                out += " "
            }
            out += "<=$max"
        }
        return out
    }
}

/**
 * Creates a VersionRange from a listProperty
 */
fun Project.versionProperty(key: String): VersionRange {
    if (!hasProperty(key)) {
        return VersionRange("", "")
    }
    val list = listProperty(key)
    for (i in 0 until list.size) {
        if (list[i] == "UNSET") {
            list[i] = ""
        }
    }
    return if (list.isEmpty()) {
        VersionRange("", "")
    } else if (list.size == 1) {
        VersionRange(list[0], "")
    } else {
        VersionRange(list[0], list[1])
    }
}

/**
 * Creates a VersionRange unless the value is UNSET
 */
fun Project.optionalVersionProperty(key: String): Optional<VersionRange> {
    val str = optionalStrProperty(key)
    if (!hasProperty(key)) {
        return Optional.empty()
    }
    if (!str.isPresent) {
        return Optional.empty()
    }
    return Optional.of(versionProperty(key))
}