import groovy.json.JsonOutput
import net.meatwo310.mdk.build.supportsGameTestServer
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("net.fabricmc.fabric-loom") apply false
    id("net.fabricmc.fabric-loom-remap") apply false
    id("net.minecraftforge.gradle") apply false
    id("net.neoforged.moddev") apply false
    id("net.neoforged.moddev.legacyforge") apply false
}

tasks.named<Wrapper>("wrapper").configure {
    distributionType = Wrapper.DistributionType.BIN
}

tasks.register("writeCiBuildMatrix") {
    val outputFile = layout.buildDirectory.file("ci/build-matrix.json")
    val ciBuildProjects = (gradle.extensions.extraProperties["ciBuildProjectNames"] as List<*>)
        .map { it.toString() }
    val supportedLoaders = setOf("fabric", "forge", "neo")

    val duplicates = ciBuildProjects
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    if (duplicates.isNotEmpty()) {
        throw GradleException("Duplicate CI build projects: ${duplicates.joinToString()}")
    }

    val ciBuildMatrix = ciBuildProjects.map { projectName ->
        val targetProject = project(":$projectName")
        val minecraftVersion = targetProject.property("minecraftVersion").toString()
        val loader = projectName.substringAfterLast("-")
        val javaVersion = targetProject.findProperty("javaVersion")?.toString() ?: "17"
        val fabricApiVersion = targetProject.findProperty("fabricApiVersion")
            ?.toString()
            ?.substringBefore("+")
            ?: "none"
        val runMcRuntimeTest = targetProject.findProperty("ciMcRuntimeTest")
            ?.toString()
            ?.toBooleanStrictOrNull()
            ?: true
        val requiresCommon = targetProject.findProperty("ciRequiresCommon")
            ?.toString()
            ?.toBooleanStrictOrNull()
            ?: true
        val modloader = if (loader == "neo") "neoforge" else loader
        val mcRuntimeTest = when (loader) {
            "forge" -> "lexforge"
            "neo" -> "neoforge"
            else -> loader
        }
        val supportsGameTestServer = minecraftVersion.supportsGameTestServer()
        val commonProjectName = "$minecraftVersion-common"

        if (loader !in supportedLoaders) {
            throw GradleException("Unsupported loader '$loader' in CI build project '$projectName'")
        }
        if (minecraftVersion.isBlank()) {
            throw GradleException("Project '$projectName' must define minecraftVersion")
        }
        if (requiresCommon && !rootProject.childProjects.containsKey(commonProjectName)) {
            throw GradleException("Project '$projectName' references missing common project '$commonProjectName'")
        }
        if (!javaVersion.matches(Regex("\\d+"))) {
            throw GradleException("Project '$projectName' has invalid javaVersion '$javaVersion'")
        }
        mapOf(
            "subproject" to projectName,
            "project_dir" to targetProject.projectDir.relativeTo(rootProject.projectDir).invariantSeparatorsPath,
            "loader" to loader,
            "minecraft" to minecraftVersion,
            "java" to javaVersion,
            "fabric_api" to fabricApiVersion,
            "supports_game_test_server" to supportsGameTestServer,
            "run_game_test_server" to (supportsGameTestServer && loader in setOf("forge", "neo")),
            "run_server" to !supportsGameTestServer,
            "run_mc_runtime_test" to runMcRuntimeTest,
            "modloader" to modloader,
            "mc_runtime_test" to mcRuntimeTest,
            "artifact_regex" to ".*$loader.*",
        )
    }

    outputs.file(outputFile)
    inputs.property("ciBuildMatrix", ciBuildMatrix)

    doLast {
        val json = JsonOutput.toJson(ciBuildMatrix)
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(json)
        }
    }
}

with(System.getProperties()) {
    val version = get("java.version")
    val vmVersion = get("java.vm.version")
    val vendor = get("java.vendor")
    val arch = get("os.arch")
    println("Configuring with Java: $version, JVM: $vmVersion ($vendor), Arch: $arch")
}

val modVersion = providers.fileContents(layout.projectDirectory.file("version.txt"))
    .asText
    .map { it.trim() }
    .get()

subprojects {
    val modGroupId = project.property("modGroupId").toString()

    extensions.extraProperties["modVersion"] = modVersion
    version = modVersion
    group = modGroupId

    tasks.withType<AbstractArchiveTask>().configureEach {
        archiveVersion.set("v$modVersion")
    }

    repositories {
        maven {
            name = "Minecraft Libraries"
            url = uri("https://libraries.minecraft.net")
        }

        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
        }

        mavenCentral()

//        flatDir {
//            dir("libs")
//        }

        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://cursemaven.com")
                }
            }
            filter {
                includeGroup("curse.maven")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Modrinth"
                    url = uri("https://api.modrinth.com/maven")
                }
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Fuzs Mod Resources"
                    url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
                }
            }
            filter {
                includeGroupByRegex("fuzs\\..+")
                includeModule("net.minecraftforge", "forgeconfigapiport-fabric")
            }
        }

        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }

//        maven {
//            name = "ModMaven"
//            url = uri("https://modmaven.dev/")
//        }
    }

    tasks.withType<JavaCompile>().configureEach {
        doFirst {
            with(javaCompiler.get().metadata) {
                println("Compiling with Java: $javaRuntimeVersion, JVM: $jvmVersion ($vendor)")
            }
        }
        options.encoding = "UTF-8"
    }

    plugins.withType<JavaLibraryPlugin> {
        dependencies {
            add("compileOnlyApi", libs.jspecify)
        }
    }

    afterEvaluate {
        tasks.withType<JavaExec>().configureEach {
            standardInput = System.`in`
        }
    }

    plugins.withId("idea") {
        configure<IdeaModel> {
            module {
                isDownloadSources = true
                isDownloadJavadoc = true
            }
        }
    }
}
