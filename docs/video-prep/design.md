# Demo project design — e-commerce store backend

**Status:** approved design, 2026-07-15. Implementation happens in a later
session from the task plan in this folder.

## Purpose

This repository is the demo project for the S05 "Agents & Skills" practical
video series. It must be a credible, well-built Spring Boot backend — and it
must deliberately *lack* the things the videos create on camera:

| Created on camera (must NOT exist here) | Video |
|---|---|
| `.github/copilot-instructions.md` (and any `.github/` customization) | copilot-instructions setup video |
| A Jira-ticket-analyst agent; a plan-and-implement agent | agents video |
| An implementation-planning skill; a change-execution skill | skills video |
| The feature/change implementation itself (from a prepared ticket) | final "in action" video |

One deliberate legacy flaw ships in the code (see §8) as material for the
instructions-audit video. Everything video-related lives in
`docs/video-prep/` — a folder the camera never opens.

## Stack & constraints

- **Java 21**, **Spring Boot 3.x**, **Maven** (with `mvnw` wrapper), single
  module.
- **MongoDB 7** as a Docker container; the app itself dockerized
  (multi-stage build); **docker-compose** boots the whole system.
- **Clean architecture** in four packages under `com.demo.store`:
  `api` → `application` → `domain`; `infrastructure` implements domain ports.
  The dependency rule is enforced by convention (no ArchUnit — no tests in
  this build).
- **DDD, pragmatic tier:** aggregates with real invariants, value objects,
  repository ports/adapters. No domain events, factories, or specification
  pattern — every concept present must earn screen time.
- **Unit tests only.** JUnit 5 + Mockito + AssertJ (all via
  `spring-boot-starter-test`). No Spring context tests, no Testcontainers, no
  Mongo in tests — see §Testing.

## Domain model (`com.demo.store.domain`)

### Shared value objects (`domain.shared`)

- **`Money`** — `BigDecimal` amount, scale 2, `RoundingMode.HALF_UP`,
  currency fixed to EUR. Operations: `add`, `subtract`, `multiply(int)`,
  `percentage(BigDecimal)`, `isNegative`. Constructor normalizes scale.
  This is the house way to represent money.
- **`Email`** — validated on construction (simple RFC-ish regex).
- **`Address`** — street, city, zip, country; all non-blank.

### Aggregates (one package each)

- **`Product`** — `ProductId` (UUID string), `sku` (unique), name,
  description, `Money price`, `int stockQuantity`, `boolean active`.
  Invariants: price non-negative, stock non-negative.
  Behavior: `changePrice(Money)`, `decreaseStock(int)` (throws
  `InsufficientStockException`), `increaseStock(int)`, `deactivate()`.
- **`Customer`** — `CustomerId`, name, `Email`, shipping `Address`. Light by
  design; exists so "customer's cart/order" is real.
- **`Cart`** — `CartId`, `customerId` (one cart per customer — cleared at
  checkout, never deleted), `List<CartItem>`; `CartItem` = productId + name
  snapshot + `Money unitPrice` snapshot + quantity (≥ 1).
  Behavior: `addItem` (merges quantities for same product), `changeQuantity`,
  `removeItem`, `total(): Money`, `clear()`.
- **`Order`** — `OrderId`, `customerId`, `List<OrderLine>` (snapshots copied
  from the cart), `Money total`, `OrderStatus status`, `Instant placedAt`.
  `OrderStatus`: `NEW → PAID → SHIPPED → DELIVERED` — transition methods
  (`markPaid`, `markShipped`, `markDelivered`) enforce the state machine and
  throw `IllegalOrderStateException` otherwise.
  **Deliberately absent:** a `CANCELLED` status and any cancellation path
  (that is the change-request ticket), and any notion of discounts (that is
  the feature ticket). Stock is decremented at checkout — which is exactly
  what makes "cancellation must release stock" a real change later.

### Repository ports (`domain.<aggregate>`)

