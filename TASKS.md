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

- [ ] Design all six screens/states and type/tone controls.
- [ ] Implement light/dark/system themes and narrow/wide layouts.
- [ ] Check TalkBack, large fonts, contrast, focus, and touch targets.
- [ ] Show labeled debug fixtures only where needed; verify release excludes them.
- [ ] Build, UI smoke-check, self-review/commit.

## Phase 3 — Audio recording

- [ ] Implement contextual permission request and permanent-denial recovery.
- [ ] Capture bounded PCM/WAV, timer, stop, cancel, errors, and sample normalization.
- [ ] Handle rotation, backgrounding, calls, permission revocation, and recorder release.
- [ ] Implement terminal cleanup and startup stale-audio sweep.
- [ ] Test real microphone and failures, build, self-review/commit.

## Phase 4 — Speech-to-text

- [ ] Pin whisper.cpp/NDK/CMake and implement JNI SpeechToTextEngine.
- [ ] Verify artifacts/licenses/hashes; load tiny.en privately on CPU.
- [ ] Implement silence handling, native abort, typed errors, and transcription correction.
- [ ] Benchmark tiny.en versus base.en for English accuracy, time, RAM, and heat.
- [ ] Audit native printing/content leakage; build/device tests, self-review/commit.

## Phase 5 — Qwen integration

- [ ] Pin llama.cpp and verify combined Whisper/Qwen ggml symbol/linkage compatibility.
- [ ] Select verified Qwen3-0.6B GGUF revision; compare mobile Q4_K_M to baseline.
- [ ] Implement JNI RewriteEngine with short template, non-thinking mode, bounded context/output.
- [ ] Implement validation, native abort, load/unload and safe error handling.
- [ ] Evaluate repeatability, fidelity, reasoning leakage, latency, and peak memory on real devices.
- [ ] Build both engines together, audit logs, self-review/commit.

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
