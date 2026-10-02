# Development tasks

Checked means completed and reviewed. Each implementation phase requires a successful build, relevant checks, self-review, updated documents, and a safe Git milestone before the next major phase. No Android implementation is authorized by Phase 0.

## Phase 0 — Architecture and setup

- [x] Inspect workspace and Git status; no application exists.
- [x] Create README, AGENTS, product, Android architecture, AI architecture, privacy, and task documents.
- [x] Research upstream Android runtime paths and tiny/base tradeoffs.
- [x] Record scope defaults, privacy lifecycle, setup needs, and unmeasured device risks.
- [x] Self-review required sections, document links, and planning-only scope.
- [x] Run documentation diff/whitespace checks; build/tests not applicable without application code.
- [x] Transition to Phase 1 only on product owner's instruction.

## Phase 1 — Android application shell

- [x] Verify environment; install ignored project-local JDK/SDK for checks and pin compatible versions plus Gradle wrapper. Android Studio remains a documented manual install.
- [x] Create one Kotlin app module with Compose, Material 3, ViewModel, StateFlow, Navigation Compose.
- [x] Configure API 28 baseline, build variants, `com.kanwarai.voiceprofessionalmessage`, and simple source structure.
- [x] Add Home, Result, History, and Settings routes with clearly labeled unavailable features.
- [x] Configure permission-free privacy-safe manifest/backup rules and Git exclusions.
- [x] Build debug and device-test APKs, pass unit tests/lint, inspect permissions, review navigation/rotation test coverage, accessibility basics, complexity, and logs.
- [ ] Execute launch, four-route navigation, and activity recreation tests on an emulator or physical device; none was available in Phase 1.

## Phase 2 — UI/UX foundation

- [x] Design the Phase 2 Home, Result, History, and Settings experiences plus working type/tone controls; actual recording/processing states remain Phase 3 work.
- [x] Implement immediate session-only light/dark/system themes and narrow/wide scroll-safe layouts.
- [x] Review TalkBack semantics, headings, selected state, large-text wrapping, contrast, focus order, and 48 dp touch targets in code/tests.
- [x] Use no debug fixtures, fake messages, fake recording states, or simulated AI behavior.
- [x] Build debug/device-test APKs, pass unit tests/lint, inspect permissions/logging, and complete code/accessibility/responsiveness self-review.
- [ ] Execute Phase 1 and Phase 2 UI tests plus visual light/dark, narrow/wide, and large-font inspection on an emulator or physical device; none was available.

## Phase 3 — Audio recording

- [x] Implement contextual permission request and permanent-denial recovery.
- [x] Capture bounded 16 kHz mono PCM/WAV, timer, stop, cancel, and typed errors; the captured PCM already matches the later STT input, so no resampling or amplitude normalization is applied.
- [x] Preserve recording across activity recreation, cancel on background/navigation, surface microphone interruption, re-check permission, and release recorder resources.
- [x] Implement terminal cleanup and startup stale-audio sweep in private cache.
- [x] Pass 17 unit tests, clean debug/device-test APK builds, lint, manifest-permission inspection, log/privacy scans, and self-review.
- [ ] Execute the compiled real-microphone, interruption, and device lifecycle tests; ADB reported no connected emulator or physical device.

## Phase 4 — Speech-to-text

- [x] Pin whisper.cpp v1.9.4 commit `927cfce34f31707e17f2bff35c349632fb9e2c3a`, NDK 27.2.12479018, and CMake 3.22.1; implement a CPU-only arm64 JNI SpeechToTextEngine.
- [x] Verify the upstream MIT license and tiny.en artifact revision, size, and SHA-256; validate and load a locally supplied model from private no-backup storage.
- [x] Implement WAV/energy validation, silence and empty-output rejection, native abort, typed errors, in-memory transcript display/correction, and terminal audio cleanup.
- [ ] Benchmark tiny.en versus base.en for English accuracy, time, RAM, and heat.
- [x] Suppress upstream Whisper/ggml callbacks, audit content logging, build the native/debug/device-test APKs, pass 23 unit tests and lint with zero errors, inspect the packaged ABI/permissions, and self-review.
- [ ] Execute real local transcription, cancellation, silence, repetition, lifecycle, memory, and pending Phase 1–3 runtime tests; ADB reported no connected device.

