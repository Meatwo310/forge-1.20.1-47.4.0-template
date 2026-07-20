pluginManagement {
    repositories {
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            name = "GTNH Maven"
            mavenContent {
                includeGroup("com.gtnewhorizons")
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
            }
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


/// Common ///
include("common")

fun includeMc(minecraftVersion: String, kind: String) {
    val projectName = "$minecraftVersion-$kind"
    include(projectName)
    project(":$projectName").projectDir = file("$minecraftVersion/$kind")
}

/// 1.7.10 ///
includeMc("1.7.10", "forge")

/// 1.18.2 ///
includeMc("1.18.2", "common")
includeMc("1.18.2", "forge")
includeMc("1.18.2", "fabric")

/// 1.19.2 ///
includeMc("1.19.2", "common")
includeMc("1.19.2", "forge")
includeMc("1.19.2", "fabric")

/// 1.20.1 ///
includeMc("1.20.1", "common")
includeMc("1.20.1", "forge")
includeMc("1.20.1", "fabric")

/// 1.21.1 ///
includeMc("1.21.1", "common")
includeMc("1.21.1", "forge")
includeMc("1.21.1", "neo")
includeMc("1.21.1", "fabric")

/// 1.21.8 ///
includeMc("1.21.8", "common")
includeMc("1.21.8", "forge")
includeMc("1.21.8", "fabric")

/// 1.21.11 ///
includeMc("1.21.11", "common")
includeMc("1.21.11", "forge")
includeMc("1.21.11", "fabric")

/// 26.1 ///
includeMc("26.1", "common")
includeMc("26.1", "fabric")
includeMc("26.1", "neo")

/// 26.1.2 ///
includeMc("26.1.2", "common")
includeMc("26.1.2", "fabric")
includeMc("26.1.2", "neo")

/// 26.2 ///
includeMc("26.2", "common")
includeMc("26.2", "fabric")
includeMc("26.2", "neo")

val ciBuildProjectNames = rootProject.children
    .map { it.name }
    .filterNot { it == "common" || it.endsWith("-common") }

gradle.extensions.extraProperties["ciBuildProjectNames"] = ciBuildProjectNames
