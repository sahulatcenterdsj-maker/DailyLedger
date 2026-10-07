> Updated review: see `REVIEW-AND-SETUP.md` for v1.4.1 changes and the current verification status.

# Daily Ledger — Start Here

Yeh project **Firebase AI Logic + encrypted Firebase account backup** architecture par hai.

## Pehle kya configure karna hai

1. Firebase project `daily-ledger-4d8ef` mein Email/Password aur Google Authentication verify karo.
2. `app/google-services.json` package `com.sadique.dailyledger` ke saath rehna chahiye.
3. Firebase AI Logic setup complete karo aur Android App Check register karo. Debug build ke liye Debug provider aur release ke liye Play Integrity use hota hai.
4. Firebase Console > App Check > APIs mein Firebase AI Logic ko production se pehle **Enforced** rakho.
5. Cloud KMS aur callable Functions ke liye `KMS-SETUP.md` follow karo.
6. `firebase deploy --only firestore:rules,storage,functions` se server-side pieces deploy karo.

## AI ka flow

Auto Fill Roman Urdu, Urdu ya English text leta hai. Firebase AI Logic structured JSON schema ke mutabiq income/expense drafts banata hai. User pehle draft ko edit/review karta hai, phir Save karta hai. Savings, loans aur kameti records AI Auto Fill se direct create nahi hote.

Saving suggestions ko aggregate monthly/category totals milte hain; transaction notes automatic insight prompt ka hissa nahi hain. App mein basic local insights AI ke baghair bhi kaam karte hain.

## Backup ka flow

Phone JSON snapshot banata hai -> random AES-256-GCM key se encrypt karta hai -> encrypted file Firebase Storage mein upload hoti hai -> DEK ko callable Function Cloud KMS se wrap karwata hai -> Firestore mein metadata save hota hai.

Naye phone par same Firebase account login karne ke baad **Backup found** dialog aata hai. App khud se restore nahi karti. User **Restore** ya **Skip** choose karta hai.

Yeh zero-knowledge E2EE nahi hai: authenticated backend/KMS key recover kar sakta hai. Lekin raw ledger snapshot Storage ya Firestore mein plaintext account backup ke taur par store nahi hota.

## Test

Server setup ke baad `TESTING.md` ke mutabiq aik hi round mein rules, Functions, unit tests, APK build aur real-device flows test karo.
