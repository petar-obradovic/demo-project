# Video-prep inventory

This folder (and this folder only) may reference the video series. The camera
never opens it.

## Seeded material

| What | Where | Used by |
|---|---|---|
| Legacy `double` money flaw | `Cart.getTotal()` (`domain/cart/Cart.java`), exposed as `total` in `CartResponse` (`api/dto/CartDtos.java`) | copilot-instructions video: the convention audit + before/after beat |
| Feature ticket (deliberately silent on combining codes) | `ticket-FEAT-101-discount-codes.md` | agents/skills videos + finale (feature-request path) |
| Change-request ticket | `ticket-CR-142-order-cancellation.md` | finale (change-request path) |

## Deliberately absent (created or implemented on camera)

- `.github/` — no copilot-instructions.md, no agents, no skills.
- Discount codes (FEAT-101) and order cancellation / CANCELLED status (CR-142).
- The design + implementation plan for this repo also live here
  (`design.md`, `implementation-plan.md`) — they are prep docs, not product docs.
