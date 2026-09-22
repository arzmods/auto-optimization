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
registry. **That constraint is gone.** Audio now goes through Simple Voice Chat, which
maintains branches all the way back to 1.12.2, so the floor is whatever it supports.

### One jar per Minecraft version

This is the part that surprises people: a Minecraft mod **cannot span versions in one
file**. You compile separately for each. Simple Voice Chat itself keeps a branch per
version for exactly this reason.

Switch targets with:

```bash
tools/set_version.sh 26.3      # or 1.21.8, 1.21.1, 1.20.1
gradle build                   # jar lands in build/libs/
```

That rewrites `gradle.properties` from `versions/<mc>.properties`, which carries the
Minecraft version, Java level, Loom version and Fabric API version for that target.
Shipping four versions means running it four times.

### The targets

| Minecraft | Java | Fabric | NeoForge | Forge |
|---|---|---|---|---|
| **26.3** | 25 | ✅ | ✅ | — |
| **1.21.8** | 21 | ✅ | ✅ | — |
| **1.21.1** | 21 | ✅ | ✅ | fading |
| **1.20.1** | 17 | ✅ | **does not exist** | ✅ |

Two things worth knowing before you spend effort:

- **NeoForge does not exist for 1.20.1.** It forked from Forge at 1.20.2, so 1.20.1 is a
  Fabric-or-Forge choice.
- **Forge is essentially a 1.20.1 story now.** NeoForge replaced it for 1.20.2 onward.
  Building Forge jars for 26.3 is mostly wasted effort; build NeoForge there instead.

So the realistic shipping matrix is six jars, not twelve: Fabric and Forge on 1.20.1,
Fabric and NeoForge on each of the newer three.

### Below 1.20.5

Item components did not exist yet, so the default `giveCommand` will not parse. Use the
NBT form in `config/jukeboxhits/config.json`:

```json
"giveCommand": "give %player% %disc%{display:{Name:%name%}}"
```

No Java changes needed.

### Where these numbers came from

The Minecraft, Java and **Fabric API** versions in `versions/*.properties` were read from
the upstream Git tags, so they are real. The **NeoForge and Forge** versions are `+`
ranges picked by convention and were *not* verified - this session could not reach their
Maven repositories. Pin them properly when you set up those builds.

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
