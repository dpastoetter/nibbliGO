# nibbliGO

A local-first Android companion: an evolving **Pixel Friend** on Home, plus on-device **Assist** (chat, agent tools, prompt lab). Inference runs on your phone with LiteRT — no cloud model calls.

**Privacy:** LiteRT inference on device. Network is used only for Hugging Face model downloads (and optional OAuth), not for sending your chats to a cloud LLM.

## Screenshots

| Home (super dark) | Pixel Friend talk | Manage → Models |
|:---:|:---:|:---:|
| ![Home super dark](docs/screenshots/home-super-dark.png) | ![Pet home](docs/screenshots/pet-home.png) | ![Manage models](docs/screenshots/manage-models.png) |

Regenerate after UI changes:

```bash
./scripts/capture-readme-screenshots.sh   # requires adb + running emulator/device
```

## Features

### Home — Pixel Friend

- Retro **P1 LCD** pet with care actions (feed, play, clean, medicine, sleep).
- **Talk** sheet plus Home quick actions: **Talk to me** (voice), **How are you?**, and **Stop** (cancels an in-flight reply).
- In **talk mode**, the pet moves to the bottom-left of the LCD and the LLM reply is centered on-screen (scrolls when long); conversation history appears below the stat strip in the companion panel.
- A **chat bar** at the bottom of Home lets you type messages to nibbli anytime (same on-device Pixel Friend model as the quick chips).
- **Talk to me** and quick chips use the **Pixel Friend** on-device model (Settings → on-device models), not Agent Chat.
- **LCD Items** — all customization lives on the P1 shell: care menu → **Items** → ◀ exit · ▶ browse · ● equip. Cycle **wearables** (collar, star patch, aurora aura), **scenes** (cozy, stars, clouds, night), and **floor props** (ball, plant, mat) with live preview on the LCD. Unlocks via care score, evolution, arcade wins, and daily quest bonus.
- **Arcade** — retro **Snack Drop** and **Tidy Tap** minigames (Home → Play); wins unlock floor props.
- **Visit** — scan a friend’s visit QR to show their nibbli on your LCD for 24h (local-only, no server); share your own QR from Home → Visit.
- **Share** — export PNG cards (today, evolution, quote, minigame score).
- Optional **mood pulse** — spontaneous LLM lines while Home is visible, the app is in the foreground, and nibbli is awake (Settings → mood pulse: off / normal / quiet).
- Diary export and home-screen **widget**.
- Pet engine **warm-loads** when you open Home so the first chip reply is faster.

### Assist

- **Local Chat** — streaming chat with a downloaded LiteRT model.
- **Agent Chat** — tool-calling agent with **confirm before run** for sensitive actions (email, calendar, etc.).
- **Prompt Lab** — prompt playground on device.

### Manage

