# Daily Ledger — Native Android

A clean, local-first personal finance app written in Kotlin + Jetpack Compose.

## Included
- Google Sign-In screen (Credential Manager; requires your OAuth client ID)
- Separate Dashboard, Transactions, Loans, Kameti, Savings modules
- PKR-first money model using integer paisa (no floating-point storage)
- Room local database with account-scoped records
- Dark / Light / System themes
- Loan repayments and outstanding balances
- Kameti installment history and payout status
- Direct savings and leftover savings separately
- Encrypted Google Drive `appDataFolder` backup/sync
- WorkManager background sync and reminder checks
- AES-GCM cloud-backup encryption using a user sync passphrase
- Sync passphrase stored locally using Android Keystore
- Biometric/device-credential app lock
- CSV and PDF exports to Downloads

## Open and build
1. On Linux/macOS, run `./gradlew --version` once. On Windows run `gradlew.bat --version`. The bootstrap script downloads the official Gradle wrapper jar if it is missing.
2. Open this folder in Android Studio and let Gradle sync.
3. For local-only testing, press Run. The login screen includes an offline profile.
4. For Google login + Drive sync, follow `GOOGLE-SETUP.md`.
5. Build APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

See `BUILD-APK.md` for command-line build steps.

## Privacy model
Financial data is local-first. Each signed-in account has its own owner ID in Room. Google Drive backup is encrypted before upload. The backup passphrase is never uploaded; keep it safe if you want to restore on another device.

Local Room data relies on Android's app sandbox / device encryption. The Drive backup itself is application-level AES-GCM encrypted.
