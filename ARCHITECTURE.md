# Android architecture

## Recommendation

One application module, Kotlin, Compose/Material 3, MVVM, lifecycle-aware ViewModels and StateFlow, Navigation Compose. Use constructor injection with a small application dependency container initially. Avoid extra modules, a dependency injection framework, generic use-case layers, and a backend until demonstrated need.

Proposed minimum Android API 28 (Android 9), arm64-v8a for production AI devices, preferably 4 GB+ RAM. These are product targets pending native integration benchmarks. x86_64 emulator support may be added for shell/UI testing without claiming AI parity. Select and pin stable compatible SDK/AGP/Kotlin/Compose versions in Phase 1; select release target SDK against current distribution requirements in Phase 11.

## Layers and boundaries

| Package | Responsibility |
| --- | --- |
| ui | Screens, navigation, accessible controls, clipboard/share platform actions |
| workflow | ViewModel, immutable UI state, one sequential generation coordinator |
| audio | AudioRecord ownership, PCM/WAV creation, duration, cleanup |
| ai | SpeechToTextEngine and RewriteEngine interfaces, model repository, native adapters |
| data | Room history repository, DataStore preferences, private file storage |

Engines accept bounded input and return typed success/failure; UI has no native handles. Speech recognizer consumes local audio; rewriter consumes text and configuration. Neither owns navigation or history. A ModelRepository validates availability, version, format, hash, and runtime compatibility. Dependencies are replaceable for tests without shipping fake engines.

## State management and navigation

Model the workflow as Idle, Recording, Transcribing, Rewriting, Result, and RecoverableError with stage-specific data. A single job prevents concurrent capture/inference or double-tap submission. Immutable StateFlow is collected with lifecycle awareness. Treat share/copy as explicit user actions; avoid replaying them on rotation. Navigate Home, Result(id), History, Settings using IDs only, never private text in route arguments.

ViewModels survive rotation. Save only non-sensitive route/configuration in SavedStateHandle; keep unsaved transcripts in memory and saved results in the private database. Process death loses unsaved work and returns to a safe idle state. Do not persist private text in framework saved-state bundles.

## Storage

Use Room for history (id, transcript, message, tone, type, createdAt, updatedAt) and DataStore for theme/model preference. Use transactions for replacement/deletion. Surface save failures separately from successful AI work. All stores are app-private and excluded from backup/device transfer. Use cache storage for temporary audio and no-backup private files for models; see PRIVACY.md. No storage permission or public media directory is needed.

## Recording and lifecycle

Use AudioRecord for 16 kHz mono PCM16, writing bounded chunks to a private WAV file; check supported configuration and resample if the device cannot supply 16 kHz directly. Native Whisper receives normalized float PCM. Test short reads, read errors, unavailable devices, and correct WAV finalization. AudioRecord is always released in finally; cleanup failures are surfaced or queued for retry with safe diagnostics.

Foreground capture only: when the activity actually backgrounds (not configuration change), cancel capture and discard audio. Handle calls, microphone revocation, focus/interruption, and route changes. Rotation must not create a second recorder. No foreground recording service in V1.

## Threading and resource ownership

File/database/download work uses Dispatchers.IO. Native inference uses a bounded worker dispatcher plus one mutex across both engines. Run STT, release its context, then load/run Qwen; retain transcript for regeneration. Release contexts after completion or cancellation to favor memory safety over warm-start speed. Avoid parallel model preloading.

The ViewModel owns the generation job; configuration changes preserve it. Actual backgrounding or leaving the workflow cancels inference and releases resources. Kotlin cancellation alone does not stop blocking JNI: bridges must poll a native abort flag/callback and close handles only after workers return. Never free a running context. Timeouts request cooperative abort; a watchdog detects failures but cannot safely kill arbitrary native threads. Native crashes remain a release risk requiring device tests. Model downloads may use WorkManager for resumable background work; no private payload is passed to workers.

### Phase 6 workflow implementation

`HomeViewModel` owns one sealed workflow state and one inference mutex. Stop transitions directly from recording through finalization into transcription. The transcription block releases Whisper in `finally`; WAV deletion then runs in non-cancellable cleanup. Only a transcript-ready state can request Qwen. Rewriting acquires the same mutex, defensively releases Whisper, and always releases Qwen in `finally`. Duplicate taps cannot create another operation while the current job is active.

The result state contains the corrected transcript, immutable last generated value, editable visible value, rewrite metrics, and regeneration-confirmation state. This makes manual edits explicit. Regeneration uses the corrected transcript and current configuration, preserves the prior edited result on failure/cancellation, and never depends on audio. Copy/share are one-shot UI calls derived from the current visible value and are not stored as replayable navigation events. Start over keeps type/tone but clears all private draft state and waits for cancelled native work before final resource release.

## Error handling and verification

Typed failures distinguish permission, capture, empty audio, missing/corrupt model, unsupported hardware, insufficient storage, inference cancellation/timeout, invalid output, and persistence failure. Do not swallow exceptions or misreport failures as results. Preserve the transcript after rewrite failure and the previous edited result after regeneration failure. Cleanup runs on all exits and startup sweeps stale cache audio.

Unit-test workflow transitions, cancellation, prompt validation, and repository behavior. Instrument lifecycle, permission, recording, native ABI loading, and storage/backup behavior. Benchmark cold/warm model load, peak PSS/RSS, inference, temperature, and repeated runs on real hardware. Build and self-review each phase before proceeding.
