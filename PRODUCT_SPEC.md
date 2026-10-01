# Product specification

## Objective and target user

Help people who find speaking easier than composing polished text create everyday workplace messages and emails. The user controls the final text and chooses where to send it. V1 is Android only, private by default, with no paid AI services.

## Core journey

Open → choose Message/Email and Professional/Friendly/Concise → record → stop → local transcription → local rewrite → review transcript and message → edit → copy or native share. First use checks model readiness and offers downloads before recording. Downloaded models enable offline use.

## V1 scope and screens

| Screen/state | Required behavior |
| --- | --- |
| Home / Record | Configuration, model readiness, microphone permission, record, history/settings entry points |
| Recording | Visible elapsed time, stop, cancel, accessible recording indicator |
| Processing | Separate transcribing/rewriting status, indeterminate progress where exact progress is unavailable, cancel |
| Generated message | Transcript, editable rewrite, regenerate, copy, Android Share Sheet |
| History | Newest first, open saved results, delete one, clear all with confirmation |
| Settings | System/light/dark appearance, model readiness/download/remove, privacy explanation, clear history |

Recording and processing are workflow states within the Home route. Proposed initial limits: 120 seconds per note; a 2,048-token rewrite context with up to 384 output tokens. Validate these in AI phases and show clear limits; never silently truncate meaning. English speech and English output are the initial supported scope. Multilingual or mixed Urdu/English support requires separate evaluation and is not promised in V1.

## Feature requirements

- Ask for microphone permission when Record is tapped; explain refusal and provide a settings route after permanent denial. No recording before permission.
- Capture foreground audio, stop/cancel safely, display duration using a monotonic clock, handle unavailable microphone and interruptions, and delete temporary files.
- Message type and tone apply to generation and regeneration. Email may use readable paragraphs but must not invent recipient, subject facts, signatures, or commitments.
- Preserve transcript meaning; remove filler and repetition, fix grammar, and improve clarity. Show no reasoning text. Never claim factual correctness is guaranteed.
- Allow transcript correction before retry/regeneration to recover from recognition mistakes. Keep the edited message until a replacement is accepted; warn before overwriting unsaved edits.
- Store successful results automatically in local history: transcript, edited message, configuration, and local timestamps. Save message edits after a short debounce and flush on explicit copy/share or leaving the editor. Regeneration updates that entry after success; failures preserve the old result.
- Copy provides confirmation. Share sends only the selected final message as plain text through Android's chooser; the app does not automatically send it.
- Support system/light/dark themes, font scaling, TalkBack labels, minimum 48 dp touch targets, adequate contrast, keyboard access where applicable, and layouts for narrow/wide displays.

## Explicit exclusions

iOS/web apps, accounts, cloud sync/backup, cloud speech or text APIs, agents/tools, automatic sending, live streaming transcription, background recording, audio history/export, attachments, subscriptions, advertising, analytics, multilingual guarantees, and GPU/NPU requirements. No fine-tuning or custom backend in V1.

## Error states and recovery

| Failure | Response |
| --- | --- |
| Permission refused / microphone busy | Explain, enable settings or retry; remain idle |
| Recording interrupted / process killed | Stop and discard recording; explain restart when possible |
| Empty/silent speech | No rewrite/history item; ask for a clearer recording |
| Models missing / offline download | Open model management; retain offline history access |
| Download interrupted / corrupt / insufficient storage | Resume or retry verified download; never load partial models |
| Transcription failure | Clear failed audio after terminal failure; offer new recording |
| Rewrite failure / invalid or incomplete output | Keep transcript and previous result; offer correction/retry/copy transcript |
| Memory pressure / native failure / timeout | Release resources; allow safe retry, smaller STT model, or transcript use |
| Local save / clipboard / share failure | Preserve active draft where possible and report failure truthfully |

## Success criteria and release gates

All core flows work on the declared supported devices, including airplane mode after download. No private content appears in logs or outbound network traffic. Permission denial, cancellation, rotation, backgrounding, process death, model deletion, storage exhaustion, and history deletion have verified behavior. TalkBack and large text remain usable. No main-thread inference or ANRs occur during the test workload.

Before release, evaluate at least 50 synthetic/consented notes covering filler, accents, names, numbers, dates, negation, noise, and all six type/tone combinations. Provisional target: at least 90% acceptable rewrites by human review, with zero observed critical invented commitments/facts in that set; this is not a general guarantee. Proposed performance target: a 30-second note completes in 30 seconds or less on the selected reference phone. Device selection, actual latency, quality results, and any revised thresholds must be documented before release.
