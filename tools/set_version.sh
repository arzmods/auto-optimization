#!/usr/bin/env bash
# Switch which Minecraft version the build targets.
#
#     tools/set_version.sh 26.3
#     tools/set_version.sh 1.20.1
#
# Then build as usual. One jar per Minecraft version - that is how Minecraft modding
# works, so shipping four versions means running this four times.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-}"

if [[ -z "$TARGET" ]]; then
    echo "usage: tools/set_version.sh <minecraft-version>" >&2
    echo >&2
    echo "available:" >&2
    for f in "$ROOT"/versions/*.properties; do
        name="$(basename "$f" .properties)"
        desc="$(head -1 "$f" | sed 's/^# *//')"
        printf "  %-10s %s\n" "$name" "$desc" >&2
    done
    exit 1
fi

SRC="$ROOT/versions/$TARGET.properties"
if [[ ! -f "$SRC" ]]; then
    echo "error: no version file for $TARGET" >&2
    echo "looked for: versions/$TARGET.properties" >&2
    exit 1
fi

# Everything below the marker is shared and must survive the swap.
SHARED="$(sed -n '/^# Shared across every target/,$p' "$ROOT/gradle.properties")"

{
    cat "$SRC"
    echo
    echo "$SHARED"
} > "$ROOT/gradle.properties"

echo "Now targeting Minecraft $TARGET:"
grep -E "^(minecraft_version|java_version|fabric_api_version|neoforge_version|forge_version)=" \
    "$ROOT/gradle.properties" | sed 's/^/  /'
echo
echo "Build with:  gradle build        (output in build/libs/)"
