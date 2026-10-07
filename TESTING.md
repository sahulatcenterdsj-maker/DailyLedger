> Updated review: see `REVIEW-AND-SETUP.md` for v1.4.1 changes and the current verification status.

# Testing Daily Ledger

Run production testing only after Firebase AI Logic, App Check, Cloud KMS, Functions, Firestore rules and Storage rules are configured.

## Automated checks

From the project root:

```bash
npm install
npm test
npm --prefix functions install
npm --prefix functions run lint
npm --prefix functions test
bash tests/run-core-tests.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

`npm test` starts local Firestore + Storage emulators and verifies UID isolation, V1/V2 metadata validation, exact allowed fields, encrypted object ownership, content type, immutability and delete access. Functions tests cover authenticated UID derivation, 32-byte DEK validation, UID/version AAD, server-side metadata lookup, KMS configuration validation and the two `enforceAppCheck: true` callable declarations.

The Android/unit suite should additionally cover AES-GCM round trip/tamper/wrong key, AI parser rejection, totals, data upgrade/import and UI review flows. `LedgerRepository.importJson` performs restore inside a Room transaction.

## One complete device test round

Use a debug build with a registered App Check debug token. Then verify:

- Email/password and Google sign-in.
- Dashboard/UI, selected-city weather refresh and transaction sounds.
- Add/edit/delete income and expense; confirm balance progress updates.
- AI Auto Fill with Roman Urdu/Urdu/English; confirm drafts are editable and nothing saves before Review & Save.
- AI saving suggestions and offline/local fallback.
- First encrypted account backup, then a second changed backup.
- Fresh second device: sign in to same account, confirm **Backup found** appears and no background auto-restore occurs; test Skip and Restore.
- V1 legacy restore followed by successful V2 migration.
- Two-device conflict: newer remote data must not be silently overwritten by stale local data.
- Delete Cloud Backup: local records remain and automatic backup is turned off.
- Sign out/account switch and sign back in; key recovery must come from authenticated Functions/KMS when local cache was cleared.
- CSV/PDF export and optional manual Google Drive copy.

For stale-DEK recovery, create/rotate the account backup from another device while the first device still has its old cached key, then restore on the first device. It should refresh the server-wrapped key once and retry decryption exactly once. A genuinely damaged/tampered ciphertext must still fail after that retry.

## CI

`.github/workflows/build-apk.yml` now tests Firebase rules, backup-key Functions, backup core, Android unit/instrumentation tests and APK signing/package identity. It tests the current Firebase-only architecture.

## Environment limitation during this edit

The editing environment could not reach `services.gradle.org`, so a fresh local Gradle distribution could not be downloaded here. Final Android compilation still needs Android Studio/CI or another machine with Gradle dependencies available.
