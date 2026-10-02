# AI Voice Note → Professional Message

Speak naturally, then review a clean message you can edit, copy, or share.

## Current status

Phase 6 connects the complete memory-only workflow (2 October 2026): record, automatic local transcription, editable transcript review, explicit local rewriting, editable result, protected regeneration, clipboard copy, native sharing, and Start over. History, model downloads, and persistent settings remain intentionally unavailable until their planned phases.

The package/application ID is `com.kanwarai.voiceprofessionalmessage`.

## For the product owner

1. Read [PRODUCT_SPEC.md](PRODUCT_SPEC.md) for what the first version will do.
2. Read [TASKS.md](TASKS.md) to follow progress. Checked boxes mean completed work.
3. Phase 6 provides the complete active-session workflow. Phase 7 will add private local history; current drafts disappear on process death or Start over.
4. Before device testing, have an Android phone and USB cable available. We propose Android 9 or newer, a 64-bit processor, and preferably at least 4 GB RAM. These are starting targets, not proven compatibility guarantees.

The planned app works offline after its free AI models are downloaded. It will not upload recordings or messages to an AI service. Review generated text before sharing: small models can misunderstand speech or change meaning.

## Development setup

Install Android Studio from its [official download page](https://developer.android.com/studio). Use its bundled Java runtime and SDK Manager to install Android SDK Platform 36, SDK Build Tools, platform tools, Android NDK 27.2.12479018, CMake 3.22.1, and an emulator image. The checked-in Gradle wrapper downloads Gradle automatically; no separate Gradle installation is needed.

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
| Android NDK | 27.2.12479018 |
| CMake | 3.22.1 |
| Native ABI | arm64-v8a |
| whisper.cpp | v1.9.4 / `927cfce34f31707e17f2bff35c349632fb9e2c3a` |
| llama.cpp | v0.4.1 / `b29c606e28a01b1bc8c1351026a0fa6e616bf6c4` |

The compatible March 2026 AndroidX line is intentional: newer September releases require compileSdk 37 and AGP 9.1+, while API 37 was not selected as a stable platform for this phase.

### Build

Open the root folder in Android Studio, allow SDK/Gradle sync, then run the `app` configuration. From a configured terminal:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Phase 5 verification results and remaining device checks are recorded in [docs/PHASE5_QWEN.md](docs/PHASE5_QWEN.md).

## Phase 5 rewriting behavior

- Rewriting begins only after the user taps **Rewrite message** from a transcript. It accepts only the transcript, Message/Email type, and the selected tone.
- Qwen runs locally through a narrow `RewriteEngine`; Compose never calls JNI. Prompt construction treats transcript text as untrusted content and requests Qwen's `/no_think` behavior.
- The 2,048-token context reserves up to 384 generated tokens. Inputs that do not fit are rejected without truncation. Validation rejects incomplete, repetitive, reasoning/template-leaking, and suspicious number/date-changing output.
- Whisper and Qwen have separate shared libraries with hidden native internals. Whisper is released before the single Qwen context is created; Qwen is released before recording again, discard, navigation, or ViewModel destruction.
- No transcript or output is logged, persisted, copied, shared, or sent over a network in this phase.

### Development Qwen installation

Obtain `Qwen3-0.6B-Q8_0.gguf` from the official `Qwen/Qwen3-0.6B-GGUF` repository at revision `23749fefcc72300e3a2ad315e1317431b06b590a`. Verify exactly 639,446,688 bytes and SHA-256 `18d608d38b934c86fc3f3a050157b2d4df8d12330de6d13af3ba201edd0e6539`, then install it as `no_backup/models/Qwen3-0.6B-Q8_0.gguf` using the same debug-only `run-as` process described for Whisper. The app verifies it again before native loading. The model is intentionally absent from Git and the APK.

## Phase 3 recording behavior

- Tapping Record requests microphone permission in context. A normal denial offers retry; permanent denial links to Android app settings.
- Audio uses Android `AudioRecord` with the voice-recognition source, 16 kHz mono 16-bit PCM, and a standard WAV header. No resampling or amplitude normalization is needed because this is the planned speech-to-text input format.
- Recording stops at two minutes. The UI shows elapsed time and explicit Stop and Cancel actions; Record again discards the previous temporary WAV.
- Rotation keeps the active ViewModel and recording. Backgrounding, leaving the workflow, microphone interruption, or recorder failure cancels the partial file and releases resources.
- Temporary files use opaque names under private app cache. Cancellation and workflow exit delete them; process-death leftovers are swept on the next application start.
- Phase 3 adds only `RECORD_AUDIO`. It adds no network, storage, phone, location, camera, or background-microphone permission.

## Phase 4 transcription behavior

- The app accepts only its expected 16 kHz mono 16-bit PCM WAV, screens clear silence using sample energy, and decodes PCM directly to floats without resampling.
- whisper.cpp runs through a narrow JNI layer on background dispatchers. English greedy decoding uses at most four CPU threads. Translation, diarization, word timestamps, GPU use, and native transcript/progress printing are disabled.
- The native context is loaded lazily once per Home workflow, receives cooperative abort requests during inference, and is released with the ViewModel. Cancellation, backgrounding, and navigation prevent further processing and delete the WAV. The initial upstream model-load call has no abort callback, so it must return before its context can be safely released; this remains to be measured on a device.
- Successful transcripts remain only in memory and can be corrected locally. Empty, whitespace-only, and known no-speech output is rejected. Phase 6 uses the corrected transcript as the rewrite source and deletes the completed WAV first.
- Only `arm64-v8a` is built. Native targets use focused `-O3` optimization even in debug builds so future measurements are representative.

### Development model installation

Phase 4 intentionally contains no model downloader and no `INTERNET` permission. Obtain `ggml-tiny.en.bin` from the official `ggerganov/whisper.cpp` Hugging Face repository at revision `5359861c739e955e79d9a303bcbc70fb988958b1`. Verify exactly 77,704,715 bytes and SHA-256 `0d686a2a6a22b02da2ef3101d4c86e68461363a623c58f27f81b1b2d36b42317`.

For a debug installation, first launch the app so its private directories exist, copy the verified file to a temporary device location, then use Android's debug-only `run-as` facility to copy it to `no_backup/models/ggml-tiny.en.bin` and remove the temporary copy. The app verifies the size and hash again before loading. Do not commit the model; Phase 8 will provide the production verified download and installation flow.

The benchmark path captures audio duration, model-load time, transcription time, and real-time factor without logging transcript content. Run the same consented corpus with tiny.en and a separately verified base.en model on one physical phone, recording Android Studio Profiler peak memory and device thermal observations. No comparison is complete because no Android device is connected.

## Phase 6 complete workflow

- Stop automatically begins local transcription. The completed WAV is deleted in a non-cancellable cleanup immediately after Whisper finishes, before Qwen can start.
- **Review transcript** keeps an editable in-memory transcript as the source of truth. The corrected text, current type, and current tone are passed to Qwen only after **Create message**.
- One workflow mutex serializes Whisper and Qwen. Whisper is released after transcription; Qwen is loaded only for rewrite and released after success, failure, or cancellation.
- The final message is directly editable. Regenerate reuses the corrected transcript and current type/tone without recording or transcribing again. If the message was edited, a confirmation dialog warns that regeneration will replace it. Failed regeneration preserves the edited result.
- Copy places only the visible final message on Android's clipboard and confirms **Message copied**. Share opens Android's standard `ACTION_SEND` chooser with only that text and `text/plain` after an explicit tap.
- Start over cancels active work, waits for native work to stop before release, clears transcript/result/errors, deletes temporary audio, and keeps type/tone selections for convenience.
- Transcript and result text remain only in ViewModel memory. No Room, DataStore, history, network permission, automatic saving, or automatic external action is present.

See [docs/PHASE6_WORKFLOW.md](docs/PHASE6_WORKFLOW.md) for verification status and remaining device risks.

## Phase 2 UI behavior

- Home uses a single-column phone layout and a restrained two-column layout on wider windows. Content scrolls on short screens and with large text, and the main content width is capped for readability.
- Message type offers Message and Email, defaulting to Message. Tone offers Professional, Friendly, and Concise, defaulting to Professional.
- Type and tone are held in ViewModel/StateFlow state and restored through activity recreation. They are not stored after a fresh app session.
- Settings offers System, Light, and Dark appearance modes. Changes apply immediately across every route and survive activity recreation, without DataStore persistence.
- Result and History contain honest empty states. Settings explains manual Phase 4 speech-model verification, future rewriting, and the current local-only privacy boundary.
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
