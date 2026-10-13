# Bookstore

A small online bookstore, written to be analysed by Arch Anchor. It has four layers, thirteen
classes and one architectural mistake, and it depends on nothing but the JDK.

It is meant to be read and analysed, not run: there is no JDBC driver on the classpath and no
HTTP server in front of the controllers. It does compile:

```bash
mvn clean compile
```

## Intended architecture

```
web  ──▶  service  ──▶  persistence  ──▶  java.sql
 │           │               │
 └───────────┴───────────────┴──▶  model  ──▶  java.time
```

Each layer talks to the one below it, and every layer may use the model. Only `persistence`
touches the database.

| Module | Classes | Depends on |
|---|---|---|
| `bookstore/web` | `CatalogController`, `CustomerController`, `OrderController` | `bookstore.service`, `bookstore.model` |
| | `SalesReport` | `java.sql`, `bookstore.model` |
| `bookstore/service` | `CatalogService`, `CustomerService`, `OrderService` | `bookstore.persistence`, `bookstore.model` |
| `bookstore/persistence` | `BookRepository`, `CustomerRepository`, `OrderRepository` | `java.sql`, `bookstore.model` |
| `bookstore/model` | `Book`, `Customer`, `Order` | `java.time` |

## What is wrong with it

`SalesReport` lives in the `web` package, next to the page that shows it, and finds the
best-selling book by querying the database itself:

```java
package bookstore.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
// ...

public Optional<Book> bestseller() throws SQLException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl);
            Statement query = connection.createStatement();
            ResultSet row = query.executeQuery(BESTSELLER)) {
```

A web-layer class has gone around the service layer and straight to JDBC. Nothing in the code
marks it; finding it is the tool's job.

## What Arch Anchor reports

Everything in this section is the tool's output for this project, recorded from a run. Nothing
is written by hand.

### Modules and reference classes

| Module | Reference class | Similarity | Violations |
|---|---|---|---|
| `bookstore/model` | `Book` | 100.00 % | 0 |
| `bookstore/persistence` | `BookRepository` | 100.00 % | 0 |
| `bookstore/service` | `CatalogService` | 100.00 % | 0 |
| `bookstore/web` | `CatalogController` | 75.00 % | 1: `java.sql` |

Project similarity is 93.75 %.

The classes of `model`, `persistence` and `service` have identical dependency sets, so within
each of those modules every class rates the same and the first one by file name is elected.

In `web` the election has something to decide:

| Class | Similarity | Violations it would leave as reference class | Rate |
|---|---|---|---|
| `CatalogController` | 0.8333 | 1 | **0.5** |
| `CustomerController` | 0.8333 | 1 | 0.5 |
| `OrderController` | 0.8333 | 1 | 0.5 |
| `SalesReport` | 0.5 | 1 | 0.3 |

Each candidate would leave one of the module's dependencies uncovered (`java.sql` for a
controller, `bookstore.service` for `SalesReport`), so similarity settles it: the three
controllers resemble each other and `SalesReport` resembles none of them. `CatalogController`
is the first of the three equals.

### Constraints

The reference classes' dependencies become the modules' constraints:

```
AC1 - bookstore/model CAN-DEPEND java.time
AC2 - bookstore/persistence CAN-DEPEND bookstore.model
AC3 - bookstore/persistence CAN-DEPEND java.sql
AC4 - bookstore/service CAN-DEPEND bookstore.model
AC5 - bookstore/service CAN-DEPEND bookstore.persistence
AC6 - bookstore/web CAN-DEPEND bookstore.model
AC7 - bookstore/web CAN-DEPEND bookstore.service
```

### The violation

`web` may depend on `bookstore.model` and `bookstore.service`. `SalesReport` also depends on
`java.sql`, which is the one violation in the project:

