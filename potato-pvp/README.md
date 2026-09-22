# Potato PvP

Minecraft runs badly on a lot of machines. This turns it down as far as it goes,
then lets you turn individual bits back up.

Out of the box: **no particles, no block textures, minimum animations.** There's
no setup step - install it and it's already on the lowest preset.

Press **Z** in game to change any of it.

This mod lives in the `potato-pvp/` folder of the `auto-optimization` repo. It
is completely separate from the Auto Optimization mod at the repo root and the
two do not affect each other. There are two builds here, one per loader:
`fabric/` and `neoforge/`. You only need one.

## The three settings

Each cycles None -> Minimum -> Medium. Nothing applies until you press Done, so
you can flick through without the game stuttering.

### Particles

* **None** - nothing spawns at all.
* **Minimum** - only what you fight with: crits, enchanted hits, damage
  indicators, sweep attacks, explosions, potion clouds, dragon breath, sonic
  boom, fishing bobber splash.
* **Medium** - nearly everything, minus the ambient drizzle: smoke, rain
  splashes, dripping water and lava, portal swirls, campfire smoke, villager
  hearts.

The mace smash and wind charge ground slam are dropped on None and Minimum, and
only come back on Medium.

### Textures

Block textures and end crystals are touched. Items, mobs, players and the
interface keep their real textures on every level.

* **None** - one flat colour per texture. This is the "no textures" look.
* **Minimum** - a 2x2 grid per texture. A hint of shape, nothing more.
* **Medium** - a 4x4 grid. Blurry but clearly recognisable.

### Animations

* **None** - every animated texture freezes on frame one, end crystals stop
  spinning and bobbing. View bobbing, entity shadows, glint movement, nausea
  warp, speed FOV stretch and damage tilt all off.
* **Minimum** - water, lava and fire still move, because you need to read them.
  Portals, sea lanterns and prismarine freeze. End crystals turn slowly.
* **Medium** - animations run normally, screen effects at half strength.

## Built for fighting, not just for frames

Most potato mods strip everything and leave the game unreadable. This one keeps
what you need:

* **Your hotbar stays sharp.** Telling a gapple from an ender pearl loses more
  fights than the frames win, so item textures are never touched.
* **Combat particles survive on Minimum.** You can still read a crit, a pot, an
  explosion.
* **Totem pops are cut to a flicker** - 2 particles instead of about 30. You
  keep the sound, so you always know it happened.
* **The mace ground slam is gone** on None and Minimum, so a smash attack does
  not fill your screen at the worst possible moment.
* **The menu never pauses the game.** You might be mid-fight.

## How the texture reduction works

Each texture is cut into a grid, and every cell becomes the average colour of
its pixels. Transparency is kept per pixel, so leaves, glass and iron bars keep
their shape instead of turning into solid cubes.

This happens while textures are decoded, so the saving is real GPU memory - not
a filter drawn over the top.

## Installing

1. Install Fabric or NeoForge for Minecraft 26.3
2. On **Fabric**, also put **Fabric API** in your mods folder
3. Drop the Potato PvP jar in `mods`

Settings save to `config/potatopvp.json` and can be edited by hand.

## Notes

Changing the Textures setting triggers a resource reload when you press Done.
That takes a few seconds and is normal.

Potato PvP writes to Minecraft's own video settings (view bobbing, entity
shadows, mipmap levels, particle level). That's the point of a preset, but your
vanilla video settings will change with it.

If Z does nothing, another mod may have taken the key - rebind it under
Options -> Controls -> Key Binds, in the Potato PvP category.

Client-side only. MIT licensed.

## Building it yourself

You need **JDK 25**.

```bash
cd potato-pvp/fabric      # or potato-pvp/neoforge
./gradlew build           # or: gradle build
```

The jar lands in `build/libs/`.

Every version number lives in `gradle.properties` in each loader folder. The
"Which versions exist" job in the Potato PvP Actions run prints the versions
that actually exist for a given Minecraft version, and the "Minecraft API
names" job reads real class and method names out of the Minecraft jar - both
are there to make a version bump a matter of fact rather than guesswork.
