# Phase 6 workflow

## User flow

The Home workflow is Record → automatic transcription → Review transcript → Create message → editable result. Transcript corrections become the sole rewrite and regeneration source. Message/Email and Professional/Friendly/Concise selections carry through; changing them after generation leaves the visible draft untouched until explicit regeneration.

The result supports direct editing, Regenerate, Copy, Share, Edit transcript, and Start over. Regeneration warns before replacing manual edits. A failed or cancelled regeneration restores the previous edited result. Copy uses only the visible message and confirms success. Share opens Android's native chooser with `ACTION_SEND`, `text/plain`, and only the visible message.

## Lifetime and cleanup

One coroutine job prevents duplicate stage actions and one mutex serializes both inference engines. Whisper runs first and is released in `finally`. The WAV is deleted immediately after transcription finishes, including terminal failure and cancellation. Qwen consumes only transcript text, creates at most one context, and is released in `finally` after each attempt. Start over and workflow exit request native cancellation, wait for active work where lifecycle permits, delete audio, and release both engines.

Transcript and message drafts live only in ViewModel memory. Process death or Start over loses them. Type and tone remain selected after Start over. No history, Room, DataStore, model download, network path, foreground service, automatic save, copy, or share was added.

## Verification status

Workflow unit tests cover recording/transcription transitions, transcript correction, typed request propagation, sequential release, duplicate rewrite prevention, transcription/rewrite cancellation, editable results, protected regeneration, failure preservation, regeneration without Whisper/audio, external payload construction, and Start over. The debug and instrumentation APKs, lint, packaged ABI/libraries, permissions, source privacy, and prohibited-artifact checks form the static gate.

No Android device is attached. End-to-end native inference, clipboard UI, share chooser, TalkBack focus/announcements, process background recovery, peak memory, model load time, tokens/second, overall latency, repeated workflows, and thermal behavior remain open device checks. No device compatibility or performance claim is made.
