# Build Native Android APK

## Android Studio
1. Open this folder in Android Studio.
2. Wait for Gradle sync to complete.
3. Build menu:
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
4. Debug APK path:
   `app/build/outputs/apk/debug/app-debug.apk`

## Command line
With Android SDK and JDK 17+ configured:

```bash
./gradlew assembleDebug
```
For Windows:
```cmd
gradlew.bat assembleDebug
```

Release builds should use your own signing keystore generated from Android Studio's **Generate Signed Bundle / APK** wizard.
