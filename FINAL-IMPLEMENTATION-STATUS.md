> Updated review: see `REVIEW-AND-SETUP.md` for v1.4.1 changes and the current verification status.

# Daily Ledger – Final implementation status

This source tree combines the UI/weather/sound redesign with the Firebase AI and encrypted account-backup hardening work.

## Implemented

- Modern Compose dashboard, balance progress/percentages, recent activity/spending visuals, polished cards/effects.
- Manual-city weather (no GPS permission) using current conditions with a cached refresh and Open-Meteo attribution.
- Optional soft retro transaction sounds for income, expense and reviewed AI batch-save success.
- Firebase AI Logic Android SDK (`firebase-ai`) on Firebase Android BoM 34.19.0.
- Gemini Flash model constant: `gemini-3.5-flash-lite`.
- AI structured JSON response schemas plus local validation; AI creates reviewable drafts only.
- Play Integrity App Check for distributed builds.
- AES-256-GCM encrypted Firebase account backup with a random 256-bit DEK and fresh 96-bit nonce per encryption.
- Cloud KMS envelope key wrapping through authenticated callable Functions.
- Callable Functions use `request.auth.uid`, server-side Firestore key lookup and `enforceAppCheck: true`.
- KMS AAD binds key operations to Firebase UID + backup format version.
- Fresh-install `Backup found -> Restore / Skip` prompt; no silent restore overwrite.
- V1 backup restore + explicit V2 migration with a newly generated wrapped DEK.
- Stale local DEK protection: cache is tied to the current wrapped-key fingerprint and decryption retries once after a server refresh.
- V2 plaintext hash for change detection plus ciphertext SHA-256 and AES-GCM authentication.
- New encrypted object cleanup when Firestore metadata commit fails; old object cleanup only after commit succeeds.
- Cloud backup deletion removes authoritative metadata first and then best-effort ciphertext cleanup.
- Owner-scoped Firestore and Storage security rules with V1 read/migration compatibility.
- Android system cloud/device backup disabled/excluded so the plaintext Room/DataStore state cannot bypass the app's encrypted backup design.
- Old Cloudflare/Groq/Wrangler runtime/build references removed.
- GitHub Actions updated for Android + Firebase rules/functions tests.
- KMS provisioning guide in `KMS-SETUP.md`.

## Offline verification completed in this workspace

- Backup core Java checks: 19 passed.
- Cloud Function pure-handler tests: 7 passed.
- Cloud Function JavaScript syntax/lint checks: passed.
- Android manifest / backup XML parsing: passed.
- Static scan: no Cloudflare, workers.dev, Groq, Wrangler, firebase-vertexai or ai-service.json runtime references remain.

## Must be completed on a machine with internet / Firebase access

1. Android Studio Gradle Sync, `testDebugUnitTest`, `assembleDebug`, and connected Android tests.
2. `npm install` at project root and in `functions/`, then Firebase Emulator rules tests.
3. Create/authorize the Cloud KMS key and set the required `KMS_KEY_NAME` Functions parameter as documented in `KMS-SETUP.md`.
4. Deploy Functions, Firestore rules and Storage rules.
5. Register App Check debug token for development and configure Play Integrity; enforce App Check for production AI usage.
6. Test same-account backup/restore on two devices/emulators before release.

The account-backup design is encrypted and supports same-account recovery without a passphrase, but it is not zero-knowledge E2EE because the trusted Firebase Function + Cloud KMS backend can recover the DEK for an authenticated account.
