import net.meatwo310.mdk.build.*
import org.gradle.api.file.DuplicatesStrategy

plugins {
    `java-library`
    idea
    id("net.neoforged.moddev.legacyforge")
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
val parchmentMinecraftVersion = project.property("parchmentMinecraftVersion").toString()
val parchmentMappingsVersion = project.property("parchmentMappingsVersion").toString()

val forgeFullVersion = "$minecraftVersion-$forgeVersion"
val commonProject = ":$minecraftVersion-common"
val sharedCommonProject = ":common"
evaluationDependsOn(sharedCommonProject)
configureCiRuntimeMods()
configurePlatformArtifacts(loader = "forge", javaVersion = 17, sourcesJarTaskName = "sourcesJar")

dependencies {
    implementation(project(commonProject))
    annotationProcessor("${versionCatalog.module(VersionCatalogLibrary.Mixin)}:${versionCatalog.version(VersionCatalogVersion.Mixin)}:processor")
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
    languageVersion = JavaLanguageVersion.of(17)
    @Suppress("UnstableApiUsage")
    vendor = JvmVendorSpec.JETBRAINS
}

java {
    withSourcesJar()
}

configurations {
    val localRuntime = create("localRuntime")
    runtimeClasspath.get().extendsFrom(localRuntime)
}

legacyForge {
    version = forgeFullVersion
    validateAccessTransformers = true

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    runs {
        create("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        if (minecraftVersion.supportsGameTestServer()) {
            create("gameTestServer") {
                type = "gameTestServer"
                systemProperty("forge.enabledGameTestNamespaces", modId)
            }
        }

        create("data") {
            data()
            gameDirectory = file("run-data")
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(project(commonProject).sourceSets.main.get())
            sourceSet(project(sharedCommonProject).sourceSets.main.get())
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
}

// When no @Mixin classes exist, the annotation processor generates no refmap/TSRG output.
// reobfJar passes the TSRG to ART via --names and fails with FileNotFoundException if missing.
// This task creates an empty-but-valid placeholder so the build succeeds regardless.
val ensureMixinRefmap = tasks.register("ensureMixinRefmap") {
    mustRunAfter(tasks.named("compileJava"))
    val tsrgPath = project.layout.buildDirectory.get().asFile.resolve("mixin/$modId.refmap.json.mappings.tsrg").absolutePath
    doLast {
        val tsrg = File(tsrgPath)
        if (!tsrg.exists()) {
            tsrg.parentFile.mkdirs()
            tsrg.writeText("tsrg2 left right\n")
        }
    }
}

tasks.named("reobfJar") {
    dependsOn(ensureMixinRefmap)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
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
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
legacyForge.ideSyncTask(generateModMetadata)

tasks.named("sourcesJar") {
    dependsOn(generateModMetadata)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(commonProject).sourceSets.main.get().output)
    from(project(sharedCommonProject).sourceSets.main.get().output)
    manifest.attributes(mapOf("MixinConfigs" to "$modId.mixins.json"))
}
