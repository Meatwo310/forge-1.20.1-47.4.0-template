package net.meatwo310.mdk.build

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishOptions
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import me.modmuss50.mpp.platforms.modrinth.ModrinthOptions
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Restricts a publishing DSL to metadata that callers may configure safely.
 *
 * Each exposed property is the corresponding property from Mod Publishing Plugin's native options. The native options
 * themselves remain private so callers cannot configure tokens, files, API endpoints, artifact identity, or other
 * values owned by the publishing convention.
 */
open class PublishingOptionsFacade(
    private val publishOptions: PublishOptions,
    private val curseForgeOptions: CurseforgeOptions,
    private val modrinthOptions: ModrinthOptions,
) {
    val displayName: Property<String>
        get() = publishOptions.displayName
    val releaseType: Property<ReleaseType>
        get() = publishOptions.type

    val curseForge = CurseForgePublishingFacade(curseForgeOptions)
    val modrinth = ModrinthPublishingFacade(modrinthOptions)

    fun curseForge(action: Action<CurseForgePublishingFacade>) = action.execute(curseForge)

    fun modrinth(action: Action<ModrinthPublishingFacade>) = action.execute(modrinth)

    internal fun inheritFrom(defaults: PublishingOptionsFacade) {
        displayName.convention(defaults.displayName)
        releaseType.convention(defaults.releaseType)
        curseForge.inheritFrom(defaults.curseForge)
        modrinth.inheritFrom(defaults.modrinth)
    }

    internal fun applyTo(options: CurseforgeOptions, generatedDisplayName: Provider<String>) {
        options.displayName.set(displayName.orElse(generatedDisplayName))
        if (releaseType.isPresent) options.type.set(releaseType)
        curseForge.applyTo(options)
    }

    internal fun applyTo(options: ModrinthOptions, generatedDisplayName: Provider<String>) {
        options.displayName.set(displayName.orElse(generatedDisplayName))
        if (releaseType.isPresent) options.type.set(releaseType)
        modrinth.applyTo(options)
    }
}

open class CurseForgePublishingFacade internal constructor(
    private val options: CurseforgeOptions,
) {
    val projectId: Property<String>
        get() = options.projectId
    val projectSlug: Property<String>
        get() = options.projectSlug
    val client: Property<Boolean>
        get() = options.client
    val server: Property<Boolean>
        get() = options.server

    internal fun inheritFrom(defaults: CurseForgePublishingFacade) {
        projectId.convention(defaults.projectId)
        projectSlug.convention(defaults.projectSlug)
        client.convention(defaults.client)
        server.convention(defaults.server)
    }

    internal fun applyTo(target: CurseforgeOptions) {
        if (projectId.isPresent) target.projectId.set(projectId)
        if (projectSlug.isPresent) target.projectSlug.set(projectSlug)
        if (client.isPresent) target.client.set(client)
        if (server.isPresent) target.server.set(server)
    }
}

open class ModrinthPublishingFacade internal constructor(
    private val options: ModrinthOptions,
) {
    val CLIENT_ONLY: ModrinthEnvironment
        get() = options.CLIENT_ONLY
    val SERVER_ONLY: ModrinthEnvironment
        get() = options.SERVER_ONLY
    val DEDICATED_SERVER_ONLY: ModrinthEnvironment
        get() = options.DEDICATED_SERVER_ONLY
    val CLIENT_AND_SERVER: ModrinthEnvironment
        get() = options.CLIENT_AND_SERVER
    val SERVER_ONLY_CLIENT_OPTIONAL: ModrinthEnvironment
        get() = options.SERVER_ONLY_CLIENT_OPTIONAL
    val CLIENT_ONLY_SERVER_OPTIONAL: ModrinthEnvironment
        get() = options.CLIENT_ONLY_SERVER_OPTIONAL
    val CLIENT_OR_SERVER_PREFERS_BOTH: ModrinthEnvironment
        get() = options.CLIENT_OR_SERVER_PREFERS_BOTH
    val CLIENT_OR_SERVER: ModrinthEnvironment
        get() = options.CLIENT_OR_SERVER
    val SINGLEPLAYER_ONLY: ModrinthEnvironment
        get() = options.SINGLEPLAYER_ONLY

    val projectId: Property<String>
        get() = options.projectId
    val environment: Property<ModrinthEnvironment>
        get() = options.environment
    val featured: Property<Boolean>
        get() = options.featured

    internal fun inheritFrom(defaults: ModrinthPublishingFacade) {
        projectId.convention(defaults.projectId)
        environment.convention(defaults.environment)
        featured.convention(defaults.featured)
    }

    internal fun applyTo(target: ModrinthOptions) {
        if (projectId.isPresent) target.projectId.set(projectId)
        if (environment.isPresent) target.environment.set(environment)
        if (featured.isPresent) target.featured.set(featured)
    }
}

/** Configures publishing overrides for this platform project. */
fun Project.platformPublishing(action: Action<PublishingOptionsFacade>) =
    action.execute(configurePlatformPublishing())

internal fun Project.configurePlatformPublishing(): PublishingOptionsFacade {
    extensions.findByName("platformPublishing")?.let { return it as PublishingOptionsFacade }

    val modPublish = rootProject.extensions.findByType(ModPublishExtension::class.java)
        ?: throw GradleException("The root project must apply mod-publish-conventions")
    val defaults = rootProject.extensions.findByName("modPublishing") as? PublishingOptionsFacade
        ?: throw GradleException("The root project must configure modPublishing")
    val publishing = extensions.create(
        "platformPublishing",
        PublishingOptionsFacade::class.java,
        modPublish.publishOptions {}.get(),
        modPublish.curseforgeOptions {}.get(),
        modPublish.modrinthOptions {}.get(),
    )
    publishing.inheritFrom(defaults)
    return publishing
}

internal fun Project.platformPublishingFacade(): PublishingOptionsFacade =
    extensions.findByName("platformPublishing") as? PublishingOptionsFacade
        ?: throw GradleException("Project '$name' must configure platformArtifacts before platformPublishing")
