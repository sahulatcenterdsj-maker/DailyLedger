# Daily Ledger 1.5.1 architecture

## Device and identity

Firebase Authentication provides Google/email sign-in. Firebase UID scopes ledger rows and backup permissions; it is not a secret and is never used as encryption key material. SQLCipher encrypts the Room database using a random device key wrapped in Android Keystore. Existing plaintext databases are checkpointed, exported to a verified encrypted temporary file, and atomically replaced. Corruption/open failures preserve the file and stop startup safely. Android automatic backup/device transfer is excluded; account backup is the recovery route.

## Ledger model

Room schema 4 adds loan contacts, payment methods and committee members. All owner migration, deletion and JSON backup flows include these records. Ledger JSON format 3 is independent of cloud envelope V3: formats 1/2 remain importable. Loan overpayment and duplicate/personal-limit kameti turns are rejected transactionally. Marking a member turn received updates its schedule status; the actual payout amount is recorded separately through Receive amount.

## Cloud account backup

1. Export canonical JSON for the active account.
2. Generate a random AES-256 data key, or recover the current V3 key through authenticated callable Functions.
3. Encrypt with AES-GCM and a fresh 96-bit nonce. AAD binds UID and revision. The change-detection fingerprint is HMAC-derived; ciphertext additionally has a SHA-256 integrity check.
4. Upload ciphertext to the immutable owner/revision Storage path.
5. Commit metadata using a Firestore revision transaction. A failed revision comparison cannot overwrite another phone. A timeout has unknown outcome, so it must not trigger deletion of the potentially committed object.
6. Delete the old object only after a confirmed metadata commit. Ambiguous failures may leave ciphertext orphans for later administration; preserving a usable backup takes priority.

Functions wrap/unwrap through Cloud KMS with UID/version AAD. Unwrap accepts expected revision/fingerprint/version but never a client-selected user or wrapped key: it reads the authenticated user's current Firestore metadata. Exact metadata matching prevents caching a newly rotated key under an older envelope. Firestore V1 and encrypted Storage V2 can be read; new writes use V3 only.

A fresh empty phone asks Restore / Skip. Local edits with a different remote revision require explicit restore or replace. Delete compares the revision and disables automatic backup on that phone. Other devices with an initialized old revision stop on a missing remote rather than silently recreating it. Storage retention/soft-delete and orphan cleanup are separate from deleting the active metadata.

## AI and external requests

The offline rules engine handles common income/expense drafts and calculates spending insights on-device with no API. It normalizes grouped rupees and Urdu digits, accepts k/hazar/lakh forms, and rejects ambiguous batches as a whole instead of dropping lines. Loan, saving and kameti requests stay in their dedicated ledgers.

Optional Gemini through Firebase AI Logic handles fallback Auto Fill and additional suggestions only. Fresh opt-in consent, Auth/App Check preflight, bounded generation, account/consent rechecks and local parsing guard the workflow. Built-in category aggregates may be sent; custom labels, identity and transaction notes are excluded from automatic summaries. Only Auto Fill text unrecognized locally is sent to the enabled cloud fallback. Cached suggestions are Keystore-encrypted.

Play Integrity is used for the distributed APK; no shared debug token. AI limits in the app are local convenience controls, while Firebase/model quotas remain separate. KMS Functions additionally enforce 30 key operations per account per UTC day. Optional city weather calls Open-Meteo without GPS permission.

## Trust and cost

The backend can recover backup keys. This is managed account encryption, not zero-knowledge E2EE. Functions, Storage and KMS require Blaze/billing; linking billing also affects Gemini prices. The APK contains no KMS key material or service-account credential. A copied public development signing key is not a production signing identity. See `REVIEW-AND-SETUP.md` for deployment and testing limits.
