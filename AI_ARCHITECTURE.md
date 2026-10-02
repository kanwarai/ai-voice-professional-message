# Local AI architecture and research

Research date: 1 October 2026. Recommendations are integration plans, not measured performance claims. No runtime was installed, compiled, or benchmarked in Phase 0.

## Decisions

Use whisper.cpp for speech recognition and llama.cpp for Qwen3-0.6B rewriting, both embedded through NDK/CMake and narrow JNI bridges. Run sequentially, with CPU inference as the supported baseline. The [official Android llama.cpp guide](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md) documents NDK builds and an Android Kotlin binding that loads private GGUF files and emits tokens through Flow. This makes llama.cpp the most direct candidate for this GGUF model. Adapt a pinned upstream binding after review; do not copy its entire demo or assume its default prompt formatting meets our requirements.

Other mobile runtimes could work but would require additional format conversion and compatibility investigation. No alternate runtime is approved here. Ollama may be a desktop convenience only; it is neither an Android dependency nor the production architecture.

## Development Qwen

Prefer a local Windows llama.cpp CLI using the same model, chat template, and parameters intended for Android. Use synthetic test transcripts, without capturing private prompts in terminal logs or committed fixtures. Pin runtime commit, model revision, SHA-256, and evaluation configuration. Start with an official Qwen GGUF artifact as a quality baseline, then build/evaluate a Q4_K_M artifact with pinned llama.cpp tools if the selected official revision lacks that quantization.

