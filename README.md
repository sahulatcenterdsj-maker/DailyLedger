# Daily Ledger

A native Android personal-finance app for PKR income, spending, savings, loans and kameti. Version **1.4.1** retains the uploaded dashboard, themes, weather and sounds, and adds safer encryption, backup recovery and Firebase AI handling.

- Salary minus monthly expenses; savings and kameti have separate totals.
- Multiple kameti shares and partial receiving.
- Google/email sign-in using Firebase Authentication.
- Firebase Gemini suggestions and reviewed Auto Fill. No AI chat and no automatic saving of generated entries.
- SQLCipher-encrypted local ledger; existing records migrate without a destructive database reset.
- AES-256-GCM account backups, with same-account recovery and no extra backup passphrase through authenticated Functions + Cloud KMS.
- Explicit Restore / Skip on a new phone; conflicting phone revisions require a choice.
- Optional manual Drive copies, CSV/PDF exports, city weather and transaction sounds.

## Setup and privacy

Read [Review and setup](REVIEW-AND-SETUP.md), [Firebase AI setup](FIREBASE-AI-SETUP.md), and [KMS setup](KMS-SETUP.md).

**The KMS/Functions/Storage backup needs Blaze billing.** It is managed account encryption: authorized backend administrators can recover the key. It is **not zero-knowledge E2EE**. UID scopes access; a random 256-bit key encrypts the backup. Firebase contains ciphertext and metadata, while KMS holds the wrapping key. Old formats are readable for migration but new client writes must use V3.

Backups depend on successful uploads, account access, and the server key remaining available. Check the app's last successful backup status; an enabled switch alone does not mean a backup exists. CSV/PDF exports remain readable. AI is separately opt-in, with its own data disclosure and usage limits.

## Build and verify

```bash
bash tests/run-core-tests.sh
npm --prefix functions test
npm install
npm test
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

GitHub Actions builds and verifies the APK and publishes its test reports. Install an update over the existing app; do not clear its data to update. The fixed development signer preserves update compatibility, but is not suitable as a private production signing identity.
