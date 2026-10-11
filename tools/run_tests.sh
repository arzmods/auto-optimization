#!/usr/bin/env bash
# Compiles and runs the standalone tests.
#
# These cover the pure-Java parts (audio conversion, disc tagging) and need no Minecraft,
# no Gradle and no network - so they run anywhere a JDK is installed:
#
#     tools/run_tests.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

echo "compiling..."
javac -d "$OUT" \
    "$ROOT/src/main/java/com/example/jukeboxhits/core/audio/Pcm.java" \
    "$ROOT/src/main/java/com/example/jukeboxhits/core/audio/AudioDecoder.java" \
    "$ROOT/src/main/java/com/example/jukeboxhits/core/DiscTag.java" \
    "$ROOT/src/test/java/com/example/jukeboxhits/core/audio/PcmTest.java" \
    "$ROOT/src/test/java/com/example/jukeboxhits/core/audio/AudioDecoderTest.java" \
    "$ROOT/src/test/java/com/example/jukeboxhits/core/DiscTagTest.java"

failed=0
for test in \
    com.example.jukeboxhits.core.audio.PcmTest \
    com.example.jukeboxhits.core.audio.AudioDecoderTest \
    com.example.jukeboxhits.core.DiscTagTest
do
    echo
    echo "=== $test ==="
    java -cp "$OUT" "$test" || failed=1
done

echo
if [ "$failed" -ne 0 ]; then
    echo "SOME TESTS FAILED"
    exit 1
fi
echo "ALL TESTS PASSED"