`ProductRepository`, `CustomerRepository`, `CartRepository`
(`findByCustomerId`), `OrderRepository` (`findByCustomerId`). Plain
interfaces over domain types — no Spring types in signatures.

## Application layer (`com.demo.store.application`)

Thin use-case services orchestrating domain objects through ports:

- **`CatalogService`** — create product, list active, get by id, adjust price.
- **`CustomerService`** — register, get.
- **`CartService`** — get-or-create active cart, add/change/remove item
  (validates the product exists and is active; snapshots name + price).
- **`CheckoutService`** — the one real orchestration: load cart (must be
  non-empty), verify + decrease stock per line, build the `Order` from cart
  snapshots with `PricingCalculator`, save order, clear cart.
- **`OrderService`** — get, list by customer, `pay`/`ship`/`deliver`
  transitions.
- **`PricingCalculator`** — computes line totals and the order total, in
  `Money` end to end. (Kept as a named component because the on-camera
  feature work will extend pricing — it gives the future task cards a clean
  seam.)

Application exceptions: `NotFoundException`, `EmptyCartException` — plus the
domain's `InsufficientStockException` and `IllegalOrderStateException`
passing through.

## API layer (`com.demo.store.api`)

REST controllers + request/response DTOs + mappers. **Domain types never
serialize** — every response is a DTO.

| Resource | Endpoints |
|---|---|
| Products | `POST /api/products` · `GET /api/products` · `GET /api/products/{id}` |
| Customers | `POST /api/customers` · `GET /api/customers/{id}` |
| Cart | `GET /api/carts/{customerId}` · `POST /api/carts/{customerId}/items` · `PATCH /api/carts/{customerId}/items/{productId}` · `DELETE /api/carts/{customerId}/items/{productId}` |
| Orders | `POST /api/orders/checkout/{customerId}` · `GET /api/orders/{id}` · `GET /api/orders?customerId=` · `POST /api/orders/{id}/pay` · `POST /api/orders/{id}/ship` · `POST /api/orders/{id}/deliver` |

Error handling: `GlobalExceptionHandler` (`@RestControllerAdvice`) maps
exceptions to a consistent JSON problem body
`{timestamp, status, error, message, path}`:
404 (`NotFoundException`), 400 (validation, `EmptyCartException`),
409 (`InsufficientStockException`, `IllegalOrderStateException`).
Request DTO validation via `jakarta.validation` annotations.

## Infrastructure (`com.demo.store.infrastructure`)

- **Documents & mappers:** `ProductDocument`, `CustomerDocument`,
  `CartDocument`, `OrderDocument` + hand-written mapping in the adapters (no
  MapStruct). `Money` amounts stored as `Decimal128` via
  `@Field(targetType = FieldType.DECIMAL128)`; currency code stored alongside.
- **Adapters:** `Mongo*Repository` classes implement the domain ports,
  delegating to Spring Data Mongo interfaces over the documents.
- **Indexes** (annotation-driven, `auto-index-creation: true`): unique `sku`;
  unique `carts.customerId` (one cart per customer); `orders.customerId`.
- **`DemoDataSeeder`** — idempotent `CommandLineRunner`: ~12 products across
  a few categories with realistic EUR prices, 2 customers. Every video opens
  on a populated store.
- Config: `application.yml` (local: `mongodb://localhost:27017/store`) +
  `SPRING_DATA_MONGODB_URI` override for compose.

**Convention (replaces a SQL-era "migrations" rule):** all Mongo access goes
through the repository ports — Spring Data / `MongoTemplate` types never
appear outside `infrastructure`. This is one of the auditable house rules for
the instructions video.

## Docker

- **`Dockerfile`** — multi-stage: `maven:3.9-eclipse-temurin-21` build (layer
  the dependency download), run on `eclipse-temurin:21-jre`, non-root user.
