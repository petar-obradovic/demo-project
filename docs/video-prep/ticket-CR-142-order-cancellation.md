# CR-142 — Allow customers to cancel an order before it ships

| | |
|---|---|
| **Type** | Change request |
| **Priority** | Medium |
| **Reporter** | Support |
| **Component** | Orders |

## Description

Support keeps handling cancellation requests manually. Customers should be
able to cancel their own order as long as it has not shipped.

## Current behavior

Orders move NEW → PAID → SHIPPED → DELIVERED. There is no cancellation; stock
reserved at checkout is never released.

## Requested behavior

- Orders in status **NEW** or **PAID** can be cancelled by the customer.
- A cancelled order **releases its reserved stock** back to the products.
- Orders in **SHIPPED** or later **cannot** be cancelled — attempting it is a
  clear error, not a silent no-op.

## Out of scope

- Refund processing.
- Email notifications.
