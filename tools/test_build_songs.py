#!/usr/bin/env python3
"""Self-test for build_songs.py. Run: python3 tools/test_build_songs.py"""

import importlib.util
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

spec = importlib.util.spec_from_file_location("build_songs", ROOT / "tools/build_songs.py")
build_songs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build_songs)

failures = []


def check(name, condition, detail=""):
    if condition:
        print(f"  PASS  {name}")
    else:
        print(f"  FAIL  {name}  {detail}")
        failures.append(name)


def synthetic_ogg(sample_rate: int, total_samples: int) -> bytes:
    """Minimal Ogg Vorbis byte layout: an identification header page and a final page."""
    ident_payload = (
        b"\x01vorbis"
        + (0).to_bytes(4, "little")      # vorbis version
        + bytes([1])                     # channels
        + sample_rate.to_bytes(4, "little")
        + b"\x00" * 16
    )
    first_page = (
        b"OggS" + bytes([0, 2])
        + (0).to_bytes(8, "little")      # granule position
        + b"\x00" * 12                   # serial, sequence, checksum
        + bytes([1, len(ident_payload)])
        + ident_payload
    )
    last_page = (
        b"OggS" + bytes([0, 4])
        + total_samples.to_bytes(8, "little")
        + b"\x00" * 13
    )
    return first_page + last_page


print("ogg_duration_seconds")
with tempfile.TemporaryDirectory() as tmp:
    ogg = Path(tmp) / "t.ogg"

    ogg.write_bytes(synthetic_ogg(44100, 44100 * 210))
    check("reads 210s at 44.1kHz", build_songs.ogg_duration_seconds(ogg) == 210.0,
          f"got {build_songs.ogg_duration_seconds(ogg)}")

    ogg.write_bytes(synthetic_ogg(48000, 48000 * 95))
    check("reads 95s at 48kHz", build_songs.ogg_duration_seconds(ogg) == 95.0,
          f"got {build_songs.ogg_duration_seconds(ogg)}")

    ogg.write_bytes(b"this is not an ogg file at all")
    check("returns None on garbage", build_songs.ogg_duration_seconds(ogg) is None)

    ogg.write_bytes(synthetic_ogg(0, 1000))
    check("returns None on zero sample rate", build_songs.ogg_duration_seconds(ogg) is None)

print("\nfull generator run")
backup = tempfile.mkdtemp()
targets = [
    ROOT / "src/main/resources/jukeboxhits/songs.json",
    ROOT / "src/main/resources/assets/jukeboxhits/sounds.json",
    ROOT / "src/main/resources/assets/jukeboxhits/lang/en_us.json",
]
for t in targets:
    if t.exists():
        shutil.copy2(t, Path(backup) / t.name)
song_dir = ROOT / "src/main/resources/assets/jukeboxhits/sounds/song"
jukebox_dir = ROOT / "src/main/resources/data/jukeboxhits/jukebox_song"
test_ogg = song_dir / "song_1.ogg"

try:
    song_dir.mkdir(parents=True, exist_ok=True)
    test_ogg.write_bytes(synthetic_ogg(44100, 44100 * 184))

    catalog_path = targets[0]
    catalog = json.loads(catalog_path.read_text())
    catalog["songs"][0].update({"title": "Test Tone", "artist": "Self Test", "enabled": True})
    catalog_path.write_text(json.dumps(catalog, indent=2))

    result = subprocess.run([sys.executable, str(ROOT / "tools/build_songs.py")],
                            capture_output=True, text=True)
    check("generator exits 0", result.returncode == 0, result.stderr.strip())
    check("reports the baked song", "/song 1" in result.stdout, result.stdout.strip())

    sounds = json.loads(targets[1].read_text())
    check("sounds.json has the entry", "song_1" in sounds)
    check("sound streams from disk",
          sounds.get("song_1", {}).get("sounds", [{}])[0].get("stream") is True)
    check("sound points at the ogg",
          sounds.get("song_1", {}).get("sounds", [{}])[0].get("name") == "jukeboxhits:song/song_1")

    lang = json.loads(targets[2].read_text())
    check("lang entry reads nicely", lang.get("jukeboxhits.song.song_1") == "Self Test - Test Tone",
          lang.get("jukeboxhits.song.song_1"))

    jukebox = json.loads((jukebox_dir / "song_1.json").read_text())
    check("length auto-detected from the ogg", jukebox.get("length_in_seconds") == 184.0,
          f"got {jukebox.get('length_in_seconds')}")
    check("sound_event wired", jukebox.get("sound_event") == {"sound_id": "jukeboxhits:song_1"})
    check("comparator_output in 1..15", 1 <= jukebox.get("comparator_output", 0) <= 15)
    check("description is translatable",
          jukebox.get("description") == {"translate": "jukeboxhits.song.song_1"})

    # Disabling the slot again must clean the generated file back up.
    catalog["songs"][0]["enabled"] = False
    catalog_path.write_text(json.dumps(catalog, indent=2))
    subprocess.run([sys.executable, str(ROOT / "tools/build_songs.py")], capture_output=True, text=True)
    check("stale jukebox_song file removed", not (jukebox_dir / "song_1.json").exists())
finally:
    test_ogg.unlink(missing_ok=True)
    for t in targets:
        saved = Path(backup) / t.name
        if saved.exists():
            shutil.copy2(saved, t)
    shutil.rmtree(backup, ignore_errors=True)

print()
if failures:
    print(f"{len(failures)} check(s) failed: {', '.join(failures)}")
    sys.exit(1)
print("all checks passed")
