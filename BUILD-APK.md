# Build APK

## Android Studio

1. Extract the ZIP. Open the `DailyLedgerNative` folder containing `settings.gradle.kts`.
2. Complete Firebase setup and put `google-services.json` in `app/`.
3. Allow Gradle sync and install the SDK components requested by the project (compileSdk 37).
4. Use Android Studio's APK build action. The debug output is `app/build/outputs/apk/debug/app-debug.apk`.

## Terminal

Use Java 17 or a compatible Android Studio Gradle JDK and an Android SDK.

Linux/macOS:
```sh
chmod +x gradlew
./gradlew assembleDebug
```

Windows:
```bat
gradlew.bat assembleDebug
```

`local.properties` can contain the machine's `sdk.dir`; do not copy another machine's SDK path. The supplied Gradle wrapper verifies the download checksum.

## GitHub Actions

The included workflow builds a debug APK. Set repository secret `GOOGLE_SERVICES_JSON` to the contents of your Firebase Android configuration. This is a client configuration file; never put a service-account private key in the app or that secret. With no client configuration supplied, the build intentionally has inactive account actions.

## Verification status

Gradle could not be downloaded in the editing environment (`Network is unreachable`). An APK has not been built or run here. Core Java backup tests passed; see `TESTING.md` for checks to run after configuration.
