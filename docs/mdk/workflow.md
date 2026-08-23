# Standard Workflow

Use this flow for normal code, build, and template changes.

## 1. Identify Scope

Before editing, answer these questions from the repository:

- Which Minecraft versions are affected?
- Which loaders are affected: Fabric, LexForge, NeoForge, or all?
- Is the change shared across all versions, one Minecraft version, or one
  loader project?
- Does it touch only runtime code, or also config, metadata, build logic, CI, or
  release behavior?

Check `settings.gradle.kts` for the project list in the repository you are
editing. Directory names that are not included there are not build participants.

## 2. Choose The Owning Location

Use the narrowest correct location:

- Put cross-version shared Java in `common/src/main`.
- Put cross-version shared config declarations in `common/src/config`.
- Put one-version shared Java in `<mc>/common/src/main`.
- Put one-version shared config helpers in `<mc>/common/src/config`.
- Put loader startup code in `<mc>/fabric`, `<mc>/forge`, or `<mc>/neo`.
- Keep standalone 1.7.10 Forge code and build configuration in
  `1.7.10/forge`; it uses GTNHGradle and does not share a `1.7.10/common`
  project.
- Put Fabric client-only code in `<mc>/fabric/src/client`.
- Put loader client config UI code in `src/configClient` where that project has
  the source set.
- Put generated metadata shape in `src/main/templates`.
- Put build behavior in `buildSrc/src/main/kotlin` unless it is truly unique to
  one subproject.

## 3. Inspect Existing Patterns

Search before changing:

```bash
rg "symbol_or_property_name"
rg --files '<mc>/fabric' '<mc>/forge' '<mc>/neo' '<mc>/common' common
```

For repeated version/loader files, inspect at least one older Forge project, one
Fabric project, and one NeoForge project when applicable. Keep names, packages,
and source set layout consistent with nearby examples.

## 4. Edit

Keep edits small and paired:

- If code references a shared API, update every version-specific implementation
  that must compile against it.
- If metadata values change, prefer `gradle.properties` over template rewrites.
- If metadata structure changes, update the relevant template and convention
  plugin inputs together.
- If adding dependencies, update the relevant subproject `build.gradle.kts` and
  version property or `libs.versions.toml` entry. Use the dependency table in
  `docs/README.md` to choose between compile, local runtime, CI runtime staging,
  and production metadata changes.
- If adding a project, update `settings.gradle.kts` and make sure the project
  name follows `<mc>-<loader>` or `<mc>-common`, with its directory under
  `<mc>/<loader>` or `<mc>/common`.
- Set `ciRequiresCommon=false` only for a deliberately standalone loader
  project. The existing example is `1.7.10-forge`.

Do not edit generated build outputs.

## 5. Verify Locally

Run the narrowest build first:

```bash
./gradlew :<project>:build
```

Examples:

```bash
./gradlew :1.7.10-forge:build
./gradlew :1.20.1-fabric:build
./gradlew :1.20.1-forge:build
./gradlew :26.1-neo:build
```

For shared changes, build representative downstream projects:

```bash
./gradlew :1.18.2-forge:build :1.20.1-fabric:build :26.1-neo:build
```

For CI matrix or project inclusion changes:

```bash
./gradlew --configuration-cache --no-daemon writeCiBuildMatrix
cat build/ci/build-matrix.json
```

For publishing changes, configure a dry run against an existing GitHub Release
and limit it to one platform while iterating:

```bash
./gradlew publishMods \
  -PpublishGitHubRepository=owner/repository \
  -PpublishTag=v1.0.0 \
  -PpublishProjects=1.21.1-fabric \
  -PpublishDestination=modrinth \
  -PpublishDryRun=true
```

The build downloads release metadata and assets directly through the GitHub
REST API. Public releases require no local GitHub CLI or token; set
`GITHUB_TOKEN` for private repositories or authenticated API access.

For Forge or NeoForge game test server support:

```bash
./gradlew :<forge-or-neo-project>:runGameTestServer
```

For older projects that do not support `runGameTestServer`, use a server smoke
test:

```bash
mkdir -p <project>/run
printf 'eula=true\n' > <project>/run/eula.txt
printf 'stop\n' | ./gradlew :<project>:runServer 2>&1 | tee runServer.log
grep -E '\[Server thread/INFO\].*Stopping server$' runServer.log
```

## 6. Match CI Expectations

The GitHub `Build` workflow derives platform projects from that repository's
`settings.gradle.kts`, excluding `common` and `*-common`. A platform project
must have:

- a valid `minecraftVersion`;
- a loader suffix of `fabric`, `forge`, or `neo`;
- a matching `<minecraftVersion>-common` project unless it explicitly sets
  `ciRequiresCommon=false`;
- a numeric `javaVersion` when set;
- buildable jars under the configured project directory's `build/libs`;
- `platformArtifacts` metadata naming the main jar task and, when generated,
  the sources jar task;
- optional `platformPublishing` overrides when its publication differs from the
  root `modPublishing` defaults;
- optional runtime jars staged by `collectCiRuntimeMods`.

Run `writeCiBuildMatrix` whenever those assumptions might have changed.

## 7. Final Check

Before handing off:

- Run `git status --short`.
- Confirm only intended files changed.
- State which verification commands passed or could not be run.
- Mention any project/version/loader combinations that were not verified.
