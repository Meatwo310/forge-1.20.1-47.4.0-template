# Change Recipes

Use the checklist closest to the requested change. Keep scope narrow unless the
request explicitly asks for a broad update.

## Add Shared Mod Code

- Decide whether the code is cross-version (`common/src/main`) or tied to one
  Minecraft version (`<mc>/common/src/main`).
- Keep package names aligned with existing `net.meatwo310.examplemod` classes.
- If the shared API changes, update every loader project that calls it.
- Build representative downstream projects:

```bash
./gradlew :1.18.2-forge:build :1.20.1-fabric:build :26.1-neo:build
```

## Add Loader-Specific Code

- Put Fabric code under `<mc>/fabric`, LexForge code under `<mc>/forge`, and
  NeoForge code under `<mc>/neo`.
- Keep entrypoint names consistent with `gradle.properties` and metadata
  templates.
- For Fabric client code, use `src/client/java`.
- For Forge and NeoForge client-only code, follow the existing project pattern
  before adding a new source set.
- Build the affected project:

```bash
./gradlew :<mc>-<loader>:build
```

## Add Or Change Config

- Put shared declarations in `common/src/config`.
- Put version-specific config helpers in `<mc>/common/src/config`.
- Put loader config registrar code in `<mc>/<loader>/src/config`.
- Put client config UI code in `<mc>/<loader>/src/configClient` when that loader
  convention supports it.
- Confirm the affected project applies the right `*-config-conventions` plugin.
- For Fabric config dependencies, check Forge Config API Port and any runtime UI
  mods such as Mod Menu or Forge Config Screens.
- Build one affected project per loader family.

## Add A New Minecraft Version

- Add `<mc>/common`.
- Add one or more loader project directories: `<mc>/fabric`, `<mc>/forge`, `<mc>/neo`.
- Add `include(...)` entries in `settings.gradle.kts`.
- Add each subproject `build.gradle.kts`.
- Add each subproject `gradle.properties` with `minecraftVersion` and required
  loader, Java, mapping, and dependency versions.
- Copy only the needed source files from the nearest existing version, then
  adapt API differences.
- Add metadata templates under each loader project's `src/main/templates`.
- Run:

```bash
./gradlew --configuration-cache --no-daemon writeCiBuildMatrix
./gradlew :<mc>-common:build :<mc>-<loader>:build
```

The 1.7.10 GTNHGradle target is a deliberate standalone exception. Do not use
it as the pattern for adding other Minecraft versions unless they also cannot
consume the repository's shared convention projects.

## Add A New Loader Project For An Existing Version

- Confirm `<mc>/common` exists and is included.
- Add `<mc>/fabric`, `<mc>/forge`, or `<mc>/neo`.
- Add the project to `settings.gradle.kts`.
- Use the matching convention plugins:
  - Fabric: `fabric-loom-mod-conventions` or `fabric-loom-remap-mod-conventions`
  - Fabric API: `fabric-api-conventions`
  - Fabric config (1.20.1+): `fabric-config-conventions`
  - Fabric Legacy config (1.18.2-1.19.2): `fabric-legacy-config-conventions`
  - LexForge Legacy: `lexforge-legacy-mod-conventions`
  - LexForge Legacy config: `lexforge-legacy-config-conventions`
  - LexForge (ForgeGradle 7+): `lexforge-mod-conventions`
  - LexForge config: `lexforge-config-conventions`
  - NeoForge: `neoforge-mod-conventions`
  - NeoForge config: `neoforge-config-conventions`
- Add loader metadata templates and runtime dependencies.
- Run `writeCiBuildMatrix` and the new project build.

## Change The 1.7.10 GTNH Project

- Keep implementation and resources under `1.7.10/forge`; there is no
  `1.7.10/common` project.
- Keep GTNHGradle settings in `1.7.10/forge/gradle.properties`.
- Add dependencies and repositories through `dependencies.gradle` and
  `repositories.gradle` in that project.
- Keep `ciRequiresCommon=false` so CI accepts the standalone project.
- Run:

```bash
./gradlew --configuration-cache --no-daemon writeCiBuildMatrix
./gradlew :1.7.10-forge:build
```

## Change Mod Metadata Values

- Prefer root `gradle.properties` for shared values such as mod id, name,
  license, authors, URLs, credits, and Fabric entrypoints.
- Prefer subproject `gradle.properties` for Minecraft, loader, Java, mappings,
  and runtime dependency versions.
- Edit templates only when the metadata shape changes.
- Build one Fabric and one Forge/NeoForge project if template expansion changed.

## Change Metadata Template Structure

- Fabric: edit `src/main/templates/fabric.mod.json` and check
  `fabric-mod-conventions`.
- LexForge: edit `src/main/templates/META-INF/mods.toml` and check
  `lexforge-mod-conventions` or `lexforge-legacy-mod-conventions`, according to the ForgeGradle generation.
- NeoForge: edit `src/main/templates/META-INF/neoforge.mods.toml` and check
  `neoforge-mod-conventions`.
- If a new placeholder is introduced, add it to the matching `replaceProperties`
  or default metadata map.
- Run the affected loader project build and inspect generated resources if the
  change is risky.

## Change Dependencies

- Read the `Dependencies` section in `docs/README.md` first.
- Use `gradle/libs.versions.toml` for shared aliases.
- Use subproject `gradle.properties` for per-version dependency versions.
- Add local `runClient` / `runServer` dependencies in the affected loader
  project's `build.gradle.kts`.
- Use `ciRuntimeMods` for jars that GitHub Actions runtime tests must copy into
  `run/mods`; do not use it as a local run dependency.
- If code references dependency classes, add a compile dependency as well as any
  runtime or CI staging dependency that target needs.
- Build the affected project and confirm `build/ciRuntimeMods` when runtime mods
  are expected.

## Change buildSrc Conventions

- Identify which convention plugin owns the behavior.
- Keep common behavior in helper functions under `net/meatwo310/mdk/build`.
- Avoid duplicating logic across Fabric, Forge, and NeoForge conventions unless
  their Gradle plugins require different APIs.
- Run at least:

```bash
./gradlew --configuration-cache --no-daemon writeCiBuildMatrix
./gradlew :1.20.1-fabric:build :1.20.1-forge:build :26.1-neo:build
```

## Change CI Or Release Behavior

- For build matrix behavior, update `writeCiBuildMatrix` in root
  `build.gradle.kts`.
- For workflow execution behavior, update `.github/workflows/build.yml`.
- For version bumping, jar collection, release notes, tags, or GitHub Release
  behavior, update `.github/workflows/release.yml`.
- Run `writeCiBuildMatrix` locally after matrix-related changes.
- Check that docs-only changes are still ignored by the build workflow unless
  the requested change says otherwise.

## Change Release Version

- Edit `version.txt` only when intentionally setting the release version.
- Keep the value as a plain semantic version with no `v` prefix.
- Release tags are created as `v<version>` by the workflow.
- Do not change archive version formatting unless release artifact names are
  part of the task.
