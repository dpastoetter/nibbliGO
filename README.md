# nibbliGO

A playful, local-first Android companion that pairs an evolving AI pet with on-device assistant tools — chat, prompt lab, image Q&A, audio scribe, safe actions, models, and benchmarks.

**Privacy:** This build uses a **fake on-device runtime** for demos. No cloud inference. Replace with LiteRT / Google AI Edge–compatible execution when ready.

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- Device/emulator API 31+ (Android 12+)

## Build & run

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

### Run on Pixel 9a emulator (no Android Studio)

```bash
# One-shot: start AVD, install, launch (requires ANDROID_HOME / SDK)
./scripts/run-pixel9a-emulator.sh
```

Manual steps if the emulator was unstable:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

# Cold boot with 4GB RAM (avoids corrupt snapshot + OOM on pixel_9 profile)
emulator -avd Pixel_9a_API_35 -no-snapshot-load -no-snapshot-save -memory 4096 -accel on -gpu host &

adb wait-for-device
adb shell 'while [[ $(getprop sys.boot_completed) != 1 ]]; do sleep 2; done'

./gradlew :app:installDebug
adb shell am start -n com.nibbli.nibbligo/.MainActivity
```

The AVD `Pixel_9a_API_35` uses the **pixel_9** device profile (Google’s SDK does not ship a `pixel_9a` hardware id yet). Display name is Pixel 9a for clarity.

Open the project in Android Studio and run the `app` configuration if you prefer the IDE.

## Module map

```
app/                  → Shell, navigation, Hilt entry
core/model/           → Shared domain types
core/designsystem/    → Theme, colors, components
core/ui/              → Loading, empty, error states
core/domain/          → Repository contracts, PetEventBus
core/storage/         → Room, DataStore, repositories
core/runtime/         → InferenceRuntime + FakeInferenceRuntime
core/agent/           → Agent orchestrator, tool registry, SKILL.md loader
core/runtime-litert/  → LiteRT-LM bridge + runtime preference
feature/agent/        → Agent Chat (tool calling UI)
feature/pet/          → Pet simulation + home UI
feature/chat/         → Local chat with streaming
feature/promptlab/    → Prompt playground
feature/image/        → Ask Image (placeholder URI flow)
feature/audio/        → Audio Scribe
feature/actions/      → Safe actions + skills
feature/models/       → Model browser/install
feature/benchmark/    → Per-model benchmarks
feature/settings/     → Privacy & storage
```

## End-to-end demo flow

1. **Manage → Models** → Install `nibbli-fast`
2. **Assist → Agent Chat** → Send `remind me to stretch` → confirm tool → see result
3. **Home** → Pet gains trust/mood from `PetEvent.AgentStepCompleted`

Or use **Local Chat** for non-agent streaming.

## Agentic AI (Gallery-inspired)

- **Agent Chat** (`Assist → Agent Chat`): ReAct-style loop with confirm-gated tools
- **Tool registry**: Built-in tools (notes, reminders, clipboard, mobile actions) + imported skills
- **SKILL.md packages**: Gallery-compatible layout under `assets/skills/`; bundled `nibbli_tasks` and `nibbli_clipboard`
- **LiteRT runtime** (Settings): Prefer LiteRT when a `.litertlm` file exists in `files/models/`
- **Models**: `gemma-4-e2b-it` and `functiongemma-270m` catalog entries for future LiteRT downloads

### Authoring a skill package

```
my_skill/
  SKILL.md      # YAML frontmatter + ### tool: name blocks
  scripts/      # optional JS (sandbox planned)
```

Example `SKILL.md`:

```markdown
---
name: My Skill
description: Does one thing locally
version: 1.0.0
permissions: local_storage
---
### tool: do_thing
```

Import via **Do →** installed packages list (bundled skills load at app start).

## Plugging in a real on-device runtime

1. Implement `InferenceRuntime` in a new module (e.g. `core:runtime-litert`).
2. Load model files from `context.filesDir/models/`.
3. Bind your implementation in a Hilt module instead of `FakeInferenceRuntime`:

```kotlin
@Binds @Singleton
abstract fun bindInferenceRuntime(impl: LiteRTInferenceRuntime): InferenceRuntime
```

4. Gate features with `capabilitiesFor(modelId)` (vision/audio flags).

Feature modules depend only on `InferenceRuntime` — no UI changes required.

## Fake runtime behavior

- Simulated model load (300–800 ms)
- Word-by-word streaming for chat
- Template completions for Prompt Lab presets
- Vision/audio gated by catalog modalities
- Synthetic benchmark metrics

## Tests

```bash
./gradlew test
./gradlew connectedAndroidTest   # device/emulator required
```

- Unit: `PetSimulationEngine`, `ModelCatalog`, `FakeInferenceRuntime`, `SkillManifestParser`, `AgentOrchestrator`
- UI: model install, chat send (instrumented)

## Roadmap

- Full LiteRT-LM Engine integration (replace demo delegate)
- JS skill WebView sandbox
- MCP client for external tool servers
- CameraX + gallery integration for Ask Image
