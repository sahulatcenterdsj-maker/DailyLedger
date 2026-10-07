> Updated review: see `REVIEW-AND-SETUP.md` for v1.4.1 changes and the current verification status.

# Cloud KMS + Firebase Functions setup

Daily Ledger uses one Cloud KMS symmetric KEK to wrap per-account random backup DEKs. The KEK key material never belongs in the APK, Firestore or this repository. The **KMS resource name is not secret**.

Production Functions deployment requires the Firebase project to use the Blaze plan, and Cloud KMS requires billing to be enabled.

## Recommended resources

Project: `daily-ledger-4d8ef`

- Key ring: `dailyledger-keyring`
- Location: `global`
- Symmetric key: `backup-kek`
- KMS resource name:
  `projects/daily-ledger-4d8ef/locations/global/keyRings/dailyledger-keyring/cryptoKeys/backup-kek`

Using gcloud while the correct project is selected:

```bash
gcloud config set project daily-ledger-4d8ef
gcloud services enable cloudkms.googleapis.com
gcloud kms keyrings create dailyledger-keyring --location=global
gcloud kms keys create backup-kek \
  --location=global \
  --keyring=dailyledger-keyring \
  --purpose=encryption
```

For default 2nd-gen Firebase Functions, the runtime service account is normally the Compute Engine default service account. For this project number it is:

`876166304446-compute@developer.gserviceaccount.com`

Grant only encrypt/decrypt use on the CryptoKey:

```bash
gcloud kms keys add-iam-policy-binding backup-kek \
  --location=global \
  --keyring=dailyledger-keyring \
  --member="serviceAccount:876166304446-compute@developer.gserviceaccount.com" \
  --role="roles/cloudkms.cryptoKeyEncrypterDecrypter"
```

If Firebase shows a different runtime service account for the deployed functions, grant the role to that service account instead.

## Functions parameter

`functions/index.js` uses Firebase parameterized configuration:

`defineString("KMS_KEY_NAME")`

On deployment, Firebase CLI will require/prompt for a value. Use:

`projects/daily-ledger-4d8ef/locations/global/keyRings/dailyledger-keyring/cryptoKeys/backup-kek`

Then deploy:

```bash
npm --prefix functions install
firebase deploy --only functions
```

## App Check

Both callable key Functions declare `enforceAppCheck: true`. In Firebase Console, also register the Android app with App Check. Distributed builds use Play Integrity; no shared debug token is included.

Firebase AI Logic should likewise be set to App Check **Enforced** in Firebase Console before production use.

## Rules

Deploy the owner-scoped backup rules:

```bash
firebase deploy --only firestore:rules,storage
```

## Security properties

- Function UID always comes from `request.auth.uid`.
- `unwrapBackupKey` ignores client-supplied UID/wrapped-key values and reads Firestore metadata server-side.
- Cloud KMS AAD is `uid:v3` for new writes (`uid:v2` for legacy decryption); decryption requires the same AAD.
- The DEK is random 256-bit key material and is never derived from UID, email, password or auth token.
- This design is account-based encrypted backup, not zero-knowledge E2EE.
