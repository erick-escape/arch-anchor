# Bookstore, in PHP

The [bookstore](../bookstore/README.md) again, class for class, in PHP: the same four layers,
the same thirteen classes and the same architectural mistake. It is here to show that Arch
Anchor recovers an architecture from how the code is organised, not from the language it is
written in. Analysed, the two projects give the same reference classes, the same violation and
the same three recommendations, with the same numbers.

Like the Java one it is meant to be read and analysed, not run: there is no database behind the
repositories and no HTTP server in front of the controllers. Analysing it needs nothing
installed, neither PHP nor `composer install`. The code is valid PHP 8.1:

```bash
find src -name '*.php' -exec php -l {} \;
```

## Intended architecture

```
Web  ──▶  Service  ──▶  Persistence  ──▶  ext-pdo
 │           │               │
 └───────────┴───────────────┴──▶  Model  ──▶  ext-date
```

Each layer talks to the one below it, and every layer may use the model. Only `Persistence`
touches the database.

| Module | Classes | Depends on |
|---|---|---|
| `Bookstore/Web` | `CatalogController`, `CustomerController`, `OrderController` | `Bookstore\Service`, `Bookstore\Model` |
| | `SalesReport` | `ext-pdo`, `Bookstore\Model` |
| `Bookstore/Service` | `CatalogService`, `CustomerService`, `OrderService` | `Bookstore\Persistence`, `Bookstore\Model` |
| `Bookstore/Persistence` | `BookRepository`, `CustomerRepository`, `OrderRepository` | `ext-pdo`, `Bookstore\Model` |
| `Bookstore/Model` | `Book`, `Customer`, `Order` | `ext-date` |

Three kinds of name appear in that table:

- A **module** is a directory that holds classes, named after it: `Bookstore/Web` is
  `src/Bookstore/Web`. Which directories hold the project's own classes comes from the
  `autoload` section of [`composer.json`](composer.json).
- A **package** is a namespace, written as PHP writes it: `Bookstore\Web`.
- A class that **PHP itself provides** has no namespace to be filed under, so it is filed under
  what provides it, with the name Composer gives that in a `require` section: `PDO`,
  `PDOStatement` and `PDOException` are `ext-pdo`, and `DateTimeImmutable` is `ext-date`. The
  core, SPL and the standard extension, which no PHP is without, are one package, `php`.

## What is wrong with it

`SalesReport` lives in the `Web` namespace, next to the page that shows it, and finds the
best-selling book by querying the database itself:

```php
namespace Bookstore\Web;

use Bookstore\Model\Book;
use PDO;
use PDOStatement;

// ...

private function bestsellerQuery(): PDOStatement
{
    return (new PDO($this->dsn))->query(self::BESTSELLER);
}
```

A web-layer class has gone around the service layer and straight to PDO. Nothing in the code
marks it; finding it is the tool's job.

## What Arch Anchor reports

Everything in this section is the tool's output for this project, recorded from a run. Nothing
is written by hand. `BookstorePhpRunningExampleTest` holds the tool to it, and to giving every
class the dependencies its Java counterpart has.

### Modules and reference classes

| Module | Reference class | Similarity | Violations |
|---|---|---|---|
| `Bookstore/Model` | `Book` | 100.00 % | 0 |
| `Bookstore/Persistence` | `BookRepository` | 100.00 % | 0 |
| `Bookstore/Service` | `CatalogService` | 100.00 % | 0 |
| `Bookstore/Web` | `CatalogController` | 75.00 % | 1: `ext-pdo` |

Project similarity is 93.75 %.

