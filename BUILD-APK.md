# Build APK

## Android Studio

1. Extract the project ZIP and open the `DailyLedger-main` folder that contains `settings.gradle.kts`.
2. Keep the configured `app/google-services.json` in place (or replace it only with a fresh file for the same Firebase Android app).
3. Let Gradle sync and install the Android SDK components requested by the project (`compileSdk 35`).
4. Build the debug APK. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Terminal

Use Java 17 and an Android SDK.

```bash
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug
```

`local.properties` should point to the local SDK only; do not commit another machine's SDK path.

## GitHub Actions

The included workflow tests Firebase rules and Functions, runs backup-core tests, builds/tests Android, verifies package/signing identity, and uploads `DailyLedger.apk`.

## Current edit environment

This editing environment could not reach `services.gradle.org`, so it could not download the Gradle distribution. Use Android Studio or CI for the final compile/device pass.
