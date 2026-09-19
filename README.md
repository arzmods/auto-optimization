# Hardware Scaler

Detects your system hardware at startup and applies a tiered performance preset.

Supported: **Minecraft 26.3** on **Fabric** and **NeoForge**. Client-side only.

## How it works

On startup the mod reads your CPU core count, total RAM and GPU (via OSHI),
scores them, and picks a tier:

| Tier   | Render distance |
|--------|-----------------|
| LOW    | 6               |
| MEDIUM | 10              |
| HIGH   | 16              |

## Layout

| Folder      | What it is                                                         |
|-------------|--------------------------------------------------------------------|
| `common/`   | Hardware detection and preset logic. Shared — compiled into both jars. |
| `fabric/`   | Fabric entrypoint (`HardwareScalerClient`) + `fabric.mod.json`      |
| `neoforge/` | NeoForge entrypoint + `neoforge.mods.toml`                          |

Version numbers (Minecraft, Fabric API, NeoForge, OSHI, plugins) all live in `gradle.properties`.

## Building

```bash
gradle wrapper --gradle-version 9.6.0   # first time only
./gradlew build
```

Output jars:

* `fabric/build/libs/auto-optimization-fabric-1.0.0.jar`
* `neoforge/build/libs/auto-optimization-neoforge-1.0.0.jar`

OSHI and JNA are bundled inside both jars, so there is nothing extra to install.
The Fabric build additionally needs **Fabric API** in your `mods` folder.

## Running in dev

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```
