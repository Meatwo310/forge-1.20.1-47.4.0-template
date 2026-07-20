import com.gtnewhorizons.gtnhgradle.GTNHGradlePlugin.GTNHExtension
import com.gtnewhorizons.gtnhgradle.modules.ToolchainModule

plugins {
    id("com.gtnewhorizons.gtnhconvention") version "2.0.20" apply false
}

val modId = rootProject.property("modId").toString()
val modGroup = rootProject.property("modGroupId").toString()
val minecraftVersion = project.property("minecraftVersion").toString()

extra["modName"] = rootProject.property("modName")
extra["modId"] = modId
extra["modGroup"] = modGroup
extra["customArchiveBaseName"] = "$modId-$minecraftVersion-forge"
extra["generateGradleTokenClass"] = "$modGroup.Tags"

apply(plugin = "com.gtnewhorizons.gtnhconvention")

val modMetadata = mapOf(
    "modDescription" to rootProject.property("modDescription").toString(),
    "modAuthors" to rootProject.property("modAuthors").toString(),
    "modCredits" to rootProject.property("modCredits").toString(),
    "modDisplayUrl" to rootProject.property("modDisplayUrl").toString(),
    "modIssueTrackerUrl" to rootProject.property("modIssueTrackerUrl").toString(),
)

extensions.getByType<GTNHExtension>().extensions.configure<ToolchainModule> {
    mcmodInfoProperties.putAll(modMetadata)
}
