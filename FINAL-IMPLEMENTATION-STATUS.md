# Version 1.5.1 status

VersionCode 7 / versionName 1.5.1. The v1.5 Offline Mini AI, loans/contacts/payment history and kameti member/turn features are merged. The uploaded UI, weather, sounds and finance features are retained. Review corrections include SQLCipher local encryption, durable device-key storage, safe database migration, V3 owner/revision-bound cloud encryption, race-safe KMS key recovery, timeout-safe upload handling, transactional backup deletion, no plaintext downgrade in rules, provider-specific AI consent, encrypted advice cache and Play Integrity for shared APKs.

See `REVIEW-AND-SETUP.md` for the findings and privacy/cost boundaries. Build and test results are recorded in `TESTING.md` after CI finishes. Live Firebase AI and two-device KMS backup/restore require owner setup; they are not claimed verified by local or emulator tests.
