#!/usr/bin/env bash
# Convert a folder of audio files into the Ogg Vorbis files Minecraft can play.
#
#   tools/convert_to_ogg.sh ~/Music/for-minecraft
#
# Output lands in src/main/resources/assets/jukeboxhits/sounds/song/ as song_1.ogg,
# song_2.ogg, ... matching the slot codes in songs.json.
#
# Mono is used on purpose: Minecraft positions jukebox audio in 3D, and stereo files
# do not attenuate properly with distance.

set -euo pipefail

INPUT_DIR="${1:-}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT/src/main/resources/assets/jukeboxhits/sounds/song"
START_CODE="${2:-1}"

if [[ -z "$INPUT_DIR" || ! -d "$INPUT_DIR" ]]; then
    echo "usage: tools/convert_to_ogg.sh <folder-of-audio-files> [starting-code]" >&2
    exit 1
fi

if ! command -v ffmpeg >/dev/null 2>&1; then
    echo "error: ffmpeg is not installed." >&2
    echo "  macOS:   brew install ffmpeg" >&2
    echo "  Windows: winget install ffmpeg" >&2
    echo "  Linux:   sudo apt install ffmpeg" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"
code="$START_CODE"

shopt -s nullglob nocaseglob
for src in "$INPUT_DIR"/*.{mp3,wav,flac,m4a,aac,opus,ogg}; do
    [[ -e "$src" ]] || continue
    dest="$OUT_DIR/song_${code}.ogg"
    echo "[$code] $(basename "$src")"
    ffmpeg -loglevel error -y -i "$src" -c:a libvorbis -q:a 5 -ac 1 -ar 44100 "$dest"
    echo "      -> $(basename "$dest")  ($(du -h "$dest" | cut -f1))"
    code=$((code + 1))
done
shopt -u nullglob nocaseglob

converted=$((code - START_CODE))
if [[ "$converted" -eq 0 ]]; then
    echo "No audio files found in $INPUT_DIR" >&2
    exit 1
fi

echo
echo "Converted $converted file(s) into codes $START_CODE..$((code - 1))."
echo "Next: fill in the titles in src/main/resources/jukeboxhits/songs.json,"
echo "set enabled to true on those slots, then run: python3 tools/build_songs.py"
