# nibbliGO

A local-first Android companion: an evolving **Pixel Friend** on Home, plus on-device **Assist** (chat, agent tools, prompt lab). Inference runs on your phone with LiteRT — no cloud model calls.

**Privacy:** Pixel Friend talk and Chat run on-device with LiteRT — not on a nibbliGO cloud LLM. See [Privacy at a glance](#privacy-at-a-glance) below.

## Screenshots

| Home (super dark) | Pixel Friend talk | Manage → Models |
|:---:|:---:|:---:|
| ![Home super dark](docs/screenshots/home-super-dark.png) | ![Pet home](docs/screenshots/pet-home.png) | ![Manage models](docs/screenshots/manage-models.png) |

Regenerate after UI changes:

```bash
./scripts/capture-readme-screenshots.sh   # requires adb + running emulator/device
```

## Privacy at a glance

nibbliGO has **no nibbliGO account**, **no cloud sync**, and **no analytics or crash-reporting SDK** in the app. There is no backend that collects your chats, pet saves, or companion memory. GitHub release builds compile the open-source project only — they do not receive your personal app data.

### Stays on your phone

| Data | Storage |
|------|---------|
| Pet state, stats, unlocks, streaks, daily quest | Local DB / app files |
| Home + Chat messages | Room (`nibbli.db`) |
| Companion profile, approved memory, settings | DataStore |
| Onboarding names and “about you” | DataStore |
| Visit postcards (after QR scan) | Local storage |
| Benchmark runs, saved prompts | Room |

Pixel Friend and Chat **inference** runs locally with LiteRT after you download a model. Approved memory facts and companion profile are injected into on-device prompts only — not uploaded for inference elsewhere.

### Network use (limited)

| When | What goes out |
|------|----------------|
| **Manage → Models** install | Hugging Face (or CDN) — model weight files only |
| **Settings** HF OAuth or pasted token | `huggingface.co` — token stored locally; sent as `Authorization: Bearer …` for gated downloads only |
| **Optional MCP URL** (Agent / Do flows) | Whatever server **you** configure for tool calls |

Public models (e.g. Qwen 2.5 1.5B, SmolLM2 360M) download without sign-in.

### When data can leave your device (you control it)

| Action | What happens |
|--------|----------------|
| **Share** (today card, evolution, visit QR, diary) | Android share sheet — you pick the destination app |
| **Agent** email / calendar / maps | System app intents after you confirm; email is not sent automatically |
| **Visit QR** | Encoded on-device; a friend scans it on their phone — no nibbliGO server |
| **Talk to me** (mic) | Android speech recognition (FUTO Voice Input, Google, etc.) — may use that app’s on-device or network STT policy, not nibbliGO servers |
| **MCP tools** | Only if you point Agent at an external StreamableHTTP server |

### What nibbliGO does not do

- Upload chats, memories, or pet progress to a nibbliGO cloud
- Run hidden cloud LLM inference for Home or Chat
- Ship Firebase, Sentry, Mixpanel, or similar telemetry
- Sync data across devices (not in this build)

### Permissions (why they exist)

- **INTERNET** — model downloads; optional HF OAuth; MCP servers you configure
- **RECORD_AUDIO** — voice input when you tap the mic
- **CAMERA** — visit QR scanner (frames stay on-device for decoding)
- **POST_NOTIFICATIONS** — local pet reminders (needs, streak, daily quest)

**Practical tip:** For maximum privacy, use text talk, install models without HF sign-in when possible, and do not configure external MCP URLs. More detail in **Manage → Help → FAQ** under “Privacy & on-device AI”.

## Features

### Home — Pixel Friend

- Retro **P1 LCD** pet with care actions (feed, play, clean, medicine, sleep). The ◀ ● ▶ bar has TalkBack labels; the center **●** button pulses when nibbli has an active need (Hungry, Tired, etc.).
- First-run **coach marks** overlay the care bar; they auto-dismiss when you confirm any care action (or tap **Got it**).
- **Suggestion chips** above the talk bar — **How are you?**, **Good morning**, **Let's play!** — hidden while a reply is generating or the keyboard is open.
- **Talk to me** (mic) sends voice into home talk; a **chat bar** below the LCD types messages anytime. **Stop** appears on the talk bar during generation (not a separate quick chip).
- In **talk LCD mode**, dialogue overlays the green screen and the companion panel dims to a one-line stat summary; full stats and history return when talk ends.
- **Daily quest** row in the header (feed · play · talk checkmarks with tap hints).
- **Welcome-back banner** when your care streak needs a check-in — **Feed**, **Play**, **Talk**, or **Later**.
- **Celebrations** (queued dialogs) when you complete the daily quest or unlock wearables, scenes, and props — **Equip now** opens the LCD Items picker; **Nice!** shows the next celebration if several fired at once.
- Typed Home talk and the **Chat** tab share one **Pixel Friend (Home)** thread (user + assistant lines only).
- After the first Pixel Friend model installs, nibbli sends a one-time **first-talk greeting** using your onboarding name.
- After talk, nibbli may **propose a memory** (“Remember this?”) — you approve before it is saved to **Manage → Companion**.
- **LCD Items** — care menu → **Items** → ◀ exit · ▶ browse · ● equip (wearables, scenes, floor props). Unlocks via care score, evolution, arcade wins, and daily quest bonus. **Manage → Collection** lists everything you've unlocked with hints.
- **Arcade** — **Snack Drop** and **Tidy Tap** minigames (Home → Play); wins unlock floor props.
- **Visit** — scan a friend's visit QR for a 24h LCD guest (local-only). Postcards include a care tip and borrowed prop/scene souvenir.
- **Share** — export PNG cards (today, evolution, quote, minigame score). **Diary** export from the quick-action strip.
- **Quick actions** below the talk bar: **Play**, **Share**, **Visit**, **Diary** (keyboard hides the strip while typing).
- Optional **mood pulse** — spontaneous LLM lines while Home is visible and nibbli is awake (**Manage → Companion → Behavior**: off / normal / quiet).
- **Sound & haptics** on care confirm, tap, quest complete, and minigame win (**Manage → Companion → Behavior** toggle).
- **Pet notifications** — needs, streak-at-risk, and evening daily-quest reminders with **Open Home** and **Quick feed** actions (**Settings** toggle).
- Home-screen **widget** — stage glyph, mood, streak, active need, and daily quest progress (e.g. `2/3`).
- Pet engine **warm-loads** when you open Home; initial load shows **Waking up nibbli…** (egg glyph) instead of a generic spinner.
- Post-onboarding **model setup** banner (collapsible; **Resuming download…** when a partial file exists).
- First launch **onboarding** (7 steps): pet name, caretaker, personality, about/goal, terms, then optional **model download** before **Meet nibbli**. Low-RAM devices auto-recommend **SmolLM2 360M** instead of Qwen.
- Home header badge shows the active **Pixel Friend model** and LiteRT backend (**GPU**, **CPU**, or **NPU**). **Warming up model…** shows backend or “~few seconds”. Tap **refresh** beside the badge to reload after changing accelerator in Settings.
- **P1 LCD** unified card shell in light/dark themes; fixed dark ink on the green screen.

### Chat — Pixel Friend

- **Chat** tab is your Pixel Friend only — same conversation as Home talk, on-device, no separate Assist thread.
- Card-style message bubbles (no speech tails); streaming replies with a keyboard-aware composer.
- Link to **Agent Chat** for email/calendar tasks.

### Assist (Manage hub)

- **Agent Chat** — tool-calling agent with **confirm before run** for sensitive actions (email, calendar, etc.). Uses companion profile + memory as local context on the first turn.
- **Prompt Lab** — prompt playground on device (A/B one message, see exact system prompts).
- **Benchmark** — compare TTFT and tokens/sec on your phone with plain-language tips.

### Manage

- **Models** — download LiteRT weights from Hugging Face (see [Model catalog](#model-catalog) below). Shows **Starting download…**, **Resuming download…**, or progress % while a worker runs.
- **Companion** — profile, memory, **Pixel Friend talk model**, personality, comment-on-chat, mood pulse, **sound & haptics**. Link to **Collection**.
- **Collection** — unlocked vs locked LCD wearables, scenes, and floor props with unlock hints.
- **Learn edge AI** — Benchmark, Prompt Lab, and Agent shortcuts with short on-device AI literacy copy.
- **Help & learning** — read-only FAQ for nibbliGO (privacy, models, Home vs Chat, arcade, visits) and **Basic AI Knowledge** (what LLMs are, hallucinations, on-device vs cloud).
- **Settings** — appearance (theme + **accent color**), **pet notifications**, privacy, storage, HF token, default app model, LiteRT accelerator, delete chat history.
- **Coming soon** cards on Manage and Sense hubs for Do/MCP and multimodal features not yet product-ready.

**Appearance** (in Settings): Light, Dark, **Super dark** (OLED-friendly), or System; five accent palettes — **Teal**, **Lavender**, **Sage**, **Dusk**, **Sand**.

### Sense (hub — multimodal stubs)

- **Sense** hub (nav graph only; not on the bottom bar) links to **Ask Image** and **Audio Scribe**.
- Both screens show **Coming soon** banners aligned with the hub messaging; controls are disabled until camera/gallery picker and audio transcription ship.
- **Ask Image** — vision Q&A layout preview; gallery URI field disabled with helper text.
- **Audio Scribe** — transcript/summary placeholder for a future multimodal LiteRT model.

### Phone tools (Agent Chat, after confirm)

Opens system apps via [`MobileActionsPerformer`](core/mobile-actions/src/main/kotlin/com/nibbli/nibbligo/core/mobileactions/MobileActionsPerformer.kt): flashlight, create contact, **email draft** (`ACTION_SENDTO`), maps, Wi‑Fi settings, calendar event.

### Navigation

Bottom tabs: **Home**, **Chat**, **Manage** (tabs stay visible while typing). Agent, Benchmark, and Prompt Lab live under **Manage → Learn edge AI**. **Sense** and **Do** hubs exist in the nav graph but are hidden from the bottom bar in this build. **Collection** is reachable from **Manage** (tile) or **Companion** (link).

## Model catalog

| Model | Size (approx.) | Best for | HF sign-in |
|--------|----------------|----------|------------|
| **Qwen 2.5 1.5B Instruct** | ~1.6 GB | **Pixel Friend** (recommended), general chat | No |
| **SmolLM2 360M Instruct** | ~374 MB | Fast dev / emulator / low-RAM phones, no-login | No |
| **FunctionGemma 270M** | ~289 MB | **Agent** mobile actions (CPU) | Yes |
| **Gemma 3 1B IT** | ~584 MB | Pixel Friend / chat (quality) | Yes |
| **Gemma 4 E2B** | ~2.4 GB | Rich chat, thinking trace | No |
| **DeepSeek R1 Distill 1.5B** | ~1.8 GB | Reasoning-style chat | No |

**Recommended paths**

- **Home talk (recommended):** install **Qwen 2.5 1.5B Instruct** after onboarding (~1.6 GB, no HF login); or **SmolLM2 360M** for emulator / low-RAM devices (picked automatically during onboarding).
- **Agent email/calendar:** install **FunctionGemma 270M** (gated; accept license + HF token in Settings).
- **Chat tab:** uses the same Pixel Friend model as Home (**Manage → Companion → Talk model**).

Large downloads **resume** from partial `.download` temp files via [`HfFileDownloader`](core/hf-download/src/main/kotlin/com/nibbli/nibbligo/core/hf/download/HfFileDownloader.kt); the UI shows **Resuming download…** when restarting.

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

1. Complete **onboarding** (7 steps: name, caretaker, personality, about, goal, terms, optional model download) on first launch.
2. If you skipped download, use the Home banner or **Manage → Models** — install **Qwen 2.5 1.5B Instruct** (or **SmolLM2 360M** on emulator / low-RAM).
3. **Home** — wait for warm load; check the model badge (e.g. `SmolLM2 360M Instruct · CPU` on emulator); tap **How are you?** or type in the chat bar.
4. **Manage → Learn edge AI → Agent** — install **FunctionGemma 270M** for tool calls; ask e.g. “draft an email about lunch”, then confirm.

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
adb shell am start -n com.nibbli.nibbligo/.MainActivity
```

**Emulator note:** LiteRT usually runs on **CPU** on AVDs. Settings → **LiteRT accelerator → Auto** skips GPU probing on emulators. On a physical Pixel, Auto tries **GPU (OpenCL)** first — the app declares vendor libs (`libOpenCL.so`, `libOpenCL-pixel.so`, etc.) in `AndroidManifest.xml` like [Edge Gallery](https://github.com/google-ai-edge/gallery). Run **Manage → Benchmark → Pet paths** and check the backend line (`Device (GPU)` vs `Device (CPU)`).

Fresh onboarding test: `adb shell pm clear com.nibbli.nibbligo` then relaunch.

## Demo flow

1. **Onboarding** — set pet name and caretaker; accept terms; optionally download the recommended model on step 7.
2. **Manage → Models** — install **Qwen 2.5 1.5B Instruct** (or SmolLM2 on emulator) if not done in onboarding.
3. **Manage → Settings → Appearance** — try **Super dark** and accent swatches (Teal, Lavender, Sage, Dusk, Sand).
4. **Manage → Companion** — set personality, mood pulse, and **sound & haptics**; note the **Collection** link.
5. **Home** — dismiss coach marks or confirm a care action; complete feed/play/talk for the **daily quest**; try **How are you?** suggestion chip.
6. **Home → Play** — **Snack Drop** or **Tidy Tap**; win to unlock a floor prop and trigger a **celebration** → **Equip now**.
7. Care menu → **Items** on the P1 LCD — browse and equip wearables, scenes, and props.
8. **Manage → Collection** — review locked vs unlocked items and hints.
9. **Chat** — continue the same Pixel Friend thread; composer stays above the keyboard.
10. **Manage → Models** — install **FunctionGemma 270M** (HF token if gated).
11. **Manage → Agent** — ask for an email or flashlight; confirm the tool card.
12. **Manage → Benchmark** or **Prompt Lab** — compare models or tweak prompts on device.
13. **Home → Visit** — share or scan a visit QR; read the care tip and borrowed souvenir.
14. **Settings** — toggle **pet notifications**; background tick worker sends needs/streak/quest reminders when enabled.
15. **Manage → Sense → Ask Image / Audio Scribe** — read **Coming soon** banners (stubs only in this build).

## Pixel Friend (simulation + LLM)

| Piece | Role |
|--------|------|
| [`PetSimulationEngine`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/domain/PetSimulationEngine.kt) | Hunger, hygiene, energy, sickness, evolution, death → new egg |
| [`PetEngagementEngine`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/domain/PetEngagementEngine.kt) | Daily quest, care streak, session open, quest recording on feed/play/talk |
| [`PetCelebrationDetector`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/ui/PetCelebrationDetector.kt) | Quest complete + item unlock events → queued celebration dialogs |
| [`PetTickWorker`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/work/PetTickWorker.kt) | Background decay (~15 min); notifications for needs, streak-at-risk, evening quest reminder |
| [`core:pet-llm`](core/pet-llm/) | LiteRT reactions for Talk, voice, and mood pulse |
| [`LiteRtEnginePool`](core/litert-engine/src/main/kotlin/com/nibbli/nibbligo/core/litert/engine/LiteRtEnginePool.kt) | Warm pet session, per-model GPU/CPU/NPU policy, conversation reset each turn |
| Model pick | **Manage → Companion → Talk model** (Auto prefers Qwen → SmolLM2 → Gemma 3 → …) |
| Status questions (“How are you?”) | On-device LLM with compact pet prompt; template fallback if inference fails |
| Game help (Talk) | Ask about care, evolution, LCD Items, arcade, visits — pet answers in character using an on-device FAQ knowledge base |
| Home + Chat history | One **Pixel Friend (Home)** thread — user and assistant lines only |
| Pet events | Chat, Agent, and actions emit [`PetEvent`](core/model) → mood/trust bumps and optional LCD reactions |
| Memory | Structured facts in Companion settings; injected into Home + Chat talk and Agent context |
| Widget | Glance widget reads [`PetWidgetSnapshot`](feature/pet/src/main/kotlin/com/nibbli/nibbligo/feature/pet/widget/PetWidgetSnapshot.kt) — glyph, mood, streak, need, quest progress |

Care works without a model; **Talk**, voice, and LLM mood lines need a downloaded `.litertlm` file.

## Agent & tools

- [`AgentOrchestrator`](core/agent/src/main/kotlin/com/nibbli/nibbligo/core/agent/AgentOrchestrator.kt) — multi-step turns, confirmation for `SENSITIVE` tools.
- [`PhoneActionAgentTools`](core/agent/src/main/kotlin/com/nibbli/nibbligo/core/agent/tools/PhoneActionAgentTools.kt) — Gallery-style phone actions for **FunctionGemma 270M**.
- [`AssistNavigationBus`](core/domain/src/main/kotlin/com/nibbli/nibbligo/core/domain/assist/AssistNavigationBus.kt) — deep links open Agent Chat (Home mic uses home talk instead).
- **SKILL.md** packages under `assets/skills/`; bundled `nibbli_tasks`, `nibbli_clipboard`.
- **MCP** — StreamableHTTP tools (see Settings / actions flows); discovered tools appear in Agent Chat with confirmation.

## Module map

```
app/                  Shell, navigation, Hilt, hub screens
core/model/           Domain types (pet, agent, theme, accents, LiteRT accelerators)
core/designsystem/    Theme (super dark + accent palettes), shared Compose UI, NibbliComingSoonCard
core/ui/              Loading / empty / error
core/domain/          Repositories, PetEventBus, AssistNavigationBus
core/storage/         Room, DataStore, ModelDownloadWorker (resume flag)
core/runtime/         InferenceRuntime interface
core/runtime-litert/  LiteRT-LM (chat, agent, pet, tools)
core/litert-engine/   Engine pool, backend resolver (Gallery-derived)
core/pet-llm/         Pet reaction LLM + prompt builder
core/agent/           Orchestrator, tool registry, skills bridge
core/mobile-actions/  Intents: email, maps, flashlight, …
core/hf-download/     Hugging Face OAuth + resumable downloads
core/mcp/             MCP tool discovery
feature/pet/          Home, pixel UI, widget, onboarding, visit QR, collection
feature/agent/        Agent Chat UI
feature/chat/         Pixel Friend chat (shared Home thread)
feature/promptlab/    Prompt Lab
feature/image/        Ask Image (coming soon stub)
feature/audio/        Audio Scribe (coming soon stub)
feature/actions/      Safe actions UI (Do route)
feature/models/       Model browser
feature/benchmark/    Benchmarks
feature/settings/     Settings, Appearance, Companion screens
```

## On-device runtime

Models live under app storage as `*.litertlm`. Chat, Agent, Prompt Lab, and pet LLM features prompt you to download from **Manage → Models** first.

- **LiteRT accelerator** (Settings): **Auto** uses per-model defaults (FunctionGemma = CPU only; chat models = GPU → CPU on device). Force **CPU** on emulators for stable dev.
- **Engine pool** sets `cacheDir` beside model files for faster reloads and unloads sessions when you change accelerator preference.
- **Download resume:** interrupted HF downloads continue from byte offset; Home/onboarding/Models UI distinguish **Starting** vs **Resuming**.
- Vision and audio multimodal inference are stubbed in this build; benchmarks may be limited depending on installed models.

## Tests

```bash
./gradlew test
./gradlew connectedAndroidTest   # device/emulator required
```

Unit tests cover `PetSimulationEngine`, `PetLcdItemCatalog`, `PetEngagementEngine`, `PetVisitQrCodec`, `ModelCatalog`, `PetPromptBuilder`, `PetGameFaqMatcher`, `AccentColors`, `LiteRtBackendResolver`, `SkillManifestParser`, `AgentOrchestrator`, phone `ToolExecutor`, and sprite/LCD helpers. CI runs `./gradlew testDebugUnitTest` on push/PR. Some instrumented flows need a downloaded model and are `@Ignore` by default.

## Releases

Debug APKs are built automatically when a version tag is pushed (`v*` or `debug-*`). Download the latest from [GitHub Releases](https://github.com/dpastoetter/nibbliGO/releases).

```bash
git tag v1.0.11 && git push origin v1.0.11   # triggers release-apk workflow
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
| `core:hf-download` | Hugging Face OAuth + token storage + resumable file download |
| `core:agent` | `GallerySkillWebViewBridge`, FunctionGemma mobile actions |

### Hugging Face OAuth (model downloads)

1. Create a [Hugging Face OAuth app](https://huggingface.co/settings/applications).
2. Add to `local.properties`:

   ```
   hf.oauth.clientId=your_client_id
   hf.oauth.redirectUri=nibbli://oauth/huggingface
   ```

3. Download models under **Manage → Models**. Public `litert-community` weights (e.g. **Qwen 2.5 1.5B**, **SmolLM2 360M**) work without sign-in.

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

- CameraX / gallery picker for **Ask Image**
- Audio Scribe transcription with a multimodal LiteRT model
- Re-enable **Sense** / **Do** bottom tabs when those hubs are product-ready
- Visit reactions, streak grace day, cloud sync (not planned for near-term builds)
