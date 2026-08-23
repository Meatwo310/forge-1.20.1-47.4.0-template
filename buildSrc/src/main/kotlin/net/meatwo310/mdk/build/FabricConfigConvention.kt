package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

enum class VersionConfigSourceSet {
    REQUIRED,
    OPTIONAL,
}

fun Project.configureFabricConfigConvention(
    minecraftVersion: String,
    forgeConfigApiPortVersion: String,
    conventionName: String,
    dependencyLibrary: VersionCatalogLibrary,
    versionConfigSourceSet: VersionConfigSourceSet,
) {
    plugins.withId("fabric-mod-conventions") {
        platformArtifacts {
            publishingDependencies {
                required("forge-config-api-port")
            }
        }
        extensions.configure<FabricModMetadataExtension>("fabricModMetadata") {
            depend("forgeconfigapiport", ">=$forgeConfigApiPortVersion")
        }
    }

    plugins.withId("java-library") {
        val config = configureConfigSourceSet()
        val configClient = configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME)
        val sourceSets = extensions.getByType<SourceSetContainer>()
        val sharedConfig = when (versionConfigSourceSet) {
            VersionConfigSourceSet.REQUIRED -> sharedConfigSourceSets(minecraftVersion, conventionName)
            VersionConfigSourceSet.OPTIONAL ->
                sharedConfigSourceSetsWithOptionalVersion(minecraftVersion, conventionName)
        }
        val client = sourceSets.findByName("client")

        config.addConfigClasspath(sharedConfig)
        configClient.addConfigClasspath(sharedConfig, config)
        if (client != null) {
            configClient.addCompileRuntimeClasspathFrom(client)
        }

        includeConfigOutput(sharedConfig, config, configClient)

        addConfigOutputTo(SourceSet.MAIN_SOURCE_SET_NAME, sharedConfig, config)
        addConfigOutputTo("client", sharedConfig, config, configClient)

        dependencies.add("runtimeOnly", files(config.output))
        dependencies.add("runtimeOnly", files(configClient.output))
    }

    fun addForgeConfigApiPortDependency(configurationName: String) {
        val dependency = "${versionCatalog.module(dependencyLibrary)}:$forgeConfigApiPortVersion"
        dependencies.add(configurationName, dependency)
        dependencies.add("ciRuntimeMods", dependency)
    }

    plugins.withId("net.fabricmc.fabric-loom") {
        addForgeConfigApiPortDependency("implementation")
    }

    plugins.withId("net.fabricmc.fabric-loom-remap") {
        addForgeConfigApiPortDependency("modImplementation")
    }
}

fun Project.disallowConfigConvention(
    disallowedConventionName: String,
    conventionName: String,
) {
    plugins.withId(disallowedConventionName) {
        error("$conventionName cannot be applied together with $disallowedConventionName.")
    }
}
