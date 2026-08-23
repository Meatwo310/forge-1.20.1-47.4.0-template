package net.meatwo310.mdk.build

import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import me.modmuss50.mpp.platforms.modrinth.ModrinthOptions
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configures repository-specific metadata using Mod Publish Plugin's native platform options.
 *
 * Execution inputs such as the release tag, selected projects, destination, and dry-run mode intentionally remain
 * Gradle properties so that each workflow invocation can choose them independently.
 */
open class ModPublishingExtension(
    private val curseForgeOptions: CurseforgeOptions,
    private val modrinthOptions: ModrinthOptions,
) {
    /** Configures native CurseForge options shared by every platform upload. */
    fun curseForge(action: Action<CurseforgeOptions>) = action.execute(curseForgeOptions)

    /** Configures native Modrinth options shared by every platform upload. */
    fun modrinth(action: Action<ModrinthOptions>) = action.execute(modrinthOptions)
}

/** CurseForge settings that a single platform publication may override. */
abstract class CurseForgePublishingOverrides {
    abstract val projectId: Property<String>
    abstract val projectSlug: Property<String>
    abstract val client: Property<Boolean>
    abstract val server: Property<Boolean>
}

/** Modrinth settings that a single platform publication may override. */
abstract class ModrinthPublishingOverrides {
    abstract val projectId: Property<String>
    abstract val environment: Property<ModrinthEnvironment>
    abstract val featured: Property<Boolean>
}

/**
 * Overrides publishing metadata for one platform project.
 *
 * Artifact identity such as the Minecraft version, mod loader, Java version, and jar files intentionally remains in
 * [PlatformArtifactsExtension] so that publishing metadata cannot disagree with the artifact being uploaded.
 */
open class PlatformPublishingExtension @Inject constructor(objects: ObjectFactory) {
    val curseForge: CurseForgePublishingOverrides = objects.newInstance(CurseForgePublishingOverrides::class.java)
    val modrinth: ModrinthPublishingOverrides = objects.newInstance(ModrinthPublishingOverrides::class.java)

    val displayName: Property<String> = objects.property(String::class.java)
    val releaseType: Property<ReleaseType> = objects.property(ReleaseType::class.java)

    fun curseForge(action: Action<CurseForgePublishingOverrides>) = action.execute(curseForge)

    fun modrinth(action: Action<ModrinthPublishingOverrides>) = action.execute(modrinth)
}

data class CurseForgePublishingOverrideValues(
    val projectId: String?,
    val projectSlug: String?,
    val client: Boolean?,
    val server: Boolean?,
)

data class ModrinthPublishingOverrideValues(
    val projectId: String?,
    val environment: ModrinthEnvironment?,
    val featured: Boolean?,
)

data class PlatformPublishingOverrides(
    val displayName: String?,
    val releaseType: ReleaseType?,
    val curseForge: CurseForgePublishingOverrideValues,
    val modrinth: ModrinthPublishingOverrideValues,
)

/** Configures publishing overrides for this platform project. */
fun Project.platformPublishing(action: Action<PlatformPublishingExtension>) =
    action.execute(configurePlatformPublishing())

internal fun Project.configurePlatformPublishing(): PlatformPublishingExtension =
    extensions.findByType(PlatformPublishingExtension::class.java)
        ?: extensions.create("platformPublishing", PlatformPublishingExtension::class.java)

internal fun Project.platformPublishingOverrides(): PlatformPublishingOverrides {
    val publishing = extensions.findByType(PlatformPublishingExtension::class.java)
        ?: throw GradleException("Project '$name' must configure platformArtifacts before platformPublishing")
    val displayName = publishing.displayName.orNull?.validatedPublishingText("displayName")
    val curseForgeProjectId = publishing.curseForge.projectId.orNull?.validatedPublishingText("CurseForge projectId")
    val curseForgeProjectSlug =
        publishing.curseForge.projectSlug.orNull?.validatedPublishingText("CurseForge projectSlug")
    val modrinthProjectId = publishing.modrinth.projectId.orNull?.validatedPublishingText("Modrinth projectId")

    return PlatformPublishingOverrides(
        displayName = displayName,
        releaseType = publishing.releaseType.orNull,
        curseForge = CurseForgePublishingOverrideValues(
            projectId = curseForgeProjectId,
            projectSlug = curseForgeProjectSlug,
            client = publishing.curseForge.client.orNull,
            server = publishing.curseForge.server.orNull,
        ),
        modrinth = ModrinthPublishingOverrideValues(
            projectId = modrinthProjectId,
            environment = publishing.modrinth.environment.orNull,
            featured = publishing.modrinth.featured.orNull,
        ),
    )
}

private fun String.validatedPublishingText(name: String): String =
    takeIf(String::isNotBlank) ?: throw GradleException("Publishing $name must not be blank")