- **`docker-compose.yml`** — `mongo:7` (named volume, `mongosh ping`
  healthcheck) + `app` (build: `.`, `depends_on: condition: service_healthy`,
  env `SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/store`, port 8080).
  `docker compose up --build` is the entire boot story.
- `.dockerignore`, `.gitignore`, and a short root `README.md` (what it is,
  how to boot, endpoint list) — written as a normal project readme, no video
  references.

## §8 Video-prep material

- **Seeded legacy flaw (in main code):** `Cart.getTotal(): double` — a legacy
  convenience method (`total().amount().doubleValue()` style arithmetic done
  the wrong way: summing `unitPrice × qty` in `double`), used by the cart
  response DTO. It violates the Money rule on purpose — the star of the
  instructions-audit before/after. A `// legacy:` comment marks it subtly.
- **`docs/video-prep/ticket-FEAT-101-discount-codes.md`** — Jira-style
  feature ticket: percentage + fixed-amount discount codes, expiry date, max
  uses, applied at checkout; **deliberately ambiguous on whether codes can be
  combined** (the analyst-agent's ask-don't-guess beat).
- **`docs/video-prep/ticket-CR-142-order-cancellation.md`** — Jira-style
  change request: customers cancel orders in NEW/PAID; cancellation releases
  reserved stock; SHIPPED+ not cancellable; refunds/emails out of scope.
- **`docs/video-prep/README.md`** — the seed inventory: what's planted where
  and which video uses it. The camera never opens this folder.
- The final video uses **one** of the two tickets (feature request or change
  request); the other is the backup. Both must be implementable against this
  domain without touching more than a handful of files.

## Testing (unit tests only)

Plain JUnit 5 with Mockito for ports and AssertJ assertions; test names follow
**given/when/then** (`givenExpiredState_whenMarkPaid_thenThrows`). No Spring
context is loaded anywhere in the test suite — everything under
`src/test/java` runs in milliseconds via `./mvnw test`.

- **Domain:** `Money` (scale/rounding, add/multiply/percentage, negative
  guard) · `Product` (stock decrease/increase invariants,
  `InsufficientStockException`) · `Cart` (add merges same product, quantity
  rules, `total()` in `Money`, `clear`) · `Order` (full state-machine matrix —
  legal transitions succeed, every illegal one throws
  `IllegalOrderStateException`).
- **Application:** services with mocked repository ports — `CheckoutService`
  (happy path builds the order from snapshots and clears the cart; empty cart
  → `EmptyCartException`; short stock → `InsufficientStockException` and
  nothing persisted) · `CartService` (product must exist and be active;
  snapshots taken) · `OrderService` (transitions delegate + persist) ·
  `PricingCalculator` (line and order totals in `Money`).
- **Not covered on purpose:** controllers/DTO mappers (thin, exercised via
  the compose'd app), infrastructure adapters (would need Mongo — integration
  tests are out of scope), and the legacy `Cart.getTotal(): double` (tests pin
  the *correct* `total(): Money` path only; the flaw stays untested bait for
  the instructions video).

## Out of scope

Integration/E2E tests (unit tests only — see §Testing),
authentication/security, payment processing (pay is a status flip), shipping
integration, frontend, admin UI, discounts, cancellation, `.github/`
customization, CI.

## Definition of done (for the implementing session)

1. `docker compose up --build` from a clean checkout → app healthy on
   `:8080`, Mongo seeded. `./mvnw test` green (unit tests only, no Docker
   needed).
2. The happy path works end to end via HTTP: create/list products → cart
   add/change → checkout → order visible → pay → ship → deliver.
3. Error paths return the problem body: unknown ids (404), empty-cart
   checkout (400), overdrawn stock (409), illegal transition (409).
4. `Cart.getTotal()` double flaw present and wired into the cart DTO; both
   tickets and the seed inventory present under `docs/video-prep/`.
5. Layer rule holds: no Spring Data types outside `infrastructure`, no domain
   types in API responses.
