# AI Voice Note → Professional Message

Speak naturally, then review a clean message you can edit, copy, or share.

## Current status

Phase 0 planning foundation is complete (1 October 2026). No Android application, build files, models, or feature implementations have been created. The directory initially contained only an initialized Git repository with no commits.

## For the product owner

1. Read [PRODUCT_SPEC.md](PRODUCT_SPEC.md) for what the first version will do.
2. Read [TASKS.md](TASKS.md) to follow progress. Checked boxes mean completed work.
3. The next phase creates an installable Android shell. Recording and AI arrive in later phases.
4. Before device testing, have an Android phone and USB cable available. We propose Android 9 or newer, a 64-bit processor, and preferably at least 4 GB RAM. These are starting targets, not proven compatibility guarantees.

The planned app works offline after its free AI models are downloaded. It will not upload recordings or messages to an AI service. Review generated text before sharing: small models can misunderstand speech or change meaning.

## Development setup

Install Android Studio from its [official download page](https://developer.android.com/studio). Use its bundled Java runtime and SDK Manager to install the Android SDK, platform tools, and an emulator image. Native AI phases also need the Android NDK and CMake through SDK Manager. Gradle will be supplied by the project's wrapper in Phase 1; no separate Gradle installation is needed. Pin compatible tool versions when creating the project.

Git is available here. Java, adb, and CMake were not found on PATH, and Android Studio/SDK were not found in the standard locations checked. This does not prove they are absent elsewhere. No software was installed during planning.

Desktop AI experiments will use local llama.cpp and whisper.cpp tools; Windows source builds may additionally need Visual Studio C++ Build Tools. Python is optional if model conversion becomes necessary. No paid AI subscription, API key, backend, or user account is needed for development or normal app use. Google Play publication later needs a developer account; any publication fee requires owner approval. Public model downloads are planned without sign-in; hosting availability remains a release check.

## Document map

- [AGENTS.md](AGENTS.md): persistent engineering rules.
- [PRODUCT_SPEC.md](PRODUCT_SPEC.md): scope and acceptance criteria.
- [ARCHITECTURE.md](ARCHITECTURE.md): Android design and lifecycle.
- [AI_ARCHITECTURE.md](AI_ARCHITECTURE.md): researched runtime choices, sources, and open risks.
- [PRIVACY.md](PRIVACY.md): data handling and deletion.
- [TASKS.md](TASKS.md): implementation phases and gates.

## Planned structure

Use one Android `app` module initially, with `ui/`, `workflow/`, `data/`, `audio/`, and `ai/` packages; native bridges live under `app/src/main/cpp/`. Create these with the real project in Phase 1, rather than empty placeholder modules now. Keep model binaries, recordings, private messages, build output, and signing keys out of Git.

There is no build to run yet. Phase 0 validation covers document completeness, links, scope consistency, and Git diff checks. Future phases must build and pass applicable tests before being marked complete.
