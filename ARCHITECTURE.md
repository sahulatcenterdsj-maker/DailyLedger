# Architecture

- `data/`: Room entities, DAO, repository, DataStore settings.
- `ui/`: Compose app shell, screens and view model.
- `auth/`: Credential Manager Google authentication.
- `sync/`: Google AuthorizationClient, Drive REST appDataFolder client, periodic WorkManager sync.
- `security/`: Android Keystore-protected sync passphrase and PBKDF2 + AES-GCM backup encryption.
- `notifications/`: daily loan/kameti reminder worker.
- `export/`: CSV/PDF export.

Google Drive is a backup/sync channel, not the primary database. Local Room is the source of truth while offline. Manual restore replaces this account's local ledger with the encrypted Drive backup.
