# AI Voice Note → Professional Message

Speak naturally, then review a clean message you can edit, copy, or share.

## Current status

Phase 3 audio recording is implemented (1 October 2026). The native Android app records real microphone input as a bounded 16 kHz mono PCM WAV in private cache, with contextual permission handling, a two-minute timer, stop/cancel/record-again controls, lifecycle cleanup, and a startup stale-file sweep. Transcription, rewriting, local history, model downloads, and persistent settings remain intentionally unavailable until their planned phases.

The package/application ID is `com.kanwarai.voiceprofessionalmessage`.

## For the product owner

1. Read [PRODUCT_SPEC.md](PRODUCT_SPEC.md) for what the first version will do.
2. Read [TASKS.md](TASKS.md) to follow progress. Checked boxes mean completed work.
3. The next phase adds local Whisper speech-to-text. Phase 3 does not download or invoke an AI model.
4. Before device testing, have an Android phone and USB cable available. We propose Android 9 or newer, a 64-bit processor, and preferably at least 4 GB RAM. These are starting targets, not proven compatibility guarantees.

The planned app works offline after its free AI models are downloaded. It will not upload recordings or messages to an AI service. Review generated text before sharing: small models can misunderstand speech or change meaning.

## Development setup

Install Android Studio from its [official download page](https://developer.android.com/studio). Use its bundled Java runtime and SDK Manager to install Android SDK Platform 36, SDK Build Tools, platform tools, and an emulator image. Native AI phases later need the Android NDK and CMake. The checked-in Gradle wrapper downloads Gradle automatically; no separate Gradle installation is needed.

Android Studio is not installed in the standard locations on the current machine. For verification, Phase 1 used ignored, project-local copies of JDK 17 and Android SDK Platform 36; these are intentionally not committed. No emulator or physical device was available, so actual launch, navigation, and rotation tests remain pending.

### Pinned project versions

| Component | Version |
| --- | --- |
| Package/application ID | `com.kanwarai.voiceprofessionalmessage` |
| minSdk | 28 (Android 9) |
| targetSdk / compileSdk | 36 (stable Android 16) |
| Android Gradle Plugin | 8.13.2 |
| Gradle wrapper | 8.13 |
| Kotlin / Compose compiler plugin | 2.2.21 |
| Compose | Stable BOM 2026.03.01 |
| Navigation Compose | 2.9.8 |
| Lifecycle | 2.10.0 |
| Activity Compose | 1.12.4 |
| Java bytecode target | 17 |
| SDK Build Tools used locally | 35.0.0 (36.0.0 also installed) |

The compatible March 2026 AndroidX line is intentional: newer September releases require compileSdk 37 and AGP 9.1+, while API 37 was not selected as a stable platform for this phase.

### Build

Open the root folder in Android Studio, allow SDK/Gradle sync, then run the `app` configuration. From a configured terminal:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. On 1 October 2026, a clean build produced the debug and device-test APKs, 17 unit tests passed, and lint completed with zero errors. APK inspection found only `RECORD_AUDIO` plus AndroidX's app-scoped dynamic-receiver signature permission. Source scans found no content logging, networking, storage permission, AI runtime, persistence layer, or audio artifact. Lint's 11 warnings are version-update notices for the deliberately pinned API 36-compatible toolchain. ADB reported no connected device, so the compiled UI, real-microphone, interruption, and lifecycle tests were not executed.

## Phase 3 recording behavior

- Tapping Record requests microphone permission in context. A normal denial offers retry; permanent denial links to Android app settings.
- Audio uses Android `AudioRecord` with the voice-recognition source, 16 kHz mono 16-bit PCM, and a standard WAV header. No resampling or amplitude normalization is needed because this is the planned speech-to-text input format.
- Recording stops at two minutes. The UI shows elapsed time and explicit Stop and Cancel actions; Record again discards the previous temporary WAV.
- Rotation keeps the active ViewModel and recording. Backgrounding, leaving the workflow, microphone interruption, or recorder failure cancels the partial file and releases resources.
- Temporary files use opaque names under private app cache. Cancellation and workflow exit delete them; process-death leftovers are swept on the next application start.
- Phase 3 adds only `RECORD_AUDIO`. It adds no network, storage, phone, location, camera, or background-microphone permission.

## Phase 2 UI behavior

- Home uses a single-column phone layout and a restrained two-column layout on wider windows. Content scrolls on short screens and with large text, and the main content width is capped for readability.
- Message type offers Message and Email, defaulting to Message. Tone offers Professional, Friendly, and Concise, defaulting to Professional.
- Type and tone are held in ViewModel/StateFlow state and restored through activity recreation. They are not stored after a fresh app session.
- Settings offers System, Light, and Dark appearance modes. Changes apply immediately across every route and survive activity recreation, without DataStore persistence.
- Result and History contain honest empty states. Settings labels both future AI models as not installed and explains the current local-only privacy boundary.
- Accessibility foundations include heading semantics, radio-button selection state, meaningful action labels, decorative-icon suppression, 48 dp or larger touch targets, high-contrast light/dark palettes, flexible selector layouts, and scrollable large-text layouts.
- No preview fixture, fake message, fake recording state, permission, persistence layer, network call, or AI behavior is included.

Desktop AI experiments will use local llama.cpp and whisper.cpp tools; Windows source builds may additionally need Visual Studio C++ Build Tools. Python is optional if model conversion becomes necessary. No paid AI subscription, API key, backend, or user account is needed for development or normal app use. Google Play publication later needs a developer account; any publication fee requires owner approval. Public model downloads are planned without sign-in; hosting availability remains a release check.

## Document map

- [AGENTS.md](AGENTS.md): persistent engineering rules.
- [PRODUCT_SPEC.md](PRODUCT_SPEC.md): scope and acceptance criteria.
- [ARCHITECTURE.md](ARCHITECTURE.md): Android design and lifecycle.
- [AI_ARCHITECTURE.md](AI_ARCHITECTURE.md): researched runtime choices, sources, and open risks.
- [PRIVACY.md](PRIVACY.md): data handling and deletion.
- [TASKS.md](TASKS.md): implementation phases and gates.

## Project structure

The single `app` module currently contains `audio/`, `navigation/`, `presentation/`, `ui/components/`, `ui/screens/`, and `ui/theme/`. Later packages such as `ai/` and `data/` will be created only when their phases begin. Model binaries, recordings, private messages, build output, local SDK paths, and signing keys remain outside Git.