| | |
|---|---|
| Class | `SalesReport` in `bookstore/web` |
| Forbidden package | `java.sql` |
| Types used | `Connection`, `DriverManager`, `ResultSet`, `SQLException`, `Statement` |
| Suggested target | `bookstore/persistence` |
| Similarity improvement | +25.0 % |
| New violations in the target | 0 |

### Recommendations

Three structural changes would remove the violation. The tool rates each as
`0.5 × Δ similarity + 0.5 × Δ violations` and lists them in that order, followed by the
violation itself:

| Recommendation | Rate | Priority | Similarity | Violations |
|---|---|---|---|---|
| Split `bookstore/web` into the three controllers and `SalesReport` | 0.625 | High | 75.00 % → 100.00 % | 1 → 0 |
| Move `SalesReport` from `bookstore/web` to `bookstore/persistence` | 0.5625 | High | +12.5 % across the two modules | 1 → 0 |
| Merge `bookstore/persistence` and `bookstore/web` | 0.4196 | Medium | −16.1 % | 1 → 0 |
| Fix violation in `SalesReport` | | Medium | | |

The move is what an architect would do: a class that only reads the database belongs with the
repositories. The split is the same idea with a new module for reporting. The merge also makes
the violation disappear, because `persistence` is allowed to use `java.sql`, but it costs
cohesion, and it is rated last for that reason.

### After applying the move

Apply **Move "SalesReport"** from the recommendations panel:

| Module | Reference class | Classes | Similarity | Violations |
|---|---|---|---|---|
| `bookstore/model` | `Book` | 3 | 100.00 % | 0 |
| `bookstore/service` | `CatalogService` | 3 | 100.00 % | 0 |
| `bookstore/web` | `CatalogController` | 3 | 100.00 % | 0 |
| `bookstore/persistence` | `BookRepository` | 4, now with `SalesReport` | 100.00 % | 0 |

The recommendations panel is empty.

Switching `BookRepository` to MUST and exporting then gives:

```
AC1 - bookstore/model CAN-DEPEND java.time
AC2 - bookstore/service CAN-DEPEND bookstore.model
AC3 - bookstore/service CAN-DEPEND bookstore.persistence
AC4 - bookstore/web CAN-DEPEND bookstore.model
AC5 - bookstore/web CAN-DEPEND bookstore.service
AC6 - bookstore/persistence MUST-DEPEND bookstore.model
AC7 - bookstore/persistence MUST-DEPEND java.sql
```

The mode belongs to the reference class, so both of `BookRepository`'s dependencies become
obligations. Here both hold for every class in the module, `SalesReport` included.

## Try it

1. Start the tool (see the [root README](../../README.md#quick-start)).
2. **Add Project**. Either import `https://github.com/erick-escape/arch-anchor` with
   **Subdirectory** `examples/bookstore` and **Project name** `bookstore`, or upload this
   directory from the **Local directory** tab.
3. **Analyze**. Four modules appear, each with the reference class and similarity listed above.
4. Open `bookstore/web` in the sidebar to see its classes, and expand **Dependencies** to find
   `java.sql` among them.
5. In the recommendations panel, open **Fix violation in "SalesReport"**, then apply
   **Move "SalesReport"**.
6. Open `bookstore/persistence`, switch `BookRepository` to **MUST**, and choose **Export ACs**
   from the three-dot menu.

## Why it looks the way it does

Three properties of the analyses shaped this example. They are worth knowing before changing it.

- **`SalesReport` uses the database instead of the service layer, not in addition to it.** A
  class that depends on everything its siblings do *plus* one more package leaves no dependency
  of the module uncovered, so it is elected reference class and the extra package becomes a
  constraint instead of a violation.
- **The clean layers are uniform.** A module of three or more classes whose dependency sets
  differ always gets a split recommendation, because a one-class module counts as fully
  cohesive.
- **The merge cannot be designed away.** Whenever another module's reference class is allowed
  the package that violates here, merging the two modules removes the violation and is
  recommended.
