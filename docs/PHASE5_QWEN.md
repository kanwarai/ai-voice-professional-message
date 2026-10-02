# Phase 5 local message rewriting

## Pinned runtime and model

The app uses llama.cpp release `v0.4.1` at commit `b29c606e28a01b1bc8c1351026a0fa6e616bf6c4` (MIT). Its vendored source and license are in `app/src/main/cpp/third_party/llamacpp`.

The only accepted model is the official `Qwen/Qwen3-0.6B-GGUF` artifact at revision `23749fefcc72300e3a2ad315e1317431b06b590a`:

- File: `Qwen3-0.6B-Q8_0.gguf`
- Quantization: Q8_0
- Size: `639446688` bytes
- SHA-256: `18d608d38b934c86fc3f3a050157b2d4df8d12330de6d13af3ba201edd0e6539`
- License: Apache-2.0

The official repository does not publish a 4-bit artifact. Q8_0 is used to retain official provenance; its larger storage and memory cost is a known device risk. The model is deliberately absent from Git and the APK. Install it at `<app noBackupFilesDir>/models/Qwen3-0.6B-Q8_0.gguf`; a missing or mismatched file produces an honest UI error. Android backup cannot copy this directory.

## Runtime boundaries

`libqwen_rewrite.so` contains llama.cpp and its own pinned ggml objects with hidden symbol visibility. `libvoice_whisper.so` contains the older whisper.cpp ggml objects. Neither target links against the other's ggml. The workflow releases the Whisper context before creating the single Qwen context, retains at most that one Qwen context for retry, and releases it before recording again or leaving the workflow.

Context is fixed at 2,048 tokens and generation at 384 tokens. The complete templated prompt plus the full output allowance must fit; input is rejected rather than truncated. The official Qwen `/no_think` soft switch is included and reasoning/template leakage is rejected. Sampling is conservative and repeatable for a fixed build: temperature 0.7, top-k 20, top-p 0.8, presence penalty 1.5, fixed seed `0x51A7E`. Cross-device bit-for-bit determinism is not claimed.

The validator rejects empty, incomplete, excessive, repetitive, reasoning-tagged, template-leaking, and suspicious outputs that introduce numeric/date facts absent from the transcript. This is a safeguard, not a proof of semantic fidelity; the UI always keeps the original visible for review.

No transcript or generated message is logged, persisted, backed up, or sent over a network. Phase 5 stops at the controlled transcript/rewrite comparison; copy, share, history, and the final Result workflow remain out of scope.

## Evaluation and device checklist

`docs/qwen_phase5_evaluation.json` is a synthetic corpus covering both message types, all tones, prompt injection, dates, amounts, names, disfluency, and long-input rejection. Do not add real user text. On a physical arm64 device with the verified model installed, record cold model-load time, prompt-evaluation time, generation time, generated tokens, peak RSS, and output disposition. Exercise background cancellation and low-memory failure. This repository has no device attached, so those measurements are pending rather than estimated.
