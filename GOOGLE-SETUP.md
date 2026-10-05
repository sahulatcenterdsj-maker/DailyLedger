# Google sign-in and optional Drive backup

Google login now signs in to **Firebase Authentication**. A locally decoded Google token is not used as an authenticated session.

1. Follow `START-HERE-ROMAN-URDU.md` to create the Firebase Android app, register SHA-1/SHA-256 and enable Google and Email/Password.
2. A current `google-services.json` is already placed in `app/` in this prepared ZIP. If Google is not enabled yet, enable Google first and then download a fresh `google-services.json` and replace the existing file in `app/`.
3. The Google Services plugin generates `default_web_client_id`. This is the Web OAuth client ID used by Credential Manager, not the Android client ID. A `GOOGLE_WEB_CLIENT_ID` in local.properties is only a fallback; Firebase configuration is still required.
4. For optional Drive copies, enable Google Drive API in the Google Cloud project backing this same Firebase app. Ensure its Android OAuth registration matches the package and signing certificate. Configure the OAuth consent screen; when in testing mode, add the Google account as a test user.
5. Sign in with Google, open Settings, save a Drive backup passphrase, and use **Save copy**. The app requests the narrow `drive.appdata` authorization separately. The backup lives in the app's hidden Drive folder.

Account backup through Firestore needs no Drive permission and works with email/password accounts too. Drive copies are additional manual backups. A Drive restore needs the same Google account and passphrase, including after reinstalling or resetting the Firebase password.

If Google is linked to a Firebase account, Firebase keeps its provider identity. This app does not guess an account by email or create a fake local login. If Firebase reports an account/provider collision, sign in using the previously registered method; the app does not automatically merge separate accounts.

Official documentation:
- https://firebase.google.com/docs/auth/android/google-signin
- https://developers.google.com/identity/authorization/android/authorize-access
- https://developers.google.com/workspace/drive/api/guides/appdata
