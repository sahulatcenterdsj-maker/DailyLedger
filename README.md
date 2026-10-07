# Daily Ledger

A native Android personal-finance app for PKR income, spending, savings, loans and kameti. Version **1.5.1** combines the uploaded Offline Mini AI, loan contacts and kameti turns with the reviewed encryption and backup fixes.

- Salary minus monthly expenses; savings and kameti have separate totals.
- Multiple kameti shares, partial receiving, structured member turns, contact actions and reminders.
- Borrowed/lent loans with payment methods, complete payment history and overpayment checks.
- Google/email sign-in using Firebase Authentication.
- Offline Mini AI: reviewed Roman Urdu/Urdu/English quick entry and local spending/saving suggestions. Common entries require no internet, model download or paid API. It is a small rules engine, not an LLM.
- Optional Firebase Gemini fallback for complex wording; separate consent required. No AI chat and no automatic saving of generated entries.
- 93 built-in income/expense categories; a wallet icon for savings.
- SQLCipher-encrypted local ledger; existing records migrate without a destructive database reset.
- AES-256-GCM account backups, with same-account recovery and no extra backup passphrase through authenticated Functions + Cloud KMS.
- Explicit Restore / Skip on a new phone; conflicting phone revisions require a choice.
- Optional manual Drive copies, CSV/PDF exports, city weather and transaction sounds.

See [v1.5 feature notes](OFFLINE-MINI-AI-LOANS-KAMETI.md). Phone contacts use the system picker; Call/SMS/WhatsApp actions require the user to send or call.

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
