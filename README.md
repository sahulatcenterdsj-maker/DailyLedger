# Daily Ledger — Android 1.1

Read **START-HERE-ROMAN-URDU.md** for setup in Roman Urdu.

This update adds a real Firebase account layer to the Kotlin/Compose app:
- A login page with Google sign-in, email/password sign-in, sign-up and password reset.
- Firebase-verified account IDs and retained sessions. No new offline-profile bypass.
- Account-scoped local records and Firebase Firestore account backups.
- Automatic account backup after edits (debounced), on launch/sign-in, and periodically when online.
- Automatic initial restore when the same account is used on an empty new installation.
- Explicit restore/replace choices when another phone has a different backup; no automatic merge.
- Optional manual Google Drive copies, AES-GCM encrypted with the account's Drive passphrase.
- Existing transactions, loans, kameti, savings, themes, app lock and exports.

## Required setup

Supply your own `app/google-services.json`, enable Email/Password and Google in Firebase Authentication, create a default Standard Cloud Firestore database, and publish the supplied `firestore.rules` before testing online accounts. Register the signing certificate fingerprints in Firebase. See the Roman Urdu guide for the exact debug fingerprints.

Without Firebase configuration, the app can display its login page but online account actions are disabled. No Firebase project was created or deployed with this ZIP.

Firebase Spark provides a free quota for this design; this is not unlimited hosting. No Cloud Storage bucket, Cloud Functions, paid server or service-account private key is used.

## Build

Open this folder in Android Studio. Add your Firebase configuration, sync Gradle, then build the APK. The existing package `com.sadique.dailyledger` and debug signing key have been preserved; versionCode is now 2.

A successful prior install can be upgraded only if it used the same signing key. Do not uninstall an existing app containing unbacked-up records. Previous verified Google records migrate to the corresponding Firebase UID; old offline records can be imported explicitly from Settings (see the guide).

## Data protection and limits

Firebase handles authentication and its normal transport/storage encryption. Server rules restrict backup reads/writes to the corresponding UID. Project administrators can access account backup data; account backup is not end-to-end encrypted. App login passwords are handled by the Firebase SDK and never placed in ledger settings or backup JSON.

Drive copies use application-level AES-GCM encryption and a separate passphrase kept in Android Keystore. Device-local Room data relies on the Android sandbox/device encryption, as before. Android automatic backup excludes Keystore-bound secrets and Firebase Auth shared preferences.

Each account has one latest compressed Firestore snapshot, capped at 900,000 Base64 characters to stay below the document size limit. This is backup/restore, not real-time multi-device record merging. Background execution follows Android scheduling constraints. If a quota, size limit or conflict blocks a backup, local records remain available and Settings shows the status.

## Verification

19 backup-core Java checks passed. Android compilation, live authentication, emulator security-rule checks and UI/device testing still require the environment/configuration described in `TESTING.md`. The supplied ZIP is source code, not a verified APK.
