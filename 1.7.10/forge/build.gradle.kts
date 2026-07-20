import com.gtnewhorizons.gtnhgradle.GTNHGradlePlugin.GTNHExtension
import com.gtnewhorizons.gtnhgradle.modules.ToolchainModule

plugins {
    id("com.gtnewhorizons.gtnhconvention") version "2.0.20"
}

configurations.named("api") {
    dependencies.removeIf { it.group == "org.jspecify" && it.name == "jspecify" }
}

val modMetadata = mapOf(
    "modDescription" to project.property("modDescription").toString(),
    "modAuthors" to project.property("modAuthors").toString(),
    "modCredits" to project.property("modCredits").toString(),
    "modDisplayUrl" to project.property("modDisplayUrl").toString(),
    "modIssueTrackerUrl" to project.property("modIssueTrackerUrl").toString(),
)

extensions.getByType<GTNHExtension>().extensions.configure<ToolchainModule> {
    mcmodInfoProperties.putAll(modMetadata)
}
