# Daily Ledger 1.5.1 — Offline Mini AI, Loans & Kameti

## Offline Mini AI

Daily Ledger now includes a specialized offline-first finance assistant designed for low-memory Android phones. It does not bundle a large LLM model file and does not require a paid AI API for its core functions.

- Roman Urdu, Urdu and English quick-entry parsing for common income/expense phrases.
- Deterministic amount/category extraction, including `k`, `hazar`, `lakh/lac`, grouped rupees such as `1,500`, and Urdu digits.
- Ambiguous, mixed dedicated-ledger or over-10-entry batches are not silently truncated. Every amount, category and date must be reviewed.
- Exact spending comparisons, saving rate, potential-saving estimate, budget pressure and money-health score are calculated locally from ledger data.
- Local suggestions highlight high-spend categories, saving opportunities and outstanding borrowed/lent balances.
- Loans, savings and kameti phrases are deliberately sent to their dedicated app sections instead of being silently recorded as ordinary expenses.
- Optional Cloud AI remains available as a fallback for wording the Mini AI cannot understand.

This is intentionally a compact specialized offline engine, not a general chatbot. That keeps startup/storage/RAM use low and makes it appropriate for 4 GB RAM phones. A neural on-device model can be added later without changing the ledger data model.

## Loans

Loans support both directions:

- **Borrowed**: money the user owes; repayments reduce the outstanding balance.
- **Lent**: money another person owes the user; received payments reduce the outstanding balance.

Each loan can store phone and WhatsApp contact information. The detail UI exposes Call, SMS and WhatsApp actions with pre-filled reminder text. The user always confirms/sends the message; Daily Ledger does not silently send messages.

Payment history stores date, amount, payment method and note. Over-payment is rejected and a fully repaid loan is marked settled.

## Kameti

Kameti supports structured member turns in addition to legacy schedule text. Each member turn can store:

- name and contact details;
- turn number and automatically derived turn month;
- whether the turn belongs to the signed-in user;
- received/not-received status and received date;
- notes.

The app validates duplicate turns and limits the user's own turns according to the committee share count. It shows the user's upcoming turn and a countdown, member payment/receiving status, and member Call/SMS/WhatsApp actions.

Kameti installments and receipts keep histories and payment methods. Reminder notifications cover due loan payments, kameti installments and the user's approaching kameti turn.

## Compatibility

- Room database schema: **v4** (`committee_members` added).
- Ledger JSON backup format: **v3** (structured committee members added).
- Backup import remains compatible with legacy v1/v2 ledger JSON.
- Existing Firebase encrypted cloud-backup envelope remains separate from this ledger JSON version.

## Contact picker

Loan persons and kameti members can be selected from Android's system phone-number picker or entered manually. The picker uses one-time access to the selected phone row, so Daily Ledger does not request broad `READ_CONTACTS` permission just to attach a contact.

## Quick-entry examples

The offline parser accepts both separated and compact common forms, for example:

- `doodh 300 aur petrol 2000, salary 60000 mili`
- `doodh 300 petrol 2k salary 35 hazar`

Every generated transaction remains a draft until the user reviews and saves it.

## Review improvements

The local database is SQLCipher-encrypted with a device Keystore-protected random key. Migration is verified before atomic replacement. Cloud backups remain managed account encryption (not zero-knowledge E2EE) and require server configuration plus Blaze billing. Offline Mini AI does not depend on this paid cloud setup. The budget score is only a local heuristic from recorded entries, not a credit score.
