# Daily Ledger 1.4.1 review

The uploaded project is a useful native finance app foundation. Salary/expense tracking, separate savings and kameti totals, partial committee receipts, reviewed AI entry, themes, weather and optional transaction sounds are retained. It is a personal ledger, not a bank integration or accounting/tax compliance product.

## Issues corrected

- Added SQLCipher encryption for the local Room ledger, with a random key protected by Android Keystore. Existing plaintext databases migrate on a temporary encrypted file, are verified, and are atomically replaced. Failure does not erase the original. Room schema version remains 2.
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

1. Enable Firebase AI Logic (Gemini Developer API) and configure Android App Check / Play Integrity. For APK distribution outside Play, use Firebase's documented outside-Play provider settings; do not require PLAY_RECOGNIZED or LICENSED verdicts. Keep device integrity checks enabled.
2. Decide whether to enable Blaze. If approved, follow `KMS-SETUP.md`, then deploy Functions, Firestore and Storage rules. Do not delete/disable the KMS key versions while backups depend on them.
3. On two real phones, verify same-account backup/restore, an account mismatch, cancelled restore, offline edits and a changed remote revision.

The developer signing key matches the existing APK to allow an update. It is a public development key, not a private production signing identity. Public distribution should move to a separately managed production signing/release process.

## Data boundaries

Firebase Auth profile fields and backup metadata remain visible to authorized administrators. The ledger database and V3 cloud payload are encrypted. CSV/PDF exports are readable files the user explicitly creates. Legacy backups, downloaded exports, storage soft-delete/version history and previously written disk blocks are not retroactively erased by encryption.

Gemini receives only the data stated in the app's separate consent; encrypted backup does not make AI requests end-to-end encrypted. Weather is optional and sends the selected city to Open-Meteo, without GPS permission.

## Verification status

Local Java backup-policy/codec checks: 19 passed. Cloud Function handler checks: 11 passed. Android compilation, device tests, rules emulator tests and APK signing verification are run by the repository workflow; use the final recorded build result rather than assuming this checklist means they passed. Live project AI inference and live two-device KMS restore require owner cloud setup and are not verified here.
