# Dependency Recipes For Agents

Use the `Dependencies` section in `docs/README.md` as the source of truth for
configuration names. This file only adds agent-facing checks.

## Classify Before Editing

Answer these in order:

- Which included subproject owns the dependency? Check `settings.gradle.kts`.
- Is it a mod jar or a normal Java library?
- Does source code import or call dependency classes?
- Is it needed only for local `runClient` / `runServer`?
- Does GitHub Actions need the jar copied into `run/mods`?
- Does production loader metadata need to require the dependency?

Do not use `ciRuntimeMods` just because a dependency is runtime-only. Use it only
when the external runtime test in CI needs a physical jar next to the built mod.

## Existing Special Cases

- `fabric-api-conventions` already adds Fabric API with the correct Fabric
  configuration for the selected Loom plugin.
- `fabric-config-conventions` and `fabric-legacy-config-conventions` already add
  the matching Forge Config API Port dependency to compile classpaths,
  `ciRuntimeMods`, and generated Fabric metadata.
- Existing Fabric Mod Menu dependencies are local runtime UI helpers; they are
  not staged through `ciRuntimeMods`.
- Existing LexForge Legacy Configured dependencies are local runtime UI helpers.
- Prefer the configurations documented in `docs/README.md`; do not introduce
  seldom-used configurations unless a loader plugin requires them for the
  concrete dependency.

## Metadata Checklist

Gradle dependencies and production loader metadata are separate.

- Add Fabric metadata through `fabricModMetadata.depend(...)` for systematic
  convention-owned dependencies.
- Add target-specific Fabric metadata in `src/main/templates/fabric.mod.json`.
- Add LexForge production dependencies in
  `src/main/templates/META-INF/mods.toml`.
- Add NeoForge production dependencies in
  `src/main/templates/META-INF/neoforge.mods.toml`.
- Add CurseForge and Modrinth project-page relations under
  `platformArtifacts { publishingDependencies { ... } }` in each affected
  platform subproject or a convention applied to those subprojects. These
  relations do not change Gradle dependencies or loader metadata.
- Do not mark soft integrations as required metadata.

## Verification

Run:

```bash
./gradlew :<mc>-<loader>:build
```

If `ciRuntimeMods` was changed, confirm staging:

```bash
find <mc>/<loader>/build/ciRuntimeMods -maxdepth 1 -type f -name '*.jar'
```

If project inclusion or CI matrix assumptions changed, run:

```bash
./gradlew --configuration-cache --no-daemon writeCiBuildMatrix
```
