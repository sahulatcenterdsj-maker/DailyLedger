# Daily Ledger v1.3.0: login, account backup aur AI

Is updated project mein:
- App pehli dafa khulne par login page aata hai.
- Sign in with Google: pehli dafa account banta hai, agli dafa wohi account khulta hai.
- Sign up: naam, email, password aur confirm password se naya account banta hai.
- Sign in: email/password se login hota hai.
- Forgot password: email par password reset link milta hai.
- Har login ID ka alag automatic account backup hai.
- Google se login walay account ke liye optional encrypted Google Drive copy bhi hai.

## Pehle yeh samajh lo

Current v1.3.0 app Firebase project `daily-ledger-4d8ef` aur shared Cloudflare/Groq AI service ke liye configure hai. Apni fork/deployment banate waqt apna Firebase project, signing fingerprints aur service settings use karo. Neeche wali Firebase checklist fresh setup ya troubleshooting ke liye hai.

Fresh installation par login page khulta hai. Successful login ke baad session yaad rehta hai; har launch par password dobara nahi dena hota. Settings se Sign out karne par login page phir khulta hai. Purane version ka offline bypass hata diya gaya hai.

## Ek dafa Firebase setup

1. https://console.firebase.google.com/ kholo aur apna project banao. Spark plan rakho; is implementation ko paid Storage ya Cloud Functions ki zaroorat nahi.
2. Android app add karo. Package name bilkul yeh ho: `com.sadique.dailyledger`.
3. Neeche diye hue debug SHA-1 aur SHA-256 project settings mein add karo.
4. Authentication ke Sign-in method mein **Email/Password** aur **Google** enable karo. Google ke liye support email select karo.
5. Cloud Firestore ka **Standard edition**, default database banao. Production mode use karo. Database location soch kar select karo.
6. Firestore ke Rules tab mein is project ki `firestore.rules` file ka poora text paste karke Publish karo. Test mode ya public read/write rules mat lagao.
7. Firebase Console se sahi project ki **google-services.json** download karke project ke `app/google-services.json` mein rakho. Google provider ya signing fingerprints badalne ke baad current configuration download karke build mein use karo.
8. Android Studio mein project kholo, Gradle Sync karo, phir APK build karo. Configuration na ho to login screen dikhegi, lekin online login ke buttons disabled honge.

Debug SHA-1:
`C6:E1:9D:A9:60:97:E5:42:4A:34:76:6E:15:74:6E:DC:A7:C0:F7:07`

Debug SHA-256:
`4C:22:14:C4:D4:B6:E3:8B:FA:81:44:F1:41:CC:24:A7:18:00:3F:55:20:41:06:17:76:6D:19:5C:CD:2D:23:1D`

Yeh is ZIP ke debug key ke fingerprints hain. Release ya Play Store ki signing key alag ho to us ke fingerprints bhi add karne honge. Purana debug key project mein preserve hai.

## Backup kaise chalega

Login ke baad account backup default taur par on hai. Record change karne par internet milne ke baad backup schedule hota hai; Android background mein delay kar sakta hai. Settings mein **Back up now** se foran koshish aur status check kar sakte ho.

Naye phone par wohi login karo. Agar us phone par is account ka data nahi hai aur server par backup hai, online check par restore ho jata hai. Internet na ho to backup check baad mein hota hai. Agar local aur remote dono mein mukhtalif data ho to automatic overwrite rukta hai. Settings mein backup restore karna ya is phone ka data backup par likhna tum select karte ho. Restore local records replace karta hai; is se pehle zaroori naya data export kar lo.

Settings mein **Automatic backup** off bhi kar sakte ho. Automatic account backup ke liye alag encryption passphrase nahi chahiye. Firebase connection aur server storage encrypted hain aur server rules access login ID tak rakhte hain. Yeh end-to-end encryption nahi; Firebase project administrator ko server data tak access hota hai.

## Google Drive ki extra copy

Google account se login ke baad Settings mein Google Drive copy milti hai. Is ke liye usi Google Cloud project mein Drive API enable karo; detail `GOOGLE-SETUP.md` mein hai. Passphrase set karke **Save copy** dabao aur Drive permission allow karo. Yeh extra copy manually save hoti hai; automatic backup Firebase account mein hota hai.

Drive se restore ke liye wohi Google account aur wohi passphrase chahiye. Firebase password reset se Drive passphrase recover nahi hota.

## Cloudflare aur AI

App mein AI chat nahi; automatic saving suggestions aur reviewed Auto Fill hai. Basic spending observations offline chalti hain. Cloud AI use karne ke liye **Settings → Cloud AI → Enable AI** karo. Auto Fill ki entries review/edit karne ke baad **Save reviewed entries** se save hoti hain.

Current server `https://dailyledger.sadique6571.workers.dev` hai. Owner ne 7 October 2026 ko `/health` par `ready:true` confirm kiya. Yeh sirf key configured hone ki tasdeeq hai; asal signed-in AI request ka test abhi alag karna hai.

Cloudflare Worker ki **Settings → Runtime variables and secrets → Production → Add variable** mein Key `GROQ_API_KEY`, type **Secret**, aur Value mein Groq key do; phir **Add variable and deploy**. Build section wala secret running Worker ko available nahi hota. `FIREBASE_PROJECT_ID` app ke Firebase project se match hona chahiye aur `AI_QUOTA` Durable Object binding chahiye. Poori owner guide [backend/README.md](backend/README.md) mein hai.

Users ko API key nahi deni parti. Automatic suggestions ke liye category totals/comparisons aur Auto Fill ke liye typed text Worker/Groq ko jata hai. Firebase ID token sirf Worker ko authentication ke liye jata hai. Is data handling aur admin access ki tafseel [README.md](README.md) mein hai.

## Free quota

Firebase Spark mein email/password aur Google login, aur Firestore ki free usage quota milti hai. Unlimited storage ka wada nahi. Free quota khatam ho to cloud operation ruk sakta hai; phone ka local data rehta hai. Is app ka account snapshot compressed form mein 900,000 characters tak rakha gaya hai; bohat bara data ho to Drive copy/export use karo.

## Purane data ke bare mein

Purane Google account ka local data Firebase se usi Google identity ki tasdeeq ke baad nayi account ID se attach kiya jata hai. Purane offline profiles ka data delete nahi hota. Login ke baad Settings mein **Previous offline data** ka option aayega, agar aise records phone mein hain. Sahi profile select karke **Import records** confirm karo; us ke records tumhare current account mein aa jayenge aur future backups mein shamil honge. App uninstall mat karo jab tak purana data backup/import na ho jaye.

## Verification

Version 1.3.0 ki Android APK build, backend checks, 19 Java backup checks aur Android data/UI checks GitHub CI mein pass hue. App ka package aur signing certificate previous APK se match karte hain. User ne Firebase login chalne ki tasdeeq ki hai. Production rules/IAM, live backup/restore aur signed-in AI inference ko in automated checks ka hissa na samjho; unki separate device verification chahiye. Details [TESTING.md](TESTING.md) mein hain.

Official setup aur quota sources:
- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/auth/android/google-signin
- https://firebase.google.com/docs/firestore/security/rules-conditions
- https://firebase.google.com/docs/projects/billing/firebase-pricing-plans
- https://firebase.google.com/docs/firestore/quotas
- https://firebase.google.com/support/privacy
