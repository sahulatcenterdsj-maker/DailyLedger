# Firebase integration status

Prepared project status:

- `app/google-services.json` is included.
- Firebase project: `daily-ledger-4d8ef`.
- Android package: `com.sadique.dailyledger`.
- Google Services Gradle plugin, Firebase BoM, Firebase Authentication and Cloud Firestore dependencies are already configured.
- Firestore rules and indexes are included in the project.
- Existing core backup tests pass (19 checks).

Important: the supplied Firebase config currently contains no OAuth client entry. Google Sign-In therefore still requires enabling the Google provider in Firebase Authentication, adding the app's SHA-1/SHA-256 fingerprints, and then downloading a fresh `google-services.json` from Firebase and replacing `app/google-services.json`.

Email/password authentication and Firestore also require their corresponding Firebase Console setup to be enabled/published.

A full Android Gradle build could not be executed in the preparation environment because downloading the Gradle distribution was blocked by network access.
