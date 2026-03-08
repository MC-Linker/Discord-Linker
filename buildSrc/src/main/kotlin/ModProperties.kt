import org.gradle.api.Project

class ModProperties(project: Project) {
    val id = project.property("mod.id").toString()
    val displayName = project.property("mod.display_name").toString()
    val version = project.property("version").toString()
    val description = project.optionalStrProperty("mod.description").orElse("")
    val authors = project.property("mod.authors").toString()
    val icon = project.property("mod.icon").toString()
    val issueTracker = project.optionalStrProperty("mod.issue_tracker").orElse("")
    val license = project.optionalStrProperty("mod.license").orElse("")
    val sourceUrl = project.optionalStrProperty("mod.source_url").orElse("")
    val generalWebsite = project.optionalStrProperty("mod.general_website").orElse(sourceUrl)
}