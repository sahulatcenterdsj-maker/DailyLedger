# Shared AI activation

Daily Ledger 1.3 provides automatic saving suggestions and reviewed Auto Fill. **No chat.** Users sign in through Firebase; they never enter a Groq key.

## Current deployment

The configured public Worker root is `https://dailyledger.sadique6571.workers.dev`. The owner observed `ready:true` at `/health` on 7 October 2026 after adding the runtime secret. This only confirms that a key is present, not that the key/model works or a signed-in AI request succeeds.

The Android 1.3.0 app reads its endpoint from [the configuration on codex/build-apk-20261005](https://github.com/sahulatcenterdsj-maker/DailyLedger/blob/codex/build-apk-20261005/ai-service.json). That address has been published. Live signed-in Auto Fill and suggestion verification is still pending. This service is for AI processing; Firestore and optional Google Drive handle ledger backups.

## One-time activation by the app owner

1. In Cloudflare Workers & Pages, import GitHub repository `sahulatcenterdsj-maker/DailyLedger`, branch `codex/build-apk-20261005`, root `/`. Deploy command: `npm run deploy`. Choose the Workers Free plan.
2. Open the intended Worker and use **Settings → Runtime variables and secrets → Production → Add variable**. Enter Key `GROQ_API_KEY`, tick **Secret**, paste the Groq key into Value, and select **Add variable and deploy**. **Build variables/secrets do not reach the running Worker.** Keep `FIREBASE_PROJECT_ID` set to `daily-ledger-4d8ef` for this app; a fork must use its own matching Firebase project. Keep the key out of chat, screenshots, source and Android.
3. Deploy. The included Wrangler migration creates the SQLite-backed quota Durable Object automatically. No Firebase billing upgrade or Firestore rule changes are required.
4. Open `https://YOUR-WORKER.YOUR-SUBDOMAIN.workers.dev/health`. `ready: true` means a key is configured; live inference must still be tested while signed in.
5. Set the public `endpoint` field in `ai-service.json` on this branch to that HTTPS Worker root, without `/health` or `/v1`. This URL is public and may be shared with the developer; the key must remain secret.
6. Apps discover the address within 15 minutes (force-close/reopen to refresh sooner). Enable Cloud AI in Settings, then test Auto Fill, review/edit/save, and automatic dashboard suggestions. No new APK is needed just to activate the address.

CLI alternative: first make sure `wrangler.jsonc` targets the intended Worker/account (the current deployed Worker is `dailyledger`; the checked-in example name is `daily-ledger-ai`). Then run `npm install`, `npx wrangler login`, `npx wrangler secret put GROQ_API_KEY`, and `npm run deploy`. Enter the key only in the secret prompt. Do not accidentally create a second Worker when updating an existing deployment.

## Privacy boundaries

Firebase account backups use Firebase transport/storage encryption, but their compressed payloads remain readable to authorized project administrators. They are not end-to-end encrypted. Optional Drive copies are separately AES-GCM encrypted with the user's passphrase; that passphrase is not sent to this Worker or stored in Firestore. See the main [privacy explanation](../README.md#encryption-aur-admin-access).

The Worker receives a Firebase ID token to authenticate the caller, and receives the summary or Auto Fill text in memory for processing. It does not forward the ID token to Groq. Both the Worker and Groq can process the submitted content; HTTPS is not end-to-end secrecy from these services. The current code does not persist or log prompts/financial records. This is a statement about the code, not an audit or retention guarantee for provider systems.

## Behavior

- Salary minus expenses, category comparisons and basic saving observations run locally offline. Savings and kameti remain separate; receiving supports multiple partial receipts.
- Opt-in cloud suggestions send only category totals, period and calculated comparisons. They omit account identity, transaction notes and individual entries. Custom category labels are included in the totals. Auto Fill sends the user's entered text and today's date.
- Drafts have no account IDs or database commands. At most ten income/expense drafts can be reviewed/edited and explicitly saved. Atomic, idempotent writes derive the owner from the current Firebase login.
- Firebase signatures, audience, issuer, time claims and user identity are verified on the server. Anonymous access is rejected. Only `/v1/insights`, `/v1/autofill`, `/health` exist.
- Atomic daily quotas: 2 cloud suggestions and 10 Auto Fill requests per user; 100 provider requests per day and 8 per minute across the entire app. Groq's token or request limits may be reached earlier. The Groq account must remain on Free; an independently upgraded paid account may incur charges.
- Insight results are cached only for the matching local summary and account. Cloud refresh attempts are at least one hour apart; local observations update immediately. No automatic paid fallback.
- The server stores hashed account counters only, replaced daily. It does not log secrets, prompts or financial data. Groq's own retention terms still apply. Observability is disabled.
- Model: Groq `qwen/qwen3.8-27b`, with strict JSON outputs. It is a preview model; server configuration can be updated if retired.
- Savings icons use a wallet throughout. ZIP palettes Aqua, Sunset, Forest, Rose and Midnight are included with readable Material 3 surfaces.

## Tests

`npm test` covers signed tokens, rejected claims/signatures, quotas, no chat endpoint, input/output validation, provider failures, summary consistency and key non-disclosure. `npm run check:worker` bundles without deployment. Android CI covers data upgrades, backups, salary math, partial receiving, parsing, account isolation, atomic draft saves, editable review UI, themes and icons. Live provider verification requires the owner's key and deployed Worker.
