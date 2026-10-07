# Firebase integration status — 1.5.1

Project: `daily-ledger-4d8ef`; package: `com.sadique.dailyledger`; versionCode: 7.

Offline Mini AI parses common entries and calculates local spending insights without contacting a cloud provider.

Implemented in source: Firebase Auth, Firestore metadata, Storage ciphertext, callable KMS Functions, Firebase AI Logic (`gemini-3.5-flash-lite`), Play Integrity App Check, encrypted device database and encrypted AI/key caches. No Cloudflare/Groq request remains in the Android app. Historical backend files in the repository may remain for older APKs.

The repository CI runs Firestore/Storage rules, Function handlers, Java backup checks, JVM tests, Android instrumentation, UI screenshot capture and APK signer verification.

Live activation still requires owner-side Firebase AI/App Check configuration and, if the owner accepts Blaze billing, KMS/Functions/Storage setup and rules deployment. Building the APK does not deploy or verify these services. No account upgrade or paid resource provisioning is performed by this review.

See `REVIEW-AND-SETUP.md`, `FIREBASE-AI-SETUP.md`, and `KMS-SETUP.md`.
