import groovy.json.JsonOutput
import net.meatwo310.mdk.build.*
import org.gradle.api.file.DuplicatesStrategy

plugins {
    `java-library`
    idea
}

val modId = project.property("modId").toString()
val modName = project.property("modName").toString()
val modLicense = project.property("modLicense").toString()
val modVersion = project.property("modVersion").toString()
val modAuthors = project.property("modAuthors").toString()
val modDescription = project.property("modDescription").toString()
val modDisplayUrl = project.property("modDisplayUrl").toString()
val modIssueTrackerUrl = project.property("modIssueTrackerUrl").toString()
val modCredits = project.property("modCredits").toString()
val modFabricEnvironment = project.property("modFabricEnvironment").toString()
val modFabricEntrypoint = project.property("modFabricEntrypoint").toString()
val modFabricClientEntrypoint = project.property("modFabricClientEntrypoint").toString()
val minecraftVersion = project.property("minecraftVersion").toString()
val javaVersion = project.property("javaVersion").toString()

val commonProject = ":$minecraftVersion-common"
val sharedCommonProject = ":common"
evaluationDependsOn(sharedCommonProject)
val generatedModMetadataDir = layout.buildDirectory.dir("generated/sources/modMetadata")
val fabricModMetadata = extensions.create<FabricModMetadataExtension>("fabricModMetadata")
configureCiRuntimeMods()
configurePlatformArtifacts(loader = "fabric", sourcesJarTaskName = "sourcesJar")

dependencies {
    implementation(project(commonProject))
}

base {
    archivesName = "$modId-$minecraftVersion-fabric"
}

sourceSets.main.get().resources.srcDir(generatedModMetadataDir)

fabricModMetadata.depends.putAll(linkedMapOf(
    "fabricloader" to ">=${versionCatalog.version(VersionCatalogVersion.FabricLoader)}",
    "minecraft" to "~$minecraftVersion",
    "java" to ">=$javaVersion",
))

fun defaultModMetadata(fabricDependencies: Map<String, String>) = linkedMapOf<String, Any?>(
    "schemaVersion" to 1,
    "id" to modId,
    "version" to modVersion,
    "name" to modName,
    "description" to modDescription,
    "authors" to modAuthors.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    "contact" to linkedMapOf(
        "homepage" to modDisplayUrl,
        "issues" to modIssueTrackerUrl,
        "sources" to modDisplayUrl,
    ),
    "license" to modLicense,
    "custom" to linkedMapOf(
        "credits" to modCredits,
    ),
    "environment" to modFabricEnvironment,
    "entrypoints" to linkedMapOf(
        "main" to listOf(modFabricEntrypoint),
        "client" to listOf(modFabricClientEntrypoint),
    ),
    "depends" to fabricDependencies,
)

val generateModMetadata = tasks.register<GenerateFabricModMetadata>("generateModMetadata") {
    templateFile.set(layout.projectDirectory.file("src/main/templates/fabric.mod.json"))
    outputFile.set(generatedModMetadataDir.map { it.file("fabric.mod.json") })
    defaultModMetadataJson.set(fabricModMetadata.depends.map { JsonOutput.toJson(defaultModMetadata(it)) })
}

tasks.processResources {
    dependsOn(generateModMetadata)
}

java.toolchain {
    languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
    @Suppress("UnstableApiUsage")
    vendor = JvmVendorSpec.JETBRAINS
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion.toInt()
}

java {
    withSourcesJar()
}

tasks.named("sourcesJar") {
    dependsOn(generateModMetadata)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(commonProject).sourceSets.main.get().output)
    from(project(sharedCommonProject).sourceSets.main.get().output)
}
