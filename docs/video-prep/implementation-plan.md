# Demo Store Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the e-commerce demo backend specified in `docs/video-prep/design.md` — a Java 21 / Spring Boot 3 store (products, customers, carts, orders) with clean architecture + pragmatic DDD, MongoDB via docker-compose, unit tests only.

**Architecture:** Four packages under `com.demo.store`: `domain` (aggregates, value objects, repository ports — no Spring), `application` (use-case services), `api` (controllers + DTOs, domain never serializes), `infrastructure` (Mongo documents/mappers/adapters, seeder). Dependency rule: api → application → domain; infrastructure implements domain ports.

**Tech Stack:** Java 21, Spring Boot 3.4.x (web, data-mongodb, validation), JUnit 5 + Mockito + AssertJ (unit tests only, no Spring context in tests), MongoDB 7 (Docker), Maven with wrapper.

**Working directory:** `C:\projects\demo-project` (git repo already initialized; `docs/video-prep/design.md` is the spec).

**Conventions for every task:** constructor injection only; money passes through `Money` — never `double`/`float` (one deliberate legacy exception in Task 6); test names are `given…_when…_then…`; commit after every task.

---

### Task 1: Maven scaffold & runnable app skeleton

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `src/main/java/com/demo/store/StoreApplication.java`
- Create: `src/main/resources/application.yml`
- Create (generated): `mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`

- [ ] **Step 1: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
    <relativePath/>
  </parent>

  <groupId>com.demo</groupId>
  <artifactId>store</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>demo-store</name>
  <description>E-commerce store backend demo (clean architecture + DDD)</description>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

*(Version note: any Spring Boot 3.3+/3.4+ patch works; pin whatever is current when executing.)*

- [ ] **Step 2: Write `.gitignore`**

```gitignore
target/
.idea/
*.iml
.vscode/
.DS_Store
```

- [ ] **Step 3: Write `StoreApplication.java`**

```java
package com.demo.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
```

- [ ] **Step 4: Write `src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: demo-store
  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/store}
      auto-index-creation: true

server:
  port: 8080
```

- [ ] **Step 5: Generate the Maven wrapper**

Run from the repo root (needs any locally installed Maven 3.9+ once; afterwards only the wrapper is used):

```bash
mvn -N wrapper:wrapper -Dmaven=3.9.9
```

Expected: creates `mvnw`, `mvnw.cmd`, `.mvn/wrapper/`. (If no local Maven exists, copy the wrapper files from a `start.spring.io` zip for the same Boot version — identical content.)

- [ ] **Step 6: Verify it compiles and boots without Mongo**

```bash
./mvnw -q compile
```

Expected: `BUILD SUCCESS`. (Do not `spring-boot:run` yet — no Mongo is up; boot verification happens in Task 15.)

- [ ] **Step 7: Commit**

```bash
git add pom.xml .gitignore src .mvn mvnw mvnw.cmd
git commit -m "chore: scaffold Spring Boot 3 / Java 21 Maven project"
```

---

### Task 2: `Money` value object (TDD)

**Files:**
- Create: `src/main/java/com/demo/store/domain/shared/Money.java`
- Test: `src/test/java/com/demo/store/domain/shared/MoneyTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.demo.store.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void givenUnscaledAmount_whenConstructed_thenNormalizedToScale2HalfUp() {
        assertThat(Money.of("10.005").amount()).isEqualByComparingTo(new BigDecimal("10.01"));
        assertThat(Money.of("10").amount().scale()).isEqualTo(2);
    }

    @Test
    void givenTwoAmounts_whenAdded_thenSumsExactly() {
        assertThat(Money.of("19.99").add(Money.of("0.01"))).isEqualTo(Money.of("20.00"));
    }

    @Test
    void givenAmount_whenSubtracted_thenDifferenceExact() {
        assertThat(Money.of("20.00").subtract(Money.of("0.01"))).isEqualTo(Money.of("19.99"));
    }

    @Test
    void givenUnitPrice_whenMultipliedByQuantity_thenNoDrift() {
        assertThat(Money.of("19.99").multiply(3)).isEqualTo(Money.of("59.97"));
    }

    @Test
    void givenAmount_whenPercentageTaken_thenRoundedHalfUp() {
        assertThat(Money.of("59.97").percentage(new BigDecimal("0.10"))).isEqualTo(Money.of("6.00"));
    }

    @Test
    void givenNegativeAmount_whenIsNegative_thenTrue() {
        assertThat(Money.of("-0.01").isNegative()).isTrue();
        assertThat(Money.zero().isNegative()).isFalse();
    }

    @Test
    void givenDifferentCurrencies_whenAdded_thenThrows() {
        Money eur = Money.of("1.00");
        Money usd = new Money(new BigDecimal("1.00"), java.util.Currency.getInstance("USD"));
        assertThatThrownBy(() -> eur.add(usd)).isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=MoneyTest
```

Expected: compilation error — `Money` does not exist. (A compile failure of the test source counts as the red step.)

- [ ] **Step 3: Implement `Money`**

```java
package com.demo.store.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** House representation of money: BigDecimal, scale 2, HALF_UP, EUR by default. */
public record Money(BigDecimal amount, Currency currency) {

    public static final Currency EUR = Currency.getInstance("EUR");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, EUR);
    }

    public static Money zero() {
        return of(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    public Money percentage(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=MoneyTest
```

Expected: `Tests run: 7, Failures: 0, Errors: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/domain/shared/Money.java src/test/java/com/demo/store/domain/shared/MoneyTest.java
git commit -m "feat(domain): Money value object (scale 2, HALF_UP, EUR)"
```

---

### Task 3: `Email` & `Address` value objects (TDD)

**Files:**
- Create: `src/main/java/com/demo/store/domain/shared/Email.java`
- Create: `src/main/java/com/demo/store/domain/shared/Address.java`
- Test: `src/test/java/com/demo/store/domain/shared/EmailTest.java`
- Test: `src/test/java/com/demo/store/domain/shared/AddressTest.java`

- [ ] **Step 1: Write the failing tests**

`EmailTest.java`:

