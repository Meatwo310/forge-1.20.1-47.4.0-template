import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.meatwo310.mdk.build.*

val modId = project.property("modId").toString()
val minecraftVersion = project.property("minecraftVersion").toString()
val fabricApiVersion = project.property("fabricApiVersion").toString()

plugins.withId("fabric-mod-conventions") {
    requirePublishedDependency("fabric-api")
    extensions.configure<FabricModMetadataExtension>("fabricModMetadata") {
        depend("fabric-api", "*")
    }
}

fun configureFabricApiTests() {
    if (minecraftVersion.supportsGameTestServer()) {
        extensions.configure<FabricApiExtension>("fabricApi") {
            val testModId = "$modId-test"

            @Suppress("UnstableApiUsage")
            configureTests {
                createSourceSet = true
                modId = testModId
                enableGameTests = true
                enableClientGameTests = true
                eula = true
            }
        }
    }
}

plugins.withId("net.fabricmc.fabric-loom") {
    dependencies.add("implementation", "${versionCatalog.module(VersionCatalogLibrary.FabricApi)}:$fabricApiVersion")
    configureFabricApiTests()
}

plugins.withId("net.fabricmc.fabric-loom-remap") {
    dependencies.add("modImplementation", "${versionCatalog.module(VersionCatalogLibrary.FabricApi)}:$fabricApiVersion")
    configureFabricApiTests()
}
