package net.meatwo310.mdk.build

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration
import javax.inject.Inject

@DisableCachingByDefault(because = "GitHub Releases are remote mutable inputs")
abstract class DownloadGitHubRelease @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
    }

    @get:Input
    abstract val repository: Property<String>

    @get:Input
    abstract val tag: Property<String>

    @get:Input
    abstract val requiredAssetNames: ListProperty<String>

    @get:Input
    abstract val apiUrl: Property<String>

    @get:Internal
    abstract val accessToken: Property<String>

    @get:OutputDirectory
    abstract val artifactsDirectory: DirectoryProperty

    @get:OutputFile
    abstract val changelogFile: RegularFileProperty

    @TaskAction
    fun download() {
        val repositoryName = repository.get()
        if (!repositoryName.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) {
            throw GradleException("Invalid GitHub repository '$repositoryName'; expected owner/repository")
        }

        val outputDirectory = artifactsDirectory.get().asFile
        fileSystemOperations.delete {
            delete(outputDirectory)
        }
        outputDirectory.mkdirs()

        val release = getJson(
            apiUri("repos/$repositoryName/releases/tags/${encodePathSegment(tag.get())}"),
        )
        val assets = (release["assets"] as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .associateBy { it["name"]?.toString().orEmpty() }
        val requiredAssets = requiredAssetNames.get().distinct()
        val missingAssets = requiredAssets.filterNot(assets::containsKey)
        if (missingAssets.isNotEmpty()) {
            throw GradleException(
                "GitHub Release ${tag.get()} is missing required assets: ${missingAssets.joinToString()}",
            )
        }

        for (assetName in requiredAssets) {
            if (assetName != outputDirectory.resolve(assetName).name) {
                throw GradleException("Invalid GitHub Release asset name '$assetName'")
            }
            val assetId = assets.getValue(assetName)["id"]?.toString()?.toLongOrNull()
                ?: throw GradleException("GitHub Release asset '$assetName' has no numeric ID")
            val asset = downloadAsset(apiUri("repos/$repositoryName/releases/assets/$assetId"))
            Files.write(outputDirectory.resolve(assetName).toPath(), asset)
        }

        changelogFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(release["body"]?.toString().orEmpty(), StandardCharsets.UTF_8)
        }
    }

    private fun getJson(uri: URI): Map<*, *> {
        val response = apiClient.send(apiRequest(uri, GITHUB_JSON), HttpResponse.BodyHandlers.ofByteArray())
        checkResponse(response, uri)
        return JsonSlurper().parse(response.body()) as? Map<*, *>
            ?: throw GradleException("GitHub API returned an unexpected response for $uri")
    }

    private fun downloadAsset(uri: URI): ByteArray {
        val response = apiClient.send(apiRequest(uri, OCTET_STREAM), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() in REDIRECT_STATUS_CODES) {
            val location = response.headers().firstValue("Location").orElse(null)
                ?: throw GradleException("GitHub API redirected $uri without a Location header")
            val downloadUri = uri.resolve(location)
            if (downloadUri.scheme != "https") {
                throw GradleException("GitHub API returned a non-HTTPS asset URL")
            }
            val downloadRequest = HttpRequest.newBuilder(downloadUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", OCTET_STREAM)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build()
            val downloadResponse = redirectClient.send(
                downloadRequest,
                HttpResponse.BodyHandlers.ofByteArray(),
            )
            checkResponse(downloadResponse, downloadUri)
            return downloadResponse.body()
        }
        checkResponse(response, uri)
        return response.body()
    }

    private fun apiRequest(uri: URI, accept: String): HttpRequest {
        val request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", USER_AGENT)
        accessToken.orNull?.takeIf(String::isNotBlank)?.let { token ->
            request.header("Authorization", "Bearer $token")
        }
        return request.GET().build()
    }

    private fun checkResponse(response: HttpResponse<ByteArray>, uri: URI) {
        if (response.statusCode() !in 200..299) {
            val details = response.body().toString(StandardCharsets.UTF_8).take(500)
            throw GradleException("GitHub API request to $uri failed with HTTP ${response.statusCode()}: $details")
        }
    }

    private fun apiUri(path: String): URI {
        val uri = URI.create("${apiUrl.get().trimEnd('/')}/$path")
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw GradleException("GitHub API URL must use HTTPS")
        }
        return uri
    }

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private companion object {
        const val GITHUB_JSON = "application/vnd.github+json"
        const val OCTET_STREAM = "application/octet-stream"
        const val GITHUB_API_VERSION = "2022-11-28"
        const val USER_AGENT = "custom-mdk"
        val REQUEST_TIMEOUT: Duration = Duration.ofMinutes(5)
        val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
        val apiClient: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()
        val redirectClient: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }
}
