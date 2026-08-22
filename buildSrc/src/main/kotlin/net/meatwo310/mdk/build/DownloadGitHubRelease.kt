package net.meatwo310.mdk.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@DisableCachingByDefault(because = "GitHub Releases are remote mutable inputs")
abstract class DownloadGitHubRelease @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:Input
    abstract val repository: Property<String>

    @get:Input
    abstract val tag: Property<String>

    @get:Input
    abstract val requiredAssetNames: ListProperty<String>

    @get:OutputDirectory
    abstract val artifactsDirectory: DirectoryProperty

    @get:OutputFile
    abstract val changelogFile: RegularFileProperty

    @TaskAction
    fun download() {
        val outputDirectory = artifactsDirectory.get().asFile
        fileSystemOperations.delete {
            delete(outputDirectory)
        }
        outputDirectory.mkdirs()

        execOperations.exec {
            commandLine(
                "gh",
                "release",
                "download",
                tag.get(),
                "--repo",
                repository.get(),
                "--dir",
                outputDirectory.absolutePath,
                "--pattern",
                "*.jar",
            )
        }

        val missingAssets = requiredAssetNames.get().filterNot {
            outputDirectory.resolve(it).isFile
        }
        if (missingAssets.isNotEmpty()) {
            throw GradleException(
                "GitHub Release ${tag.get()} is missing required assets: ${missingAssets.joinToString()}",
            )
        }

        val changelog = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(
                "gh",
                "release",
                "view",
                tag.get(),
                "--repo",
                repository.get(),
                "--json",
                "body",
                "--jq",
                ".body",
            )
            standardOutput = changelog
        }

        changelogFile.get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(changelog.toByteArray())
        }
    }
}
