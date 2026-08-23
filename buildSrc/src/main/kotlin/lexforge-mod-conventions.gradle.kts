import net.meatwo310.mdk.build.*
import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import org.gradle.api.file.DuplicatesStrategy

plugins {
    `java-library`
    idea
    id("net.minecraftforge.gradle")
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
val minecraftVersion = project.property("minecraftVersion").toString()
val minecraftVersionRange = project.property("minecraftVersionRange").toString()
val forgeVersion = project.property("forgeVersion").toString()
val forgeVersionRange = project.property("forgeVersionRange").toString()
val loaderVersionRange = project.property("loaderVersionRange").toString()
val javaVersion = project.property("javaVersion").toString()
val forgeEventbusValidatorVersion = project.findProperty("forgeEventbusValidatorVersion")?.toString()

val forgeFullVersion = "$minecraftVersion-$forgeVersion"
val commonProject = ":$minecraftVersion-common"
val sharedCommonProject = ":common"
evaluationDependsOn(commonProject)
evaluationDependsOn(sharedCommonProject)
configureCiRuntimeMods()
configurePlatformArtifacts(loader = "forge", javaVersion = javaVersion.toInt(), sourcesJarTaskName = "sourcesJar")

val minecraft = extensions.getByType(MinecraftExtensionForProject::class.java)
val fg = extensions.getByType(ForgeGradleExtension::class.java)

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {
    compileOnly(project(commonProject))
    implementation(minecraft.dependency("net.minecraftforge:forge:$forgeFullVersion").asProvider())
    annotationProcessor("${versionCatalog.module(VersionCatalogLibrary.Mixin)}:${versionCatalog.version(VersionCatalogVersion.Mixin)}:processor")
    if (forgeEventbusValidatorVersion != null) {
        annotationProcessor("net.minecraftforge:eventbus-validator:$forgeEventbusValidatorVersion")
    }
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
    exclude("**/*.bbmodel")
    exclude("src/generated/**/.cache")
}

base {
    archivesName = "$modId-$minecraftVersion-forge"
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

configurations {
    val localRuntime = create("localRuntime")
    runtimeClasspath.get().extendsFrom(localRuntime)
}

minecraft.mappings("official", minecraftVersion)

minecraft.runs {
    configureEach {
        workingDir = layout.projectDirectory.dir("run")
        systemProperty("forge.enabledGameTestNamespaces", modId)
        mods {
            create(modId) {
                source(sourceSets.main.get())
            }
        }
    }

    create("client")

    create("server") {
        args("--nogui")
    }

    if (minecraftVersion.supportsGameTestServer()) {
        create("gameTestServer")
    }

    create("data") {
        workingDir = layout.projectDirectory.dir("run-data")
        args(
            "--mod", modId,
            "--all",
            "--output", layout.projectDirectory.dir("src/generated/resources"),
            "--existing", layout.projectDirectory.dir("src/main/resources"),
        )
    }
}

if (forgeEventbusValidatorVersion != null) {
    minecraft.runs.configureEach {
        systemProperty("eventbus.api.strictRuntimeChecks", "true")
    }
}

val generateModMetadata = tasks.register<GenerateLexForgeModMetadata>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "forge_version" to forgeVersion,
        "forge_version_range" to forgeVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "mod_credits" to modCredits,
        "mod_display_url" to modDisplayUrl,
        "mod_issue_tracker_url" to modIssueTrackerUrl,
    )
    properties.set(replaceProperties)
    templateDirectory.set(layout.projectDirectory.dir("src/main/templates"))
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main.get().resources.srcDir(generateModMetadata.flatMap { it.outputDirectory })

// ForgeGradle 7 scans each output directory as a separate mod root. Keep the
// generated metadata and compiled @Mod class in the same root for dev runs.
val mainClassesDir = layout.buildDirectory.dir("classes/java/main")
sourceSets.main.get().output.setResourcesDir(mainClassesDir.get().asFile)
tasks.named<ProcessResources>("processResources") {
    destinationDir = mainClassesDir.get().asFile
}

val stageCommonForRuns = tasks.register<Copy>("stageLexForgeCommonForRuns") {
    dependsOn(
        project(commonProject).tasks.named("classes"),
        project(sharedCommonProject).tasks.named("classes"),
    )
    from(project(commonProject).sourceSets.main.get().output)
    from(project(sharedCommonProject).sourceSets.main.get().output)
    into(mainClassesDir)
}
tasks.matching { it.name.startsWith("run") }.configureEach {
    dependsOn(stageCommonForRuns)
}

tasks.named("sourcesJar") {
    dependsOn(generateModMetadata)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(commonProject).sourceSets.main.get().output)
    from(project(sharedCommonProject).sourceSets.main.get().output)
    from(generateModMetadata)
    manifest.attributes(mapOf("MixinConfigs" to "$modId.mixins.json"))
}
