package net.meatwo310.mdk.build

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.AbstractArchiveTask

abstract class PlatformArtifactsExtension {
    abstract val minecraftVersion: Property<String>
    abstract val loader: Property<String>
    abstract val artifactLoader: Property<String>
    abstract val archiveBaseName: Property<String>
    abstract val mainJarTaskName: Property<String>
    abstract val sourcesJarTaskName: Property<String>
}

data class PlatformArtifacts(
    val minecraftVersion: String,
    val loader: String,
    val artifactLoader: String,
    val archiveBaseName: String,
    val mainArtifactName: String,
    val sourcesArtifactName: String?,
) {
    val releaseArtifactNames: List<String> = listOfNotNull(mainArtifactName, sourcesArtifactName)
    val mainArtifactRegex: String = mainArtifactName.toRegexLiteral()
}

fun Project.configurePlatformArtifacts(
    loader: String,
    mainJarTaskName: String = "jar",
    sourcesJarTaskName: String? = null,
): PlatformArtifactsExtension {
    val defaultArtifactLoader = when (loader) {
        "fabric", "forge" -> loader
        "neo" -> "neoforge"
        else -> throw GradleException("Unsupported platform loader '$loader' in project '$name'")
    }
    val metadata = extensions.create("platformArtifacts", PlatformArtifactsExtension::class.java).apply {
        val configuredMinecraftVersion = project.property("minecraftVersion").toString()
        val modId = project.property("modId").toString()
        minecraftVersion.set(configuredMinecraftVersion)
        this.loader.set(loader)
        artifactLoader.set(defaultArtifactLoader)
        archiveBaseName.set("$modId-$configuredMinecraftVersion-$defaultArtifactLoader")
        this.mainJarTaskName.set(mainJarTaskName)
        if (sourcesJarTaskName != null) {
            this.sourcesJarTaskName.set(sourcesJarTaskName)
        }
    }

    pluginManager.withPlugin("base") {
        extensions.configure(BasePluginExtension::class.java) {
            archivesName.set(metadata.archiveBaseName)
        }
    }

    return metadata
}

fun Project.platformArtifacts(): PlatformArtifacts {
    val metadata = extensions.findByType(PlatformArtifactsExtension::class.java)
        ?: throw GradleException("Project '$name' must configure platformArtifacts")
    val minecraftVersion = metadata.minecraftVersion.orNull.orEmpty()
    val loader = metadata.loader.orNull.orEmpty()
    val artifactLoader = metadata.artifactLoader.orNull.orEmpty()
    val archiveBaseName = metadata.archiveBaseName.orNull.orEmpty()

    if (minecraftVersion.isBlank()) {
        throw GradleException("Project '$name' must define a non-blank minecraftVersion")
    }
    if (loader.isBlank()) {
        throw GradleException("Project '$name' must define a non-blank platform loader")
    }
    if (artifactLoader.isBlank()) {
        throw GradleException("Project '$name' must define a non-blank artifact loader")
    }
    if (archiveBaseName.isBlank()) {
        throw GradleException("Project '$name' must define a non-blank archive base name")
    }

    val mainArtifactName = artifactFileName(metadata.mainJarTaskName.get(), "main")
    val sourcesArtifactName = metadata.sourcesJarTaskName.orNull?.let { taskName ->
        artifactFileName(taskName, "sources")
    }

    return PlatformArtifacts(
        minecraftVersion = minecraftVersion,
        loader = loader,
        artifactLoader = artifactLoader,
        archiveBaseName = archiveBaseName,
        mainArtifactName = mainArtifactName,
        sourcesArtifactName = sourcesArtifactName,
    )
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

private fun String.toRegexLiteral(): String = buildString {
    append('^')
    for (character in this@toRegexLiteral) {
        if (character in "\\.^$|?*+()[]{}") {
            append('\\')
        }
        append(character)
    }
    append('$')
}
