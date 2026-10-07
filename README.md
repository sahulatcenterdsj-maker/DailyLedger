# Daily Ledger — Android v1.3.0

PKR mein salary, rozmarra kharchay, qarz, kameti aur savings ka hisaab rakhne wali native Android app. Kotlin, Jetpack Compose aur Room par bani hai; Android 8.0 ya us se naya version chahiye.

**Aapka record phone par save hota hai. Account backup successful hone ke baad wohi login use karke us backup ko doosre phone par restore kiya ja sakta hai.** Phone badalne ya app uninstall karne se pehle Settings mein last successful backup check karein.

**Privacy:** Firebase account backup transfer aur server storage par encrypted hai, lekin **Firestore read permission wale project administrators usay read/decode kar sakte hain**. Yeh end-to-end encrypted backup nahi hai. Google Drive ki optional copy alag passphrase se encrypt hoti hai.

## App mein kya hai

- Google sign-in, email/password sign-in, sign-up aur password reset.
- Dashboard par monthly salary, monthly expenses aur remaining salary. Kharcha salary se minus hota hai; savings aur kameti ke totals alag hain.
- Transactions, loans/repayments, savings, kameti installments aur multiple shares ki partial receiving/history.
- 93 built-in categories, category search aur custom category likhne ka option.
- Offline observations: sab se bara kharcha, available history ke mutabiq comparison aur bachhat ke mumkin options.
- Optional Cloud AI suggestions aur **AI Auto Fill**. Roman Urdu, Urdu ya English text se income/expense drafts bante hain; user review/edit karke khud save karta hai. **AI chat nahi hai.**
- Aqua, Sunset, Forest, Rose, Midnight, light/dark/system themes; savings ke liye wallet icon.
- Account backup, optional encrypted Drive copy, biometric/device lock aur CSV/PDF exports.

## Firebase, Cloudflare aur Groq integration

| Service | App mein kaam |
| --- | --- |
| Firebase Authentication | Login, user identity aur password reset. App login password ko ledger backup mein save nahi karti. |
| Cloud Firestore | Har account ka latest backup aur same account par restore. |
| Google Drive | User ke Google account mein manually saved, passphrase-encrypted extra copy. |
| Cloudflare Worker | Signed-in AI requests verify karta hai, usage limits lagata hai aur Groq ko request bhejta hai. Yeh ledger backup server nahi hai. |
| Groq | Cloud suggestions aur Auto Fill ke AI responses. App users ko apni API key nahi banani parti. |