In `Web` the three controllers each have a similarity of 0.8333 and `SalesReport` one of 0.5:
the controllers resemble each other and `SalesReport` resembles none of them, so
`CatalogController`, the first of the three equals, is elected. The Java bookstore's README
[works the election through](../bookstore/README.md#modules-and-reference-classes); the
arithmetic is the same here.

### Constraints

The reference classes' dependencies become the modules' constraints:

```
AC1 - Bookstore/Model CAN-DEPEND ext-date
AC2 - Bookstore/Persistence CAN-DEPEND Bookstore\Model
AC3 - Bookstore/Persistence CAN-DEPEND ext-pdo
AC4 - Bookstore/Service CAN-DEPEND Bookstore\Model
AC5 - Bookstore/Service CAN-DEPEND Bookstore\Persistence
AC6 - Bookstore/Web CAN-DEPEND Bookstore\Model
AC7 - Bookstore/Web CAN-DEPEND Bookstore\Service
```

### The violation

`Web` may depend on `Bookstore\Model` and `Bookstore\Service`. `SalesReport` also depends on
`ext-pdo`, which is the one violation in the project:

| | |
|---|---|
| Class | `SalesReport` in `Bookstore/Web` |
| Forbidden package | `ext-pdo` |
| Types used | `PDO`, `PDOStatement` |
| Suggested target | `Bookstore/Persistence` |
| Similarity improvement | +25.0 % |
| New violations in the target | 0 |

### Recommendations

| Recommendation | Rate | Priority | Similarity | Violations |
|---|---|---|---|---|
| Split `Bookstore/Web` into the three controllers and `SalesReport` | 0.625 | High | 75.00 % → 100.00 % | 1 → 0 |
| Move `SalesReport` from `Bookstore/Web` to `Bookstore/Persistence` | 0.5625 | High | +12.5 % across the two modules | 1 → 0 |
| Merge `Bookstore/Persistence` and `Bookstore/Web` | 0.4196 | Medium | −16.1 % | 1 → 0 |
| Fix violation in `SalesReport` (Violations tab) | | | | |

These are the Java bookstore's rates to the last digit, for the reasons its README gives.

### After applying the move

Apply **Move "SalesReport"** from the recommendations panel:

| Module | Reference class | Classes | Similarity | Violations |
|---|---|---|---|---|
| `Bookstore/Model` | `Book` | 3 | 100.00 % | 0 |
| `Bookstore/Service` | `CatalogService` | 3 | 100.00 % | 0 |
| `Bookstore/Web` | `CatalogController` | 3 | 100.00 % | 0 |
| `Bookstore/Persistence` | `BookRepository` | 4, now with `SalesReport` | 100.00 % | 0 |

The recommendations panel is empty.

Switching `BookRepository` to MUST and exporting then gives:

```
AC1 - Bookstore/Model CAN-DEPEND ext-date
AC2 - Bookstore/Service CAN-DEPEND Bookstore\Model
AC3 - Bookstore/Service CAN-DEPEND Bookstore\Persistence
AC4 - Bookstore/Web CAN-DEPEND Bookstore\Model
AC5 - Bookstore/Web CAN-DEPEND Bookstore\Service
AC6 - Bookstore/Persistence MUST-DEPEND Bookstore\Model
AC7 - Bookstore/Persistence MUST-DEPEND ext-pdo
```

## Try it

1. Start the tool (see [Run it](../../README.md#run-it) in the root README).
2. On the home page, paste `https://github.com/erick-escape/arch-anchor`, open **Options**, set
   **Subdirectory** to `examples/bookstore-php` and **Project name** to `bookstore-php`, and click
   **Analyze**. From a clone, **Upload folder** takes this directory instead.
3. The analysis opens: four modules, each with the reference class and similarity listed above.
4. Open `Bookstore/Web` in the modules panel to see its classes. Among its dependencies,
   `ext-pdo` is dashed in red: the violation.
5. In the recommendations panel's **Violations** tab, open the details of **Fix violation in
   "SalesReport"**; then, under **Refactorings**, apply **Move "SalesReport"** and confirm.
6. Open `Bookstore/Persistence`, switch `BookRepository` to **MUST**, and choose **Export ACs**
   in the top bar.

## Why it looks the way it does

The [three properties](../bookstore/README.md#why-it-looks-the-way-it-does) that shaped the Java
bookstore shaped this one too. Keeping the two alike added three more, each of which says
something about how a PHP project is read.

- **A dependency is a class the file names.** In PHP a class is named in an import, a type, an
  attribute, `new`, `instanceof`, `catch`, or before `::`. An object that is only passed along
  or called is not named, and nothing is inferred about it. `$book->publishedOn()->format(…)` in
  `BookRepository` handles a `DateTimeImmutable` without depending on `ext-date`, as the Java
  repository handles a `LocalDate` without importing `java.time`.
- **Dates are read from text in the model, not in the repositories.** A repository that turned
  a column into a date itself, with `new DateTimeImmutable($row['published_on'])`, would depend
  on `ext-date`, which its Java counterpart does not on `java.time`. `Book::withPublicationDate`
  and its two siblings take the text instead.
- **No class of the package `php` is named.** `Exception`, `Countable`, `ArrayObject` and the
  like are filed under `php`, and a module that names one depends on it. The repositories
  catch `PDOException`, which is `ext-pdo`, and nothing wider.

A controller names the model class it handles for the first reason: `CatalogController` has
`describe(Book $book)`, and `OrderController` narrows what the services return with
`instanceof`. Written without them, the controllers would still work, and would not depend on
`Bookstore\Model`.
