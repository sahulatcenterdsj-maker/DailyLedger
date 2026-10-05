# Verification

## Completed here

`bash tests/run-core-tests.sh` compiled and ran the actual `BackupPolicy.java` and `SnapshotCodec.java` production helpers using Java 17: **19 checks passed**.

Cases include new-account upload, fresh-device restore, local/remote conflicts, stale-device protection, intentional deletion of all records, retry after a committed write, deleted remote snapshots, UTF-8/Urdu round-trip, SHA-256 integrity, corrupted gzip, invalid encoding and size/decompression bounds.

Project XML/JSON/YAML and ZIP integrity were also checked during packaging.

## Not completed here

Full Android compilation and device/UI execution were blocked before dependency resolution: Gradle could not download because the execution environment's network was unavailable. No Firebase configuration was supplied. Real Google sign-in, signup/reset email, Firestore access-rule execution, Google Drive consent and live backup/restore have not been verified. No APK is included.

## Security-rule tests (local emulator only)

With Node and the Firebase Emulator Suite available, install test-only dependencies locally:

```sh
npm install --no-save firebase @firebase/rules-unit-testing firebase-tools
npx firebase emulators:exec --only firestore --project demo-daily-ledger "node tests/firestore-rules.test.mjs"
```

The included 9 rule cases cover owner access, unauthenticated access, cross-account access and invalid document writes. The demo project uses the local emulator and must not be replaced by a production deployment command for this test. These tests are supplied but were not executed here.

## Android smoke checks after Firebase setup

1. Fresh install: login page; wrong password and bad signup input show errors without opening the ledger.
2. Sign up with email/password; add an income and expense; force an account backup and confirm its success status.
3. Sign out, sign in again, reset password by email, and verify account continuity.
4. Google sign-in first creates an account; subsequent sign-in returns to the same UID. Canceling the Google picker keeps the login screen.
5. Second device: same account, empty local DB, online initial check restores all six ledger collections. A different account cannot see the first account's data.
6. Offline change: records remain local; backup runs after connectivity returns. Disable automatic backup and check that new automatic requests do not upload.
7. Two devices with divergent edits: stale upload is blocked. Restore/replace choices require confirmation; canceling leaves the data unchanged.
8. Google Drive: save an encrypted copy, restore with correct passphrase, reject wrong passphrase without clearing local records.
9. Upgrade without uninstalling from the same signed debug app: Google local records migrate after verified Google login; check old offline profiles using the guide.
10. Check dark mode, small-screen keyboard scrolling, fingerprint lock, and sign-out/account switching during a pending backup.
