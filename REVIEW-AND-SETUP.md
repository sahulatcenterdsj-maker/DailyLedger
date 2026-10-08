# Daily Ledger 1.6.1 review

The uploaded project is a useful native finance app foundation. Salary/expense tracking, separate savings and kameti totals, partial committee receipts, reviewed AI entry, themes, weather and optional transaction sounds are retained. It is a personal ledger, not a bank integration or accounting/tax compliance product.

## Version 1.6.1 handoff integration

The latest handoff is merged selectively into the encrypted project. It adds a separate Udhar Saman ledger with shops/contacts, purchase and due dates, partial payments, account-isolated backup/restore and reviewed offline entry. The catalog now has 105 categories with more specific item matching. It keeps repeated purchases and rejects an unclear or oversized batch instead of silently dropping entries.

Quick entry honors explicit ISO, DD/MM/YYYY and named dates. Omitted years use the current year and omitted dates use today; invalid dates and ambiguous `kal` request correction. Each draft remains editable before saving, including separate purchase/due dates for Udhar. Udhar is tracked separately from cash expenses so it is not counted twice against salary.

The aqua interface includes the scenic home header, salary-minus-expenses balance, separate savings/kameti totals, category chart, reviewed entry cards and accurate backup status. Savings use a wallet icon. The release is non-debuggable and excludes Firebase's debug App Check provider. Settings can test app verification without sending ledger entries; the actual Firebase console setup remains a separate step.

## Retained version 1.5 features

The new ZIP adds offline quick-entry parsing and finance insights, borrowed/lent loan contact actions and payment methods/history, and kameti member turns with contact picking and reminders. Core offline behavior needs no AI key or paid API. The parser is a focused rules engine, not an on-device LLM; complex wording needs manual entry or optional cloud fallback.

Review changes also prevent silent batch truncation, parse grouped rupees and Urdu digits, use category names from the ledger, preserve full loan history, make loan forms scrollable, and avoid contact-action crashes when another app is unavailable. Essential food is excluded from optional-spending reduction estimates and future-dated savings are excluded from current progress.

## Issues corrected

- Added SQLCipher encryption for the local Room ledger, with a random key protected by Android Keystore. Existing plaintext databases migrate on a temporary encrypted file, are verified, and are atomically replaced. Failure does not erase the original. Room schema version is 5, with additive migrations for contacts, payment methods, structured kameti members and Udhar. Ledger JSON format is 4 and includes Udhar purchases/payments; older payloads remain readable. This is separate from the encrypted cloud envelope, which remains V3.
- Added a non-deleting SQLCipher corruption handler and startup retry screen. Android's automatic backup remains excluded because device keys cannot be copied to another phone.
- Fixed a backup data-loss risk: an ambiguous Firestore timeout must not delete the newly uploaded ciphertext, because the transaction may already have committed.
- Cloud backup deletion now compares the observed revision transactionally. It cannot silently delete a newer backup from another device.
- V3 backup payloads authenticate the Firebase UID and backup revision with AES-GCM AAD. Change detection uses a keyed fingerprint instead of a plaintext SHA-256 hash. V1/V2 restore remains supported; new writes use V3 only.
- Key recovery now checks the exact revision, format and wrapped-key fingerprint. A different server key cannot be cached under stale metadata.
- New rules prohibit plaintext/older-format writes and validate the exact owner/revision object path. Storage wildcard syntax and object metadata validation were corrected.
- KMS Functions retain Auth and App Check checks, reject anonymous users, limit key requests to 30/account/UTC day and cap instances at 3. These are abuse controls, not a spending guarantee.
- AI has fresh provider-specific consent, fixed safe error messages, bounded requests, account/consent rechecks, structured output validation and local limits. Custom category names are omitted from automatic cloud summaries. Cached advice is Keystore-encrypted.
- Distributed APKs use Play Integrity instead of development-only App Check debug tokens.

## Cloud setup and cost

This ZIP chose a managed **Firebase Functions + Cloud KMS + Storage** recovery design. It restores after the same Firebase login without another passphrase. Firebase administrators with sufficient KMS/runtime privileges can decrypt records. This is **not WhatsApp-style zero-knowledge end-to-end encryption**. A user ID authorizes the account; it is never the encryption key.

Blaze billing is required for this backend. Storage/Functions may have no-cost allowances, but KMS key versions and operations can cost money. Linking billing also changes Gemini Developer API billing; do not promise that cloud AI stays free on this project. No billing upgrade or cloud deployment has been performed by this review.

Required owner setup:

1. For optional cloud AI only, enable Firebase AI Logic (Gemini Developer API) and configure Android App Check / Play Integrity. For APK distribution outside Play, use Firebase's documented outside-Play provider settings; do not require PLAY_RECOGNIZED or LICENSED verdicts. Keep device integrity checks enabled.
2. Decide whether to enable Blaze. If approved, follow `KMS-SETUP.md`, then deploy Functions, Firestore and Storage rules. Do not delete/disable the KMS key versions while backups depend on them.
3. On two real phones, verify same-account backup/restore, an account mismatch, cancelled restore, offline edits and a changed remote revision.

The developer signing key matches the existing APK to allow an update. It is a public development key, not a private production signing identity. Public distribution should move to a separately managed production signing/release process.

## Data boundaries

Firebase Auth profile fields and backup metadata remain visible to authorized administrators. The ledger database and V3 cloud payload are encrypted. CSV/PDF exports are readable files the user explicitly creates. Legacy backups, downloaded exports, storage soft-delete/version history and previously written disk blocks are not retroactively erased by encryption.

Gemini receives only the data stated in the app's separate consent; encrypted backup does not make AI requests end-to-end encrypted. Weather is optional and sends the selected city to Open-Meteo, without GPS permission.

## Verification status

The [final CI run](https://github.com/sahulatcenterdsj-maker/DailyLedger/actions/runs/37755903942) passed: 46 JVM unit tests, 21 Android tests, Firestore/Storage rule checks, 11 Function handler checks, 19 Java backup-core checks and release APK signing/package/provider verification. The APK is v1.6.1 (code 9). Screen previews were inspected. Details and SHA-256 are in `TESTING.md`.

Live project AI inference and live two-device KMS restore require owner cloud setup and are not verified here. Cloud Browser returned 502 / connection refused at the Firebase/Google sign-in flow, so console settings could not be checked despite the reported owner login. No paid resource provisioning or billing-plan change was performed.
