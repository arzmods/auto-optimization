# Jukebox Hits — play your own songs on a Minecraft jukebox

Type `/song 1` in game, get a music disc, drop it in a jukebox, hear your song.
100 codes are wired up and ready. You supply the music.

---

## Which Minecraft versions and loaders

**Minecraft 1.21 or newer.** This mod adds songs through the `jukebox_song` registry,
which Mojang added in 1.21. On 1.20.6 and older it does not exist, so this approach
cannot work there — 1.20.1 would need a genuinely different mod, not a port.

| Loader | Status |
|---|---|
| **Fabric** | Built and ready — this repo |
| **NeoForge** (1.21+) | Source written, in `loaders/neoforge/`, needs a build |
| **Forge** (1.21+) | Source written, in `loaders/forge/`, needs a build |

The reason all three are cheap: the songs themselves are plain resource-pack and
datapack files, which every loader reads identically. Only the ~20-line entrypoint
differs. See [`loaders/README.md`](loaders/README.md).

---

## How it works (the short version)

Minecraft can only play `.ogg` audio that ships inside the game or a mod. So this mod
bakes your songs in at build time:

```
your song.mp3  →  song_1.ogg  →  baked into the mod  →  /song 1  →  disc  →  jukebox
```

Three files control everything:

| File | What it is |
|---|---|
| `src/main/resources/jukeboxhits/songs.json` | The 100 code slots. You edit this. |
| `src/main/resources/assets/jukeboxhits/sounds/song/` | Where the `.ogg` files live. |
| `tools/build_songs.py` | Bakes both into the files Minecraft reads. |

Everything under `assets/jukeboxhits/sounds.json`, `assets/jukeboxhits/lang/` and
`data/jukeboxhits/jukebox_song/` is **generated** — never edit those by hand, the
generator overwrites them.

---

## Step 1 — Install ffmpeg

This converts your music into the format Minecraft needs. One-time setup.

- **Windows:** open PowerShell, run `winget install ffmpeg`
- **Mac:** `brew install ffmpeg`
- **Linux:** `sudo apt install ffmpeg`

Check it worked: `ffmpeg -version` should print something.

## Step 2 — Put your music in a folder

Make a folder anywhere, e.g. `~/Music/for-minecraft`, and drop your audio in it.
`.mp3`, `.wav`, `.flac`, `.m4a`, `.aac` and `.opus` all work.

The order matters — files get codes in alphabetical order. Name them `01 - ...`,
`02 - ...` if you want a specific order.

## Step 3 — Convert them

On Mac or Linux:

```bash
tools/convert_to_ogg.sh ~/Music/for-minecraft
```

On Windows, use Git Bash to run that same line — or convert each song by hand:

```bash
ffmpeg -i "your song.mp3" -c:a libvorbis -q:a 5 -ac 1 -ar 44100 \
  src/main/resources/assets/jukeboxhits/sounds/song/song_1.ogg
```

This produces `song_1.ogg`, `song_2.ogg`, ... in the sounds folder.

> **Why mono (`-ac 1`)?** Minecraft positions jukebox audio in 3D. Stereo files don't
> fade properly as you walk away — they just play at full volume everywhere.

## Step 4 — Fill in the titles

Open `src/main/resources/jukeboxhits/songs.json`. Each line is one code:

```json
{"code": 1, "id": "song_1", "title": "Slot 1 - empty", "artist": "", "file": "song_1.ogg", "length_seconds": 180, "enabled": false}
```

Change `title`, `artist`, and flip `enabled` to `true`:

```json
{"code": 1, "id": "song_1", "title": "Midnight Drive", "artist": "Some Artist", "file": "song_1.ogg", "length_seconds": 180, "enabled": true}
```

Leave `id` and `code` alone. Ignore `length_seconds` — it gets measured from the
actual audio file automatically.

## Step 5 — Bake it in

```bash
python3 tools/build_songs.py
```

It prints exactly what got baked in:

```
baked in 2 song(s)
  /song 1    Some Artist - Midnight Drive  (3m24s)
  /song 2    Some Artist - Another Track   (2m51s)

98 slot(s) still empty (enabled: false)
```

If a slot is enabled but its `.ogg` is missing, it tells you which file it expected.

## Step 6 — Build the mod

```bash
./gradlew build
```

There is no Gradle wrapper committed yet. If `./gradlew` is missing, either generate it
once with `gradle wrapper` (needs Gradle installed), or just run `gradle build`.

The finished `.jar` lands in `build/libs/`. Drop it in your `mods` folder along with
**Fabric API**.

