# Verification — Daily Ledger 1.3.0

## Completed automated checks

The [successful APK workflow run](https://github.com/sahulatcenterdsj-maker/DailyLedger/actions/runs/37481745184) built app version `1.3.0` / version code `4` from commit `d69e564ed5c3c74f3ae56fb64c7f7ef3a5996a80`.

- Shared AI backend: 13 Node tests passed, including signed-token verification, invalid claims/signatures, request/output validation, quotas, unavailable provider handling and absence of an AI chat route. Wrangler dry-run bundling passed.
- Backup core: 19 Java checks passed using the production `BackupPolicy.java` and `SnapshotCodec.java` helpers.
- Android JVM tests and APK/test-APK compilation passed.
- Android emulator: 12 instrumentation tests passed, covering data upgrades, partial receiving, AI draft parsing/account isolation, atomic/idempotent saves and review UI.
- APK package, version, checksum and signing certificate were checked. The signing certificate matches the preceding installed development build.
- Wallet savings, Auto Fill review and spending-insight screenshots were inspected. Screenshots and generated report files are in the workflow artifacts.

Backup-helper cases include new-account upload decisions, fresh-device restore decisions, local/remote conflicts, stale-device protection, intentional deletion of records, committed-write retry, deleted remote snapshots, Urdu round-trip, integrity hashes, damaged gzip and size/decompression limits. These are local automated checks, not production Firebase round trips.

The later endpoint-configuration change does not change the APK binary. Documentation-only changes do not rerun these Android checks or produce a new APK.

## Live verification boundaries

The owner confirmed that Firebase login works. On 7 October 2026 the owner also observed `ready:true` at `https://dailyledger.sadique6571.workers.dev/health`, and that public Worker root was added to the configuration read by app 1.3.0.

`ready:true` only means a Groq key is configured. It does not test provider credentials, model availability, authenticated inference, quotas, or saving returned drafts. Live signed-in Auto Fill and automatic cloud suggestions are still pending device verification.

Production Firestore rules/IAM, cross-account server access, current live backup/restore and Google Drive consent/passphrase recovery were not audited by this documentation update. Automated local tests do not establish those production guarantees. Firebase backups are admin-readable; optional Drive copies are passphrase-encrypted, as explained in [README.md](README.md).

## Repeat local checks when changing the relevant code

```sh
npm test
npm run check:worker
bash tests/run-core-tests.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

The Worker dry-run requires installed npm dependencies. Android checks require the configured SDK/JDK; connected tests need an emulator/device. No live provider secret is needed for the automated backend fixture tests.

## Firestore security-rule tests

The supplied `tests/firestore-rules.test.mjs` is a separate emulator test suite. The APK workflow listed above does not execute it. Use a local demo project, not the production database:

```sh
npm install --no-save firebase @firebase/rules-unit-testing firebase-tools
npx firebase emulators:exec --only firestore --project demo-daily-ledger "node tests/firestore-rules.test.mjs"
```

These cases cover owner access, unauthenticated/cross-account access and invalid document writes. A local rules test does not prove which rules are currently deployed in production.

## Device checks after configuration

1. Sign in/sign up, verify invalid credentials stay on the login screen, and check password reset and Google picker cancellation.
2. Add an income/expense, use **Back up now**, and check the successful account-backup status.
3. On an empty second installation, sign in to the same account and verify all backed-up ledger collections, including kameti receipts, restore. Check account isolation.
4. Test offline changes, reconnected backup, and automatic-backup off. With divergent edits on two phones, check that overwrite is blocked until an explicit restore/replace choice.
5. Save an encrypted Drive copy, restore with its original passphrase, and verify a wrong passphrase leaves existing local records intact.
6. Upgrade using the same signing identity without uninstalling; check existing records, multiple kameti shares and partial receiving history.
7. Enable Cloud AI consent, generate Auto Fill drafts with a sample income/expense sentence, inspect/edit amounts and categories, and save only real records. Discard test drafts rather than adding them to the ledger.
8. Check automatic cloud suggestions with recorded expenses and confirm the app remains usable if the provider is unavailable or quota is reached.
9. Check themes, keyboard scrolling, biometric/device lock and account switching during pending backup or AI requests.