- **Models** — download LiteRT weights from Hugging Face (see [Model catalog](#model-catalog) below).
- **Appearance** — Light, Dark, **Super dark** (OLED-friendly midnight), or System.
- **Settings** — privacy, storage, HF token, **default app model** vs **Pixel Friend model**, **LiteRT accelerator** (Auto / GPU / CPU / NPU), pixel friend personality (Playful / Calm / Curious), mood pulse.

### Phone tools (Agent Chat, after confirm)

Opens system apps via [`MobileActionsPerformer`](core/mobile-actions/src/main/kotlin/com/nibbli/nibbligo/core/mobileactions/MobileActionsPerformer.kt): flashlight, create contact, **email draft** (`ACTION_SENDTO`), maps, Wi‑Fi settings, calendar event.

### Navigation

Bottom tabs: **Home**, **Assist**, **Manage**. Sense and Do hubs exist in the nav graph but are hidden from the bottom bar in this build.

## Model catalog

| Model | Size (approx.) | Best for | HF sign-in |
|--------|----------------|----------|------------|
| **SmolLM2 360M Instruct** | ~374 MB | **Pixel Friend**, emulator, no-login dev | No |
| **FunctionGemma 270M** | ~289 MB | **Agent** mobile actions (CPU) | Yes |
| **Gemma 3 1B IT** | ~584 MB | Pixel Friend / chat (quality) | Yes |
| **Gemma 4 E2B** | ~2.4 GB | Rich chat, thinking trace | No |
| **Qwen 2.5 1.5B** | ~1.6 GB | General chat | No |
| **DeepSeek R1 Distill 1.5B** | ~1.8 GB | Reasoning-style chat | No |

**Recommended paths**

- **Home talk only:** install **SmolLM2 360M** (public, fast on emulator CPU).
- **Agent email/calendar:** install **FunctionGemma 270M** (gated; accept license + HF token in Settings).
- **General chat:** pick any chat model; set **Default app model** in Settings.

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- Device or emulator **API 31+** (Android 12+)

## Quick start

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.nibbli.nibbligo/.MainActivity
```

1. **Manage → Models** — download **SmolLM2 360M Instruct** (~374 MB, no HF login) for Home talk and emulator testing.
2. **Home** — wait a few seconds for warm load; tap **How are you?** or use **Talk to me**.
3. **Assist → Agent Chat** — install **FunctionGemma 270M** for tool calls; ask e.g. “draft an email about lunch”, then confirm.

### Emulator (Pixel 9a profile)

```bash
./scripts/run-pixel9a-emulator.sh
```

Or manually:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
emulator -avd Pixel_9a_API_35 -no-snapshot-load -memory 4096 &
adb wait-for-device
./gradlew :app:installDebug
```

**Emulator note:** LiteRT usually runs on **CPU** on AVDs. Settings → **LiteRT accelerator → Auto** skips GPU probing on emulators. On a physical Pixel, Auto prefers GPU (and NPU where supported).

## Demo flow

1. **Manage → Models** — install **SmolLM2 360M Instruct**.
2. **Manage → Appearance** — try **Super dark**.
3. **Home** — feed/play; tap **How are you?**; use **Stop** if a reply is taking too long; try **Talk to me** (mic → Pixel Friend LLM).
4. **Manage → Models** — install **FunctionGemma 270M** (HF token if gated).
5. **Assist → Agent Chat** — ask for an email or flashlight; confirm the tool card.
6. **Home → Play** — try **Snack Drop** or **Tidy Tap** in the arcade.
7. Care menu → **Items** on the P1 LCD — browse and equip wearables, scenes, and props (unlock via care, evolution, and arcade wins).
8. **Home → Visit** — share or scan a visit QR to see a friend’s nibbli on your LCD.

## Pixel Friend (simulation + LLM)

| Piece | Role |
|--------|------|
| [`PetSimulationEngine`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/domain/PetSimulationEngine.kt) | Hunger, hygiene, energy, sickness, evolution, death → new egg |
| [`PetTickWorker`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/work/PetTickWorker.kt) | Background decay (~15 min); notifications when needs stay critical |
| [`core:pet-llm`](core/pet-llm/) | LiteRT reactions for Talk, voice, and mood pulse |
| [`LiteRtEnginePool`](core/litert-engine/src/main/kotlin/com/nibbli/nibbligo/core/litert/engine/LiteRtEnginePool.kt) | Warm pet session, per-model GPU/CPU/NPU policy, conversation reset each turn |
| Model pick | Settings → **Pixel Friend model** (Auto prefers SmolLM2 → Gemma 3 → …) |
| Status questions (“How are you?”) | On-device LLM with compact pet prompt; template fallback if inference fails |
| Game help (Talk) | Ask about care, evolution, LCD Items, arcade, visits — pet answers in character using an on-device FAQ knowledge base |
| Home talk history | Saved to **Assist → Local Chat** as **Pixel Friend (Home)** |

Care works without a model; **Talk**, voice, and LLM mood lines need a downloaded `.litertlm` file.

## Agent & tools

- [`AgentOrchestrator`](core/agent/src/main/kotlin/com/nibbli/nibbligo/core/agent/AgentOrchestrator.kt) — multi-step turns, confirmation for `SENSITIVE` tools.
- [`PhoneActionAgentTools`](core/agent/src/main/kotlin/com/nibbli/nibbligo/core/agent/tools/PhoneActionAgentTools.kt) — Gallery-style phone actions for **FunctionGemma 270M**.
- **SKILL.md** packages under `assets/skills/`; bundled `nibbli_tasks`, `nibbli_clipboard`.
- **MCP** — StreamableHTTP tools (see Settings / actions flows); discovered tools appear in Agent Chat with confirmation.

## Module map

```
app/                  Shell, navigation, Hilt
core/model/           Domain types (pet, agent, theme, LiteRT accelerators)
core/designsystem/    Theme (incl. super dark), shared Compose UI
core/ui/              Loading / empty / error
core/domain/          Repositories, PetEventBus
core/storage/         Room, DataStore
core/runtime/         InferenceRuntime interface
core/runtime-litert/  LiteRT-LM (chat, agent, pet, tools)
core/litert-engine/   Engine pool, backend resolver (Gallery-derived)
core/pet-llm/         Pet reaction LLM + prompt builder
core/agent/           Orchestrator, tool registry, skills bridge
core/mobile-actions/  Intents: email, maps, flashlight, …
core/hf-download/     Hugging Face OAuth + downloads
core/mcp/             MCP tool discovery
feature/pet/          Home, pixel UI, widget
feature/agent/        Agent Chat UI
feature/chat/         Local chat
feature/promptlab/    Prompt Lab
feature/image/        Ask Image
feature/audio/        Audio Scribe
feature/actions/      Safe actions UI (Do route)
feature/models/       Model browser
feature/benchmark/    Benchmarks
feature/settings/     Settings screen
```

## On-device runtime

Models live under app storage as `*.litertlm`. Chat, Agent, Prompt Lab, and pet LLM features prompt you to download from **Manage → Models** first.

- **LiteRT accelerator** (Settings): **Auto** uses per-model defaults (FunctionGemma = CPU only; chat models = GPU → CPU on device). Force **CPU** on emulators for stable dev.
- **Engine pool** sets `cacheDir` beside model files for faster reloads and unloads sessions when you change accelerator preference.
- Vision, audio, and benchmarks may be limited depending on the installed model and build.

## Tests

```bash
./gradlew test
./gradlew connectedAndroidTest   # device/emulator required
```

Unit tests cover `PetSimulationEngine`, `PetLcdItemCatalog`, `PetEngagementEngine`, `PetVisitQrCodec`, `ModelCatalog`, `PetPromptBuilder`, `PetGameFaqMatcher`, `LiteRtBackendResolver`, `SkillManifestParser`, `AgentOrchestrator`, phone `ToolExecutor`, and sprite/LCD helpers. Some instrumented flows need a downloaded model and are `@Ignore` by default.

## Releases

Debug APKs are built automatically when a version tag is pushed (`v*` or `debug-*`). Download the latest from [GitHub Releases](https://github.com/dpastoetter/nibbliGO/releases).

```bash
git tag v1.0.4 && git push origin v1.0.4   # triggers release-apk workflow
```

Local build:

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Google AI Edge Gallery

nibbliGO ports patterns from [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0). See [NOTICE](NOTICE) and [LICENSE](LICENSE).

| Module | Role |
|--------|------|
| `core:litert-engine` | LiteRT `Engine` / `Conversation`, GPU/CPU/NPU fallback |
| `core:hf-download` | Hugging Face OAuth + token storage |
| `core:agent` | `GallerySkillWebViewBridge`, FunctionGemma mobile actions |

### Hugging Face OAuth (model downloads)

1. Create a [Hugging Face OAuth app](https://huggingface.co/settings/applications).
2. Add to `local.properties`:

   ```
   hf.oauth.clientId=your_client_id
   hf.oauth.redirectUri=nibbli://oauth/huggingface
   ```

3. Download models under **Manage → Models**. Public `litert-community` weights (e.g. **SmolLM2 360M**) work without sign-in.

   **Gated models:** **Settings** → paste a [HF access token](https://huggingface.co/settings/tokens), or use OAuth after step 2. Required for **FunctionGemma 270M** and **Gemma 3 1B IT**.

Redirect: `nibbli://oauth/huggingface` in [`MainActivity`](app/src/main/kotlin/com/nibbli/nibbligo/MainActivity.kt). Allowlist aligned with [Gallery 1.0.15](https://github.com/google-ai-edge/gallery/blob/main/model_allowlists/1_0_15.json).

### MCP servers

Configure StreamableHTTP MCP in the actions/settings flows (see [Gallery MCP guide](https://github.com/google-ai-edge/gallery/blob/main/mcp/README.md)). Tools show up in Agent Chat with confirmation.

### Gallery submodule (reference)

```bash
git submodule update --init third_party/gallery
```

## License

nibbliGO is licensed under the [Apache License 2.0](LICENSE).

Portions adapt patterns from [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0); see [NOTICE](NOTICE) and `Ported from gallery@...` comments in source.

**Model weights** downloaded via Manage → Models are not part of this repository. Each `.litertlm` file remains under its publisher’s terms on Hugging Face (e.g. Gemma, Qwen, SmolLM).

## Roadmap

- CameraX / gallery picker for Ask Image
- Download progress and resume for large LiteRT models
- Re-enable Sense / Do bottom tabs when those hubs are product-ready
