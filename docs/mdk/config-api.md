# Config API Notes For Agents

Use this file when editing the shared config system or adding config entries.
`docs/README.md` contains the user-facing overview; this file defines the API
surface agents should treat as template-user facing.

## Preferred Declaration Shape

Declare config entries with one `ConfigEntryBuilder` per config class or nested
category class. Prefer `category(...)` plus nested classes for hierarchy.
Avoid `push(...)` and `pop()` in normal feature work; they are low-level escape
hatches for unusual generated or migration-oriented structures.

```java
public final class ServerConfig {
    private static final ConfigEntryBuilder BUILDER = new ConfigEntryBuilder();

    public static final ConfigEntry.BooleanEntry ENABLE_FEATURE =
            BUILDER.comment("Enable the main server feature.")
                    .define("enableFeature", true);

    public static final ConfigEntries ADVANCED =
            BUILDER.comment("Advanced server settings.")
                    .category("advanced", Advanced.ENTRIES);

    public static final class Advanced {
        private static final ConfigEntryBuilder BUILDER = new ConfigEntryBuilder();

        public static final ConfigEntry.IntEntry CACHE_SIZE =
                BUILDER.comment("Maximum cache size.")
                        .defineInRange("cacheSize", 256, 0, 8192);

        public static final ConfigEntries ENTRIES = BUILDER.build();
    }

    public static final ConfigEntries ENTRIES = BUILDER.build();
}
```

Do not add private empty constructors to examples or new template config
classes solely for style. They add noise and do not clarify the config API.

## Definition API

`ConfigEntryBuilder` is the normal entry-definition API:

- `comment(String comment)` appends a comment for the next entry or category.
  Consecutive calls are joined with newlines in declaration order. Blank
  comments are treated as absent during binding and must not be forwarded to
  loader-specific config builders.
- `translation(String translationKey)` assigns the exact translation key to
  the next entry or category without deriving it from the category path.
- `worldRestart()` marks the next entry as requiring a world restart.
- `gameRestart()` marks the next entry as requiring a game restart on
  NeoForge. Platforms using the Forge Config API ignore it.
- `define(...)` defines unrestricted primitive, boolean, or string values. Supported overloads are `int`, `long`, `double`, `boolean`, and `String`.
- `defineInRange(...)` defines ranged numeric values. Supported overloads are `int`, `long`, and `double`.
- `defineList(...)` defines a list from a default value, a new-element supplier for config screens, and an element validator.
- `defineEnum(...)` defines an enum value from its default enum constant.
- `category(String key, ConfigEntries children)` appends a nested section and should be preferred for hierarchy.
- `push(String key)`, `pop()`, and `pop(int count)` manually open and close
  sections. Treat them as low-level API.
- `build()` returns the immutable `ConfigEntries` for the class or category.

`ConfigEntries` is normally produced by `build()` or `category(...)`:

- `ConfigEntries.category(String key, String comment, ConfigEntries children)` builds a category directly.
- `append(ConfigEntries other)` merges entries, usually through `ConfigDeclaration.append(...)`.
- `bindTo(ConfigVisitor visitor)` is binding infrastructure, not normal template-user code.

## Value API

Store returned entries as public static fields when game code needs to read or
write them:

- `get()` reads the current value.
- `set(T value)` updates the current value without forcing an immediate save.
- `flush()` saves the current value through the bound loader config value.
- `setAndFlush(T value)` updates and saves.
- `defaultValue()` returns the declared default.
- `key()` returns the config key.
- `comment()` returns the declared comment.

Primitive entries also implement supplier interfaces:

- `ConfigEntry.IntEntry#getAsInt()`
- `ConfigEntry.LongEntry#getAsLong()`
- `ConfigEntry.DoubleEntry#getAsDouble()`
- `ConfigEntry.BooleanEntry#getAsBoolean()`

Use the primitive supplier methods in gameplay code when they make call sites
clearer. Use `get()` for strings, lists, enums, and generic code.

## Declaration API

Expose files from a `ModConfigs`-style class:

- `ConfigDeclaration.of(ConfigSide side, ConfigEntries entries)`
- `ConfigDeclaration.of(ConfigSide side, ConfigEntries entries, String fileName)`
- `ConfigSide.SERVER` maps to the loader server config type.
- `ConfigSide.CLIENT` maps to the loader client config type.
- `ConfigSide.COMMON` maps to the loader common config type.

