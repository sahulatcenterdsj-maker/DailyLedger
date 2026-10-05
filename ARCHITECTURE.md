# Architecture

Compose UI -> AccountManager -> Firebase Auth (email/password or Google Credential Manager token).
The persisted Firebase session is reconciled before routing to login or the ledger. Local display preferences alone cannot grant online-account access.

The Firebase UID scopes Room records, settings, Drive passphrases and Firestore document paths. Older Google records migrate only after Firebase verifies the same Google provider ID. Older offline profiles require an explicit import confirmation in Settings. Imported record IDs are namespaced by the destination account, including payment references.

Ledger edits -> Room transaction/storage -> debounced WorkManager account backup. A process mutex serializes local writes, account switches and complete snapshot operations. CloudBackup reads the actual server document, checks the last-known revision, and uses a Firestore transaction for replacement. A fresh empty device restores first. Conflicting devices require an explicit choice. Gzip/Base64 bounds and a content hash guard snapshot restoration; import is a Room transaction.

Firestore path: users/{Firebase UID}/backups/latest. Rules are deny-by-default and UID-scoped. The default Standard database is used. Firebase project credentials and live deployment are supplied by the app owner.

Optional Drive actions separately request drive.appdata authorization and store an AES-GCM encrypted backup. They are manual extra copies. Account backup through Firebase needs no Drive passphrase.
