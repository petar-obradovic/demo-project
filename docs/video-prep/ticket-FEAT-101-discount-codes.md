# FEAT-101 — Discount codes at checkout

| | |
|---|---|
| **Type** | Feature |
| **Priority** | High |
| **Reporter** | Product |
| **Component** | Checkout |

## Description

Marketing wants to run discount campaigns. Customers enter a discount code
when checking out; a valid code reduces the order total.

We need two kinds of codes:

- **Percentage codes** — e.g. `WELCOME10` takes 10% off the order total.
- **Fixed-amount codes** — e.g. `SAVE5` takes €5.00 off the order total.

Codes have an **expiry date** and a **maximum number of uses** across all
customers. An expired or exhausted code is rejected with a clear error.

## Acceptance criteria

- A valid code applied at checkout reduces the order total accordingly.
- The order records which code was applied.
- An expired code, an exhausted code, or an unknown code is rejected and the
  order is not placed.
- A fixed-amount discount never brings the total below €0.00.

## Out of scope

- Admin UI for managing codes (create them via API or seed data).
- Per-customer usage limits.
