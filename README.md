# nibbliGO

A playful, local-first Android companion that pairs an evolving AI pet with on-device assistant tools — chat, prompt lab, image Q&A, audio scribe, safe actions, models, and benchmarks.

**Privacy:** On-device only — LiteRT inference and Hugging Face model downloads. No cloud inference.

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
core/runtime/         → InferenceRuntime interface
core/runtime-litert/  → LiteRT-LM inference (required for chat/agent/pet Talk)
core/pet-llm/         → Local LLM pet reactions (LiteRT only)
core/agent/           → Agent orchestrator, tool registry, SKILL.md loader
feature/agent/        → Agent Chat (tool calling UI)
feature/pet/          → Pixel Friend pixel pet + care loop
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

1. **Manage → Models** → Download `functiongemma-270m` (~289 MB) or `gemma-4-e2b-it` (~2.6 GB)
2. **Assist → Local Chat** or **Agent Chat** — requires installed LiteRT model
3. **Home** → Pixel Friend: feed, play, clean, medicine, sleep; Talk uses installed LiteRT model
4. Pet gains trust/mood from `PetEvent.AgentStepCompleted` (toast on Home)

## Pixel Friend

- **Simulation** (`PetSimulationEngine`): hunger, hygiene, energy, sickness, evolution, death → new egg with optional `memorySummary`
- **Background ticks** (`PetTickWorker`, ~15 min): decay while away; notifications when needs stay critical
- **Voice** (`core:pet-llm`): care and Talk lines via LiteRT when a model is installed
- **Settings**: **personality** (Playful / Calm / Curious)
- **Export**: Home overflow → share pet diary (text)
- **Widget**: add **nibbliGO Pet** from the launcher widget picker (name + life stage)

Download **functiongemma-270m** first for faster emulator testing. Care mechanics work without a model; Talk and LLM reactions require an installed LiteRT model.



## Agentic AI (Gallery-inspired)

- **Agent Chat** (`Assist → Agent Chat`): ReAct-style loop with confirm-gated tools
- **Tool registry**: Built-in tools (notes, reminders, clipboard, mobile actions) + imported skills
- **SKILL.md packages**: Gallery-compatible layout under `assets/skills/`; bundled `nibbli_tasks` and `nibbli_clipboard`
- **LiteRT runtime**: bound directly — all inference requires a downloaded `.litertlm` model
- **Models**: `functiongemma-270m` and `gemma-4-e2b-it` from Hugging Face

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

## On-device runtime

LiteRT-LM loads models from `files/models/*.litertlm`. Chat, Agent, Prompt Lab, and Pixel Friend Talk show install prompts until a model is downloaded.

Vision, audio, and benchmarks are not yet supported with LiteRT in this build.

## Tests

```bash
./gradlew test
./gradlew connectedAndroidTest   # device/emulator required
```

- Unit: `PetSimulationEngine`, `ModelCatalog`, `SkillManifestParser`, `AgentOrchestrator`
- UI: instrumented flows require network + LiteRT download (marked `@Ignore` by default)

## Google AI Edge Gallery integration

nibbliGO ports infrastructure from [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0). See [NOTICE](NOTICE).

| Module | Role |
|--------|------|
| `core:litert-engine` | LiteRT-LM `Engine` / `Conversation` (from Gallery `LlmChatModelHelper`) |
| `core:hf-download` | Hugging Face OAuth + token storage |
| `core:mcp` | MCP tool discovery (StreamableHTTP) |
| `core:agent` | `GallerySkillWebViewBridge` (from `GalleryWebView`) |

### Hugging Face OAuth (model downloads)

1. Create a [Hugging Face OAuth app](https://huggingface.co/settings/applications).
2. Add to `local.properties`:
   ```
   hf.oauth.clientId=your_client_id
   hf.oauth.redirectUri=nibbli://oauth/huggingface
   ```
3. Download LiteRT models under **Manage → Models** (Wi‑Fi recommended; ~300 MB–2.6 GB). Public `litert-community` weights download without sign-in.

   **If a download is gated:** **Settings → paste a [HF access token](https://huggingface.co/settings/tokens)** (read + gated-repos), or sign in with OAuth after step 2.

OAuth redirect is handled in `MainActivity` (`nibbli://oauth/huggingface`). Model files match [Gallery allowlist 1.0.15](https://github.com/google-ai-edge/gallery/blob/main/model_allowlists/1_0_15.json).

### MCP servers

**Do → MCP servers** — add a StreamableHTTP URL (use [Gallery MCP guide](https://github.com/google-ai-edge/gallery/blob/main/mcp/README.md) + ngrok for device testing). Discovered tools appear in Agent Chat with confirmation.

### Gallery submodule (reference)

```bash
git submodule update --init third_party/gallery
```

## Roadmap

- CameraX + gallery integration for Ask Image
- Download progress UI and resume for large LiteRT models
