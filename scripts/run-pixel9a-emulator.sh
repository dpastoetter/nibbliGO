#!/usr/bin/env bash
# Run nibbliGO on the Pixel 9a AVD (pixel_9 device profile; SDK has no pixel_9a id yet).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

AVD_NAME="Pixel_9a_API_35"
PACKAGE="com.nibbli.nibbligo"

if ! avdmanager list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
  echo "Creating $AVD_NAME..."
  echo no | avdmanager create avd -n "$AVD_NAME" \
    -k "system-images;android-35;google_apis;x86_64" \
    -d pixel_9 --force
fi

if ! adb devices | grep -q "device$"; then
  echo "Starting emulator (cold boot, 4GB RAM, KVM)..."
  GPU_FLAGS=(-gpu host)
  if ! emulator -help 2>&1 | grep -q host; then
    GPU_FLAGS=(-gpu swiftshader_indirect)
  fi
  emulator -avd "$AVD_NAME" \
    -no-snapshot-load \
    -no-snapshot-save \
    -memory 4096 \
    -accel on \
    "${GPU_FLAGS[@]}" \
    -no-audio \
    > /tmp/pixel9a-emulator.log 2>&1 &
  EMU_PID=$!
  echo "Emulator PID: $EMU_PID (log: /tmp/pixel9a-emulator.log)"

  adb wait-for-device
  echo "Waiting for Android to boot..."
  for _ in $(seq 1 120); do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      break
    fi
    sleep 2
  done
else
  echo "Device already connected."
fi

echo "Building and installing nibbliGO..."
cd "$ROOT"
./gradlew :app:installDebug -q

echo "Launching app..."
adb shell am start -n "$PACKAGE/.MainActivity"

echo "Done. nibbliGO should be open on $AVD_NAME."
