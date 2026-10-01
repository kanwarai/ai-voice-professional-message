# Engineering rules

## Project constraints

Build a simple native Android V1 using Kotlin, Jetpack Compose, Material 3, MVVM, ViewModel, StateFlow, and Navigation Compose. Use local whisper.cpp for speech recognition and local Qwen3-0.6B for rewriting only. Never send audio or transcripts to third-party AI APIs. Do not run Ollama inside Android. No paid dependencies without approval.

Phase 3 is complete except for checks that require a connected Android device. Do not begin Phase 4 or later work until the product owner requests it. Read the product, architecture, AI, privacy, and task documents before work. Explain important choices plainly and make routine technical decisions without unnecessary questions.

## Required rules

1. Keep architecture simple.
2. Do not overengineer V1.
3. Never silently ignore exceptions.
4. Handle Android lifecycle correctly.
5. Run builds after meaningful changes.
6. Run tests where applicable.
7. Fix build failures before proceeding.
8. Do not leave fake production implementations.
9. Clearly label temporary development implementations.
10. Do not hard-code secrets.
11. Do not introduce paid dependencies without approval.
12. Prefer stable and maintained libraries.
13. Do not rewrite unrelated working code.
14. Keep documentation updated.
15. Keep TASKS.md updated.
16. Mark completed items explicitly.
17. Document unresolved technical risks.
18. Perform self-review after each implementation phase.
19. Use clear Git commits at safe milestones if Git is initialized.
20. Accessibility should be considered during UI implementation.
21. Privacy-sensitive data must not appear in logs.
22. The application should degrade gracefully when AI models fail.
23. The UI must never freeze while model inference is running.
24. Long-running AI work must occur away from the main UI thread.
25. Do not move to the next major phase while the current phase is broken.

## Practical interpretation

Use safe error codes rather than private payloads in diagnostics, including native libraries. Propagate cancellation and release native resources. Never substitute a canned rewrite for actual model success. Test doubles belong in tests or explicitly labeled debug-only demonstrations. Document unrun checks and blockers honestly. For documentation-only work without a build project, record that builds are not applicable. Pin native revisions, dependency versions, and model hashes before integration; benchmark on real devices before promising compatibility or speed.
