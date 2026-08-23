plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

dependencies {
    implementation(libs.mod.publish.plugin)
    implementation(libs.neoforged.moddev.gradle)
    implementation(libs.forgegradle.plugin)
    implementation(libs.fabric.loom)
}
