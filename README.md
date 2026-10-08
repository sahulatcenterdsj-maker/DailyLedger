# Daily Ledger

A native Android personal-finance app for PKR income, spending, savings, loans and kameti. Version **1.6.1** integrates the handoff’s Udhar Saman ledger and expanded categories with the encrypted database, protected backups and refreshed aqua interface.

- Salary minus monthly expenses; savings and kameti have separate totals.
- Multiple kameti shares, partial receiving, structured member turns, contact actions and reminders.
- Borrowed/lent loans with payment methods, complete payment history and overpayment checks.
- Google/email sign-in using Firebase Authentication.
- Offline Mini AI: reviewed Roman Urdu/Urdu/English quick entry and local spending/saving suggestions. Common entries require no internet, model download or paid API. It is a small rules engine, not an LLM.
- Optional Firebase Gemini fallback for complex wording; separate consent required. No AI chat and no automatic saving of generated entries.
- 105 built-in income/expense categories; a wallet icon for savings and a green bar-chart launcher icon.
- Separate **Udhar Saman** ledger: goods bought on credit, shops/contacts, due dates, partial payments and editable offline auto-fill.
- Dated quick entry: `1 Oct 2026 doodh 150, 2 Oct 2026 petrol 1,500` prepares two entries on those exact dates. ISO and DD/MM/YYYY are supported; omitted years use the current year and omitted dates use today. Invalid dates and ambiguous `kal` request correction.
- Review/edit every date, amount and category before saving. Repeated purchases are retained; retries do not duplicate a saved batch.
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
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease
./gradlew connectedDebugAndroidTest
```

GitHub Actions builds and verifies the non-debuggable release APK and publishes its test reports. Install an update over the existing app; do not clear its data to update. The fixed development signer preserves update compatibility, but is not suitable as a private production signing identity.

## Integration status

Cloud AI uses **Firebase AI Logic + Gemini**, authenticated through the existing Firebase project. This Android version does not call the old Cloudflare/Groq worker; legacy worker source is retained for older clients. Firebase Authentication handles sign-in, and the optional backup backend uses Firestore metadata, Storage ciphertext, authenticated Cloud Functions and Cloud KMS. AI activation and backup activation are separate.

`AI_APP_CHECK` identifies an app-attestation failure, not proof that a phone is uncertified. Release builds use Play Integrity. Private `debug` builds use individually registered test tokens and must not be distributed. Use **Settings → Check app verification**, then confirm Firebase registration, the actual signing SHA-256 and outside-Play settings. Never add a universal debug token or disable server enforcement to make AI appear to work.

Source/CI checks do not prove a successful request to the live Firebase project. App Check/AI activation and KMS cloud recovery require a signed-in project owner to complete the console setup. Live console access in this review was blocked by a Cloud Browser 502 response; this is not a verified Firebase service outage. Current build results and remaining live checks are recorded in [TESTING.md](TESTING.md).

## Verified APK

Version **1.6.1** (code **9**) passed [CI and Android device tests](https://github.com/sahulatcenterdsj-maker/DailyLedger/actions/runs/37755903942): 46 unit tests, 21 Android tests, Firebase rules, Function handlers and backup-core checks. The non-debuggable release keeps the earlier package/certificate for in-place updates. See [test results and checksum](TESTING.md). Live optional cloud services still need the owner setup described above.
