import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import net.meatwo310.mdk.build.DownloadGitHubRelease
import net.meatwo310.mdk.build.ModPublishingExtension
import net.meatwo310.mdk.build.platformArtifacts

plugins {
    id("me.modmuss50.mod-publish-plugin")
}

val modPublish = extensions.getByType<ModPublishExtension>()
val curseForgeOptions = modPublish.curseforgeOptions {}.get()
val modrinthOptions = modPublish.modrinthOptions {}.get()
extensions.create(
    "modPublishing",
    ModPublishingExtension::class.java,
    curseForgeOptions,
    modrinthOptions,
)

data class PublishTarget(
    val projectName: String,
    val minecraftVersion: String,
    val modLoader: String,
    val javaVersion: Int,
    val mainArtifactName: String,
    val sourcesArtifactName: String?,
    val curseForgeRequiredDependencies: Set<String>,
    val modrinthRequiredDependencies: Set<String>,
)

fun String.toPublishTaskSuffix(): String =
    split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .joinToString("") { part -> part.replaceFirstChar { it.titlecase() } }

val modVersion = providers.fileContents(layout.projectDirectory.file("version.txt"))
    .asText
    .map { it.trim() }
    .get()
val modName = providers.gradleProperty("modName").get()
val publishTag = providers.gradleProperty("publishTag").orElse("v$modVersion")
val publishDestination = providers.gradleProperty("publishDestination")
    .orElse("both")
    .map { it.lowercase() }
val publishDryRun = providers.gradleProperty("publishDryRun")
    .map { it.toBooleanStrict() }
    .orElse(true)
val publishReleaseType = providers.gradleProperty("publishReleaseType")
    .orElse("stable")
    .map { ReleaseType.valueOf(it.uppercase()) }
val publishRepository = providers.gradleProperty("publishGitHubRepository")
val githubToken = providers.environmentVariable("GITHUB_TOKEN")
    .orElse(providers.environmentVariable("GH_TOKEN"))
val githubApiUrl = providers.environmentVariable("GITHUB_API_URL")
    .orElse("https://api.github.com")
val publishInputDirectory = layout.buildDirectory.dir("publish/input")
val publishChangelogFile = layout.buildDirectory.file("publish/RELEASE_NOTES.md")

val downloadPublishRelease = tasks.register<DownloadGitHubRelease>("downloadPublishRelease") {
    group = "publishing"
    description = "Downloads jars and release notes from the selected GitHub Release."
    repository.set(publishRepository)
    tag.set(publishTag)
    artifactsDirectory.set(publishInputDirectory)
    changelogFile.set(publishChangelogFile)
    apiUrl.set(githubApiUrl)
    accessToken.set(githubToken)
}

tasks.withType<PublishModTask>().configureEach {
    dependsOn(downloadPublishRelease)
}

gradle.projectsEvaluated {
    val allPublishTargets = (gradle.extensions.extraProperties["ciBuildProjectNames"] as List<*>)
        .map { it.toString() }
        .map { projectName ->
            val artifacts = project(":$projectName").platformArtifacts()
            PublishTarget(
                projectName = projectName,
                minecraftVersion = artifacts.minecraftVersion,
                modLoader = artifacts.modLoader,
                javaVersion = artifacts.javaVersion,
                mainArtifactName = artifacts.mainArtifactName,
                sourcesArtifactName = artifacts.sourcesArtifactName,
                curseForgeRequiredDependencies = artifacts.curseForgeRequiredDependencies,
                modrinthRequiredDependencies = artifacts.modrinthRequiredDependencies,
            )
        }
    val requestedPublishProjects = providers.gradleProperty("publishProjects")
        .orNull
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        .orEmpty()
    val unknownPublishProjects = requestedPublishProjects - allPublishTargets.map { it.projectName }.toSet()
    if (unknownPublishProjects.isNotEmpty()) {
        throw GradleException("Unknown publishProjects: ${unknownPublishProjects.sorted().joinToString()}")
    }
    val publishTargets = if (requestedPublishProjects.isEmpty()) {
        allPublishTargets
    } else {
        allPublishTargets.filter { it.projectName in requestedPublishProjects }
    }
    val requiredPublishAssets = publishTargets.flatMap { target ->
        listOfNotNull(target.mainArtifactName, target.sourcesArtifactName)
    }
    downloadPublishRelease.configure {
        requiredAssetNames.set(requiredPublishAssets)
    }

    val selectedPublishDestination = publishDestination.get()
    if (selectedPublishDestination !in setOf("both", "curseforge", "modrinth")) {
        throw GradleException(
            "Unsupported publishDestination '$selectedPublishDestination'; use both, curseforge, or modrinth.",
        )
    }

    publishMods {
        dryRun.set(publishDryRun)
        changelog.set(providers.fileContents(publishChangelogFile).asText)
        type.set(publishReleaseType)

        for (target in publishTargets) {
            val taskSuffix = target.projectName.toPublishTaskSuffix()
            val mainFile = publishInputDirectory.map { it.file(target.mainArtifactName) }
            val sourcesFile = target.sourcesArtifactName?.let { artifactName ->
                publishInputDirectory.map { it.file(artifactName) }
            }
            val releaseVersion = "${target.minecraftVersion}-${target.modLoader}-v$modVersion"
            val displayName = "$modName $releaseVersion"

            if (selectedPublishDestination in setOf("both", "curseforge")) {
                curseforge("curseforge$taskSuffix") {
                    from(curseForgeOptions)
                    accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
                    file.set(mainFile)
                    version.set(releaseVersion)
                    this.displayName.set(displayName)
                    modLoaders.add(target.modLoader)
                    minecraftVersions.add(target.minecraftVersion)
                    javaVersions.add(JavaVersion.toVersion(target.javaVersion))
                    if (sourcesFile != null) {
                        additionalFile(sourcesFile) {
                            name.set("Sources")
                        }
                    }
                    requires(*target.curseForgeRequiredDependencies.toTypedArray())
                }
            }

            if (selectedPublishDestination in setOf("both", "modrinth")) {
                modrinth("modrinth$taskSuffix") {
                    from(modrinthOptions)
                    accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
                    file.set(mainFile)
                    version.set(releaseVersion)
                    this.displayName.set(displayName)
                    modLoaders.add(target.modLoader)
                    minecraftVersions.add(target.minecraftVersion)
                    if (sourcesFile != null) {
                        additionalFile(sourcesFile) {
                            type.set(SOURCES_JAR)
                        }
                    }
                    requires(*target.modrinthRequiredDependencies.toTypedArray())
                }
            }
        }
    }
}
