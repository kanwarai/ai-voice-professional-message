# AI Voice Note → Professional Message

Speak naturally, then review a clean message you can edit, copy, or share.

## Current status

Phase 1 application shell is complete (1 October 2026). It is a buildable native Android app with Home, Result, History, and Settings routes. Recording, transcription, rewriting, local history, model downloads, and persistent settings are intentionally unavailable until their planned phases.

The package/application ID is `com.kanwarai.voiceprofessionalmessage`.

## For the product owner

1. Read [PRODUCT_SPEC.md](PRODUCT_SPEC.md) for what the first version will do.
2. Read [TASKS.md](TASKS.md) to follow progress. Checked boxes mean completed work.
3. The next phase establishes the fuller UI/UX foundation. Recording and AI arrive in later phases.
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

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. On 1 October 2026, the debug APK, unit tests, compiled device-test APK, and lint all completed successfully. Lint reported zero errors; version-update notices reflect the deliberate API 36 compatibility choice. The device navigation tests compile but were not executed because no emulator/device was connected.

Desktop AI experiments will use local llama.cpp and whisper.cpp tools; Windows source builds may additionally need Visual Studio C++ Build Tools. Python is optional if model conversion becomes necessary. No paid AI subscription, API key, backend, or user account is needed for development or normal app use. Google Play publication later needs a developer account; any publication fee requires owner approval. Public model downloads are planned without sign-in; hosting availability remains a release check.

## Document map

- [AGENTS.md](AGENTS.md): persistent engineering rules.
- [PRODUCT_SPEC.md](PRODUCT_SPEC.md): scope and acceptance criteria.
- [ARCHITECTURE.md](ARCHITECTURE.md): Android design and lifecycle.
- [AI_ARCHITECTURE.md](AI_ARCHITECTURE.md): researched runtime choices, sources, and open risks.
- [PRIVACY.md](PRIVACY.md): data handling and deletion.
- [TASKS.md](TASKS.md): implementation phases and gates.

## Project structure

The single `app` module currently contains `navigation/`, `presentation/`, `ui/screens/`, and `ui/theme/`. Later packages such as `audio/`, `ai/`, and `data/` will be created only when their phases begin. Model binaries, recordings, private messages, build output, local SDK paths, and signing keys remain outside Git.
