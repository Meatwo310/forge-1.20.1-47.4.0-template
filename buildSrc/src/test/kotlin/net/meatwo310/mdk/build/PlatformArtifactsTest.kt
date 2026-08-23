package net.meatwo310.mdk.build

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlatformArtifactsTest {
    @Test
    fun `configures publishing dependencies through platform artifacts`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.extraProperties["minecraftVersion"] = "1.21.1"
        project.configurePlatformArtifacts(loader = "fabric", javaVersion = 21)

        project.platformArtifacts {
            publishingDependencies {
                required("shared-slug")
                optional(curseForgeSlug = "curseforge-slug", modrinthSlug = "modrinth-slug")
                incompatible(curseForgeSlug = "curseforge-only")
                embedded(modrinthSlug = "modrinth-only")
            }
        }

        val metadata = project.extensions.getByType(PlatformArtifactsExtension::class.java)
        assertEquals(
            setOf(
                PublishedDependency("shared-slug", PublishedDependencyType.REQUIRED),
                PublishedDependency("curseforge-slug", PublishedDependencyType.OPTIONAL),
                PublishedDependency("curseforge-only", PublishedDependencyType.INCOMPATIBLE),
            ),
            metadata.curseForgeDependencies.get(),
        )
        assertEquals(
            setOf(
                PublishedDependency("shared-slug", PublishedDependencyType.REQUIRED),
                PublishedDependency("modrinth-slug", PublishedDependencyType.OPTIONAL),
                PublishedDependency("modrinth-only", PublishedDependencyType.EMBEDDED),
            ),
            metadata.modrinthDependencies.get(),
        )
    }

    @Test
    fun `rejects a publishing dependency without a slug`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.extraProperties["minecraftVersion"] = "1.21.1"
        project.configurePlatformArtifacts(loader = "fabric", javaVersion = 21)

        assertFailsWith<GradleException> {
            project.platformArtifacts {
                publishingDependencies {
                    required()
                }
            }
        }
    }

    @Test
    fun `requires platform artifacts to be configured first`() {
        val project = ProjectBuilder.builder().build()

        assertFailsWith<GradleException> {
            project.platformArtifacts {
                publishingDependencies {
                    required("dependency")
                }
            }
        }
    }
}
