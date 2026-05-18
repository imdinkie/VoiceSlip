# VoiceSlip

VoiceSlip is an Android voice dictation workflow app built for fast capture, transcription, rewriting, and insertion into other apps.

The app records audio, sends it through a configurable pipeline, applies cleanup and style rules, and pastes the result back into the target app through an accessibility service. The goal is to make voice input usable for everyday messaging, notes, and app-specific writing flows instead of stopping at raw transcription.

## What VoiceSlip Tries To Do

Most speech-to-text apps stop at a transcript. VoiceSlip treats dictation as a pipeline:

1. Capture audio quickly from anywhere on the device.
2. Transcribe it with a model that fits the job.
3. Optionally rewrite or clean it into a chosen style.
4. Insert the final text into the active app.

That makes it useful for cases like:

- sending messages that sound like you instead of raw transcripts
- using different writing styles for different apps or contexts
- biasing transcription toward names, products, jargon, and recurring terms
- switching between low-latency direct audio models and more structured multi-step pipelines

## Current Feature Set

- Floating recording bubble powered by an accessibility service
- Android app-to-app insertion flow for dictated text
- Three pipeline modes:
  - Pure transcription
  - Transcription plus post-processing
  - Audio direct
- Provider support:
  - Mistral for transcription and audio-chat/direct flows
  - Groq for transcription and text post-processing
  - OpenRouter for text post-processing and dynamic audio-capable models
- Per-provider model selection and cached model lists
- OpenRouter audio model favorites and picker flows
- Shared dictionary for names, brands, and domain-specific spelling constraints
- Style system with built-in and custom prompts
- App/category-based style routing
- Recording history with retry support
- Language preservation and optional language hints
- Pipeline preview UI showing how the current configuration will behave

## How The Pipeline Works

VoiceSlip supports three execution patterns.

### Pure transcription

Audio goes straight to a transcription model and the transcript becomes the final inserted text.

### Transcription plus post-processing

Audio is first transcribed. The raw transcript is then sent to a text model with cleanup rules, style instructions, and dictionary constraints. This is the most controllable path when you want the final output to sound intentional rather than verbatim.

### Audio direct

Audio is sent to an audio-capable chat model that returns final insertable text in one step. This is the simplest path when you want speed and are comfortable letting the audio model handle both recognition and rewrite behavior.

## Providers And Models

The app currently supports these provider families:

- `Mistral`
  - Voxtral Mini transcription endpoint
  - Voxtral Mini audio chat
  - Voxtral Small audio chat/direct
- `Groq`
  - Whisper Large V3
  - Whisper Large V3 Turbo
  - Text post-processing models fetched from the API
- `OpenRouter`
  - Text models for post-processing
  - Dynamic audio-capable models for transcription and audio-direct flows

OpenRouter audio support is dynamic rather than hardcoded. Compatible audio models are fetched from the OpenRouter models API and cached locally. Favorites can be pinned so frequently used models remain easy to select.

## Dictionary Behavior

VoiceSlip includes a shared dictionary for spelling constraints such as names, companies, product terms, and domain-specific vocabulary.

Dictionary behavior depends on the execution path:

- Mistral transcription endpoint uses `context_bias`, which only accepts provider-safe single tokens. Multi-word entries are split into safe terms before submission.
- Audio-chat and audio-direct prompts still receive the original full phrases so phrase-aware models can use them directly.
- Cleanup and post-processing always see the full dictionary as prompt guidance.

This split is intentional because the Mistral transcription API and the chat-style audio paths have different contracts.

## App Structure

High-level areas in the current app:

- `Interaction`: bubble behavior and runtime interaction settings
- `Permissions`: microphone, overlay, accessibility, notifications, and related setup
- `API keys`: provider keys stored locally on-device
- `Models`: pipeline mode, transcription model, audio-direct model, post-processing model, language hints, and preview
- `Style`: built-in/custom styles and app/category routing
- `Dictionary`: spelling constraints and routing visibility
- `History`: recent recordings, results, and retry handling

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- Android Accessibility Service for insertion workflow
- Gradle Kotlin DSL
- GitHub Actions for CI

## Requirements

- Android Studio or a working Android SDK toolchain
- JDK 21 recommended for the Gradle/AGP setup in this repository
- Android SDK for compile/target SDK 36
- At least one provider API key to do anything useful at runtime
- Android device or emulator for manual testing

The app module currently targets:

- `minSdk = 29`
- `targetSdk = 36`
- `compileSdk = 36`

## Local Development

### Build the debug APK

```bash
./gradlew :app:assembleDebug
```

Generated artifact:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Build V1 APK release artifacts

The manual GitHub `Android Release` workflow runs the release quality gate, builds the tester debug APK and signed release APK, and generates checksums. VoiceSlip V1 does not build app bundles because store publication is deferred.

### Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

### Install on a connected device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissions And Why They Exist

- `RECORD_AUDIO`: capture dictated audio
- `SYSTEM_ALERT_WINDOW`: show the floating bubble
- `BIND_ACCESSIBILITY_SERVICE`: insert text into other apps and understand the active target
- `INTERNET`: call provider APIs
- `POST_NOTIFICATIONS`: foreground-service and runtime notifications on newer Android versions
- `WAKE_LOCK` and foreground-service permissions: keep recording/transcription flows alive reliably

## Privacy And Security Notes

- Audio and text may be sent to third-party model providers depending on the selected pipeline.
- API keys are configured locally in the app.
- Recordings and history are stored locally by the app unless explicitly deleted.
- Android backup and device transfer are disabled for V1 so VoiceSlip data stays inside the app install unless the user sends audio/text to a selected provider.

## Status

VoiceSlip is an actively evolving project. The model architecture already supports dynamic OpenRouter audio routing, provider-specific post-processing selections, style-based rewriting, and provider-specific dictionary behavior, but there is still room to harden the UX, privacy story, onboarding, and distribution flow before a wider public launch.
