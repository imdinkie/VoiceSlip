# VoiceSlip Agent Context

- This is the VoiceSlip Android app repository.
- Machine-specific agent notes belong in `AGENTS.local.md`, which is gitignored.
- After building the Android app, automatically install the debug APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk` when a device is connected via ADB.
- Agents must never install release, signed release, staging, universal, Play-signed, F-Droid, or any other non-debug build on a connected phone.
- The only APK agents may install via ADB is `app/build/outputs/apk/debug/app-debug.apk`.
- Do not run `adb install` on release APKs, `.aab` files, generated universal APKs, Play/F-Droid artifacts, or manually signed artifacts unless the user explicitly overrides this rule in the current conversation.

## Agent skills

### Issue tracker

Issues and PRDs are tracked in GitHub Issues for `imdinkie/VoiceSlip`. See `docs/agents/issue-tracker.md`.

### Triage labels

This repo uses the default triage label vocabulary. See `docs/agents/triage-labels.md`.

### Domain docs

This is a single-context repo; prefer repo-local `CONTEXT.md` and `docs/adr/`. See `docs/agents/domain.md`.
