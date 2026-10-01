# Privacy and data lifecycle

## V1 promise

Recordings, transcripts, and generated text stay on the device. No cloud inference, analytics, advertising SDK, crash-upload SDK, or account is planned. Model downloads are the only app-initiated network operation; clipboard/share transfers happen only on explicit user action. This is a design specification, not a claim about an implemented app.

## Microphone and temporary audio

Request RECORD_AUDIO at first recording action. Record only while the app is visible, with duration and clear stop/cancel controls. Use a private cache WAV file with a random filename containing no personal information. Do not save audio history or export audio in V1.

Delete audio after successful transcription, terminal transcription failure, recording cancellation/interruption, timeout, or navigation cancellation. Retain it only during active transcription, not for rewrite regeneration. Always close recorder/file/native readers before deletion. If the app crashes, sweep stale audio at the next launch before accepting new recordings. Cache files may exist until that sweep after process death; do not promise immediate crash-time deletion. Report cleanup failures with safe codes and retry later. Temporary audio is excluded from backup.

## Transcript and message handling

Hold unsaved transcript/results in memory, never in logs, route arguments, notifications, saved-state bundles, or download worker inputs. Successful history entries contain transcript and final editable message plus tone/type/timestamps. Display that storage behavior in first-use/privacy text. Regeneration uses the transcript, never original audio. Failed rewriting leaves the transcript available in the active session; discarding it or process death removes unsaved work.

## Local history and settings

Use an app-private Room database. Exclude database, preferences containing private metadata, audio, and models from Android cloud backup and device-to-device transfer rules; verify both supported backup mechanisms. Uninstall removes private app data, subject to Android behavior. There is no cloud restore or cross-device sync.

Default protection is Android app sandboxing and device storage encryption, not additional application-level encrypted database storage. A compromised/unlocked device can expose local data. App locking and stronger database encryption are outside V1 unless the threat model changes.

Delete one entry or clear all after confirmation, remove it from visible state, and ensure it stays absent after restart. Clear active drafts referencing deleted entries to prevent accidental reinsertion. Use appropriate SQLite secure-delete/checkpoint maintenance outside active transactions where supported and verify behavior; logical deletion is required, forensic erasure on flash storage cannot be guaranteed. History deletion does not delete models. Model removal deletes installed and partial weights and does not delete history.

## Logs and external actions

Do not log audio, transcripts, output tokens, private messages, secrets, or payload-bearing exceptions in Kotlin or C++. Disable upstream printing/verbose native logging; sanitize error callbacks. Diagnostics may include stage, safe error code, model version, and non-content timing/memory metrics locally. Tests use synthetic/consented material and private artifacts stay out of Git.

Copy transfers the message to Android's clipboard, which is outside the app's storage control. Share transfers only final text to the app the user chooses; that app may upload it. Explain this at relevant actions without claiming continued control over copies. Deletion here cannot recall shared text or erase another app's clipboard/history. No automatic message sending.

## Permissions and networking

RECORD_AUDIO is the sole runtime permission planned. INTERNET is a normal manifest permission for model downloads; ACCESS_NETWORK_STATE may be needed for download constraints. No contacts, location, camera, external storage, phone, or background microphone permission is planned. If download notifications later require notification permission, reassess necessity and document it before requesting it.

Use HTTPS, approved download hosts, and a pinned artifact manifest. Public hosts may receive IP address and download metadata, never private notes. Test offline operation and observe network traffic during capture/transcription/rewrite to verify the boundary. Audit backups, logs, crash paths, deletion, and permission denial before release.