```java
package com.demo.store.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void givenValidAddress_whenConstructed_thenHoldsValue() {
        assertThat(new Email("ana@example.com").value()).isEqualTo("ana@example.com");
    }

    @Test
    void givenInvalidAddress_whenConstructed_thenThrows() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

`AddressTest.java`:

```java
package com.demo.store.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AddressTest {

    @Test
    void givenAllFields_whenConstructed_thenOk() {
        assertThatNoException().isThrownBy(
                () -> new Address("Main St 1", "Belgrade", "11000", "RS"));
    }

    @Test
    void givenBlankField_whenConstructed_thenThrows() {
        assertThatThrownBy(() -> new Address(" ", "Belgrade", "11000", "RS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Address("Main St 1", "Belgrade", "11000", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest="EmailTest,AddressTest"
```

Expected: compilation error — classes do not exist.

- [ ] **Step 3: Implement**

`Email.java`:

```java
package com.demo.store.domain.shared;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    public Email {
        if (value == null || !SIMPLE_EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}
```

`Address.java`:

```java
package com.demo.store.domain.shared;

public record Address(String street, String city, String zip, String country) {

    public Address {
        requireNonBlank(street, "street");
        requireNonBlank(city, "city");
        requireNonBlank(zip, "zip");
        requireNonBlank(country, "country");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest="EmailTest,AddressTest"
```

Expected: `Tests run: 4, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/domain/shared src/test/java/com/demo/store/domain/shared
git commit -m "feat(domain): Email and Address value objects"
```

---

### Task 4: `Product` aggregate + repository port (TDD)

**Files:**
- Create: `src/main/java/com/demo/store/domain/product/ProductId.java`
- Create: `src/main/java/com/demo/store/domain/product/Product.java`
- Create: `src/main/java/com/demo/store/domain/product/InsufficientStockException.java`
- Create: `src/main/java/com/demo/store/domain/product/ProductRepository.java`
- Test: `src/test/java/com/demo/store/domain/product/ProductTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.demo.store.domain.product;

import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Product laptop() {
        return Product.create("SKU-1", "Laptop", "13-inch laptop", Money.of("999.90"), 10);
    }

    @Test
    void givenNewProduct_whenCreated_thenActiveWithIdAndStock() {
        Product p = laptop();
        assertThat(p.id()).isNotNull();
        assertThat(p.active()).isTrue();
        assertThat(p.stockQuantity()).isEqualTo(10);
    }

    @Test
    void givenNegativePrice_whenCreated_thenThrows() {
        assertThatThrownBy(() -> Product.create("SKU-2", "X", "d", Money.of("-1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNegativeStock_whenCreated_thenThrows() {
        assertThatThrownBy(() -> Product.create("SKU-2", "X", "d", Money.of("1.00"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenEnoughStock_whenDecreased_thenReduced() {
        Product p = laptop();
        p.decreaseStock(4);
        assertThat(p.stockQuantity()).isEqualTo(6);
    }

    @Test
    void givenTooLittleStock_whenDecreased_thenThrowsAndUnchanged() {
        Product p = laptop();
        assertThatThrownBy(() -> p.decreaseStock(11))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(p.stockQuantity()).isEqualTo(10);
    }

    @Test
    void givenProduct_whenStockIncreased_thenAdded() {
        Product p = laptop();
        p.increaseStock(5);
        assertThat(p.stockQuantity()).isEqualTo(15);
    }

    @Test
    void givenNegativePrice_whenPriceChanged_thenThrows() {
        Product p = laptop();
        assertThatThrownBy(() -> p.changePrice(Money.of("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenProduct_whenDeactivated_thenInactive() {
        Product p = laptop();
        p.deactivate();
        assertThat(p.active()).isFalse();
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=ProductTest
```

Expected: compilation error — classes do not exist.

- [ ] **Step 3: Implement**

`ProductId.java`:

```java
package com.demo.store.domain.product;

import java.util.Objects;
import java.util.UUID;

public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "value");
    }

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID().toString());
    }
}
```

`InsufficientStockException.java`:

```java
package com.demo.store.domain.product;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(ProductId productId, int requested, int available) {
        super("Insufficient stock for product %s: requested %d, available %d"
                .formatted(productId.value(), requested, available));
    }
}
```

`Product.java`:

```java
package com.demo.store.domain.product;

import com.demo.store.domain.shared.Money;

import java.util.Objects;

public class Product {

    private final ProductId id;
    private final String sku;
    private String name;
    private String description;
    private Money price;
    private int stockQuantity;
    private boolean active;

    public Product(ProductId id, String sku, String name, String description,
                   Money price, int stockQuantity, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.sku = requireNonBlank(sku, "sku");
        this.name = requireNonBlank(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.price = requireNonNegative(price);
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must be >= 0");
        }
        this.stockQuantity = stockQuantity;
        this.active = active;
    }

    public static Product create(String sku, String name, String description,
                                 Money price, int initialStock) {
        return new Product(ProductId.newId(), sku, name, description, price, initialStock, true);
    }

    public void changePrice(Money newPrice) {
        this.price = requireNonNegative(newPrice);
    }

    public void decreaseStock(int quantity) {
        requirePositive(quantity);
        if (quantity > stockQuantity) {
            throw new InsufficientStockException(id, quantity, stockQuantity);
        }
        stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        requirePositive(quantity);
        stockQuantity += quantity;
    }

    public boolean canFulfill(int quantity) {
        return quantity <= stockQuantity;
    }

    public void deactivate() {
        this.active = false;
    }

    public ProductId id() { return id; }
    public String sku() { return sku; }
    public String name() { return name; }
    public String description() { return description; }
    public Money price() { return price; }
    public int stockQuantity() { return stockQuantity; }
    public boolean active() { return active; }

    private static Money requireNonNegative(Money money) {
        Objects.requireNonNull(money, "price");
        if (money.isNegative()) {
            throw new IllegalArgumentException("price must not be negative");
        }
        return money;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
```

`ProductRepository.java` (port — plain interface, no Spring):

```java
package com.demo.store.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId id);

    List<Product> findAllActive();
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=ProductTest
```

Expected: `Tests run: 8, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/domain/product src/test/java/com/demo/store/domain/product
git commit -m "feat(domain): Product aggregate with stock invariants and repository port"
```

---
### Task 5: `Customer` aggregate + repository port (TDD)

**Files:**
- Create: `src/main/java/com/demo/store/domain/customer/CustomerId.java`
- Create: `src/main/java/com/demo/store/domain/customer/Customer.java`
- Create: `src/main/java/com/demo/store/domain/customer/CustomerRepository.java`
- Test: `src/test/java/com/demo/store/domain/customer/CustomerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.demo.store.domain.customer;

import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void givenValidData_whenRegistered_thenHasIdAndFields() {
        Customer c = Customer.register("Ana", new Email("ana@example.com"),
                new Address("Main St 1", "Belgrade", "11000", "RS"));
        assertThat(c.id()).isNotNull();
        assertThat(c.name()).isEqualTo("Ana");
        assertThat(c.email().value()).isEqualTo("ana@example.com");
    }

    @Test
    void givenBlankName_whenRegistered_thenThrows() {
        assertThatThrownBy(() -> Customer.register(" ", new Email("a@b.com"),
                new Address("s", "c", "z", "RS")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=CustomerTest
```

Expected: compilation error — classes do not exist.

- [ ] **Step 3: Implement**

`CustomerId.java`:

```java
package com.demo.store.domain.customer;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
    }

    public static CustomerId newId() {
        return new CustomerId(UUID.randomUUID().toString());
    }
}
```

`Customer.java`:

```java
package com.demo.store.domain.customer;

import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;

import java.util.Objects;

public class Customer {

    private final CustomerId id;
    private final String name;
    private final Email email;
    private final Address shippingAddress;

    public Customer(CustomerId id, String name, Email email, Address shippingAddress) {
        this.id = Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name;
        this.email = Objects.requireNonNull(email, "email");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress");
    }

    public static Customer register(String name, Email email, Address shippingAddress) {
        return new Customer(CustomerId.newId(), name, email, shippingAddress);
    }

    public CustomerId id() { return id; }
    public String name() { return name; }
    public Email email() { return email; }
    public Address shippingAddress() { return shippingAddress; }
}
```

`CustomerRepository.java`:

```java
package com.demo.store.domain.customer;

import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=CustomerTest
```

Expected: `Tests run: 2, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/domain/customer src/test/java/com/demo/store/domain/customer
git commit -m "feat(domain): Customer aggregate and repository port"
```

---

### Task 6: `Cart` aggregate + repository port (TDD) — includes the deliberate legacy flaw

**Files:**
- Create: `src/main/java/com/demo/store/domain/cart/CartId.java`
- Create: `src/main/java/com/demo/store/domain/cart/CartItem.java`
- Create: `src/main/java/com/demo/store/domain/cart/Cart.java`
- Create: `src/main/java/com/demo/store/domain/cart/ItemNotInCartException.java`
- Create: `src/main/java/com/demo/store/domain/cart/CartRepository.java`
- Test: `src/test/java/com/demo/store/domain/cart/CartTest.java`

> **Video note:** `Cart.getTotal(): double` is the *intentional* legacy flaw from
> the spec (§8). It violates the Money rule on purpose, is used by the cart DTO
> (Task 14), and gets **no test**. Do not "fix" it; do not test it.

- [ ] **Step 1: Write the failing tests** (they cover the correct `total(): Money` path only)

```java
package com.demo.store.domain.cart;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final ProductId laptop = ProductId.newId();
    private final ProductId mouse = ProductId.newId();

    private Cart cart() {
        return Cart.createFor(CustomerId.newId());
    }

    @Test
    void givenEmptyCart_whenItemAdded_thenContainsItem() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        assertThat(c.items()).hasSize(1);
        assertThat(c.items().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void givenItemInCart_whenSameProductAdded_thenQuantitiesMerge() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.addItem(laptop, "Laptop", Money.of("999.90"), 2);
        assertThat(c.items()).hasSize(1);
        assertThat(c.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void givenZeroQuantity_whenAdded_thenThrows() {
        assertThatThrownBy(() -> cart().addItem(laptop, "Laptop", Money.of("1.00"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenItemInCart_whenQuantityChanged_thenUpdated() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.changeQuantity(laptop, 5);
        assertThat(c.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void givenProductNotInCart_whenQuantityChanged_thenThrows() {
        assertThatThrownBy(() -> cart().changeQuantity(mouse, 2))
                .isInstanceOf(ItemNotInCartException.class);
    }

    @Test
    void givenItemInCart_whenRemoved_thenGone() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.removeItem(laptop);
        assertThat(c.items()).isEmpty();
    }

    @Test
    void givenProductNotInCart_whenRemoved_thenThrows() {
        assertThatThrownBy(() -> cart().removeItem(mouse))
                .isInstanceOf(ItemNotInCartException.class);
    }

    @Test
    void givenItems_whenTotal_thenSumInMoney() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 2);
        c.addItem(mouse, "Mouse", Money.of("19.99"), 3);
        assertThat(c.total()).isEqualTo(Money.of("2059.77"));
    }

    @Test
    void givenItems_whenCleared_thenEmpty() {
        Cart c = cart();
        c.addItem(laptop, "Laptop", Money.of("999.90"), 1);
        c.clear();
        assertThat(c.items()).isEmpty();
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=CartTest
```

Expected: compilation error — classes do not exist.

- [ ] **Step 3: Implement**

`CartId.java`:

```java
package com.demo.store.domain.cart;

import java.util.Objects;
import java.util.UUID;

public record CartId(String value) {

    public CartId {
        Objects.requireNonNull(value, "value");
    }

    public static CartId newId() {
        return new CartId(UUID.randomUUID().toString());
    }
}
```

`CartItem.java`:

```java
package com.demo.store.domain.cart;

import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;

import java.util.Objects;

/** Snapshot of a product at the moment it entered the cart. */
public record CartItem(ProductId productId, String name, Money unitPrice, int quantity) {

    public CartItem {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
    }

    CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, name, unitPrice, newQuantity);
    }
}
```

`ItemNotInCartException.java`:

```java
package com.demo.store.domain.cart;

import com.demo.store.domain.product.ProductId;

public class ItemNotInCartException extends RuntimeException {

    public ItemNotInCartException(ProductId productId) {
        super("Product %s is not in the cart".formatted(productId.value()));
    }
}
```

`Cart.java`:

```java
package com.demo.store.domain.cart;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cart {

    private final CartId id;
    private final CustomerId customerId;
    private final List<CartItem> items;

    public Cart(CartId id, CustomerId customerId, List<CartItem> items) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
    }

    public static Cart createFor(CustomerId customerId) {
        return new Cart(CartId.newId(), customerId, new ArrayList<>());
    }

    public void addItem(ProductId productId, String name, Money unitPrice, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        int existing = indexOf(productId);
        if (existing >= 0) {
            CartItem item = items.get(existing);
            items.set(existing, item.withQuantity(item.quantity() + quantity));
        } else {
            items.add(new CartItem(productId, name, unitPrice, quantity));
        }
    }

    public void changeQuantity(ProductId productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        int index = requireIndex(productId);
        items.set(index, items.get(index).withQuantity(quantity));
    }

    public void removeItem(ProductId productId) {
        items.remove(requireIndex(productId));
    }

    public Money total() {
        return items.stream()
                .map(item -> item.unitPrice().multiply(item.quantity()))
                .reduce(Money.zero(), Money::add);
    }

    // legacy: pre-Money convenience kept for old integrations — used by the cart API response
    public double getTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.unitPrice().amount().doubleValue() * item.quantity();
        }
        return total;
    }

    public void clear() {
        items.clear();
    }

    public CartId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public List<CartItem> items() { return List.copyOf(items); }

    private int indexOf(ProductId productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }

    private int requireIndex(ProductId productId) {
        int index = indexOf(productId);
        if (index < 0) {
            throw new ItemNotInCartException(productId);
        }
        return index;
    }
}
```

`CartRepository.java`:

```java
package com.demo.store.domain.cart;

import com.demo.store.domain.customer.CustomerId;

import java.util.Optional;

public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findByCustomerId(CustomerId customerId);
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=CartTest
```

Expected: `Tests run: 9, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/domain/cart src/test/java/com/demo/store/domain/cart
git commit -m "feat(domain): Cart aggregate with merge/quantity rules and Money total"
```

---

### Task 7: `Order` aggregate + state machine (TDD)

**Files:**
- Create: `src/main/java/com/demo/store/domain/order/OrderId.java`
- Create: `src/main/java/com/demo/store/domain/order/OrderStatus.java`
- Create: `src/main/java/com/demo/store/domain/order/OrderLine.java`
- Create: `src/main/java/com/demo/store/domain/order/IllegalOrderStateException.java`
- Create: `src/main/java/com/demo/store/domain/order/Order.java`
- Create: `src/main/java/com/demo/store/domain/order/OrderRepository.java`
- Test: `src/test/java/com/demo/store/domain/order/OrderTest.java`

> **Video note:** `OrderStatus` has NO `CANCELLED` value and `Order` has no
> cancellation method — the change-request ticket adds those on camera. Do not
> add them here.

- [ ] **Step 1: Write the failing tests**

```java
package com.demo.store.domain.order;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order newOrder() {
        OrderLine line = new OrderLine(ProductId.newId(), "Laptop",
                Money.of("999.90"), 2, Money.of("1999.80"));
        return Order.place(CustomerId.newId(), List.of(line), Money.of("1999.80"));
    }

    @Test
    void givenPlacedOrder_thenStatusNewWithTimestampAndTotal() {
        Order o = newOrder();
        assertThat(o.status()).isEqualTo(OrderStatus.NEW);
        assertThat(o.placedAt()).isNotNull();
        assertThat(o.total()).isEqualTo(Money.of("1999.80"));
    }

    @Test
    void givenNoLines_whenPlaced_thenThrows() {
        assertThatThrownBy(() -> Order.place(CustomerId.newId(), List.of(), Money.zero()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNewOrder_whenPaidShippedDelivered_thenWalksTheHappyPath() {
        Order o = newOrder();
        o.markPaid();
        assertThat(o.status()).isEqualTo(OrderStatus.PAID);
        o.markShipped();
        assertThat(o.status()).isEqualTo(OrderStatus.SHIPPED);
        o.markDelivered();
        assertThat(o.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void givenNewOrder_whenShippedOrDelivered_thenThrows() {
        assertThatThrownBy(() -> newOrder().markShipped())
                .isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(() -> newOrder().markDelivered())
                .isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void givenPaidOrder_whenPaidAgainOrDelivered_thenThrows() {
        Order paid = newOrder();
        paid.markPaid();
        assertThatThrownBy(paid::markPaid).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(paid::markDelivered).isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void givenDeliveredOrder_whenAnyTransition_thenThrows() {
        Order o = newOrder();
        o.markPaid();
        o.markShipped();
        o.markDelivered();
        assertThatThrownBy(o::markPaid).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(o::markShipped).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(o::markDelivered).isInstanceOf(IllegalOrderStateException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=OrderTest
```

Expected: compilation error — classes do not exist.

- [ ] **Step 3: Implement**

`OrderId.java`:

```java
package com.demo.store.domain.order;

import java.util.Objects;
import java.util.UUID;

public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID().toString());
    }
}
```

`OrderStatus.java`:

```java
package com.demo.store.domain.order;

public enum OrderStatus {
    NEW, PAID, SHIPPED, DELIVERED
}
```

`OrderLine.java`:

```java
package com.demo.store.domain.order;

import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;

import java.util.Objects;

/** Immutable snapshot of one cart line at checkout time. */
public record OrderLine(ProductId productId, String name, Money unitPrice,
                        int quantity, Money lineTotal) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(lineTotal, "lineTotal");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
    }
}
```

`IllegalOrderStateException.java`:

```java
package com.demo.store.domain.order;

public class IllegalOrderStateException extends RuntimeException {

    public IllegalOrderStateException(OrderStatus current, String attempted) {
        super("Cannot %s an order in status %s".formatted(attempted, current));
    }
}
```

`Order.java`:

```java
package com.demo.store.domain.order;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.shared.Money;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private final Money total;
    private OrderStatus status;
    private final Instant placedAt;

    public Order(OrderId id, CustomerId customerId, List<OrderLine> lines,
                 Money total, OrderStatus status, Instant placedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order must have at least one line");
        }
        this.lines = List.copyOf(lines);
        this.total = Objects.requireNonNull(total, "total");
        this.status = Objects.requireNonNull(status, "status");
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
    }

    public static Order place(CustomerId customerId, List<OrderLine> lines, Money total) {
        return new Order(OrderId.newId(), customerId, lines, total,
                OrderStatus.NEW, Instant.now());
    }

    public void markPaid() {
        requireStatus(OrderStatus.NEW, "pay");
        status = OrderStatus.PAID;
    }

    public void markShipped() {
        requireStatus(OrderStatus.PAID, "ship");
        status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        requireStatus(OrderStatus.SHIPPED, "deliver");
        status = OrderStatus.DELIVERED;
    }

    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public List<OrderLine> lines() { return lines; }
    public Money total() { return total; }
    public OrderStatus status() { return status; }
    public Instant placedAt() { return placedAt; }

    private void requireStatus(OrderStatus expected, String attempted) {
        if (status != expected) {
            throw new IllegalOrderStateException(status, attempted);
        }
    }
}
```

`OrderRepository.java`:

```java
package com.demo.store.domain.order;

import com.demo.store.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomerId(CustomerId customerId);
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=OrderTest
```

Expected: `Tests run: 6, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/domain/order src/test/java/com/demo/store/domain/order
git commit -m "feat(domain): Order aggregate with NEW->PAID->SHIPPED->DELIVERED state machine"
```

---

### Task 8: Application exceptions + `PricingCalculator` (TDD)

**Files:**
- Create: `src/main/java/com/demo/store/application/NotFoundException.java`
- Create: `src/main/java/com/demo/store/application/EmptyCartException.java`
- Create: `src/main/java/com/demo/store/application/PricingCalculator.java`
- Test: `src/test/java/com/demo/store/application/PricingCalculatorTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.demo.store.application;

import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void givenUnitPriceAndQuantity_whenLineTotal_thenExactMoney() {
        assertThat(calculator.lineTotal(Money.of("19.99"), 3)).isEqualTo(Money.of("59.97"));
    }

    @Test
    void givenLines_whenOrderTotal_thenSumOfLineTotals() {
        OrderLine a = new OrderLine(ProductId.newId(), "A", Money.of("999.90"), 2, Money.of("1999.80"));
        OrderLine b = new OrderLine(ProductId.newId(), "B", Money.of("19.99"), 3, Money.of("59.97"));
        assertThat(calculator.orderTotal(List.of(a, b))).isEqualTo(Money.of("2059.77"));
    }

    @Test
    void givenNoLines_whenOrderTotal_thenZero() {
        assertThat(calculator.orderTotal(List.of())).isEqualTo(Money.zero());
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=PricingCalculatorTest
```

Expected: compilation error — classes do not exist.

- [ ] **Step 3: Implement**

`NotFoundException.java`:

```java
package com.demo.store.application;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, String id) {
        super("%s %s not found".formatted(what, id));
    }
}
```

`EmptyCartException.java`:

```java
package com.demo.store.application;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException(String customerId) {
        super("Cart for customer %s is empty".formatted(customerId));
    }
}
```

`PricingCalculator.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computes line and order totals. Kept as a named component: pricing is the
 * seam future features (discounts, taxes) will extend.
 */
@Component
public class PricingCalculator {

    public Money lineTotal(Money unitPrice, int quantity) {
        return unitPrice.multiply(quantity);
    }

    public Money orderTotal(List<OrderLine> lines) {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.zero(), Money::add);
    }
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=PricingCalculatorTest
```

Expected: `Tests run: 3, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/application src/test/java/com/demo/store/application
git commit -m "feat(application): pricing calculator and application exceptions"
```

---
### Task 9: `CatalogService` & `CustomerService` (TDD, mocked ports)

**Files:**
- Create: `src/main/java/com/demo/store/application/CatalogService.java`
- Create: `src/main/java/com/demo/store/application/CustomerService.java`
- Test: `src/test/java/com/demo/store/application/CatalogServiceTest.java`
- Test: `src/test/java/com/demo/store/application/CustomerServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`CatalogServiceTest.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void givenValidData_whenCreateProduct_thenSavedAndReturned() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product created = catalogService.createProduct(
                "SKU-1", "Laptop", "13-inch", Money.of("999.90"), 10);

        assertThat(created.sku()).isEqualTo("SKU-1");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void givenUnknownId_whenGetProduct_thenNotFound() {
        ProductId id = ProductId.newId();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProduct(id))
                .isInstanceOf(NotFoundException.class);
    }
}
```

`CustomerServiceTest.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void givenValidData_whenRegister_thenSavedAndReturned() {
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer c = customerService.register("Ana", "ana@example.com",
                new Address("Main St 1", "Belgrade", "11000", "RS"));

        assertThat(c.email()).isEqualTo(new Email("ana@example.com"));
    }

    @Test
    void givenUnknownId_whenGetCustomer_thenNotFound() {
        CustomerId id = CustomerId.newId();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(NotFoundException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest="CatalogServiceTest,CustomerServiceTest"
```

Expected: compilation error — services do not exist.

- [ ] **Step 3: Implement**

`CatalogService.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String sku, String name, String description,
                                 Money price, int initialStock) {
        return productRepository.save(Product.create(sku, name, description, price, initialStock));
    }

    public Product getProduct(ProductId id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id.value()));
    }

    public List<Product> listActiveProducts() {
        return productRepository.findAllActive();
    }
}
```

`CustomerService.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer register(String name, String email, Address shippingAddress) {
        return customerRepository.save(Customer.register(name, new Email(email), shippingAddress));
    }

    public Customer getCustomer(CustomerId id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer", id.value()));
    }
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest="CatalogServiceTest,CustomerServiceTest"
```

Expected: `Tests run: 4, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/application src/test/java/com/demo/store/application
git commit -m "feat(application): catalog and customer services"
```

---

### Task 10: `CartService` (TDD, mocked ports)

**Files:**
- Create: `src/main/java/com/demo/store/application/CartService.java`
- Test: `src/test/java/com/demo/store/application/CartServiceTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private final CustomerId customerId = CustomerId.newId();
    private Product laptop;

    @BeforeEach
    void setUp() {
        laptop = Product.create("SKU-1", "Laptop", "13-inch", Money.of("999.90"), 10);
        lenient().when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void givenNoCart_whenGetOrCreate_thenNewCartSaved() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        Cart cart = cartService.getOrCreateCart(customerId);

        assertThat(cart.customerId()).isEqualTo(customerId);
        assertThat(cart.items()).isEmpty();
    }

    @Test
    void givenActiveProduct_whenAddItem_thenSnapshotStored() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));

        Cart cart = cartService.addItem(customerId, laptop.id(), 2);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).name()).isEqualTo("Laptop");
        assertThat(cart.items().get(0).unitPrice()).isEqualTo(Money.of("999.90"));
    }

    @Test
    void givenUnknownProduct_whenAddItem_thenNotFound() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productRepository.findById(laptop.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(customerId, laptop.id(), 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void givenInactiveProduct_whenAddItem_thenRejected() {
        laptop.deactivate();
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));

        assertThatThrownBy(() -> cartService.addItem(customerId, laptop.id(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest=CartServiceTest
```

Expected: compilation error — `CartService` does not exist.

- [ ] **Step 3: Implement**

```java
package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Cart getOrCreateCart(CustomerId customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.createFor(customerId)));
    }

    public Cart addItem(CustomerId customerId, ProductId productId, int quantity) {
        Cart cart = getOrCreateCart(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId.value()));
        if (!product.active()) {
            throw new IllegalArgumentException(
                    "Product %s is not available".formatted(productId.value()));
        }
        cart.addItem(product.id(), product.name(), product.price(), quantity);
        return cartRepository.save(cart);
    }

    public Cart changeQuantity(CustomerId customerId, ProductId productId, int quantity) {
        Cart cart = getOrCreateCart(customerId);
        cart.changeQuantity(productId, quantity);
        return cartRepository.save(cart);
    }

    public Cart removeItem(CustomerId customerId, ProductId productId) {
        Cart cart = getOrCreateCart(customerId);
        cart.removeItem(productId);
        return cartRepository.save(cart);
    }
}
```

- [ ] **Step 4: Run to verify pass**

```bash
./mvnw -q test -Dtest=CartServiceTest
```

Expected: `Tests run: 4, Failures: 0` → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/application/CartService.java src/test/java/com/demo/store/application/CartServiceTest.java
git commit -m "feat(application): cart service with product snapshotting"
```

---

### Task 11: `CheckoutService` & `OrderService` (TDD, mocked ports)

**Files:**
- Create: `src/main/java/com/demo/store/application/CheckoutService.java`
- Create: `src/main/java/com/demo/store/application/OrderService.java`
- Test: `src/test/java/com/demo/store/application/CheckoutServiceTest.java`
- Test: `src/test/java/com/demo/store/application/OrderServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`CheckoutServiceTest.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.product.InsufficientStockException;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    private CheckoutService checkoutService;

    private final CustomerId customerId = CustomerId.newId();
    private Product laptop;
    private Product mouse;
    private Cart cart;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                cartRepository, productRepository, orderRepository, new PricingCalculator());
        laptop = Product.create("SKU-1", "Laptop", "13-inch", Money.of("999.90"), 10);
        mouse = Product.create("SKU-2", "Mouse", "wireless", Money.of("19.99"), 5);
        cart = Cart.createFor(customerId);
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void givenFilledCart_whenCheckout_thenOrderPlacedStockReducedCartCleared() {
        cart.addItem(laptop.id(), laptop.name(), laptop.price(), 2);
        cart.addItem(mouse.id(), mouse.name(), mouse.price(), 3);
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));
        when(productRepository.findById(mouse.id())).thenReturn(Optional.of(mouse));

        Order order = checkoutService.checkout(customerId);

        assertThat(order.total()).isEqualTo(Money.of("2059.77"));
        assertThat(order.lines()).hasSize(2);
        assertThat(laptop.stockQuantity()).isEqualTo(8);
        assertThat(mouse.stockQuantity()).isEqualTo(2);
        assertThat(cart.items()).isEmpty();
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(cart);
    }

    @Test
    void givenNoCart_whenCheckout_thenNotFound() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(customerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void givenEmptyCart_whenCheckout_thenEmptyCartException() {
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> checkoutService.checkout(customerId))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void givenShortStock_whenCheckout_thenNothingPersistedAndNoStockTouched() {
        cart.addItem(laptop.id(), laptop.name(), laptop.price(), 2);
        cart.addItem(mouse.id(), mouse.name(), mouse.price(), 6); // only 5 in stock
        when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(laptop.id())).thenReturn(Optional.of(laptop));
        when(productRepository.findById(mouse.id())).thenReturn(Optional.of(mouse));

        assertThatThrownBy(() -> checkoutService.checkout(customerId))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(laptop.stockQuantity()).isEqualTo(10);
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }
}
```

`OrderServiceTest.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.order.OrderStatus;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order newOrder() {
        OrderLine line = new OrderLine(ProductId.newId(), "Laptop",
                Money.of("999.90"), 1, Money.of("999.90"));
        return Order.place(CustomerId.newId(), List.of(line), Money.of("999.90"));
    }

    @Test
    void givenNewOrder_whenPay_thenPaidAndSaved() {
        Order order = newOrder();
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order paid = orderService.pay(order.id());

        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void givenUnknownOrder_whenPay_thenNotFound() {
        OrderId id = OrderId.newId();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.pay(id))
                .isInstanceOf(NotFoundException.class);
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./mvnw -q test -Dtest="CheckoutServiceTest,OrderServiceTest"
```

Expected: compilation error — services do not exist.

- [ ] **Step 3: Implement**

`CheckoutService.java` (verify-then-apply: check ALL stock before decreasing any — the "nothing persisted on failure" test enforces this):

```java
package com.demo.store.application;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartItem;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.product.InsufficientStockException;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PricingCalculator pricingCalculator;

    public CheckoutService(CartRepository cartRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           PricingCalculator pricingCalculator) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.pricingCalculator = pricingCalculator;
    }

    public Order checkout(CustomerId customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Cart for customer", customerId.value()));
        if (cart.items().isEmpty()) {
            throw new EmptyCartException(customerId.value());
        }

        // Pass 1: load and verify ALL stock before touching anything.
        Map<ProductId, Product> products = new HashMap<>();
        for (CartItem item : cart.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new NotFoundException("Product", item.productId().value()));
            if (!product.canFulfill(item.quantity())) {
                throw new InsufficientStockException(
                        product.id(), item.quantity(), product.stockQuantity());
            }
            products.put(product.id(), product);
        }

        // Pass 2: apply — decrease stock, build order lines from cart snapshots.
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : cart.items()) {
            Product product = products.get(item.productId());
            product.decreaseStock(item.quantity());
            productRepository.save(product);
            lines.add(new OrderLine(item.productId(), item.name(), item.unitPrice(),
                    item.quantity(), pricingCalculator.lineTotal(item.unitPrice(), item.quantity())));
        }

        Order order = orderRepository.save(
                Order.place(customerId, lines, pricingCalculator.orderTotal(lines)));

        cart.clear();
        cartRepository.save(cart);
        return order;
    }
}
```

`OrderService.java`:

```java
package com.demo.store.application;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrder(OrderId id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id.value()));
    }

    public List<Order> listByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order pay(OrderId id) {
        Order order = getOrder(id);
        order.markPaid();
        return orderRepository.save(order);
    }

    public Order ship(OrderId id) {
        Order order = getOrder(id);
        order.markShipped();
        return orderRepository.save(order);
    }

    public Order deliver(OrderId id) {
        Order order = getOrder(id);
        order.markDelivered();
        return orderRepository.save(order);
    }
}
```

- [ ] **Step 4: Run to verify pass — then run the whole suite**

```bash
./mvnw -q test
```

Expected: all tests green (`MoneyTest`, `EmailTest`, `AddressTest`, `ProductTest`, `CustomerTest`, `CartTest`, `OrderTest`, `PricingCalculatorTest`, `CatalogServiceTest`, `CustomerServiceTest`, `CartServiceTest`, `CheckoutServiceTest`, `OrderServiceTest`) → `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/application src/test/java/com/demo/store/application
git commit -m "feat(application): checkout (verify-then-apply) and order lifecycle services"
```

---

### Task 12: Mongo infrastructure — documents, mappers, adapters

**Files:**
- Create: `src/main/java/com/demo/store/infrastructure/mongo/ProductDocument.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/CustomerDocument.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/CartDocument.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/OrderDocument.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/SpringDataProductRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/SpringDataCustomerRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/SpringDataCartRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/SpringDataOrderRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/MongoProductRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/MongoCustomerRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/MongoCartRepository.java`
- Create: `src/main/java/com/demo/store/infrastructure/mongo/MongoOrderRepository.java`

No tests (unit tests only per spec; adapters would need Mongo). Verification is compilation here and the end-to-end run in Task 15. Documents are plain classes with getters/setters (Spring Data mapping); `Money` amounts are stored as `Decimal128` via `@Field(targetType = FieldType.DECIMAL128)` plus a currency code string, converted in the adapters.

- [ ] **Step 1: Write the four documents**

`ProductDocument.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("products")
public class ProductDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    private String name;
    private String description;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal priceAmount;

    private String priceCurrency;
    private int stockQuantity;
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public void setPriceAmount(BigDecimal priceAmount) { this.priceAmount = priceAmount; }
    public String getPriceCurrency() { return priceCurrency; }
    public void setPriceCurrency(String priceCurrency) { this.priceCurrency = priceCurrency; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

`CustomerDocument.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("customers")
public class CustomerDocument {

    @Id
    private String id;
    private String name;
    private String email;
    private String street;
    private String city;
    private String zip;
    private String country;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
```

`CartDocument.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document("carts")
public class CartDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String customerId;

    private List<Item> items = new ArrayList<>();

    public static class Item {
        private String productId;
        private String name;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal unitPriceAmount;

        private String unitPriceCurrency;
        private int quantity;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getUnitPriceAmount() { return unitPriceAmount; }
        public void setUnitPriceAmount(BigDecimal unitPriceAmount) { this.unitPriceAmount = unitPriceAmount; }
        public String getUnitPriceCurrency() { return unitPriceCurrency; }
        public void setUnitPriceCurrency(String unitPriceCurrency) { this.unitPriceCurrency = unitPriceCurrency; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
```

`OrderDocument.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("orders")
public class OrderDocument {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private List<Line> lines = new ArrayList<>();

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    private String totalCurrency;
    private String status;
    private Instant placedAt;

    public static class Line {
        private String productId;
        private String name;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal unitPriceAmount;

        private String unitPriceCurrency;
        private int quantity;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal lineTotalAmount;

        private String lineTotalCurrency;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getUnitPriceAmount() { return unitPriceAmount; }
        public void setUnitPriceAmount(BigDecimal unitPriceAmount) { this.unitPriceAmount = unitPriceAmount; }
        public String getUnitPriceCurrency() { return unitPriceCurrency; }
        public void setUnitPriceCurrency(String unitPriceCurrency) { this.unitPriceCurrency = unitPriceCurrency; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getLineTotalAmount() { return lineTotalAmount; }
        public void setLineTotalAmount(BigDecimal lineTotalAmount) { this.lineTotalAmount = lineTotalAmount; }
        public String getLineTotalCurrency() { return lineTotalCurrency; }
        public void setLineTotalCurrency(String lineTotalCurrency) { this.lineTotalCurrency = lineTotalCurrency; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getTotalCurrency() { return totalCurrency; }
    public void setTotalCurrency(String totalCurrency) { this.totalCurrency = totalCurrency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }
}
```

- [ ] **Step 2: Write the Spring Data interfaces**

`SpringDataProductRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SpringDataProductRepository extends MongoRepository<ProductDocument, String> {

    List<ProductDocument> findByActiveTrue();
}
```

`SpringDataCustomerRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataCustomerRepository extends MongoRepository<CustomerDocument, String> {
}
```

`SpringDataCartRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpringDataCartRepository extends MongoRepository<CartDocument, String> {

    Optional<CartDocument> findByCustomerId(String customerId);
}
```

`SpringDataOrderRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SpringDataOrderRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findByCustomerId(String customerId);
}
```

- [ ] **Step 3: Write the adapters (port implementations with inline mapping)**

`MongoProductRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
public class MongoProductRepository implements ProductRepository {

    private final SpringDataProductRepository springData;

    public MongoProductRepository(SpringDataProductRepository springData) {
        this.springData = springData;
    }

    @Override
    public Product save(Product product) {
        springData.save(toDocument(product));
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return springData.findById(id.value()).map(MongoProductRepository::toDomain);
    }

    @Override
    public List<Product> findAllActive() {
        return springData.findByActiveTrue().stream()
                .map(MongoProductRepository::toDomain)
                .toList();
    }

    static ProductDocument toDocument(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.id().value());
        doc.setSku(product.sku());
        doc.setName(product.name());
        doc.setDescription(product.description());
        doc.setPriceAmount(product.price().amount());
        doc.setPriceCurrency(product.price().currency().getCurrencyCode());
        doc.setStockQuantity(product.stockQuantity());
        doc.setActive(product.active());
        return doc;
    }

    static Product toDomain(ProductDocument doc) {
        return new Product(
                new ProductId(doc.getId()),
                doc.getSku(),
                doc.getName(),
                doc.getDescription(),
                new Money(doc.getPriceAmount(), Currency.getInstance(doc.getPriceCurrency())),
                doc.getStockQuantity(),
                doc.isActive());
    }
}
```

`MongoCustomerRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository springData;

    public MongoCustomerRepository(SpringDataCustomerRepository springData) {
        this.springData = springData;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerDocument doc = new CustomerDocument();
        doc.setId(customer.id().value());
        doc.setName(customer.name());
        doc.setEmail(customer.email().value());
        doc.setStreet(customer.shippingAddress().street());
        doc.setCity(customer.shippingAddress().city());
        doc.setZip(customer.shippingAddress().zip());
        doc.setCountry(customer.shippingAddress().country());
        springData.save(doc);
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return springData.findById(id.value()).map(doc -> new Customer(
                new CustomerId(doc.getId()),
                doc.getName(),
                new Email(doc.getEmail()),
                new Address(doc.getStreet(), doc.getCity(), doc.getZip(), doc.getCountry())));
    }
}
```

`MongoCartRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartId;
import com.demo.store.domain.cart.CartItem;
import com.demo.store.domain.cart.CartRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
public class MongoCartRepository implements CartRepository {

    private final SpringDataCartRepository springData;

    public MongoCartRepository(SpringDataCartRepository springData) {
        this.springData = springData;
    }

    @Override
    public Cart save(Cart cart) {
        CartDocument doc = new CartDocument();
        doc.setId(cart.id().value());
        doc.setCustomerId(cart.customerId().value());
        doc.setItems(cart.items().stream().map(item -> {
            CartDocument.Item i = new CartDocument.Item();
            i.setProductId(item.productId().value());
            i.setName(item.name());
            i.setUnitPriceAmount(item.unitPrice().amount());
            i.setUnitPriceCurrency(item.unitPrice().currency().getCurrencyCode());
            i.setQuantity(item.quantity());
            return i;
        }).toList());
        springData.save(doc);
        return cart;
    }

    @Override
    public Optional<Cart> findByCustomerId(CustomerId customerId) {
        return springData.findByCustomerId(customerId.value()).map(doc -> {
            List<CartItem> items = doc.getItems().stream().map(i -> new CartItem(
                    new ProductId(i.getProductId()),
                    i.getName(),
                    new Money(i.getUnitPriceAmount(), Currency.getInstance(i.getUnitPriceCurrency())),
                    i.getQuantity())).toList();
            return new Cart(new CartId(doc.getId()), new CustomerId(doc.getCustomerId()), items);
        });
    }
}
```

`MongoOrderRepository.java`:

```java
package com.demo.store.infrastructure.mongo;

import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderId;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.order.OrderStatus;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
public class MongoOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springData;

    public MongoOrderRepository(SpringDataOrderRepository springData) {
        this.springData = springData;
    }

    @Override
    public Order save(Order order) {
        OrderDocument doc = new OrderDocument();
        doc.setId(order.id().value());
        doc.setCustomerId(order.customerId().value());
        doc.setLines(order.lines().stream().map(line -> {
            OrderDocument.Line l = new OrderDocument.Line();
            l.setProductId(line.productId().value());
            l.setName(line.name());
            l.setUnitPriceAmount(line.unitPrice().amount());
            l.setUnitPriceCurrency(line.unitPrice().currency().getCurrencyCode());
            l.setQuantity(line.quantity());
            l.setLineTotalAmount(line.lineTotal().amount());
            l.setLineTotalCurrency(line.lineTotal().currency().getCurrencyCode());
            return l;
        }).toList());
        doc.setTotalAmount(order.total().amount());
        doc.setTotalCurrency(order.total().currency().getCurrencyCode());
        doc.setStatus(order.status().name());
        doc.setPlacedAt(order.placedAt());
        springData.save(doc);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return springData.findById(id.value()).map(MongoOrderRepository::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return springData.findByCustomerId(customerId.value()).stream()
                .map(MongoOrderRepository::toDomain)
                .toList();
    }

    static Order toDomain(OrderDocument doc) {
        List<OrderLine> lines = doc.getLines().stream().map(l -> new OrderLine(
                new ProductId(l.getProductId()),
                l.getName(),
                new Money(l.getUnitPriceAmount(), Currency.getInstance(l.getUnitPriceCurrency())),
                l.getQuantity(),
                new Money(l.getLineTotalAmount(), Currency.getInstance(l.getLineTotalCurrency()))))
                .toList();
        return new Order(
                new OrderId(doc.getId()),
                new CustomerId(doc.getCustomerId()),
                lines,
                new Money(doc.getTotalAmount(), Currency.getInstance(doc.getTotalCurrency())),
                OrderStatus.valueOf(doc.getStatus()),
                doc.getPlacedAt());
    }
}
```

- [ ] **Step 4: Verify compilation and that the suite still passes**

```bash
./mvnw -q test
```

Expected: `BUILD SUCCESS` (same test count as Task 11 — no new tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/infrastructure
git commit -m "feat(infrastructure): Mongo documents, Spring Data repos, and port adapters"
```

---
### Task 13: Demo data seeder

**Files:**
- Create: `src/main/java/com/demo/store/infrastructure/seed/DemoDataSeeder.java`

- [ ] **Step 1: Write the seeder** (idempotent: only seeds empty collections; uses the Spring Data repos directly — that's fine *inside* infrastructure)

```java
package com.demo.store.infrastructure.seed;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import com.demo.store.domain.shared.Money;
import com.demo.store.infrastructure.mongo.SpringDataCustomerRepository;
import com.demo.store.infrastructure.mongo.SpringDataProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final SpringDataProductRepository productDocuments;
    private final SpringDataCustomerRepository customerDocuments;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public DemoDataSeeder(SpringDataProductRepository productDocuments,
                          SpringDataCustomerRepository customerDocuments,
                          ProductRepository productRepository,
                          CustomerRepository customerRepository) {
        this.productDocuments = productDocuments;
        this.customerDocuments = customerDocuments;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) {
        if (productDocuments.count() == 0) {
            seedProducts();
            log.info("Seeded demo products");
        }
        if (customerDocuments.count() == 0) {
            seedCustomers();
            log.info("Seeded demo customers");
        }
    }

    private void seedProducts() {
        List<Product> products = List.of(
                Product.create("LAP-13", "Aurora 13 Laptop", "13-inch ultrabook, 16 GB RAM", Money.of("999.90"), 25),
                Product.create("LAP-15", "Aurora 15 Laptop", "15-inch workstation, 32 GB RAM", Money.of("1499.00"), 12),
                Product.create("MON-27", "PixelView 27 Monitor", "27-inch 4K IPS monitor", Money.of("329.99"), 40),
                Product.create("MOU-01", "GlidePro Mouse", "Wireless ergonomic mouse", Money.of("19.99"), 200),
                Product.create("KEY-75", "TactiType 75 Keyboard", "75% mechanical keyboard", Money.of("89.50"), 80),
                Product.create("HUB-07", "PortMax 7-in-1 Hub", "USB-C hub: HDMI, 3xUSB-A, SD", Money.of("45.00"), 150),
                Product.create("HDS-90", "SilentFocus Headset", "ANC over-ear headset", Money.of("129.99"), 60),
                Product.create("CAM-4K", "ClearCast 4K Webcam", "4K webcam with privacy shutter", Money.of("79.90"), 90),
                Product.create("SSD-1T", "SwiftDrive 1TB SSD", "NVMe M.2 SSD, 1 TB", Money.of("99.00"), 120),
                Product.create("DCK-02", "DeskDock Stand", "Aluminium laptop stand", Money.of("39.99"), 75),
                Product.create("CBL-2M", "DuraLink USB-C Cable 2m", "100W braided USB-C cable", Money.of("12.50"), 300),
                Product.create("BAG-15", "CommuterShell Backpack", "15-inch laptop backpack", Money.of("59.00"), 45));
        products.forEach(productRepository::save);
    }

    private void seedCustomers() {
        customerRepository.save(Customer.register("Ana Petrovic",
                new Email("ana.petrovic@example.com"),
                new Address("Kneza Milosa 12", "Belgrade", "11000", "RS")));
        customerRepository.save(Customer.register("Marko Ilic",
                new Email("marko.ilic@example.com"),
                new Address("Bulevar Oslobodjenja 45", "Novi Sad", "21000", "RS")));
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw -q test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/demo/store/infrastructure/seed
git commit -m "feat(infrastructure): idempotent demo data seeder (12 products, 2 customers)"
```

---

### Task 14: REST API layer — DTOs, controllers, exception handler

**Files:**
- Create: `src/main/java/com/demo/store/api/dto/ProductDtos.java`
- Create: `src/main/java/com/demo/store/api/dto/CustomerDtos.java`
- Create: `src/main/java/com/demo/store/api/dto/CartDtos.java`
- Create: `src/main/java/com/demo/store/api/dto/OrderDtos.java`
- Create: `src/main/java/com/demo/store/api/ProductController.java`
- Create: `src/main/java/com/demo/store/api/CustomerController.java`
- Create: `src/main/java/com/demo/store/api/CartController.java`
- Create: `src/main/java/com/demo/store/api/OrderController.java`
- Create: `src/main/java/com/demo/store/api/GlobalExceptionHandler.java`

No tests (thin layer per spec; exercised end to end in Task 15). Note the cart
response deliberately exposes `total` as a `double` via the legacy
`Cart.getTotal()` — that is the spec's seeded flaw, do not "improve" it.

- [ ] **Step 1: Write the DTO files** (records grouped per resource; requests carry validation)

`ProductDtos.java`:

```java
package com.demo.store.api.dto;

import com.demo.store.domain.product.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int initialStock) {
    }

    public record ProductResponse(String id, String sku, String name, String description,
                                  BigDecimal price, String currency, int stockQuantity,
                                  boolean active) {

        public static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.id().value(),
                    product.sku(),
                    product.name(),
                    product.description(),
                    product.price().amount(),
                    product.price().currency().getCurrencyCode(),
                    product.stockQuantity(),
                    product.active());
        }
    }
}
```

`CustomerDtos.java`:

```java
package com.demo.store.api.dto;

import com.demo.store.domain.customer.Customer;
import jakarta.validation.constraints.NotBlank;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record RegisterCustomerRequest(
            @NotBlank String name,
            @NotBlank String email,
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String zip,
            @NotBlank String country) {
    }

    public record CustomerResponse(String id, String name, String email,
                                   String street, String city, String zip, String country) {

        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(
                    customer.id().value(),
                    customer.name(),
                    customer.email().value(),
                    customer.shippingAddress().street(),
                    customer.shippingAddress().city(),
                    customer.shippingAddress().zip(),
                    customer.shippingAddress().country());
        }
    }
}
```

`CartDtos.java`:

```java
package com.demo.store.api.dto;

import com.demo.store.domain.cart.Cart;
import com.demo.store.domain.cart.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotBlank String productId, @Min(1) int quantity) {
    }

    public record ChangeQuantityRequest(@Min(1) int quantity) {
    }

    public record CartItemResponse(String productId, String name, BigDecimal unitPrice,
                                   String currency, int quantity) {

        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(
                    item.productId().value(),
                    item.name(),
                    item.unitPrice().amount(),
                    item.unitPrice().currency().getCurrencyCode(),
                    item.quantity());
        }
    }

    // legacy: total is a double via Cart.getTotal() — kept for old integrations
    public record CartResponse(String id, String customerId,
                               List<CartItemResponse> items, double total) {

        public static CartResponse from(Cart cart) {
            return new CartResponse(
                    cart.id().value(),
                    cart.customerId().value(),
                    cart.items().stream().map(CartItemResponse::from).toList(),
                    cart.getTotal());
        }
    }
}
```

`OrderDtos.java`:

```java
package com.demo.store.api.dto;

import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderLineResponse(String productId, String name, BigDecimal unitPrice,
                                    int quantity, BigDecimal lineTotal, String currency) {

        public static OrderLineResponse from(OrderLine line) {
            return new OrderLineResponse(
                    line.productId().value(),
                    line.name(),
                    line.unitPrice().amount(),
                    line.quantity(),
                    line.lineTotal().amount(),
                    line.lineTotal().currency().getCurrencyCode());
        }
    }

    public record OrderResponse(String id, String customerId, List<OrderLineResponse> lines,
                                BigDecimal total, String currency, String status,
                                Instant placedAt) {

        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.id().value(),
                    order.customerId().value(),
                    order.lines().stream().map(OrderLineResponse::from).toList(),
                    order.total().amount(),
                    order.total().currency().getCurrencyCode(),
                    order.status().name(),
                    order.placedAt());
        }
    }
}
```

- [ ] **Step 2: Write the controllers**

`ProductController.java`:

```java
package com.demo.store.api;

import com.demo.store.api.dto.ProductDtos.CreateProductRequest;
import com.demo.store.api.dto.ProductDtos.ProductResponse;
import com.demo.store.application.CatalogService;
import com.demo.store.domain.product.ProductId;
import com.demo.store.domain.shared.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(catalogService.createProduct(
                request.sku(), request.name(), request.description(),
                Money.of(request.price()), request.initialStock()));
    }

    @GetMapping
    public List<ProductResponse> list() {
        return catalogService.listActiveProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return ProductResponse.from(catalogService.getProduct(new ProductId(id)));
    }
}
```

`CustomerController.java`:

```java
package com.demo.store.api;

import com.demo.store.api.dto.CustomerDtos.CustomerResponse;
import com.demo.store.api.dto.CustomerDtos.RegisterCustomerRequest;
import com.demo.store.application.CustomerService;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.shared.Address;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody RegisterCustomerRequest request) {
        return CustomerResponse.from(customerService.register(
                request.name(), request.email(),
                new Address(request.street(), request.city(), request.zip(), request.country())));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable String id) {
        return CustomerResponse.from(customerService.getCustomer(new CustomerId(id)));
    }
}
```

`CartController.java`:

```java
package com.demo.store.api;

import com.demo.store.api.dto.CartDtos.AddItemRequest;
import com.demo.store.api.dto.CartDtos.CartResponse;
import com.demo.store.api.dto.CartDtos.ChangeQuantityRequest;
import com.demo.store.application.CartService;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.product.ProductId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts/{customerId}")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@PathVariable String customerId) {
        return CartResponse.from(cartService.getOrCreateCart(new CustomerId(customerId)));
    }

    @PostMapping("/items")
    public CartResponse addItem(@PathVariable String customerId,
                                @Valid @RequestBody AddItemRequest request) {
        return CartResponse.from(cartService.addItem(
                new CustomerId(customerId), new ProductId(request.productId()), request.quantity()));
    }

    @PatchMapping("/items/{productId}")
    public CartResponse changeQuantity(@PathVariable String customerId,
                                       @PathVariable String productId,
                                       @Valid @RequestBody ChangeQuantityRequest request) {
        return CartResponse.from(cartService.changeQuantity(
                new CustomerId(customerId), new ProductId(productId), request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable String customerId,
                                   @PathVariable String productId) {
        return CartResponse.from(cartService.removeItem(
                new CustomerId(customerId), new ProductId(productId)));
    }
}
```

`OrderController.java`:

```java
package com.demo.store.api;

import com.demo.store.api.dto.OrderDtos.OrderResponse;
import com.demo.store.application.CheckoutService;
import com.demo.store.application.OrderService;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.OrderId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    public OrderController(CheckoutService checkoutService, OrderService orderService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
    }

    @PostMapping("/checkout/{customerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@PathVariable String customerId) {
        return OrderResponse.from(checkoutService.checkout(new CustomerId(customerId)));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.from(orderService.getOrder(new OrderId(id)));
    }

    @GetMapping
    public List<OrderResponse> listByCustomer(@RequestParam String customerId) {
        return orderService.listByCustomer(new CustomerId(customerId)).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable String id) {
        return OrderResponse.from(orderService.pay(new OrderId(id)));
    }

    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable String id) {
        return OrderResponse.from(orderService.ship(new OrderId(id)));
    }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable String id) {
        return OrderResponse.from(orderService.deliver(new OrderId(id)));
    }
}
```

- [ ] **Step 3: Write the exception handler**

`GlobalExceptionHandler.java`:

```java
package com.demo.store.api;

import com.demo.store.application.EmptyCartException;
import com.demo.store.application.NotFoundException;
import com.demo.store.domain.cart.ItemNotInCartException;
import com.demo.store.domain.order.IllegalOrderStateException;
import com.demo.store.domain.product.InsufficientStockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ApiError(Instant timestamp, int status, String error,
                           String message, String path) {
    }

    @ExceptionHandler({NotFoundException.class, ItemNotInCartException.class})
    public ResponseEntity<ApiError> notFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({EmptyCartException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> badRequest(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex,
                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("invalid request body");
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({InsufficientStockException.class, IllegalOrderStateException.class})
    public ResponseEntity<ApiError> conflict(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI()));
    }
}
```

- [ ] **Step 4: Verify compilation and the suite**

```bash
./mvnw -q test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/demo/store/api
git commit -m "feat(api): REST controllers, DTOs, and global exception handler"
```

---

### Task 15: Dockerfile, docker-compose, end-to-end verification

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Create: `docker-compose.yml`

- [ ] **Step 1: Write `.dockerignore`**

```gitignore
target/
.git/
.idea/
docs/
*.iml
```

- [ ] **Step 2: Write `Dockerfile`** (multi-stage, dependency layer cached)

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/target/store-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: Write `docker-compose.yml`**

```yaml
services:
  mongo:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.runCommand('ping').ok"]
      interval: 5s
      timeout: 5s
      retries: 10

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/store
    depends_on:
      mongo:
        condition: service_healthy

volumes:
  mongo-data:
```

- [ ] **Step 4: Boot the system**

```bash
docker compose up --build -d
docker compose ps
```

Expected: both services `running`; app logs show `Seeded demo products` and `Seeded demo customers` (`docker compose logs app | grep Seeded`).

- [ ] **Step 5: Walk the happy path over HTTP** (spec DoD #2 — run each command, check the noted expectation)

```bash
# products seeded (12)
curl -s http://localhost:8080/api/products | python -c "import sys,json;print(len(json.load(sys.stdin)))"
# expected: 12

# capture a product id and create a fresh customer
PRODUCT_ID=$(curl -s http://localhost:8080/api/products | python -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
CUSTOMER_ID=$(curl -s -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","street":"Main St 1","city":"Belgrade","zip":"11000","country":"RS"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['id'])")

# add to cart (expected: items[0].quantity == 2; note "total" is a bare number — the legacy double)
curl -s -X POST http://localhost:8080/api/carts/$CUSTOMER_ID/items \
  -H "Content-Type: application/json" -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}"

# checkout (expected: 201, status NEW, total is a string-exact decimal)
ORDER_ID=$(curl -s -X POST http://localhost:8080/api/orders/checkout/$CUSTOMER_ID \
  | python -c "import sys,json;print(json.load(sys.stdin)['id'])")

# lifecycle (expected status after each: PAID, SHIPPED, DELIVERED)
curl -s -X POST http://localhost:8080/api/orders/$ORDER_ID/pay     | python -c "import sys,json;print(json.load(sys.stdin)['status'])"
curl -s -X POST http://localhost:8080/api/orders/$ORDER_ID/ship    | python -c "import sys,json;print(json.load(sys.stdin)['status'])"
curl -s -X POST http://localhost:8080/api/orders/$ORDER_ID/deliver | python -c "import sys,json;print(json.load(sys.stdin)['status'])"
```

- [ ] **Step 6: Walk the error paths** (spec DoD #3)

```bash
# unknown product -> 404 problem body
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products/nope
# expected: 404

# empty-cart checkout -> 400 (customer's cart was cleared by the checkout above)
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/orders/checkout/$CUSTOMER_ID
# expected: 400

# illegal transition -> 409 (order is DELIVERED; pay again)
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/orders/$ORDER_ID/pay
# expected: 409

# overdrawn stock -> 409 (quantity 9999)
curl -s -X POST http://localhost:8080/api/carts/$CUSTOMER_ID/items \
  -H "Content-Type: application/json" -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":9999}" > /dev/null
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/orders/checkout/$CUSTOMER_ID
# expected: 409
```

- [ ] **Step 7: Shut down and commit**

```bash
docker compose down
git add Dockerfile .dockerignore docker-compose.yml
git commit -m "feat: dockerize app and add docker-compose with MongoDB"
```

---

### Task 16: README + video-prep material + final check

**Files:**
- Create: `README.md`
- Create: `docs/video-prep/README.md`
- Create: `docs/video-prep/ticket-FEAT-101-discount-codes.md`
- Create: `docs/video-prep/ticket-CR-142-order-cancellation.md`

- [ ] **Step 1: Write the root `README.md`** (normal project readme — NO video references)

```markdown
# Demo Store

A small e-commerce store backend: products, customers, carts, and orders.
Java 21 · Spring Boot 3 · MongoDB. Clean architecture (api → application →
domain, with infrastructure implementing the domain's repository ports) and
pragmatic DDD.

## Run it

```bash
docker compose up --build
```

App: http://localhost:8080 · MongoDB: localhost:27017 (both seeded with demo
data on first start).

For local development: start only Mongo (`docker compose up mongo -d`) and run
the app with `./mvnw spring-boot:run`. Unit tests: `./mvnw test` (no Docker
needed).

## API

| Resource | Endpoints |
|---|---|
| Products | `POST /api/products` · `GET /api/products` · `GET /api/products/{id}` |
| Customers | `POST /api/customers` · `GET /api/customers/{id}` |
| Cart | `GET /api/carts/{customerId}` · `POST /api/carts/{customerId}/items` · `PATCH /api/carts/{customerId}/items/{productId}` · `DELETE /api/carts/{customerId}/items/{productId}` |
| Orders | `POST /api/orders/checkout/{customerId}` · `GET /api/orders/{id}` · `GET /api/orders?customerId=` · `POST /api/orders/{id}/pay` · `/ship` · `/deliver` |

Errors return `{timestamp, status, error, message, path}` with 404/400/409
semantics.
```

- [ ] **Step 2: Write `docs/video-prep/ticket-FEAT-101-discount-codes.md`**

```markdown
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
```

*(Deliberate ambiguity — say nothing about combining multiple codes. The
analyst agent must catch it, on camera.)*

- [ ] **Step 3: Write `docs/video-prep/ticket-CR-142-order-cancellation.md`**

```markdown
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
```

- [ ] **Step 4: Write `docs/video-prep/README.md`** (the seed inventory)

```markdown
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
```

- [ ] **Step 5: Final check against the spec's Definition of Done**

Run the checklist from `docs/video-prep/design.md`:

1. `docker compose up --build` from clean (`docker compose down -v` first) → app healthy, seeded; `./mvnw test` green.
2. Happy path via HTTP (Task 15 step 5) — works.
3. Error paths (Task 15 step 6) — 404/400/409 as expected.
4. `Cart.getTotal()` flaw present and wired into `CartResponse`; both tickets + inventory in `docs/video-prep/`.
5. Layer rule: `grep -rn "org.springframework.data" src/main/java/com/demo/store/domain src/main/java/com/demo/store/application src/main/java/com/demo/store/api` → **no matches**; `grep -rn "com.demo.store.domain" src/main/java/com/demo/store/api/dto` → only imports used for mapping *from* domain (DTOs expose primitives/records only).

- [ ] **Step 6: Commit**

```bash
git add README.md docs/video-prep
git commit -m "docs: project README and video-prep tickets/inventory"
```

---

## Execution notes

- Tasks 2–11 are strict TDD; Tasks 1 and 12–16 are scaffold/infrastructure with
  compile or end-to-end verification instead.
- Task order matters (each layer builds on the previous); within a task, steps
  are sequential.
- **Never** add: a `CANCELLED` order status, discount/promo concepts, `.github/`
  files, or a "fix" for `Cart.getTotal()` — all four are on-camera material
  (see `docs/video-prep/README.md`).
- Prerequisites on the executing machine: JDK 21, Docker Desktop, one local
  Maven 3.9+ (only for Task 1's wrapper generation), curl + python on PATH for
  Task 15's checks.
- Shell: commands are written for bash — on Windows run them in Git Bash
  (`./mvnw`); `mvnw.cmd` exists for plain PowerShell use if preferred.
