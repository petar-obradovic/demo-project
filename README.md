# Demo Store

A small e-commerce store backend: products, customers, carts, and orders.
Java 21 · Spring Boot 3 · MongoDB. Clean architecture (api → application →
domain, with infrastructure implementing the domain's repository ports) and
pragmatic DDD.

## Run it

Start MongoDB (in Docker) and run the app locally:

```bash
docker compose up -d          # MongoDB on localhost:27017
./mvnw spring-boot:run        # app on http://localhost:8080
```

The app boots against the Mongo container and seeds demo data on first start.
Running it locally means code changes show up with a quick restart — no image
rebuild.

Interactive API docs: opening http://localhost:8080 redirects to Swagger UI at
http://localhost:8080/swagger-ui.html. The generated OpenAPI 3 specification is
served at http://localhost:8080/v3/api-docs (JSON) and
http://localhost:8080/v3/api-docs.yaml (YAML).

Unit tests: `./mvnw test` (no Docker needed). Stop MongoDB with `docker compose
down` (add `-v` to also wipe the seeded data).

## API

| Resource | Endpoints |
|---|---|
| Products | `POST /api/products` · `GET /api/products` · `GET /api/products/{id}` |
| Customers | `POST /api/customers` · `GET /api/customers/{id}` |
| Cart | `GET /api/carts/{customerId}` · `POST /api/carts/{customerId}/items` · `PATCH /api/carts/{customerId}/items/{productId}` · `DELETE /api/carts/{customerId}/items/{productId}` |
| Orders | `POST /api/orders/checkout/{customerId}` · `GET /api/orders/{id}` · `GET /api/orders?customerId=` · `POST /api/orders/{id}/pay` · `/ship` · `/deliver` |

Errors return `{timestamp, status, error, message, path}` with 404/400/409
semantics.
