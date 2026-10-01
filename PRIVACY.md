# Privacy and data lifecycle

## V1 promise

Recordings stay on the device in the implemented Phase 3 recorder. Transcripts and generated text remain future work. No cloud inference, analytics, advertising SDK, crash-upload SDK, or account is planned. Model downloads are the only planned app-initiated network operation; clipboard/share transfers happen only on explicit user action.

## Microphone and temporary audio

The app requests RECORD_AUDIO at the first recording action and records only while visible, with duration and clear stop/cancel controls. It writes 16 kHz mono 16-bit PCM WAV to `cacheDir/voice_notes_temp` under an opaque random filename. It does not save audio history or export audio.

Phase 3 deletes audio on recording cancellation/interruption, when starting again, and when leaving the recording workflow. A completed recording remains only for the next transcription phase. Recorder and file handles are closed before deletion. The application sweeps files with its private recording prefix at the next launch after process death. Cache files can exist until that sweep; immediate crash-time deletion is not possible. Cleanup failures use safe errors without paths or content. Cache storage is excluded from Android backup by platform behavior and the app's backup configuration.

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
