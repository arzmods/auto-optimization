# Porting to NeoForge and Forge

## What actually differs between loaders

Almost nothing. Of the whole mod, only two things are loader-specific:

| Piece | Fabric | NeoForge | Forge |
|---|---|---|---|
| Metadata | `fabric.mod.json` | `META-INF/neoforge.mods.toml` | `META-INF/mods.toml` |
| Entrypoint | `ModInitializer` | `@Mod` | `@Mod` |
| Commands | `CommandRegistrationCallback` | `RegisterCommandsEvent` | `RegisterCommandsEvent` |
| Right-click a jukebox | `UseBlockCallback` | `PlayerInteractEvent.RightClickBlock` | `PlayerInteractEvent.RightClickBlock` |
| Voice chat plugin | `voicechat` entrypoint | `@ForgeVoicechatPlugin` | `@ForgeVoicechatPlugin` |

Everything else is shared:

- `src/main/java/com/example/jukeboxhits/core/` — no loader imports: vanilla, Brigadier,
  Simple Voice Chat's API, Gson and SLF4J only

That includes the whole upload pipeline, the code library, every command, and playback.
Simple Voice Chat's API is the same on all three loaders, so the audio path ports for
free; only plugin *discovery* differs, which is why each loader has a thin annotated
subclass of `JukeboxVoicechatPlugin`.

## Minecraft version support

The old resource-pack approach was locked to 1.21+ because it used the `jukebox_song`
registry, which did not exist before then. **That constraint is gone.** Audio now goes
through Simple Voice Chat, which does not care what Minecraft version you are on.

The floor is now whatever Simple Voice Chat itself supports, which reaches back well
before 1.21 and covers 1.20.1.

One thing does change below **1.20.5**: item components did not exist yet, so the default
`giveCommand` will not parse. Use the old NBT form in the config instead:

```json
"giveCommand": "give %player% %disc%{display:{Name:%name%}}"
```

Nothing in the Java needs to change for that.

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
session had no network access to the NeoForge or Forge Maven repositories. They are a
correct starting point that still needs a real build. Most likely to need a tweak:

- The `@Mod` constructor signature. NeoForge also accepts `(IEventBus modBus)` and
  `(IEventBus modBus, ModContainer container)`; if the no-arg form is rejected, add the
  parameter.
- `loaderVersion` / `versionRange` in the TOML files, which change per Minecraft version.
- `PlayerInteractEvent.RightClickBlock` fires for both hands on some versions; if a song
  starts twice, filter on `event.getHand()`.

The shared core in `src/main/java/.../core/` is the same code the Fabric build uses, and
its pure-Java parts are covered by `tools/run_tests.sh`.
