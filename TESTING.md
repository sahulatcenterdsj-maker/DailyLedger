# v1.6.1 verification in progress

New gates: exact supplied dates, mixed dates, invalid/leap dates, Urdu digits/grouped money, repeated purchases, whole-batch rejection, specific categories, Udhar partial payment bounds, retry idempotency, account isolation and backup rollback. Android tests exercise encrypted schema upgrades, editable dates and the new settings/navigation UI. Release packaging checks prohibit a debuggable APK or Firebase debug provider.

Locally passed: 19 Java backup-core checks and 11 Cloud Functions handler tests. Full Android CI and visual checks are pending for this revision; historical results below describe v1.5.1 only.

---

# Testing Daily Ledger 1.5.1

## Automated release verification

Verified on **2026-10-07**, commit **d81a4485ecb76dc2435322d8e6a24ce613e68f5a**:
[successful GitHub Actions run](https://github.com/sahulatcenterdsj-maker/DailyLedger/actions/runs/37652728165).

- 40 JVM unit tests: passed, no skipped tests.
- 16 Android device tests: passed, no skipped tests (Android 10 / API 29).
- Firestore and Storage emulator security-rule checks: passed.
- 11 Cloud Function handler tests and 19 Java backup-core checks: passed.
- APK package and matching development signing certificate: verified.
- Captured dashboard, separate savings totals, reviewed Auto Fill, offline insights and partial kameti receiving screens: inspected.

APK: **1.5.1**, version code **7**, package `com.sadique.dailyledger`, **37,507,047 bytes**.

SHA-256: `f86a30fb7c6d46e6d9b9178d23b4fe64700a7e254ac98d7341b5ddf0f81645f0`.

The earlier attempt passed app/device tests but failed while collecting screenshots. The final run above fixes that script and completes packaging successfully.

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
