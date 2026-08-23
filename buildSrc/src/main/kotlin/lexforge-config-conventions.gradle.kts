import net.meatwo310.mdk.build.*

val minecraftVersion = project.property("minecraftVersion").toString()
val modId = project.property("modId").toString()
val forgeConfigApiPortVersion = project(":$minecraftVersion-common")
    .property("forgeConfigApiPortVersion")
    .toString()

plugins.withId("java-library") {
    val config = configureConfigSourceSet()
    val sharedConfig = sharedConfigSourceSets(minecraftVersion, "lexforge-config-conventions")

    config.addConfigClasspath(sharedConfig)
    includeConfigOutput(sharedConfig, config)
    val main = extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME).get()
    for (configSourceSet in sharedConfig.including(config)) {
        main.compileClasspath += configSourceSet.output
    }

    // ForgeGradle 7 treats output directories as separate mod roots in dev runs.
    // Copy config classes beside the @Mod class and generated mods.toml so the
    // complete mod is available from a single root without changing jar layout.
    val stageConfigForRuns = tasks.register<Copy>("stageLexForgeConfigForRuns") {
        dependsOn(tasks.named("classes"))
        from(sharedConfig.including(config).map { it.output })
        into(layout.buildDirectory.dir("classes/java/main"))
    }
    tasks.matching { it.name.startsWith("run") }.configureEach {
        dependsOn(stageConfigForRuns)
    }
}

plugins.withId("net.minecraftforge.gradle") {
    publishDependency("forge-config-api-port")
    val dependency = "${versionCatalog.module(VersionCatalogLibrary.ForgeConfigApiPortForge)}:$forgeConfigApiPortVersion"
    dependencies.add("implementation", dependency) {
        isTransitive = false
    }
    dependencies.add("ciRuntimeMods", dependency)

    tasks.named<GenerateLexForgeModMetadata>("generateModMetadata") {
        additionalModsToml.set(
            """

            [[dependencies."$modId"]]
            modId="forgeconfigapiport"
            mandatory=true
            versionRange="[$forgeConfigApiPortVersion,)"
            ordering="NONE"
            side="BOTH"
            """.trimIndent() + "\n"
        )
    }
}
