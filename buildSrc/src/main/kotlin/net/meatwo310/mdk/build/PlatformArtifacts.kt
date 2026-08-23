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
    abstract val curseForgeDependencies: SetProperty<PublishedDependency>
    abstract val modrinthDependencies: SetProperty<PublishedDependency>

    fun publishingDependencies(configure: PublishingDependenciesSpec.() -> Unit) {
        PublishingDependenciesSpec(curseForgeDependencies, modrinthDependencies).configure()
    }
}

enum class PublishedDependencyType {
    REQUIRED,
    OPTIONAL,
    INCOMPATIBLE,
    EMBEDDED,
}

data class PublishedDependency(
    val slug: String,
    val type: PublishedDependencyType,
)

class PublishingDependencySlugs {
    var curseForge: String? = null
    var modrinth: String? = null
}

class PublishingDependenciesSpec internal constructor(
    private val curseForgeDependencies: SetProperty<PublishedDependency>,
    private val modrinthDependencies: SetProperty<PublishedDependency>,
) {
    fun required(slug: String) = add(slug, slug, PublishedDependencyType.REQUIRED)
    fun required(configure: PublishingDependencySlugs.() -> Unit) =
        add(configure, PublishedDependencyType.REQUIRED)

    fun optional(slug: String) = add(slug, slug, PublishedDependencyType.OPTIONAL)
    fun optional(configure: PublishingDependencySlugs.() -> Unit) =
        add(configure, PublishedDependencyType.OPTIONAL)

    fun incompatible(slug: String) = add(slug, slug, PublishedDependencyType.INCOMPATIBLE)
    fun incompatible(configure: PublishingDependencySlugs.() -> Unit) =
        add(configure, PublishedDependencyType.INCOMPATIBLE)

    fun embedded(slug: String) = add(slug, slug, PublishedDependencyType.EMBEDDED)
    fun embedded(configure: PublishingDependencySlugs.() -> Unit) =
        add(configure, PublishedDependencyType.EMBEDDED)

    private fun add(
        configure: PublishingDependencySlugs.() -> Unit,
        type: PublishedDependencyType,
    ) {
        val slugs = PublishingDependencySlugs().apply(configure)
        if (slugs.curseForge == null && slugs.modrinth == null) {
            throw GradleException("At least one publishing slug must be specified")
        }
        add(slugs.curseForge, slugs.modrinth, type)
    }

    private fun add(
        curseForgeSlug: String?,
        modrinthSlug: String?,
        type: PublishedDependencyType,
    ) {
        curseForgeSlug?.let { slug ->
            curseForgeDependencies.add(PublishedDependency(slug.validatedDependencySlug(), type))
        }
        modrinthSlug?.let { slug ->
            modrinthDependencies.add(PublishedDependency(slug.validatedDependencySlug(), type))
        }
    }
}

data class PlatformArtifacts(
    val minecraftVersion: String,
    val loader: String,
    val javaVersion: Int,
    val archiveBaseName: String,
    val mainArtifactName: String,
    val sourcesArtifactName: String?,
    val curseForgeDependencies: Set<PublishedDependency>,
    val modrinthDependencies: Set<PublishedDependency>,
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
        curseForgeDependencies.convention(emptySet())
        modrinthDependencies.convention(emptySet())
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
        curseForgeDependencies = metadata.curseForgeDependencies.get().sortedDependencies(),
        modrinthDependencies = metadata.modrinthDependencies.get().sortedDependencies(),
    )
}

/**
 * Configures this project's existing platform artifact metadata.
 */
fun Project.platformArtifacts(configure: PlatformArtifactsExtension.() -> Unit) {
    val metadata = extensions.findByType(PlatformArtifactsExtension::class.java)
        ?: throw GradleException("Project '$name' must configure platformArtifacts before configuring its metadata")
    metadata.configure()
}

private fun Set<PublishedDependency>.sortedDependencies(): Set<PublishedDependency> =
    sortedWith(compareBy({ it.type.name }, { it.slug })).toCollection(linkedSetOf())

private fun String.validatedDependencySlug(): String =
    takeIf(String::isNotBlank) ?: throw GradleException("Publishing dependency slugs must not be blank")

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
