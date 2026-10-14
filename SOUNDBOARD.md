# SoundboardMC

Play songs from your own computer into Simple Voice Chat. You hear them, and so does
everyone near you who hears your voice.

This is a **client-side** mod: it goes in your own `mods` folder. The server only needs
Simple Voice Chat, as usual.

## Requirements

| | |
|---|---|
| **Minecraft** | 26.3 · 1.20.1 |
| **Loader** | Fabric, with Fabric API |
| **Required mod** | **Simple Voice Chat**, on your game *and* the server |

## Using it

1. Start the game once. A `soundboard` folder appears next to your `mods` folder.
2. Copy `.mp3` or `.wav` files into it.
3. Join a world or server with Simple Voice Chat, and finish its setup (press V) if
   you have not already.
4. Press **J** to open the soundboard. Click a song to play it, **Stop** to stop.
   **Open folder** opens the soundboard folder.

The key can be changed under Options > Controls > Key Binds > Miscellaneous.

## Good to know

- Songs play even while you are not talking and even when your microphone is muted.
  They do not play while voice chat is disabled.
- Simple Voice Chat needs a microphone to be selected and working, because the song
  travels inside your microphone stream.
- One song plays at a time. Clicking another song switches to it.
- Songs longer than 15 minutes are refused for .wav files, to keep memory use low.

## Building

```bash
tools/set_version.sh 26.3      # or 1.20.1
./gradlew build                # jar lands in build/libs/
tools/run_tests.sh             # audio conversion tests, no Minecraft needed
```
