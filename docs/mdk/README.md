# MDK Agent Notes

This directory is a compact operating guide for LLM agents working on this
multi-version, multi-loader Minecraft mod template.

Keep the docs practical. Prefer project-specific facts over general Minecraft
modding explanations, and update these files when project structure or build
commands change.

## Read Order

1. `README.md` - start here for scope and navigation.
2. `project-map.md` - locate the owning project, source set, or template.
3. `workflow.md` - follow the normal edit and verification flow.
4. `change-recipes.md` - use the checklist that matches the requested change.
5. `config-api.md` - use when adding or changing shared config declarations.

## Repository Shape

The Gradle build is split by Minecraft version and loader:

- `common` contains cross-version shared code for targets other than the
  standalone 1.7.10 Forge project.
- `<mc>/common` contains code shared by loaders for one Minecraft version. The
  standalone `1.7.10-forge` GTNHGradle project is the exception and has no
  matching common project.
- `<mc>/fabric` contains Fabric loader code and metadata for one version.
- `<mc>/forge` contains LexForge loader code and metadata for one version. ForgeGradle 7 is the boundary between LexForge and LexForge Legacy conventions.
- `<mc>/neo` contains NeoForge loader code and metadata for one version.
- `buildSrc` owns reusable Gradle convention plugins and helper tasks.

The project list for the repository you are editing is defined in
`settings.gradle.kts`. Do not infer supported versions from directory names
alone; check that repository's `include(...)` entries.

Projects included by default in this template:

- `common`
- `1.7.10/forge` (standalone GTNHGradle project)
- `1.18.2/common`, `1.18.2/forge`, `1.18.2/fabric`
- `1.19.2/common`, `1.19.2/forge`, `1.19.2/fabric`
- `1.20.1/common`, `1.20.1/forge`, `1.20.1/fabric`
- `1.21.1/common`, `1.21.1/forge`, `1.21.1/neo`, `1.21.1/fabric`
- `1.21.8/common`, `1.21.8/forge`, `1.21.8/fabric`
- `1.21.11/common`, `1.21.11/forge`, `1.21.11/fabric`
- `26.1/common`, `26.1/fabric`, `26.1/neo`
- `26.1.2/common`, `26.1.2/fabric`, `26.1.2/neo`
- `26.2/common`, `26.2/fabric`, `26.2/neo`

## Key Build Inputs

- `gradle.properties` stores global mod metadata such as `modId`, `modName`,
  group, license, URLs, authors, and Fabric entrypoints.
- `version.txt` stores the release version used by all subprojects.
- Each subproject `gradle.properties` stores the target Minecraft version,
  loader versions, Java version, mappings, and runtime dependency versions.
- `1.7.10/forge/dependencies.gradle` and `repositories.gradle` contain the
  GTNHGradle project's dependency and repository declarations.
- `gradle/libs.versions.toml` stores shared plugin and dependency aliases.
- `settings.gradle.kts` controls which projects exist in the current repository
  and which loader projects are included in the CI build matrix.
- `docs/README.md` contains the dependency configuration table. Keep LLM-facing
  dependency notes aligned with that section when dependency rules change.
- `docs/README.md` contains the user-facing config overview. Keep
  `config-api.md` aligned with that section when config APIs change.

## Generated Metadata

Loader metadata is generated during Gradle resource processing:

- Fabric reads `src/main/templates/fabric.mod.json` and writes generated
  metadata with default dependencies from `fabric-mod-conventions`.
- LexForge and LexForge Legacy read `src/main/templates/META-INF/mods.toml`.
- NeoForge reads `src/main/templates/META-INF/neoforge.mods.toml`.
- The standalone 1.7.10 Forge project expands its static
  `src/main/resources/mcmod.info` through GTNHGradle.
- LexForge, LexForge Legacy, and NeoForge also include generated resources from
  `src/generated/resources`.

Edit templates when changing metadata structure. Edit Gradle properties when
changing values.

## CI And Release

The `Build` workflow ignores docs-only changes. For code changes, it:

- runs `./gradlew --configuration-cache --no-daemon writeCiBuildMatrix`;
- builds every loader project detected from `settings.gradle.kts`;
- normally requires a matching `<minecraft>-common` project, while honoring
  `ciRequiresCommon=false` for standalone projects such as `1.7.10-forge`;
- uploads each loader project's `build/libs` from its configured project directory;
- runs Forge/NeoForge game tests when supported;
- otherwise starts a server smoke test and verifies the shutdown log;
- runs `headlesshq/mc-runtime-test` against the built jars.

The `Release` workflow is manual. It can bump `version.txt`, build all platform
projects, collect release jars under `build/release/libs`, generate notes with
`git-cliff`, tag `v<version>`, and create a GitHub Release.

The manual `Publish` workflow consumes an existing GitHub Release and publishes
its declared platform artifacts to CurseForge and/or Modrinth. Publication
metadata comes from `platformArtifacts`; project IDs and supported environments
have explicit defaults in the root `modPublishing` block. Individual platform
projects can override the supported subset through `platformPublishing`. Both
blocks are restricted facades that delegate the allowed Gradle properties while
keeping tokens, files, endpoints, changelogs, and artifact identity internal.

## Agent Rules

- Keep edits scoped to the requested version, loader, and source set.
- Never remove another version or loader unless the task explicitly asks for it.
- When adding a new platform project, update `settings.gradle.kts`, the
  subproject files, properties, templates, and CI expectations together.
- Prefer existing convention plugins in `buildSrc` over ad hoc Gradle logic in
  individual subprojects.
- For config-related work, check `src/config` and `src/configClient` in
  addition to `src/main` and `src/client`.
