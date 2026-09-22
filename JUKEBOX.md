# Jukebox Hits — upload songs, get a code, play it on a jukebox

```
/song upload https://example.com/track.mp3 Midnight Drive
  -> Added as code 1

/song 1          -> you get a disc
put it in a jukebox -> it plays, for everyone nearby, in 3D
```

Short numeric codes, like Roblox. No rebuilding, no resource packs, no restarts.
Songs are uploaded once and live on the server.

---

## Requirements

| | |
|---|---|
| **Minecraft** | Anything Simple Voice Chat supports, including 1.20.1 |
| **Loader** | Fabric (built) · NeoForge / Forge (source in `loaders/`) |
| **Required mod** | **Simple Voice Chat** — server *and* every client |

On **1.20.4 and older**, item components did not exist yet, so set `giveCommand` in the
config to the older NBT form: `give %player% %disc%{display:{Name:%name%}}`

**Why Simple Voice Chat is mandatory:** vanilla Minecraft can only play audio that shipped
with the game or a resource pack. It has no way to play a file it has never seen. Simple
Voice Chat already holds a live audio connection to every client, so this mod streams your
song down that pipe. AudioPlayer and Custom Discs work the same way — there is no way
around it.

## Install

1. Install **Fabric Loader** from [fabricmc.net/use](https://fabricmc.net/use).
2. Put these in your `mods` folder, matching your Minecraft version:
   - Fabric API
   - **Simple Voice Chat**
   - this mod
3. On a server, the server needs all three too. Every player needs Simple Voice Chat.

## Using it

### Add a song

```
/song upload <direct-url> <name>
```

The URL has to be a **direct link to the file**, ending in `.mp3` or `.wav` — not a
YouTube or Spotify page. The server downloads it, decodes it, and tells you its code.

Redirects are rejected on purpose, so "share links" from cloud drives usually won't work;
use the direct-download form of the link.

### Play it

```
/song 1           get a disc for code 1, then put it in a jukebox
/song play 1      play where you stand, no disc needed
/song stop        stop everything
```

Right-click the jukebox with an empty hand to stop it.

### Browse

```
/song list        every code
/song list 2      page 2
/song search mid  find a code by name
/song remove 1    delete a song and its file
/song reload      re-read the config
```

## Settings

`config/jukeboxhits/config.json`, created on first launch:

```json
{
  "maxUploadMb": 20,
  "maxSongs": 500,
  "playbackDistance": 48.0,
  "gain": 1.0,
  "allowAllPlayersUpload": false,
  "allowAllPlayersDisc": true,
  "discItem": "minecraft:music_disc_13",
  "giveCommand": "give %player% %disc%[minecraft:custom_name=%name%]"
}
```

- **allowAllPlayersUpload** — `false` means only operators can upload or delete. Turn this
  on only if you trust everyone on the server; uploads use disk and bandwidth.
- **allowAllPlayersDisc** — anyone can get a disc for a song that already exists.
- **playbackDistance** — how far the music carries, in blocks.
- **giveCommand** — vanilla parses this, so a component-syntax change in a future
  Minecraft version is a config edit rather than a recompile.

Songs live in `config/jukeboxhits/songs/`, and the code table is `songs.json` next to it.

## How the pieces fit

```
/song upload  ->  AudioStore downloads the file
                       |
                  AudioLoader decodes it   (MP3 via Simple Voice Chat, WAV via the JDK)
                       |
                  Pcm converts to 48kHz mono, 20ms frames
                       |
/song 1       ->  disc named "♪#1 Midnight Drive"
                       |
right-click a jukebox  ->  PlaybackManager opens a positional audio channel there
```

The code travels in the **disc's display name**, not in NBT. Display names are one of the
few item APIs that have been stable across Minecraft versions, so discs keep working
across updates.

## If something goes wrong

**"Simple Voice Chat is not running on this server"**
It isn't installed, or it failed to start. Check the server log for `Connected to Simple
Voice Chat` at startup.

**"That link redirects"**
Use the direct file URL. Cloud-drive share pages redirect; their direct-download links
usually don't.

**"That file downloaded but would not decode"**
Not real audio, or an MP3 variant the decoder rejects. Convert it to `.wav` and re-upload:
`ffmpeg -i in.mp3 -c:a pcm_s16le -ac 1 -ar 48000 out.wav`

**Playing the disc does nothing**
You do not have Simple Voice Chat installed *on your client*, or voice chat is muted.

**Volume doesn't fade with distance**
It should — audio is converted to mono and positioned at the jukebox. If it doesn't,
Simple Voice Chat is probably not the one playing it.

**"Could not build the disc"**
Your Minecraft version wants different component syntax. Edit `giveCommand` in the config,
then `/song reload`.

## Tests

```bash
tools/run_tests.sh
```

Covers audio conversion (channel mixing, resampling, framing, gain clipping), WAV
decoding end to end, and disc-code encoding. Pure Java — no Minecraft, no Gradle, no
network needed.

## About what you upload

Uploading music you don't own, to a server other people can hear, is a public
performance — different from keeping a file on your own machine. On a private server with
friends, realistically nobody cares. On a public one, it's your exposure, not the mod's.

If you distribute the mod, it ships with **no music in it** — the library starts empty and
each server fills its own. That's deliberate, and it's what keeps the mod itself
distributable. Sources for music that's actually free to use:

- [freepd.com](https://freepd.com) — CC0, no attribution needed
- [OpenGameArt](https://opengameart.org) — filter to CC0, written for games
- [Musopen](https://musopen.org) — public domain classical
- [Incompetech](https://incompetech.com) / [ccMixter](http://dig.ccmixter.org) — CC-BY, credit the artist

⚠️ "No Copyright Music" and **NCS** are misleading names — those tracks *are* copyrighted,
just licensed for use with credit. Read the licence, not the channel name.