> **Heads up:** `build.gradle` asks for Java 25. If your JDK is older, either install
> Java 25 or lower the two `VERSION_25` lines and `options.release`.

## Step 7 — Play

In game:

```
/song 1          get the disc for code 1
/song list       see every installed code
/song list 2     page 2
/song search mid find a code by title or artist
/song reload     re-read the config file
```

Put the disc in a jukebox. Done.

---

## Adding more songs later

Repeat steps 3–6. Adding a song means rebuilding the mod — that's the tradeoff of the
simple approach. If you want people to add songs without rebuilding, that's the
upload-based version (see *Where this could go* below).

---

## Settings

First launch writes `config/jukeboxhits.json`:

```json
{
  "discItem": "minecraft:music_disc_13",
  "giveCommand": "give %player% %disc%[minecraft:jukebox_playable={song:\"%song%\"}]",
  "allowAllPlayers": false
}
```

- **discItem** — which disc item you get. Any music disc works.
- **giveCommand** — how the disc is built. Vanilla parses this string, so if a
  Minecraft update changes the component syntax it's a one-line fix here, no recompile.
- **allowAllPlayers** — `false` means only operators can run `/song`. Set `true` to let
  everyone use it.

Run `/song reload` after editing.

---

## If something goes wrong

**`/song 1` says "Could not build the disc"**
The component syntax doesn't match your Minecraft version. Open
`config/jukeboxhits.json` and change `giveCommand` to the alternate form:

```json
"giveCommand": "give %player% %disc%[minecraft:jukebox_playable=\"%song%\"]"
```

Then `/song reload`.

**The game logs a datapack error about `sound_event`**
Open the generated files in `src/main/resources/data/jukeboxhits/jukebox_song/` and
change:

```json
"sound_event": { "sound_id": "jukeboxhits:song_1" }
```

to the plain string form:

```json
"sound_event": "jukeboxhits:song_1"
```

If that's what your version wants, edit the same line in `tools/build_songs.py` so
future runs generate it correctly.

**The disc goes in but there's no sound**
The `.ogg` isn't where the generator expects. Re-run `python3 tools/build_songs.py` —
it lists any missing files.

**Volume doesn't fade as you walk away**
The file is stereo. Re-convert it with `-ac 1`.

**`/song` isn't recognised at all**
Fabric API isn't installed, or the mod didn't load. Check the log for
`Jukebox Hits ready with N song(s)`.

---

## About shipping this

**Do not distribute the mod with commercial music baked in.** A jar containing chart
songs is distributing copyrighted recordings — it will get taken down and it puts you
personally at risk. This is exactly why Custom Discs and AudioPlayer make users supply
their own files.

What you *can* ship:

- The mod with **zero songs**, and let people add their own (steps 2–6 above).
- The mod with music you made, or music that's actually licensed for it:
  - [Free Music Archive](https://freemusicarchive.org) — filter by CC licence
  - [Incompetech](https://incompetech.com) — Kevin MacLeod, CC-BY
  - [ccMixter](http://dig.ccmixter.org)
  - [Pixabay Music](https://pixabay.com/music/)
  - YouTube Audio Library (in YouTube Studio)

Check each track's licence. Most CC licences just require crediting the artist — put
them in the `artist` field and they show up on the disc tooltip.

---

## Where this could go

This is the simple version on purpose. The upgrade path, roughly in order of effort:

1. **Load songs from a folder at runtime** instead of baking them in — no rebuild to add
   music, but needs dynamic sound registration.
2. **Upload from in game** — `/song upload <url>`, server stores the file, hands back a
   code. This is the real Roblox model.
3. **Streaming via Simple Voice Chat** — what AudioPlayer and Custom Discs do. Removes
   the resource-pack limit entirely and gives proper positional audio.

Step 2 is where it stops being a personal jukebox and starts being something other
servers would install.

## A note on packaging

Right now this lives in the same jar as Hardware Scaler (a second `main` entrypoint in
`fabric.mod.json`). That's fine for testing. If you want to release it, move
`src/main/java/com/example/jukeboxhits/`, `src/main/resources/jukeboxhits/`,
`assets/jukeboxhits/` and `data/jukeboxhits/` into a fresh project with its own mod id —
nobody installing a jukebox wants a render-distance tweaker bundled in.

The code is already split for that: `core/` has no loader imports at all, and each
loader's entrypoint is a separate ~20-line class. [`loaders/README.md`](loaders/README.md)
covers building for all three at once.
