# Google Sign-In + Drive setup

Google login cannot be shipped with someone else's OAuth credentials. Create your own Google Cloud project once:

1. Open Google Cloud Console and create/select a project.
2. Enable **Google Drive API**.
3. Configure the OAuth consent screen.
4. Create an **Android OAuth client** for package `com.sadique.dailyledger` using your signing certificate SHA-1.
5. Create a **Web application OAuth client**. Copy its Client ID.
6. In this project, create/edit `local.properties` and add:

   `GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com`

7. Sync Gradle and rebuild.

The app uses Credential Manager for Google authentication. Drive authorization is requested separately only when the user taps Sync/Restore in Settings and requests only the `drive.appdata` scope.

## SHA-1 for debug builds
From the project root:

`./gradlew signingReport`

Copy the SHA-1 from the `debug` variant into your Android OAuth client.

## Drive backup location
The backup goes to Google's hidden `appDataFolder`; it does not clutter the user's normal My Drive files.
