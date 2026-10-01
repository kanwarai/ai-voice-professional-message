# Vendored whisper.cpp core

- Project: `ggml-org/whisper.cpp`
- Release: `v1.9.4`
- Commit: `927cfce34f31707e17f2bff35c349632fb9e2c3a`
- Source: `https://github.com/ggml-org/whisper.cpp`
- License: MIT; see `LICENSE` in this directory.

This directory contains only the Whisper public header, Whisper core implementation,
and CPU-only ggml files required by the Android build. Examples, tools, tests, and
non-CPU backends are intentionally excluded. Local modifications are confined to the
app-owned JNI and top-level CMake files outside this directory.
