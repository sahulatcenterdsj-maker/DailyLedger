> Updated review: see `REVIEW-AND-SETUP.md` for v1.4.1 changes and the current verification status.

# Architecture

## Identity and local data

Compose UI -> `AccountManager` -> Firebase Auth. The Firebase UID scopes Room rows, per-account backup state, optional Drive secret storage and the Firestore backup document. Ledger restores/imports run in a Room transaction.

## Firebase AI Logic

The Android app uses `com.google.firebase:firebase-ai` and the Gemini Developer API backend through `Firebase.ai(GenerativeBackend.googleAI())`. Auto Fill and saving suggestions use response schemas plus local `AiProtocol` validation. App Check is initialized before AI use; distributed builds use Play Integrity.

## Encrypted account backup

1. Export canonical owner-scoped JSON locally.
2. Generate/use a random 256-bit DEK.
3. Encrypt locally with AES-256-GCM and a fresh 12-byte IV.
4. Upload only ciphertext to `users/{uid}/backups/{revision}.enc` in Firebase Storage.
5. Wrap the DEK with a Cloud KMS KEK through an authenticated, App-Check-enforced callable Function.
6. Store V2 metadata at `users/{uid}/backups/latest`: revision, Storage path, wrapped key, IV, ciphertext hash, plaintext change-detection hash and server timestamp.

`unwrapBackupKey` accepts no client-selected UID or wrapped key. It derives UID from `request.auth.uid` and loads the wrapped key from Firestore server-side. Cloud KMS AAD binds the UID and backup format version.

A cached device DEK is only an optimization. If AES-GCM authentication fails, the app clears the cache, unwraps the current account key once, and retries once. A second failure is treated as damaged/tampered data.

Missing/V1 remote metadata always creates a fresh DEK + wrapped key. V1 migration uploads encrypted V2 data first and only replaces legacy metadata after the Firestore transaction succeeds. New Storage objects are deleted on metadata-commit failure; old objects are deleted only after the new metadata is committed.

## Restore policy

A new empty phone detects but does not auto-restore a remote backup. UI presents Restore / Skip. Devices with unsynced local records never silently replace remote data. Revision mismatches produce a conflict requiring explicit restore or replace.

## Trust model

This is account-based envelope encryption, not zero-knowledge E2EE. Firebase Functions/Cloud KMS are trusted and can unwrap an authenticated account's DEK. UID is used for authorization/AAD, never as key material.
