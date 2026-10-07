# Build APK

## Android Studio

1. Extract the project ZIP and open the project folder that contains `settings.gradle.kts`.
2. Keep the configured `app/google-services.json` in place (or replace it only with a fresh file for the same Firebase Android app).
3. Let Gradle sync and install the Android SDK components requested by the project (`compileSdk 37; targetSdk 36; minSdk 26`).
4. Build the debug APK. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Terminal

Use Java 21 and an Android SDK.

```bash
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug
```

`local.properties` should point to the local SDK only; do not commit another machine's SDK path.

## GitHub Actions

The included workflow tests Firebase rules and Functions, runs backup-core tests, builds/tests Android, verifies package/signing identity, and uploads `DailyLedger.apk`.

Use the signed CI artifact to update the previously supplied app. The version is 1.5.1 (code 7); package and development signer are unchanged. Do not uninstall or clear app data to install this update. Test reports include data migration, encryption, offline parsing and UI review.
