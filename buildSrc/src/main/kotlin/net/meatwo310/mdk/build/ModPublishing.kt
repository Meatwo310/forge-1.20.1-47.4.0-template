package net.meatwo310.mdk.build

import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.modrinth.ModrinthOptions
import org.gradle.api.Action

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
