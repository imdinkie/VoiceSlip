# Release Signing

VoiceSlip release builds use the application ID `com.imdinkie.voiceslip`.
Debug builds use `com.imdinkie.voiceslip.debug`.

Agents must only install debug APKs. Release APKs, AABs, universal APKs, and store-built artifacts must never be installed by automation unless explicitly requested in the current conversation.

## Local Signing

Release signing is configured through ignored `keystore.properties`:

```properties
storeFile=/absolute/path/to/voiceslip-release.jks
storePassword=...
keyAlias=voiceslip-release
keyPassword=...
```

The repository must not commit private keystores, signing passwords, or generated release artifacts.

## GitHub Actions

The manual `Android Release` workflow expects these repository secrets:

```text
VOICESLIP_KEYSTORE_BASE64
VOICESLIP_STORE_PASSWORD
VOICESLIP_KEY_ALIAS
VOICESLIP_KEY_PASSWORD
```

The workflow builds:

- `app-release.apk`
- `app-release.aab`
- `voiceslip-release.sha256`

If a `release_tag` input is provided, the workflow creates a draft GitHub Release with those artifacts.