## Phase 5 — Qwen integration

- [x] Pin llama.cpp and isolate the Whisper/Qwen ggml implementations in separate shared libraries with hidden internals; package and inspect both together.
- [x] Select the official Qwen3-0.6B Q8_0 GGUF with exact revision, size, hash, and license; the official repository has no verified 4-bit artifact.
- [x] Implement JNI RewriteEngine with the Qwen chat template, `/no_think`, a 2,048-token context, and 384-token output allowance.
- [x] Implement validation, cooperative native abort, one-context load/release, metrics, and typed safe errors.
- [ ] Evaluate repeatability, fidelity, reasoning leakage, latency, and peak memory on real devices.
- [x] Build both engines together, compile tests, audit logs/permissions/APK/symbols, and self-review.

## Phase 6 — Message generation workflow

- [ ] Connect capture → STT → release → rewrite → validation → result sequentially.
- [ ] Add stage status, cancellation, duplicate-action guards, and retry recovery.
- [ ] Implement edit, regenerate with edit preservation, copy, and native share.
- [ ] Verify offline flow, lifecycle transitions, failure preservation, build, self-review/commit.

## Phase 7 — History

- [ ] Add Room schema/repository, successful generation storage, and edit persistence.
- [ ] Implement browse/open, single delete, clear-all confirmation, and transaction failures.
- [ ] Verify restart persistence, deletion without reinsertion, migration/backup exclusions.
- [ ] Test repositories and UI, build, self-review/commit.

## Phase 8 — Settings and model management

- [ ] Persist appearance and STT preference with DataStore.
- [ ] Implement manifest-based HTTPS downloads, progress, resume/retry/cancel, hash verification.
- [ ] Check free space, atomic installation, interrupted downloads, and active-model replacement guards.
- [ ] Implement model removal/redownload and first-use privacy explanation.
- [ ] Test offline/missing/corrupt models and process death, build, self-review/commit.

## Phase 9 — Error handling and performance

- [ ] Exercise all specified error/recovery states without fake success or silent exceptions.
- [ ] Measure cold/warm loading, peak whole-app memory, latency, battery/thermal behavior.
- [ ] Verify cooperative cancellation/timeouts, no main-thread inference, no ANRs, no native leaks.
- [ ] Lock supported device/RAM tiers and limits from evidence; document unsupported devices.
- [ ] Build, regression checks, self-review/commit.

## Phase 10 — Testing and QA

- [ ] Run meaningful unit/instrumented tests for workflow, output validation, storage, permissions/lifecycle.
- [ ] Evaluate at least 50 notes including all tone/type combinations and adversarial/silent input.
- [ ] Verify airplane-mode processing, network privacy, Kotlin/native logs, backups, cleanup and deletion.
- [ ] Check real-device matrix, TalkBack, large fonts, dark/light, and landscape/wide screens.
- [ ] Resolve release blockers against PRODUCT_SPEC success criteria; build/self-review/commit.

## Phase 11 — Production/release preparation

- [ ] Remove debug paths; pin release dependencies, model manifests, runtime revisions and licenses.
- [ ] Verify current store target SDK, native page-size/ABI requirements, release packaging and size.
- [ ] Produce signed APK/AAB, protect signing keys outside Git, verify release install/upgrade.
- [ ] Prepare privacy policy, accurate store disclosures, model notices, support and offline-download wording.
- [ ] Verify download hosting and update/recovery strategy; owner handles account/fee approval if publishing.
- [ ] Complete release QA and self-review; obtain explicit publication authorization before publishing.

## Open risks / gate register

Qwen fidelity and invented facts; Whisper accents/noise; non-thinking correctness; exact model artifacts/hashes; dual ggml linkage; native cancellation/crashes; 16 KB native packaging compatibility; peak memory/thermal limits; supported device selection; model host availability. See AI_ARCHITECTURE.md. These remain unresolved until measured and verified in their phases. Phase 0 completion does not mean these production gates passed.
