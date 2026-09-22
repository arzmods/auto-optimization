# Potato PvP

A potato graphics mod for Minecraft **26.3**, built for machines that cannot
afford to be pretty. It ships in its lightest state on purpose:

| Setting    | Default   |
| ---------- | --------- |
| Particles  | **None**  |
| Textures   | **None**  |
| Animations | **Minimum** |

Press **Z** in game to change any of the three.

This mod lives in the `potato-pvp/` folder of the `auto-optimization` repo.
It is completely separate from the Auto Optimization mod at the repo root and
the two do not affect each other.

There are two builds here, one for each mod loader:

- `fabric/` -> for **Fabric**
- `neoforge/` -> for **NeoForge**

You only need one of them. They behave identically because they compile the
same code from `common/`.

---

## 1. Installing it

1. Install the loader you use (Fabric or NeoForge) for Minecraft 26.3.
2. If you are on **Fabric**, also download **Fabric API** and drop it in `mods`.
   NeoForge does not need anything extra.
3. Drop the Potato PvP `.jar` into your `mods` folder.
4. Start the game.

Where the `mods` folder is:

- Windows: press `Win + R`, type `%appdata%\.minecraft`, press Enter
- macOS: `~/Library/Application Support/minecraft`
- Linux: `~/.minecraft`

Getting the jar: every push that touches `potato-pvp/` builds both versions on
GitHub Actions. Open the **Actions** tab, click the newest **Potato PvP** run,
and grab `potato-pvp-fabric` or `potato-pvp-neoforge` from the **Artifacts** box
at the bottom. Or build it yourself, see section 4.

---

## 2. The settings menu (press Z)

Z opens a small screen with three rows. Each row is one button you click to
cycle through `None -> Minimum -> Medium -> None`.

Nothing is applied while you are clicking. Everything happens when you press
**Done**, so you can flick through the options without the game stuttering.

There is also a **Reset to Potato preset** button that puts all three back to
the defaults in the table above.

Your choices are saved to `config/potatopvp.json` and are still there next time
you launch. You can edit that file by hand if you prefer.

### Particles

| Level | What you get |
| ----- | ------------ |
| **None** | No particles at all. Nothing spawns. |
| **Minimum** | Only the particles you actually fight with: crits, enchanted hits, damage indicators, sweep attacks, totem pops, explosions, potion clouds, dragon breath, wind burst and gust, sonic boom, fishing bobber splash. Everything else is dropped. Totem pops are additionally capped, see below. |
| **Medium** | Nearly everything, minus the constant ambient drizzle - smoke, rain splashes, dripping water and lava, mycelium spores, portal swirls, campfire smoke, villager hearts, note blocks and so on. |

#### Totem pops

A vanilla totem pop throws out about thirty particles at once. On **None** and
**Minimum** only the first **two** of the burst get through and the rest are
binned, so you get a small flicker instead of a faceful.

You keep the sound, so you always know a pop happened and can keep swinging.
On **Medium** the pop is vanilla.

The other half of a pop is the full screen spinning totem. Suppressing that is
not currently possible: the hook it used to live on,
`GameRenderer.displayItemActivation`, no longer exists in 26.3 and the
replacement has not been located yet.

### Textures

This one reduces **block textures only**. Items, mobs, players, armour and the
whole interface keep their real textures on every level - being unable to tell
a gapple from an ender pearl in your hotbar would lose you more fights than the
frames would win.

Each block texture is cut into a grid and every cell is replaced by its average
colour. Transparency is kept per pixel, so leaves, glass and iron bars still
have their shape.

| Level | What you get |
| ----- | ------------ |
| **None** | One flat colour per texture. This is the "no textures" look - the world becomes flat coloured blocks. |
| **Minimum** | A 2x2 grid per texture. Barely any detail, just a hint of shape. |
| **Medium** | A 4x4 grid per texture. Blurry but clearly recognisable. |

Changing this setting triggers a resource reload when you press Done, because
the reduction happens while textures are being decoded. That takes a few
seconds and is normal.

### Animations

| Level | What you get |
| ----- | ------------ |
| **None** | Every animated block texture is frozen on its first frame - water, lava, fire, portals, sea lanterns, prismarine, magma, conduits. View bobbing, entity shadows, enchantment glint movement, the nausea warp, the speed FOV stretch and the damage tilt are all off. |
| **Minimum** | Water, lava and fire still animate because you need to read them; everything else freezes. Bobbing, shadows and screen effects stay off. |
| **Medium** | All texture animations run normally. Bobbing, shadows and screen effects come back at half strength. |

---

## 3. How it works, briefly

Everything shared lives in `potato-pvp/common/src/main/java/com/arzmods/potatopvp/`:

