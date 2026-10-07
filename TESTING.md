# Testing Daily Ledger 1.5.1

## Automated release verification

Build `919ecaa` compiled and passed JVM tests plus all **16 Android device tests** on 2026-10-07. Its artifact stage was blocked by a screenshot shell-script error; the corrected packaging build is pending.

Local execution: 19 Java backup-core checks and 11 Cloud Function handler checks passed on 2026-10-07. Firebase security rules and full Android compilation/instrumentation run in GitHub Actions.

The automated suite covers:

- Firestore/Storage owner isolation, V3-only client writes, metadata/path binding, content bounds, immutable encrypted objects and deletion.
- Function Auth/App Check declarations, anonymous rejection, UID/version KMS context and revision/fingerprint matching.
- Backup policies, stale revision conflicts, interrupted/ambiguous commits and no deletion of a potentially committed upload.
- AES-GCM round trip, wrong key/tampering, UID/revision binding and keyed change detection.
- Local database encryption, interrupted migration/retry, wrong-key preservation and Keystore namespace separation.
- Real schema 1 → 4 migration with existing records; legacy JSON restore; new loan contact/payment fields and member turns through account-isolated export/import; invalid import rollback.
- Loan overpayment/settlement, duplicate or excessive personal kameti turns, partial receiving and related-record deletion.
- Offline parsing of Roman Urdu, Urdu digits, grouped rupees and k/hazar amounts; ambiguous/dedicated/more-than-10-entry batches are not silently dropped.
- Salary/expense separation, optional-spend estimates, past-month comparisons, owner/date filtering and future savings exclusion.
- Cloud request timeouts, account/consent changes, safe failures and structured-output checks.
- Android review/edit/save UI, offline entry without cloud consent, dashboard totals and partial kameti receiving.

## Reproduce

Use Java 21, Node 22 and the Android SDK specified in the Gradle files.

```bash
npm install
npm test
npm --prefix functions install
npm --prefix functions run lint
npm --prefix functions test
bash tests/run-core-tests.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

CI uses an Android 10 (API 29) emulator. It captures UI previews and verifies the APK package/signing certificate before publishing the installable artifact. These checks do not claim coverage on every Android device.

## Live setup checks still required

The shared APK uses Play Integrity App Check, not a shared debug token. Configure the provider for installation outside Play as documented in `FIREBASE-AI-SETUP.md`. Do not disable production checks simply to make tests pass.

After owner configuration, test Google/email login, optional Gemini inference, weather/contact handlers on a real phone, and same-account encrypted backup/restore on two devices. Verify Restore/Skip, offline edits, changed remote revisions, legacy backup upgrade to envelope V3 and deleted backups. Do not erase the only local copy to test recovery.

No billing upgrade, Functions/KMS deployment or live two-device cloud restore has been performed in this review. Offline Mini AI and the local ledger do not require that optional cloud setup.
