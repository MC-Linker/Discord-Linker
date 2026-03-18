import org.gradle.api.Project

class ModProperties(project: Project) {
    val id = project.property("mod.id").toString()
    val displayName = project.property("mod.display_name").toString()
    val version = project.property("version").toString()
    val pluginDescription = project.optionalStrProperty("plugin.description").orElse("").toString()
    val modDescription = project.optionalStrProperty("mod.description").orElse("").toString()
    val authors = project.property("mod.authors").toString()
    val icon = project.property("mod.icon").toString()
    val issueTracker = project.optionalStrProperty("mod.issue_tracker").orElse("").toString()
    val license = project.optionalStrProperty("mod.license").orElse("").toString()
    val sourceUrl = project.optionalStrProperty("mod.source_url").orElse("").toString()
    val generalWebsite = project.optionalStrProperty("mod.general_website").orElse(sourceUrl).toString()
}