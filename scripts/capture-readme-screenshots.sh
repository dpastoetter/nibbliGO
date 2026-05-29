#!/usr/bin/env bash
# Capture README screenshots for nibbliGO (requires running emulator/device).
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

# Non-fatal wait (keeps capture script moving on flaky uiautomator dumps).
wait_for_text_optional() {
  wait_for_text "$@" || true
}

tap_text() {
  local text="$1"
  ui_dump
  python3 - "$text" <<'PY'
import re, sys, subprocess
label = sys.argv[1]
xml = open("/tmp/nibbli_ui.xml", encoding="utf-8", errors="replace").read()
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

go_tab() {
  local tab="$1"
  tap_text "$tab" || true
  sleep 0.8
}

go_home() {
  go_tab "Home"
  wait_for_text "nibbli" 25 || true
}

go_manage() {
  go_tab "Manage"
  wait_for_text_optional "Appearance" 8
}

go_assist() {
  go_tab "Assist"
  wait_for_text "Assist" 15 || true
}

select_theme() {
  local theme="$1"
  go_manage
  sleep 0.5
  tap_text "$theme" || true
  sleep 0.6
}

echo "Installing latest debug build…"
"$ROOT/gradlew" -q :app:installDebug

echo "Launching app…"
adb shell am force-stop com.nibbli.nibbligo
adb shell am start -n com.nibbli.nibbligo/.MainActivity
sleep 2

# --- Manage / appearance ---
go_manage
sleep 0.5
capture "manage-appearance.png"

go_manage
tap_text "Models" || adb shell input tap 320 900
wait_for_text "Models" 20 || true
sleep 0.5
capture "manage-models.png"

# --- Super dark home (hero) ---
select_theme "Super dark"
go_home
sleep 0.8
scroll_home_top
capture "home-super-dark.png"
capture "pet-home.png"
scroll_home_top
capture "pet-pixel-device.png"

# --- Light home (contrast) ---
select_theme "Light"
go_home
sleep 0.8
scroll_home_top
capture "home-light.png"

# --- Assist hub + agent ---
select_theme "Super dark"
go_assist
sleep 0.5
capture "assist-hub.png"
tap_text "Agent Chat" || adb shell input tap 540 1200
wait_for_text_optional "Ask nibbli" 8
wait_for_text_optional "Agent" 5
sleep 0.5
capture "agent-chat.png"

# --- Talk sheet ---
go_home
scroll_home_care
tap_text "Talk" || adb shell input tap 653 2083
if wait_for_text_optional "Talk to nibbli" 12 && grep -q "Talk to nibbli" /tmp/nibbli_ui.xml 2>/dev/null; then
  sleep 0.8
  capture "pet-talk-sheet.png"
  cp -f "$OUT/pet-talk-sheet.png" "$OUT/pet-talk.png"
fi
adb shell input keyevent KEYCODE_BACK
sleep 0.5

# --- Looks sheet (only if unlocked) ---
go_home
scroll_home_care
if tap_text "Looks" 2>/dev/null || wait_for_text "Looks" 3; then
  sleep 0.3
  if tap_text "Looks" 2>/dev/null; then
    sleep 0.8
    if wait_for_text "Unlocked looks" 10 || wait_for_text "Looks" 5; then
      capture "pet-looks.png"
    fi
    adb shell input keyevent KEYCODE_BACK
  fi
fi

# --- Settings (personality) ---
go_home
go_manage
tap_text "Settings" || adb shell input tap 700 1200
wait_for_text_optional "Pixel Friend" 8
wait_for_text_optional "Personality" 5
sleep 0.5
adb shell input swipe 540 1700 540 900 400
sleep 0.5
capture "pet-settings.png"

# Remove obsolete screenshot if present
rm -f "$OUT/emulator-home-latest.png"

echo "Done. Screenshots in $OUT"
