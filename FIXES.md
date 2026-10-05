# Login and account-backup update (1.1.0)

- Added Firebase-verified Google and email/password authentication, signup and reset email.
- Added login-first routing for fresh/signed-out sessions; retained valid Firebase sessions.
- Added private per-account Firestore snapshots, automatic work, initial restore and conflict protection.
- Made Drive passphrases and sync metadata account-specific; kept optional encrypted Drive copies as manual actions.
- Kept the same package/signing key and increased versionCode; added verified legacy Google-owner migration.
- Added account-namespaced import IDs, full-snapshot validation and transactional restore.
- Included server access rules, setup guides and actual backup-core tests.
- Firebase configuration/deployment, full Android build, live service verification and device UI testing remain outstanding.

---

Historical notes from the supplied ZIP follow; they describe the previous version, not new verification:

# What was fixed

Compile errors
- `DailyLedgerApp.kt`: `NavigationBarItem` was called with positional args, so the label lambda landed in the
  `modifier` slot (type mismatch). Now uses named arguments.
- `Icons.Default.ReceiptLong` -> `Icons.AutoMirrored.Filled.ReceiptLong` (non-mirrored version is deprecated/removed).
- `DashboardScreen.kt`: `sumOf(TransactionEntity::amountMinor)` is an ambiguous overload -> lambda; Long/Int literal mix fixed.
- `TopAppBar` opt-in added; `getApplication()` type made explicit in `MainViewModel`.
- `GoogleAuthManager`: no longer depends on `GoogleIdTokenCredential.uniqueId` (not present in all googleid versions);
  reads the stable account id (`sub`) and email from the ID token.
- `AppDatabase`: `exportSchema=false` (no schema directory configured).

Runtime bugs
- Drive upload used HTTP `PATCH`, which `HttpURLConnection` rejects -> now POST + `X-HTTP-Method-Override: PATCH`.
- Settings screen had no way back (only "Sign out"); added Back button + system back handling, scrolling, insets.
- Biometric lock: auto-prompts on launch, works on API 26-29, and never locks you out if the device has no screen lock.
  Enabling it is refused if nothing is enrolled. No more flash of login/unlocked UI while settings load.
- Exports: I/O moved off the main thread, works on Android 8-9 (MediaStore.Downloads is API 29+), no scientific-notation
  amounts, PDF now includes all transactions (was capped at 80), CSV includes loan payments.
- Input validation for dates/months/amounts (bad dates used to be saved and silently break filters and reminders).
- Amount fields use a decimal keyboard; large amounts no longer show as `1.0E7` when editing.
- Sync worker no longer retries forever when no passphrase is set.
- Notification small icon is now a proper monochrome icon; adaptive launcher icon added.
- Secure-prefs file excluded from Android backup (Keystore key can't be restored on another device).

Build setup
- Added the missing `gradle-wrapper.jar` and official `gradlew`.
- Added `.github/workflows/build-apk.yml` (builds and uploads the APK).
- Removed the machine-specific `local.properties` (Android Studio recreates it).
