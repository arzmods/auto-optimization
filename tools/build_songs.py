#!/usr/bin/env python3
"""
Bake the songs listed in songs.json into the resource/data files Minecraft reads.

Run this after adding or changing a song, then rebuild the mod:

    python3 tools/build_songs.py

It reads   src/main/resources/jukeboxhits/songs.json
and writes src/main/resources/assets/jukeboxhits/sounds.json
           src/main/resources/assets/jukeboxhits/lang/en_us.json
           src/main/resources/data/jukeboxhits/jukebox_song/<id>.json

A slot is only baked in if enabled is true AND its .ogg actually exists.
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NAMESPACE = "jukeboxhits"

SONGS_JSON = ROOT / "src/main/resources" / NAMESPACE / "songs.json"
OGG_DIR = ROOT / "src/main/resources/assets" / NAMESPACE / "sounds/song"
SOUNDS_JSON = ROOT / "src/main/resources/assets" / NAMESPACE / "sounds.json"
LANG_JSON = ROOT / "src/main/resources/assets" / NAMESPACE / "lang/en_us.json"
JUKEBOX_SONG_DIR = ROOT / "src/main/resources/data" / NAMESPACE / "jukebox_song"


def ogg_duration_seconds(path: Path):
    """Read an Ogg Vorbis file's length without any third-party library.

    Sample rate comes from the Vorbis identification header; total samples come
    from the granule position on the final Ogg page. Returns None if either is
    unreadable, in which case the length from songs.json is used instead.
    """
    try:
        data = path.read_bytes()
    except OSError:
        return None

    head = data.find(b"\x01vorbis")
    if head < 0:
        return None
    sample_rate = int.from_bytes(data[head + 12:head + 16], "little")
    if sample_rate <= 0:
        return None

    last_page = data.rfind(b"OggS")
    if last_page < 0:
        return None
    granule = int.from_bytes(data[last_page + 6:last_page + 14], "little", signed=True)
    if granule <= 0:
        return None

    return granule / sample_rate


def main() -> int:
    if not SONGS_JSON.exists():
        print(f"error: {SONGS_JSON} not found", file=sys.stderr)
        return 1

    catalog = json.loads(SONGS_JSON.read_text(encoding="utf-8"))
    songs = catalog.get("songs", [])

    sounds = {}
    lang = {}
    baked = []
    skipped_disabled = 0
    missing = []
    seen_codes = {}

    for song in songs:
        song_id = song.get("id")
        code = song.get("code")

        if not song.get("enabled"):
            skipped_disabled += 1
            continue
        if not song_id:
            print(f"warning: slot with code {code} has no id, skipping", file=sys.stderr)
            continue
        if code in seen_codes:
            print(f"error: code {code} is used by both '{seen_codes[code]}' and '{song_id}'",
                  file=sys.stderr)
            return 1
        seen_codes[code] = song_id

        ogg = OGG_DIR / song.get("file", f"{song_id}.ogg")
        if not ogg.exists():
            missing.append((code, song_id, ogg.name))
            continue

        detected = ogg_duration_seconds(ogg)
        length = round(detected, 2) if detected else float(song.get("length_seconds", 180))

        # Sound event: jukeboxhits:song_<n>  ->  assets/jukeboxhits/sounds/song/<file>.ogg
        sounds[song_id] = {
            "sounds": [{"name": f"{NAMESPACE}:song/{ogg.stem}", "stream": True}]
        }

        title = song.get("title") or song_id
        artist = song.get("artist") or ""
        lang[f"{NAMESPACE}.song.{song_id}"] = f"{artist} - {title}" if artist else title

        # Comparator output has to land in 1..15, like vanilla discs.
        comparator = ((int(code) - 1) % 15) + 1

        JUKEBOX_SONG_DIR.mkdir(parents=True, exist_ok=True)
        (JUKEBOX_SONG_DIR / f"{song_id}.json").write_text(
            json.dumps({
                # Inline form needs no SoundEvent registered from Java. If your Minecraft
                # version rejects it, use the plain string "jukeboxhits:<id>" instead.
                "sound_event": {"sound_id": f"{NAMESPACE}:{song_id}"},
                "description": {"translate": f"{NAMESPACE}.song.{song_id}"},
                "length_in_seconds": length,
                "comparator_output": comparator,
            }, indent=2) + "\n",
            encoding="utf-8",
        )
        baked.append((code, song_id, lang[f"{NAMESPACE}.song.{song_id}"], length))

    # Drop jukebox_song files for slots that are no longer baked in.
    JUKEBOX_SONG_DIR.mkdir(parents=True, exist_ok=True)
    keep = {f"{song_id}.json" for _, song_id, _, _ in baked}
    for stale in JUKEBOX_SONG_DIR.glob("*.json"):
        if stale.name not in keep:
            stale.unlink()
            print(f"removed stale {stale.name}")

    SOUNDS_JSON.parent.mkdir(parents=True, exist_ok=True)
    SOUNDS_JSON.write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")

    LANG_JSON.parent.mkdir(parents=True, exist_ok=True)
    LANG_JSON.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"baked in {len(baked)} song(s)")
    for code, song_id, label, length in baked:
        print(f"  /song {code:<4} {label}  ({int(length // 60)}m{int(length % 60):02d}s)")

    if missing:
        print(f"\n{len(missing)} slot(s) enabled but the .ogg is missing:")
        for code, song_id, filename in missing:
            print(f"  code {code}: expected {OGG_DIR.relative_to(ROOT)}/{filename}")

    if skipped_disabled:
        print(f"\n{skipped_disabled} slot(s) still empty (enabled: false)")

    if not baked:
        print("\nNothing baked in yet - that is fine, the mod loads with zero songs.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