Use `ConfigSide.SERVER` for values owned by a server or world: gameplay rules,
balance values, world behavior toggles, and anything a dedicated server owner
should control for connected players. Forge stores server configs under the
world/server config location and syncs them to clients during connection;
NeoForge also treats server configs as network-synced server-owned config.

Use `ConfigSide.CLIENT` for local client preferences: rendering, audio, HUD,
input, accessibility, local-only integration settings, and config screen
preferences. These values belong to the player installation, are not present on
dedicated servers, and should not affect server-authoritative gameplay.

Use `ConfigSide.COMMON` for installation-wide defaults that are meaningful on
both physical sides but are still independently owned by each installation:
startup-independent feature defaults, logging/detail level choices used by both
client and server code, or values that are safe for singleplayer and dedicated
server installations to choose separately. Do not use common config just because
both sides read a value; if multiplayer behavior must follow the server's
choice, use server config.

This template does not expose NeoForge's `STARTUP` config type through
`ConfigSide`; add a new abstraction deliberately if a future change needs it.

The optional `fileName` is trimmed. Empty names are treated as absent. Omit it
to let the loader choose its default file name.

Primary references for side semantics:

- Forge Configuration: https://docs.minecraftforge.net/en/latest/misc/config/
- NeoForge Configuration: https://docs.neoforged.net/docs/misc/config/

## Extension API

Use `ConfigDeclarations.append(...)` to add version-specific or platform-
specific entries to a declaration that already exists in `ModConfigs.ALL`:

```java
public final class VersionedModConfigs {
    public static final List<ConfigDeclaration> ALL =
            ConfigDeclarations.append(
                    ModConfigs.ALL,
                    ModConfigs.SERVER,
                    VersionedServerConfig.ENTRIES);
}
```

The target can match by object identity, record equality, or by side plus
optional file name. The helper throws when no declaration matches or when more
than one declaration matches; do not swallow those exceptions.

## Binding And Registration API

`VersionedConfigSpec` converts neutral declarations into the loader config spec
available for that Minecraft version:

| Minecraft | Config-enabled platforms | Config spec family |
|-----------|--------------------------|--------------------|
| 1.18.2-1.19.2 | Fabric, LexForge Legacy | LexForge-style `ForgeConfigSpec` |
| 1.20.1 | Fabric, LexForge Legacy | LexForge-style `ForgeConfigSpec` |
| 1.21.1 | Fabric, LexForge, NeoForge | NeoForge-style `ModConfigSpec` |
| 1.21.8-1.21.11 | Fabric, LexForge | NeoForge-style `ModConfigSpec` |
| 26.x | Fabric, NeoForge | NeoForge-style `ModConfigSpec` |

The standalone 1.7.10 Forge project is outside this shared abstraction.
Forge Config API Port adapts specs for Fabric and modern LexForge registration;
it does not determine the spec family.

- `VersionedConfigSpec.bind(ConfigDeclaration declaration)`
- `VersionedConfigSpec.bindAll(List<ConfigDeclaration> declarations)`
- `VersionedConfigSpec.bind(ConfigEntries entries)`

Register the bound configs from the loader entrypoint:

```java
PlatformConfigRegistrar.registerAll(modContainer, VersionedConfigSpec.bindAll(ModConfigs.ALL));
```

Fabric passes the mod id instead of a loader container:

```java
PlatformConfigRegistrar.registerAll(Constants.MODID, VersionedConfigSpec.bindAll(ModConfigs.ALL));
```

Older LexForge Legacy targets may expose `registerAll(List<BoundConfig>)` or
`registerAll(FMLJavaModLoadingContext, List<BoundConfig>)`; check the target
project's `PlatformConfigRegistrar`.

## Internal API Boundary

Do not use these directly in normal config additions unless you are changing
the config framework itself:

- `ConfigEntryBinder`
- `ConfigEntryBinding`
- `ConfigVisitor`
- `ConfigElement`
- `ConfigInstruction`
- `VersionedConfigSpec.BoundConfig` fields beyond passing them to registration

When changing the framework, update this file and the `Configuration System`
section in `docs/README.md` together.
