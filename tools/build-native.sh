#!/usr/bin/env bash
# One-command development build of the REAL VeloWin graphical container app.
# Requires Android SDK 35, NDK 27.3.13750724, JDK 17, Rust target and Git LFS.
set -euo pipefail
cd "$(dirname "$0")/.."
git submodule update --init third_party/winnative
git -C third_party/winnative submodule update --init --recursive
test "$(git -C third_party/winnative rev-parse HEAD)" = "8409085f706acbc0749881d9bf1e85f2f347dfd4"
git -C third_party/winnative lfs pull
python3 tools/prepare-winnative.py --source third_party/winnative --root .
(
  cd third_party/winnative
  ./gradlew :app:assembleStandardDebug --no-daemon \
      -PVERSION_NAME=VeloWin-Modular-v15-dev
)
APK="third_party/winnative/app/build/outputs/apk/standard/debug/standard.apk"
test -s "$APK"
echo "Built: $APK"
echo "SHA-256: $(sha256sum "$APK" | awk '{print $1}')"
echo "This APK includes the native container runtime; game compatibility requires device testing."
