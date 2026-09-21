# Porting to NeoForge and Forge

## What actually differs between loaders

Almost nothing. Of the whole mod, only two things are loader-specific:

| Piece | Fabric | NeoForge | Forge |
|---|---|---|---|
| Metadata | `fabric.mod.json` | `META-INF/neoforge.mods.toml` | `META-INF/mods.toml` |
| Entrypoint (~20 lines) | `ModInitializer` + `CommandRegistrationCallback` | `@Mod` + `RegisterCommandsEvent` | `@Mod` + `RegisterCommandsEvent` |

Everything else is shared:

- `src/main/java/com/example/jukeboxhits/core/` — no loader imports, only vanilla + Brigadier + Gson + SLF4J
- `src/main/resources/jukeboxhits/songs.json` — the code table
- `src/main/resources/assets/jukeboxhits/` — sounds and names
- `src/main/resources/data/jukeboxhits/jukebox_song/` — the song definitions

The songs are plain resource-pack and datapack files. Minecraft reads those the same way
on every loader, which is why the hard part ports for free.

## Minecraft version support — read this first

This mod registers songs through the **`jukebox_song`** registry, which was added in
**Minecraft 1.21** (snapshot 24w21a).

| Version | Works? |
|---|---|
| 1.21 and newer | Yes — this is what the mod targets |
| 1.20.6 and older | **No.** `jukebox_song` does not exist |

On 1.20.1 and earlier, custom discs needed a registered `SoundEvent` plus a custom item
class per song, which is a different mod, not a port of this one. 1.20.1 is still popular,
so if you want it, treat it as a separate build — and budget real time for it.

Within 1.21+, the thing most likely to break between versions is the
`minecraft:jukebox_playable` component syntax. That lives in `giveCommand` in
`config/jukeboxhits.json` precisely so it is a config edit, not a recompile.

## Porting steps

1. Start a fresh NeoForge or Forge mod project (their MDK, or Architectury if you want
   one repo to produce all three jars).
2. Copy `src/main/java/com/example/jukeboxhits/core/` across unchanged.
3. Copy the entrypoint for that loader out of `loaders/<loader>/java/`.
4. Copy the metadata from `loaders/<loader>/resources/META-INF/`.
5. Copy `src/main/resources/jukeboxhits/`, `assets/jukeboxhits/` and `data/jukeboxhits/`
   across unchanged.
6. Make sure Gson and SLF4J resolve. Both ship with Minecraft on all three loaders, so
   normally there is nothing to add.

## If you would rather have one repo build all three

Use [Architectury Loom](https://docs.architectury.dev/). The layout maps onto this one
directly:

```
common/    <- core/ goes here, plus all the resources
fabric/    <- JukeboxFabric + fabric.mod.json
neoforge/  <- JukeboxNeoForge + neoforge.mods.toml
forge/     <- JukeboxForge + mods.toml
```

That is the setup most multi-loader mods use, and the split in this repo is already
shaped for it.

## Status

The NeoForge and Forge sources here are **written but not compiled or tested** — this
session had no network access to the NeoForge or Forge Maven repositories. Treat them as
a correct starting point that still needs a real build. The two most likely spots to need
a tweak:

- The `@Mod` constructor signature. NeoForge also accepts `(IEventBus modBus)` and
  `(IEventBus modBus, ModContainer container)`; if the no-arg form is rejected, add the
  parameter.
- `loaderVersion` / `versionRange` in the TOML files, which change per Minecraft version.
