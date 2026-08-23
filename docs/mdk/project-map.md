# Project Map

This file maps repository locations to responsibilities.

## Root

`settings.gradle.kts`

- Includes every Gradle subproject that is enabled in this repository.
- Defines `ciBuildProjectNames` as all children except `common` and `*-common`.
- Controls which loader projects enter the build and release matrices.

`build.gradle.kts`

- Applies loader Gradle plugins at the root with `apply false`.
- Registers `writeCiBuildMatrix`.
- Reads each loader project's `platformArtifacts` metadata and records exact
  main, sources, and release artifact names in the CI matrix.
- Honors a loader project's `ciRequiresCommon=false` property when a standalone
  project does not have a matching `<minecraft>-common` project.
- Reads `version.txt` into `modVersion`.
- Sets common subproject group, version, repositories, Java compile encoding,
  archive version, and IDEA behavior.

`gradle.properties`

- Global mod metadata and Fabric entrypoint defaults.

`version.txt`

- Single release version used by all subprojects.

`gradle/libs.versions.toml`

- Shared plugin and dependency aliases.

## Shared Code

`common/src/main`

- Cross-version Java used by convention-backed loaders and Minecraft versions.
- The standalone 1.7.10 Forge project does not consume it.

`common/src/config`

- Cross-version config model and declaration code.

`<mc>/common/src/main`

- Java shared by all loaders for one Minecraft version.
- Use this for API differences tied to a Minecraft version.

`<mc>/common/src/config`

- Version-specific config support.

## Loader Projects

`<mc>/fabric`

- Fabric loader entrypoint and Fabric-specific integration.
- Uses either `fabric-loom-mod-conventions` or
  `fabric-loom-remap-mod-conventions`.
- Usually applies `fabric-api-conventions` and the matching config convention:
  `fabric-legacy-config-conventions` for 1.18.2-1.19.2 or
  `fabric-config-conventions` for 1.20.1 and newer.

`<mc>/forge`

- LexForge entrypoint, resources, mixin config, templates, and runtime deps.
- Uses `lexforge-mod-conventions` for ForgeGradle 7+ or `lexforge-legacy-mod-conventions` for older versions.
- Applies the matching `lexforge-config-conventions` or `lexforge-legacy-config-conventions` when config code is included.
- `1.7.10/forge` is a standalone exception: it uses GTNHGradle, keeps
  dependencies and repositories in project-local Gradle scripts, and has no
  matching `1.7.10/common` project.

`<mc>/neo`

- NeoForge entrypoint, resources, templates, and runtime deps.
- Uses `neoforge-mod-conventions`.
- Applies `neoforge-config-conventions` when config code is included.

## Source Sets

`src/main/java`

- Loader-specific main code.
- Loader entrypoints live here unless a convention says otherwise.

`src/client/java`

- Fabric client source set, including Fabric `ModClient`.

`src/config/java`

- Config implementation source set.

`src/configClient/java`

- Client config UI source set.

`src/main/resources`

- Static loader resources, such as mixin JSON files and the GTNHGradle-backed
  1.7.10 project's `mcmod.info`.

`src/main/templates`

- Loader metadata templates.
- Fabric: `fabric.mod.json`.
- LexForge and LexForge Legacy: `META-INF/mods.toml`.
- NeoForge: `META-INF/neoforge.mods.toml`.

`src/generated/resources`

- Generated data resources for Forge and NeoForge data runs.

## buildSrc

`buildSrc/src/main/kotlin/*conventions.gradle.kts`

- Precompiled Gradle convention plugins.

`common-config-conventions`

- Adds `src/config` to `common` and packages its output.

`lexforge-legacy-common-conventions`, `neoforge-common-conventions`

- Configure one-version common projects for LexForge Legacy or NeoForge tooling.

`lexforge-mod-conventions`, `lexforge-legacy-mod-conventions`, `neoforge-mod-conventions`

- Configure runs, metadata generation, Java toolchain, archives, and jar
  contents for Forge and NeoForge.

`fabric-mod-conventions`

- Configures Fabric metadata generation, Java toolchain, archives, and jar
  contents.

`fabric-loom-mod-conventions`, `fabric-loom-remap-mod-conventions`

- Configure Loom, split client source sets, Minecraft dependencies, mappings,
  and shared source sets.

`fabric-api-conventions`

- Adds Fabric API dependency and game test setup where supported.

`*-config-conventions`

- Wire `src/config` and `src/configClient` into classpaths and jars.

`net/meatwo310/mdk/build`

- Kotlin helpers for source sets, CI runtime mod staging, Fabric metadata,
  dependency version actions, version support, and version catalog access.

`PlatformArtifacts.kt`

- Defines loader identity and the Gradle tasks that produce each platform's
  main and optional sources jars.
- Reads the archive base name from the platform's Gradle `base` configuration.
- Resolves exact archive file names for CI, release collection, and downstream
  publishing logic.

`DependencyVersionConstraints.kt`

- Provides `req(version)` for `version { require(version) }` and `pin(version)`
  for `version { strictly(version) }`.

`CiRuntimeModsConvention.kt`

- Creates non-transitive `ciRuntimeMods` and `collectCiRuntimeMods`.
- Stages direct CI runtime-test jars under each configured project directory's `build/ciRuntimeMods`.

## CI And Release

`.github/workflows/build.yml`

- Detects platform projects with `writeCiBuildMatrix`.
- Uploads jars and runtime mod jars, then runs game/server/runtime tests.

`.github/workflows/release.yml`

- Manually bumps or keeps `version.txt`.
- Builds all platform projects from the CI matrix.
- Collects the exact release artifacts declared by each platform, generates
  release notes, tags, and publishes a GitHub Release.
