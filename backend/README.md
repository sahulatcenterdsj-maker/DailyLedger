# Shared AI activation

Daily Ledger 1.3 provides automatic saving suggestions and reviewed Auto Fill. **No chat.** Users sign in through Firebase; they never enter a Groq key.

## One-time activation by the app owner

1. In Cloudflare Workers & Pages, import GitHub repository `sahulatcenterdsj-maker/DailyLedger`, branch `codex/build-apk-20261005`, root `/`. Deploy command: `npm run deploy`. Choose the Workers Free plan.
2. Add `GROQ_API_KEY` as a **Secret** using your Groq Free account key. Keep `FIREBASE_PROJECT_ID` set to `daily-ledger-4d8ef`. Do not paste keys in chat, screenshots, source or Android.
3. Deploy. The included Wrangler migration creates the SQLite-backed quota Durable Object automatically. No Firebase billing upgrade or Firestore rule changes are required.
4. Open `https://YOUR-WORKER.YOUR-SUBDOMAIN.workers.dev/health`. `ready: true` means a key is configured; live inference must still be tested while signed in.
5. Set the public `endpoint` field in `ai-service.json` on this branch to that HTTPS Worker root, without `/health` or `/v1`. This URL is public and may be shared with the developer; the key must remain secret.
6. Apps discover the address within 15 minutes (force-close/reopen to refresh sooner). Enable Cloud AI in Settings, then test Auto Fill, review/edit/save, and automatic dashboard suggestions. No new APK is needed just to activate the address.

CLI alternative: `npm install`, `npx wrangler login`, `npx wrangler secret put GROQ_API_KEY`, `npm run deploy`. Enter the key in the secret prompt.

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