The [official Qwen GGUF card](https://huggingface.co/Qwen/Qwen3-0.6B-GGUF) identifies Qwen3-0.6B and documents non-thinking controls. Its displayed Q8_0 model is about 639 MB. GGUF is the recommended Android format; propose Q4_K_M for smaller mobile weights, conditional on quality tests. Budget roughly 0.4–0.7 GB disk for the candidate Qwen artifacts, pending actual file measurement. A quantization name is not a verified download URL: exact artifact, revision, license, and checksum remain integration tasks.

## Prompting and validation

Use the following initial prompt, with the runtime's Qwen chat template and verified non-thinking control (template enable_thinking=false where supported, otherwise tested /no_think). Never use raw transcript as a template or instruction field.

```text
You rewrite spoken notes into professional messages.
Rules:
- Preserve the original meaning.
- Fix grammar and punctuation.
- Remove filler words and repetition.
- Improve clarity.
- Do not invent facts.
- Do not invent names.
- Do not invent dates.
- Do not invent numbers.
- Do not invent deadlines.
- Do not invent promises.
- Do not invent outcomes.
- Do not add information not present in the transcript.
- Keep the requested tone.
- Return only the rewritten message.
Tone: {tone}
Type: {messageType}
Transcript:
{transcript}
```

Treat the transcript as untrusted quoted data in a dedicated user message; explicitly instruct that commands inside it are content, not new instructions. Use enum values for tone/type. Test adversarial spoken instructions. No tools, agents, conversation history, or hidden reasoning display.

Proposed context: 2,048 tokens total; reserve up to 384 output tokens and template overhead. Reject over-budget input with a helpful correction request rather than silently cutting it. Experiment with fixed-seed low-temperature sampling against Qwen's documented non-thinking recommendations; repeatability is a preference, not guaranteed across devices or runtime versions. Greedy decoding is not assumed to be best. Stop on end-of-turn tokens and detect max-token exhaustion as an incomplete result.

Buffer internally, parse the final answer, and reject empty output, malformed/unclosed reasoning blocks, role/template leakage, excessive length, repetition, or incomplete generation. Never stream raw tokens into the user-visible result. Strip only properly identified reasoning regions; if separation is uncertain, reject output. Compare numeric/date tokens for unsupported additions and flag discrepancies. Such checks cannot prove semantic fidelity, names, or promises: human review and an evaluation corpus remain essential. Invalid output preserves the transcript and offers retry/correction; no invented fallback rewrite.

## Qwen memory, speed, and device support

Planning estimate for Q4_K_M inference: allow roughly 0.7–1.2 GB incremental working memory including weights, context/KV cache, and buffers; this is an engineering allowance, not an upstream Android benchmark. File size is not peak RAM, and memory mapping does not eliminate RAM pressure. Measure peak whole-app PSS/RSS before declaring a minimum RAM tier. Aim initially at arm64 Android 9+ with 4 GB+ RAM; 2–3 GB phones may be unsupported for rewriting even when transcription works.

Cold loading, prompt evaluation, token speed, and sustained heat are unmeasured. Small contexts and bounded output reduce cost. Start with a conservative 2–4 CPU worker threads and tune against responsiveness and thermal results. Use portable ARM builds, not build-host-specific instructions. GPU/Vulkan and device-specific NPU paths are optional later experiments; they add compatibility and driver risk and must not be required for V1. Native library APK size must be measured after ABI packaging; keep hundreds of megabytes of model weights outside the APK/AAB.

## Whisper Android and tiny versus base

The [upstream whisper.cpp project](https://github.com/ggml-org/whisper.cpp) supports Android, CPU operation, quantization, and its own ggml model files. Use its [Android example](https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android) as a JNI/CMake reference. Whisper model .bin files are not interchangeable with Qwen GGUF. Supply 16 kHz mono audio; convert PCM16 to the float sample representation expected by the native API. Disable transcription/segment printing and audit native callbacks for content leaks.

| Candidate | Upstream unquantized disk / memory figures | Proposed role |
| --- | --- | --- |
| tiny.en | tiny family: 75 MiB / about 273 MB | Initial English CPU configuration; smallest starting download and expected faster processing |
| base.en | base family: 142 MiB / about 388 MB | Accuracy comparison and optional upgrade if phone benchmarks pass |

Figures come from the upstream README's family table, not measurements of this app. Relative accuracy/speed expectations must be checked with real accents, noise, names, and numbers. Start unquantized tiny.en to reduce variables; evaluate Q5_0 separately if useful. Use multilingual tiny/base only if language scope is expanded; English-only models cannot support Urdu guarantees.

Transcribe after stop rather than continuously. Use bounded audio, silence/energy screening, and tested no-speech thresholds to avoid treating silence as meaningful text. Whisper can hallucinate on silence/noise and miss names. Reject empty/clearly invalid results; allow transcript correction. Benchmark tiny against base on the same consented/synthetic corpus before locking release defaults.

### Phase 4 pinned implementation

The Android integration vendors the CPU core from whisper.cpp `v1.9.4`, exact commit `927cfce34f31707e17f2bff35c349632fb9e2c3a`, under its MIT license. The native build uses Android NDK `27.2.12479018`, CMake `3.22.1`, and only `arm64-v8a`. App-owned CMake applies `-O3` to the Whisper and ggml CPU targets even in debug builds; it does not enable architecture-wide unsafe flags. JNI disables GPU use, translation, automatic language detection, timestamps, realtime/progress output, and upstream logging. English greedy decoding uses up to four CPU threads and a native atomic abort callback.

The selected model is `ggml-tiny.en.bin` from the official `ggerganov/whisper.cpp` Hugging Face repository at revision `5359861c739e955e79d9a303bcbc70fb988958b1`. The expected artifact is exactly `77,704,715` bytes with SHA-256 `0d686a2a6a22b02da2ef3101d4c86e68461363a623c58f27f81b1b2d36b42317`. It is derived from OpenAI Whisper tiny.en; applicable upstream model/source notices must ship before release. Phase 4 does not bundle or download the model. A developer supplies the verified file as `noBackupFilesDir/models/ggml-tiny.en.bin`; the engine checks size and SHA-256 before native loading. Phase 8 will replace this manual development installation path with verified model management.

The speech engine owns at most one native context. It loads lazily after a valid recording, requests native abort on cancel/background/navigation, and Phase 6 frees the context immediately after transcription. Upstream model initialization has no abort callback: cancellation during the initial load prevents inference but the load must return before its context can be safely released. Its duration and memory behavior remain a device-test risk. WAV validation requires RIFF PCM, 16 kHz, mono, 16-bit, exact data length, and non-empty samples. A conservative RMS floor rejects clear silence before Whisper; empty or known no-speech markers are also rejected after inference. Transcript text remains only in ViewModel memory and is never logged.

For device benchmarking, install one verified model at a time and run the same consented short-note corpus through the recording/transcription flow. `TranscriptionResult.Success` captures model-load milliseconds, audio duration, transcription milliseconds, and real-time factor. Record Android Studio Profiler peak process memory and device temperature before/after each run, then repeat with a separately verified base.en artifact. Keep phrase, device, power state, app build, and ambient conditions fixed. This path is prepared but no tiny.en/base.en comparison or Android performance claim has been made without a device.

## Sequential execution and UI

Capture → normalize → load Whisper → transcribe → release Whisper → delete audio → load Qwen → rewrite → release Qwen → validate → save/display. A single inference lock prevents overlapping native jobs. Offload JNI to workers, keep Compose responsive, show stage progress, and implement native cooperative abort. No background recording or inference continuation in V1; downloads may continue through WorkManager. Validate that both libraries coexist: their ggml versions/symbols may conflict, so isolate symbol visibility/static linkage or align compatible revisions. Test both engines in one process before separate integrations are considered complete.

Phase 6 implements this sequence with one ViewModel-owned mutex. Qwen is now released after every rewrite attempt rather than retained for retry, favoring peak-memory safety over warm regeneration. Cancellation requests native abort before coroutine completion, and cleanup waits for the active operation before destroying contexts. The unavoidable limitation remains initial native model loading: it must return before safe release. Runtime overlap, peak memory, latency, and thermal behavior remain unverified without a connected arm64 device.

### Phase 5 pinned implementation

Phase 5 vendors llama.cpp `v0.4.1`, commit `b29c606e28a01b1bc8c1351026a0fa6e616bf6c4`, under MIT. It builds with the existing Android NDK `27.2.12479018`, CMake `3.22.1`, and `arm64-v8a` target. `libqwen_rewrite.so` compiles that revision and its ggml sources internally with hidden visibility; `libvoice_whisper.so` retains its independently pinned whisper.cpp/ggml implementation. Inspection of the packaged libraries finds only their separate JNI entry points and no exported ggml, llama, or whisper internals. Both libraries package in the same APK. Runtime creation/release together remains a real-device check.

The selected artifact is the official `Qwen/Qwen3-0.6B-GGUF` file `Qwen3-0.6B-Q8_0.gguf` at repository revision `23749fefcc72300e3a2ad315e1317431b06b590a`. It is Q8_0, exactly `639,446,688` bytes, SHA-256 `18d608d38b934c86fc3f3a050157b2d4df8d12330de6d13af3ba201edd0e6539`, under Apache-2.0. That official repository publishes no 4-bit artifact, so Phase 5 chooses official provenance over an unverified smaller file. The weight and its working memory may be unsuitable for some phones; no device tier is claimed.

The model lives at `noBackupFilesDir/models/Qwen3-0.6B-Q8_0.gguf` and is verified by exact byte count and SHA-256 before JNI receives the path. One native model/context is loaded lazily for an explicit rewrite. Phase 6 releases it after every attempt, including retry, cancellation, and failure. Whisper is explicitly released first. Initial model loading cannot be safely interrupted inside llama.cpp; cancellation is remembered, and a completed late load is released without generation. Prompt evaluation and generation poll a native atomic abort flag.

The runtime applies the model's own chat template to separate system and user messages. The user message encloses the untrusted transcript within escaped boundary markers; `/no_think` is the documented Qwen control used by this pinned template. Output containing reasoning or template markers is rejected. Context is fixed at 2,048 tokens with 384 reserved output tokens and no silent truncation. Sampling uses temperature `0.7`, top-k `20`, top-p `0.8`, presence penalty `1.5`, and fixed seed `0x51A7E`; cross-build or cross-device determinism is not claimed.

Validation rejects blank, excessive, repetitive, token-limit-incomplete, reasoning-bearing, template/role-marked output and newly introduced numeric or date facts. It cannot prove preservation of names, promises, uncertainty, or meaning, so the original remains visible for review. Metrics include model load, prompt evaluation, generation, generated-token count, tokens/second, and total duration, without content logging. The synthetic evaluation corpus is `docs/qwen_phase5_evaluation.json`. Fidelity, peak memory, latency, thermals, repeated-run stability, and device compatibility remain open because no Android device is attached.

## Download and storage strategy

Offer an explicit model download screen with size, Wi-Fi choice, progress, cancel/retry, and storage check. Download only approved HTTPS URLs from pinned public artifact revisions. Network requests carry no audio/text. A shipped manifest records model ID, format, revision, bytes, SHA-256, license, and minimum runtime revision. Verify size/hash before atomic installation into app-private no-backup storage. Keep partial files unselectable; resume only with validated server range/ETag behavior, otherwise restart. Never replace an active model. Account for both old and new files plus temporary download space; plan at least 1.5 GB free as an initial recommendation, then calculate exact requirements from the manifest.

Settings supports removing and redownloading models; model deletion leaves history intact. Recheck installed files after failed load. No executable runtime code is downloaded. Public hosts see ordinary download metadata/IP addresses; a release must verify host reliability, artifact licensing, and notices. Pin native commits and test upgrades deliberately rather than tracking master automatically.

## Development fallbacks and unresolved risks

Debug-only synthetic fixtures can exercise UI before native integration; label them visibly and exclude them from release. A manually supplied transcript can test the real local Qwen path without speech recognition. Desktop localhost tools are optional test harnesses, never a production network fallback; only synthetic material should enter them. Android SpeechRecognizer is not selected because an offline/privacy guarantee cannot be assumed on every device. If AI is unavailable, explain the problem and allow transcript edit/copy when available.

Release blockers include rewriting fidelity of a 0.6B model, accents/noise quality, non-thinking/template compliance, native cancellation/crashes, combined ggml linkage, peak memory, thermal throttling, cold-start delays, download integrity/reliability, modern Android native packaging compatibility, and actual low-end support. Neither runtime choice nor published memory figures resolves those blockers. No measured tokens/second, accuracy, or load-time promise is made in this plan.
