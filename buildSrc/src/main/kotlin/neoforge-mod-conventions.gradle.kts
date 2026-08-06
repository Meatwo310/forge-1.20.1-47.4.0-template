import net.meatwo310.mdk.build.configureCiRuntimeMods
import net.meatwo310.mdk.build.supportsGameTestServer
import org.gradle.api.file.DuplicatesStrategy

plugins {
    `java-library`
    idea
    id("net.neoforged.moddev")
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
val neoVersion = project.property("neoVersion").toString()
val javaVersion = project.property("javaVersion").toString()

val commonProject = ":$minecraftVersion-common"
val sharedCommonProject = ":common"
evaluationDependsOn(sharedCommonProject)
val loaderVersionRange = project.findProperty("loaderVersionRange")?.toString()
val parchmentMinecraftVersion = project.findProperty("parchmentMinecraftVersion")?.toString()
val parchmentMappingsVersion = project.findProperty("parchmentMappingsVersion")?.toString()
val neoDataRun = project.findProperty("neoDataRun")?.toString() ?: "data"
configureCiRuntimeMods()

dependencies {
    implementation(project(commonProject))
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
    exclude("**/*.bbmodel")
    exclude("src/generated/**/.cache")
}

base {
    archivesName = "$modId-$minecraftVersion-neoforge"
}

java.toolchain {
    languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
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

neoForge {
    version = neoVersion
    validateAccessTransformers = true

    if (parchmentMappingsVersion != null && parchmentMinecraftVersion != null) {
        parchment {
            mappingsVersion = parchmentMappingsVersion
            minecraftVersion = parchmentMinecraftVersion
        }
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        if (minecraftVersion.supportsGameTestServer()) {
            create("gameTestServer") {
                type = "gameTestServer"
                systemProperty("neoforge.enabledGameTestNamespaces", modId)
            }
        }

        create("data") {
            if (neoDataRun == "clientData") clientData() else data()
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
            logLevel = org.slf4j.event.Level.DEBUG
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

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = buildMap {
        put("minecraft_version", minecraftVersion)
        put("minecraft_version_range", minecraftVersionRange)
        put("neo_version", neoVersion)
        if (loaderVersionRange != null) put("loader_version_range", loaderVersionRange)
        put("mod_id", modId)
        put("mod_name", modName)
        put("mod_license", modLicense)
        put("mod_version", modVersion)
        put("mod_authors", modAuthors)
        put("mod_description", modDescription)
        put("mod_credits", modCredits)
        put("mod_display_url", modDisplayUrl)
        put("mod_issue_tracker_url", modIssueTrackerUrl)
    }
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.named("sourcesJar") {
    dependsOn(generateModMetadata)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(commonProject).sourceSets.main.get().output)
    from(project(sharedCommonProject).sourceSets.main.get().output)
}