Current AI server: [dailyledger.sadique6571.workers.dev](https://dailyledger.sadique6571.workers.dev/health). Owner ke 7 October 2026 ke health check mein `ready:true` tha. Is ka matlab key configured hai; successful signed-in AI response ka test alag hai.

Version 1.3.0 server address is [public configuration](https://github.com/sahulatcenterdsj-maker/DailyLedger/blob/codex/build-apk-20261005/ai-service.json) se leti hai. Sirf server address badalne ke liye naya APK zaroori nahi. Groq key Cloudflare ke **runtime Secret** mein rehti hai, APK ya public configuration mein nahi.

## Kya mera data save rahega?

Records phone par save rehte hain aur automatic account backup default taur par on hai. Record changes, app launch/sign-in aur background schedule par internet milne ke baad backup ki koshish hoti hai. Android background work ko delay kar sakta hai.

Settings mein **Back up now** aur last successful check/status se tasdeeq karein. Sirf login ho jana har nayi entry ke upload hone ki tasdeeq nahi.

Doosre phone par wohi account use karein. Us account ka local data khaali ho aur server par backup ho to online check par initial restore hota hai. Dono phones par alag changes hon to user Restore ya Replace ka faisla karta hai; records automatically merge nahi hote.

Internet, quota, backup size ya account/service access ki wajah se backup ruk sakta hai. Us waqt phone ke records rehte hain; cloud se sirf last successfully uploaded snapshot restore ho sakta hai. Har account ka **ek latest snapshot** hota hai, unlimited history nahi. App ki encoded-payload limit 900,000 characters hai. CSV/PDF exports reports hain, complete restore backup nahi.

## Encryption aur admin access

| Data/copy | Mojooda protection | Kis ko access ho sakta hai? |
| --- | --- | --- |
| Phone ka Room database | Android app sandbox/device protection; alag app-level database encryption nahi. | Firebase Console se phone-only records nahi milte; uploaded backup alag hai. |
| Firebase account backup | HTTPS aur Firestore encryption at rest. App ka payload GZIP + Base64 hai; yeh compression/encoding hai. | **Authorized project admin backup read aur decode kar sakta hai.** |
| Google Drive copy | Upload se pehle AES-256-GCM encryption; separate user passphrase se key banti hai. | Firebase admin hone se Drive copy/passphrase ka access nahi milta. Copy decrypt karne ke liye passphrase chahiye. |
| CSV/PDF exports | Readable report files; app inhein passphrase se encrypt nahi karti. | Jis ke paas exported file ka access ho woh report parh sakta hai. |
| Cloud AI input | HTTPS par Worker aur Groq ko processing ke liye bheja jata hai. | Processing services ko bheja hua content available hota hai; yeh un services se end-to-end hidden nahi. |

Supplied [Firestore rules](firestore.rules) deploy hon to ordinary signed-in user apni UID ka backup hi read/write kar sakta hai. **Yeh rules project administrators ki IAM permissions ko khatam nahi karte.** Live Firebase rules/IAM ko is documentation update mein inspect ya change nahi kiya gaya.

Firebase Authentication mein authorized admin ko user identity, jaise email/UID, nazar aa sakti hai. User ka original login password ledger backup mein nahi hota.

Drive passphrase ki local copy Android Keystore ki key se encrypted rehti hai; app usay Firebase ya AI server par upload nahi karti. Restore ke liye wohi Google account aur original passphrase chahiye. Passphrase kho jaye to Firebase password reset us encrypted Drive copy ko unlock nahi karta.

Android system backup/device transfer bhi OS aur user settings ke mutabiq local database/settings ki copy bana sakta hai. Keystore-bound secrets aur AI preferences app ke system-backup rules se exclude hain. Yeh manual encrypted Drive copy se alag mechanism hai.

**Firebase project admin se bhi financial data chhupane ke liye user-held key wali client-side/end-to-end encryption abhi add karni hogi.** Mojooda app ke liye “admin bhi data nahi dekh sakta” ya “data kabhi lose nahi hoga” ka wada sahi nahi.

## AI privacy aur usage

Cloud AI default taur par off hai. **Settings → Cloud AI → Enable AI** se user consent deta hai.

- Automatic suggestions mein period, salary/expense totals, category totals aur comparisons jate hain. Individual transaction rows/notes aur account identity Groq ke summary prompt mein nahi bheje jate; custom category names totals mein aa sakte hain.
- Auto Fill mein typed text aur current date jati hai. Text mein likhi personal detail bhi processing ka hissa hogi.
- Worker login verify karne ke liye Firebase ID token receive karta hai; token Groq ko forward nahi hota.
- Current Worker code financial prompts/records save ya log nahi karta. Durable Object mein hashed user IDs ke usage counters rehte hain. Groq ki apni data-handling policy alag apply hoti hai.
- Auto Fill zyada se zyada 10 income/expense drafts banata hai. **Save reviewed entries** dabane se pehle ledger mein kuch save nahi hota. Savings, loans aur kameti ke liye unke apne forms use karein.
- Current limits: per user 2 cloud suggestion requests/day aur 10 Auto Fill requests/day; poori service par 100/day aur 8/minute. Provider limits pehle bhi aa sakti hain. Manual entries aur local observations cloud AI ke baghair chalti hain.

## Setup aur APK

Current Firebase project ID `daily-ledger-4d8ef` hai. Apni deployment ke liye correct `app/google-services.json`, package `com.sadique.dailyledger`, signing fingerprints, Google/Email providers aur supplied Firestore rules configure karein. Tafseel: [Roman Urdu setup](START-HERE-ROMAN-URDU.md).

Cloudflare mein **Settings → Runtime variables and secrets → Production** mein `GROQ_API_KEY` ko **Secret** banayein. Build settings wala secret running Worker ko nahi milta. Firebase project ID aur `AI_QUOTA` Durable Object binding bhi correct honi chahiye. Tafseel: [Cloudflare/Groq setup](backend/README.md).

APK ke liye [GitHub Actions](https://github.com/sahulatcenterdsj-maker/DailyLedger/actions/workflows/build-apk.yml) ka successful **Build Daily Ledger APK** run kholein, **DailyLedger-APK** artifact download/unzip karke `DailyLedger.apk` install karein. Ya Android Studio mein project khol kar build karein:

```sh
./gradlew assembleDebug
```

Current version `1.3.0`, version code `4` hai. Same signing key wala APK existing app ko update karta hai. Current APK development/debug signing key use karta hai; wider production distribution ke liye private release signing configure karni chahiye.

Firebase, Cloudflare aur Groq ki free quotas ke andar service chal sakti hai; unlimited storage, availability ya permanently free usage ki guarantee nahi. Account plan aur provider limits apply hoti hain.

## Verification aur references

Version 1.3.0 ki APK build aur automated backend, backup, data migration aur Android UI checks pass hue hain. Live signed-in AI request aur production backup/restore ka current device test alag hai. Details: [TESTING.md](TESTING.md).

- [Firebase encryption](https://firebase.google.com/support/privacy)
- [Firestore encryption and authorized reads](https://docs.cloud.google.com/firestore/native/docs/server-side-encryption)
- [Firestore rules and server access](https://firebase.google.com/docs/firestore/security/rules-conditions)
- [Cloudflare runtime secrets](https://developers.cloudflare.com/workers/configuration/secrets/)
