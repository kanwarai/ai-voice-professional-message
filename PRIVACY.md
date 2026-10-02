# Privacy and data lifecycle

## V1 promise

Recordings, transcripts, and Phase 5 generated messages stay on the device. No cloud inference, analytics, advertising SDK, crash-upload SDK, or account is planned. Model downloads are the only planned app-initiated network operation; clipboard/share transfers happen only on explicit user action in a later phase.

## Microphone and temporary audio

The app requests RECORD_AUDIO at the first recording action and records only while visible, with duration and clear stop/cancel controls. It writes 16 kHz mono 16-bit PCM WAV to `cacheDir/voice_notes_temp` under an opaque random filename. It does not save audio history or export audio.

The app deletes audio on recording cancellation/interruption, transcription success or terminal failure, transcription cancellation, restart, and leaving the workflow. Recorder, native reader, and file handles close before deletion. The application sweeps files with its private recording prefix at the next launch after process death. Cache files can exist until that sweep; immediate crash-time deletion is not possible. Cleanup failures use safe errors without paths or content. Cache storage is excluded from Android backup by platform behavior and the app's backup configuration.

## Transcript and message handling

Phase 5 holds the unsaved transcript and generated message in ViewModel memory, never in logs, route arguments, notifications, saved-state bundles, or worker inputs. Discarding, leaving the workflow, or process death removes them. The Qwen model is manually installed under private no-backup storage and is verified before loading; it is not bundled or downloaded by this phase. Later successful history entries will contain transcript and final editable message plus tone/type/timestamps. Regeneration will use the transcript, never original audio.

## Local history and settings

Use an app-private Room database. Exclude database, preferences containing private metadata, audio, and models from Android cloud backup and device-to-device transfer rules; verify both supported backup mechanisms. Uninstall removes private app data, subject to Android behavior. There is no cloud restore or cross-device sync.

Default protection is Android app sandboxing and device storage encryption, not additional application-level encrypted database storage. A compromised/unlocked device can expose local data. App locking and stronger database encryption are outside V1 unless the threat model changes.

Delete one entry or clear all after confirmation, remove it from visible state, and ensure it stays absent after restart. Clear active drafts referencing deleted entries to prevent accidental reinsertion. Use appropriate SQLite secure-delete/checkpoint maintenance outside active transactions where supported and verify behavior; logical deletion is required, forensic erasure on flash storage cannot be guaranteed. History deletion does not delete models. Model removal deletes installed and partial weights and does not delete history.

## Logs and external actions

Do not log audio, transcripts, output tokens, private messages, secrets, or payload-bearing exceptions in Kotlin or C++. Disable upstream printing/verbose native logging; sanitize error callbacks. Diagnostics may include stage, safe error code, model version, and non-content timing/memory metrics locally. Tests use synthetic/consented material and private artifacts stay out of Git.

Copy occurs only after an explicit tap and transfers only the current visible final message to Android's clipboard, which is outside the app's storage control. Share occurs only after an explicit tap and opens Android's standard chooser with that text as `text/plain`; the selected app may upload or retain it. Neither action includes the transcript, audio, metrics, model paths, or hidden state. Deletion here cannot recall shared text or erase another app's clipboard/history. There is no automatic copy, share, or message sending.

## Permissions and networking

RECORD_AUDIO is the sole runtime permission currently present. Phase 5 adds no `INTERNET` or network-state permission. A later model-download phase may require normal `INTERNET` and `ACCESS_NETWORK_STATE` permissions. No contacts, location, camera, external storage, phone, or background microphone permission is planned. If download notifications later require notification permission, reassess necessity and document it before requesting it.

Use HTTPS, approved download hosts, and a pinned artifact manifest. Public hosts may receive IP address and download metadata, never private notes. Test offline operation and observe network traffic during capture/transcription/rewrite to verify the boundary. Audit backups, logs, crash paths, deletion, and permission denial before release.
