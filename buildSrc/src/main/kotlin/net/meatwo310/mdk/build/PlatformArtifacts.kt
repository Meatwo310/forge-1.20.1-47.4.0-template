package net.meatwo310.mdk.build

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.bundling.AbstractArchiveTask

abstract class PlatformArtifactsExtension {
    abstract val minecraftVersion: Property<String>
    abstract val loader: Property<String>
    abstract val javaVersion: Property<Int>
    abstract val mainJarTaskName: Property<String>
    abstract val sourcesJarTaskName: Property<String>
    /** CurseForge slugs for dependencies required by this platform artifact. */
    abstract val curseForgeRequiredDependencies: SetProperty<String>

    /** Modrinth slugs for dependencies required by this platform artifact. */
    abstract val modrinthRequiredDependencies: SetProperty<String>
}

data class PlatformArtifacts(
    val minecraftVersion: String,
    val loader: String,
    val javaVersion: Int,
    val archiveBaseName: String,
    val mainArtifactName: String,
    val sourcesArtifactName: String?,
    val curseForgeRequiredDependencies: Set<String>,
    val modrinthRequiredDependencies: Set<String>,
) {
    val modLoader: String = if (loader == "neo") "neoforge" else loader
    val releaseArtifactNames: List<String> = listOfNotNull(mainArtifactName, sourcesArtifactName)
}

fun Project.configurePlatformArtifacts(
    loader: String,
    javaVersion: Int,
    mainJarTaskName: String = "jar",
    sourcesJarTaskName: String? = null,
): PlatformArtifactsExtension {
    if (loader !in setOf("fabric", "forge", "neo")) {
        throw GradleException("Unsupported platform loader '$loader' in project '$name'")
    }
    val metadata = extensions.create("platformArtifacts", PlatformArtifactsExtension::class.java).apply {
        val configuredMinecraftVersion = project.property("minecraftVersion").toString()
        minecraftVersion.set(configuredMinecraftVersion)
        this.loader.set(loader)
        this.javaVersion.set(javaVersion)
        this.mainJarTaskName.set(mainJarTaskName)
        if (sourcesJarTaskName != null) {
            this.sourcesJarTaskName.set(sourcesJarTaskName)
        }
        curseForgeRequiredDependencies.convention(emptySet())
        modrinthRequiredDependencies.convention(emptySet())
    }

    return metadata
}

fun Project.platformArtifacts(): PlatformArtifacts {
    val metadata = extensions.findByType(PlatformArtifactsExtension::class.java)
        ?: throw GradleException("Project '$name' must configure platformArtifacts")
    val minecraftVersion = metadata.minecraftVersion.orNull.orEmpty()
    val loader = metadata.loader.orNull.orEmpty()
    val javaVersion = metadata.javaVersion.orNull
    val archiveBaseName = extensions.findByType(BasePluginExtension::class.java)
        ?.archivesName
        ?.orNull
        .orEmpty()

    if (minecraftVersion.isBlank()) {
        throw GradleException("Project '$name' must define a non-blank minecraftVersion")
    }
    if (loader.isBlank()) {
        throw GradleException("Project '$name' must define a non-blank platform loader")
    }
    if (javaVersion == null || javaVersion < 8) {
        throw GradleException("Project '$name' must define a valid platform Java version")
    }
    if (archiveBaseName.isBlank()) {
        throw GradleException("Project '$name' must configure a non-blank base archivesName")
    }

    val mainArtifactName = artifactFileName(metadata.mainJarTaskName.get(), "main")
    val sourcesArtifactName = metadata.sourcesJarTaskName.orNull?.let { taskName ->
        artifactFileName(taskName, "sources")
    }

    return PlatformArtifacts(
        minecraftVersion = minecraftVersion,
        loader = loader,
        javaVersion = javaVersion,
        archiveBaseName = archiveBaseName,
        mainArtifactName = mainArtifactName,
        sourcesArtifactName = sourcesArtifactName,
        curseForgeRequiredDependencies = metadata.curseForgeRequiredDependencies.get().toSortedSet(),
        modrinthRequiredDependencies = metadata.modrinthRequiredDependencies.get().toSortedSet(),
    )
}

/**
 * Adds a required dependency to this platform artifact's CurseForge and Modrinth publications.
 *
 * The two sites usually use different slugs for the same project. When they match, [modrinthSlug] may be omitted.
 */
fun Project.requirePublishedDependency(
    curseForgeSlug: String,
    modrinthSlug: String = curseForgeSlug,
) {
    val metadata = extensions.findByType(PlatformArtifactsExtension::class.java)
        ?: throw GradleException("Project '$name' must configure platformArtifacts before publishing dependencies")
    metadata.curseForgeRequiredDependencies.add(curseForgeSlug)
    metadata.modrinthRequiredDependencies.add(modrinthSlug)
}

private fun Project.artifactFileName(taskName: String, kind: String): String {
    val artifactTask = tasks.findByName(taskName)
        ?: throw GradleException(
            "Project '$name' declares the $kind artifact task '$taskName', but that task does not exist",
        )
    if (artifactTask is AbstractArchiveTask) {
        return artifactTask.archiveFileName.get()
    }

    val jarOutputs = artifactTask.outputs.files.files.filter { output -> output.extension == "jar" }
    if (jarOutputs.size != 1) {
        throw GradleException(
            "Project '$name' declares '$taskName' as its $kind artifact task, " +
                "but it has ${jarOutputs.size} jar outputs instead of one",
        )
    }
    return jarOutputs.single().name
}
