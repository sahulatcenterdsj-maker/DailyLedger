> Updated review: see `REVIEW-AND-SETUP.md` for v1.4.1 changes and the current verification status.

# Firebase integration status

Project configuration in this repository:

- Firebase project: `daily-ledger-4d8ef`
- Android package: `com.sadique.dailyledger`
- `app/google-services.json` includes both Android and web OAuth client entries.
- Firebase Android BoM: `34.19.0`
- Firebase AI Logic dependency: `com.google.firebase:firebase-ai`
- Current AI model constant: `gemini-3.5-flash-lite`
- Firebase Auth, Firestore, Storage, Functions and App Check dependencies are configured.
- Play Integrity App Check is used for distributed builds.
- Firestore and Storage rules are deny-by-default outside owner-scoped backup paths.
- Cloud Functions use App Check enforcement and parameterized `KMS_KEY_NAME` configuration.

Still requiring owner-side cloud setup/deployment:

- Enable/configure Firebase AI Logic in the Firebase Console.
- Register/enforce App Check and add debug token(s) for development devices.
- Enable Cloud KMS, create the KEK, grant the Functions runtime service account encrypt/decrypt permission and deploy Functions.
- Deploy Firestore/Storage rules.
- Run the complete device test plan in `TESTING.md`.

A fresh Gradle build was not possible in the editing environment because `services.gradle.org` was unreachable.