| File | Job |
| ---- | --- |
| `QualityLevel.java` | The `None / Minimum / Medium` enum. |
| `PotatoConfig.java` | Loads and saves `config/potatopvp.json`. |
| `client/PotatoOptionsScreen.java` | The screen Z opens. |
| `client/PotatoKeys.java` | The Z key itself. |
| `client/ParticleFilter.java` | Decides if a particle is allowed to spawn. |
| `client/PotatoTextures.java` | Averages the pixels of block textures. |
| `client/PotatoAnimations.java` | Decides if a texture may animate. |
| `client/PotatoOptions.java` | Pushes the levels into Minecraft's video options. |
| `mixin/ParticleEngineMixin.java` | Hooks the particle engine. |
| `mixin/SpriteContentsMixin.java` | Hooks texture loading and animation. |

Each loader folder only holds its entrypoint and its metadata file.

**Worth knowing:** Potato PvP writes to Minecraft's own video settings (view
bobbing, entity shadows, mipmap levels, graphics mode, glint speed, particle
level). That is deliberate, it is what a preset is, but it does mean your
vanilla video settings will change when you change a Potato PvP setting.

---

## 4. Building it yourself

You need **JDK 25**. Check with `java -version`.

```bash
# Fabric
cd potato-pvp/fabric
./gradlew build      # or: gradle build

# NeoForge
cd potato-pvp/neoforge
./gradlew build      # or: gradle build
```

The finished jar lands in `potato-pvp/fabric/build/libs/` or
`potato-pvp/neoforge/build/libs/`. Ignore any file ending in
`-sources.jar`, and on the Fabric side ignore `-dev.jar` too - the one you
want is the plain `potato-pvp-fabric-1.0.0.jar`.

The first build downloads Minecraft and the loader and takes a few minutes.
Later builds are fast.

---

## 5. Changing Minecraft version

Every version number lives in one file per loader, nothing is hardcoded in the
Java:

- `potato-pvp/fabric/gradle.properties` -> `minecraft_version`, `loader_version`,
  `fabric_version`, `loom_version`, `intermediary_version`
- `potato-pvp/neoforge/gradle.properties` -> `minecraft_version`, `neoforge_version`,
  `moddev_version`

If a build fails with "could not find ... version", those are the lines to
edit. The **"Which versions exist"** job in the Potato PvP Actions run prints every
version number that is actually available, so you can copy the right one
straight out of the log.

Also update `"minecraft": "~26.3"` in
`potato-pvp/fabric/src/main/resources/fabric.mod.json` and
`versionRange = "[26.3,)"` in
`potato-pvp/neoforge/src/main/resources/META-INF/neoforge.mods.toml`.

---

## 6. Known limitations

**Lightly tested.** 1.0.0 did not boot: freezing animations by reporting
sprites as not animated corrupted the texture atlas upload, because 26.3 sizes
that upload from exactly that flag. 1.0.1 freezes animations by rewriting
pixels instead, which cannot affect it. Beyond "it starts", the three settings
have had little real play testing - if one appears to do nothing, check
`latest.log` for lines starting with `[Potato PvP]`.

**The full screen totem animation still plays.** Particles from a pop are
capped to two, but the giant spinning totem cannot be suppressed: the hook it
used to live on, `GameRenderer.displayItemActivation`, does not exist in 26.3
and no replacement has been found.

**The settings screen has no title text.** 26.3 replaced
`render(GuiGraphics, ...)` with a render-state extraction model, and a
decorative heading was not worth reaching into that for. Every button is
labelled with the setting it controls.

**Mappings.** Minecraft 26.x ships deobfuscated - its version manifest has no
`client_mappings` download and Fabric publishes no yarn for 26.3 - so the
Fabric build uses intermediary and produces no sources jar. This is why the
class names in the source are real names rather than obfuscated ones.

**Two settings need a reload to take full effect.** Textures triggers a
resource reload when you press Done. Mipmap level changes made on first launch
may need a restart.

---

## 7. If something does not work

**Z does nothing.** Another mod may have taken the key. Open
`Options -> Controls -> Key Binds`, find the **Potato PvP** category, and pick a
free key. If the category is not listed at all, check `latest.log` for a
`[Potato PvP] Could not find the key mapping list` warning.

**The menu opens but nothing changes.** Check your log
(`.minecraft/logs/latest.log`) for lines starting with `[Potato PvP]`. If you
see a warning about `NativeImage pixel accessors` or `the sprite image field`,
the Textures setting could not attach on your Minecraft version - open an issue
with the version you are on.

**Textures look normal after changing the setting.** Press Done rather than
Escape, and give the resource reload a few seconds to finish.

**The game crashes on startup.** Remove the jar, start the game once to confirm
it is this mod, then open an issue with `latest.log` attached.

---

## Licence

MIT, see `LICENSE`.
