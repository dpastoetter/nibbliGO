#!/usr/bin/env bash
# Capture nibbliGO pet screenshots on a running emulator/device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/screenshots"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

mkdir -p "$OUT"

if ! adb devices | grep -q 'device$'; then
  echo "No Android device/emulator connected." >&2
  exit 1
fi

capture() {
  local name="$1"
  adb exec-out screencap -p > "$OUT/$name"
  echo "Saved $OUT/$name ($(file -b "$OUT/$name"))"
}

ui_dump() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/nibbli_ui.xml >/dev/null 2>&1
}

wait_for_text() {
  local text="$1"
  local tries="${2:-30}"
  for ((i = 0; i < tries; i++)); do
    ui_dump
    if grep -q "text=\"$text\"" /tmp/nibbli_ui.xml; then
      return 0
    fi
    sleep 0.4
  done
  echo "Timed out waiting for UI text: $text" >&2
  return 1
}

tap_text() {
  local text="$1"
  ui_dump
  python3 - "$text" <<'PY'
import re, sys, subprocess
label = sys.argv[1]
xml = open("/tmp/nibbli_ui.xml", encoding="utf-8", errors="replace").read()
# Prefer clickable ancestor; fall back to TextView bounds.
patterns = [
    rf'clickable="true"[^>]*>.*?text="{re.escape(label)}"',
    rf'text="{re.escape(label)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
]
for pat in patterns:
    m = re.search(pat, xml, re.DOTALL)
    if m and m.lastindex == 4:
        x1, y1, x2, y2 = map(int, m.groups())
        cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
        subprocess.check_call(["adb", "shell", "input", "tap", str(cx), str(cy)])
        print(f"Tapped {label} at {cx},{cy}")
        sys.exit(0)
    if m:
        break
m = re.search(rf'text="{re.escape(label)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if not m:
    sys.exit(1)
x1, y1, x2, y2 = map(int, m.groups())
cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
subprocess.check_call(["adb", "shell", "input", "tap", str(cx), str(cy)])
print(f"Tapped {label} at {cx},{cy}")
PY
}

scroll_home_top() {
  # Reveal pixel device + title at top of pet home.
  for _ in 1 2 3; do
    adb shell input swipe 540 500 540 1600 350
    sleep 0.35
  done
}

scroll_home_care() {
  for _ in 1 2; do
    adb shell input swipe 540 1600 540 700 350
    sleep 0.35
  done
}

echo "Installing latest debug build…"
"$ROOT/gradlew" -q :app:installDebug

go_home() {
  if wait_for_text "nibbliGO" 8; then
    return 0
  fi
  echo "Navigating to Home tab…"
  tap_text "Home" || adb shell input tap 100 2256
  wait_for_text "nibbliGO" 25
}

echo "Launching app…"
adb shell am force-stop com.nibbli.nibbligo
adb shell am start -n com.nibbli.nibbligo/.MainActivity
sleep 2
go_home
sleep 1

scroll_home_top
if ! wait_for_text "nibbliGO" 5; then
  go_home
  scroll_home_top
fi
capture "pet-home.png"

go_home
scroll_home_care
tap_text "Talk" || adb shell input tap 653 2083
if ! wait_for_text "Talk to nibbli" 25; then
  adb shell input tap 653 2083
  wait_for_text "Talk to nibbli" 15
fi
sleep 0.8
capture "pet-talk.png"

adb shell input keyevent KEYCODE_BACK
sleep 0.6

go_home
tap_text "Manage" || adb shell input tap 980 2256
wait_for_text "Settings" 20
sleep 0.5
tap_text "Settings" || adb shell input tap 192 797
wait_for_text "Pixel Friend" 25
sleep 0.5
# Center the Pixel Friend card in view.
adb shell input swipe 540 1700 540 900 400
sleep 0.5
capture "pet-settings.png"

go_home
scroll_home_top
capture "pet-pixel-device.png"

echo "Done. Screenshots in $OUT"
