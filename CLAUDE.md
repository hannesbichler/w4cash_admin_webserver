# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring Boot REST API (Java 21) that backs the **w4cash_admin** Angular UI
(`C:\BTech\angular\w4cash_admin`). It wraps the Oracle database of **w4cash**, a point-of-sale system
forked from Openbravo POS, talking directly to the existing schema (PRODUCTS, PEOPLE, PLACES,
CATEGORIES, TAXES, ...) over plain JDBC — it does not create its own data model.

It is an extraction from `../w4cash_webserver`: only the feature packages the admin UI calls were
carried over. Deliberately **not** here (they belong to the POS/tablet client, not the admin UI):
`ticketinfo` order handling, `payment`, `chat`, `auth` (OTP), `settings`, `receipt` (ActiveCash).
Unlike the source project this is a **single Maven module** — no `rest/` sub-module, so plain
`./mvnw test` rather than `./mvnw -pl rest test`.

## Build & run

```
./mvnw compile -q            # compile
./mvnw spring-boot:run       # run the server (use https://w4cash.bichler.tech:3000, see application.properties)
```

Run/debug via VS Code is configured (`.vscode/launch.json`, main class `w4cash.W4cashApplication`).

The Angular UI proxies `/api/*` to `https://w4cash.bichler.tech:3000` (`proxy.conf.json` in the UI project).
The backend accepts both bare routes like `/products` and `/api`-prefixed routes like
`/api/products`.

HTTPS is enabled with the repo-root PEM files `w4cash.bichler.tech.pem` and `w4cash.bichler.tech-key.pem` via
`server.ssl.certificate` and `server.ssl.certificate-private-key`.
The certificate SAN is `w4cash.bichler.tech`, so callers should resolve `w4cash.bichler.tech` to `217.154.223.125` if DNS is not already configured.

## Tests

```
./mvnw test                                   # full suite
./mvnw test -Dtest=ProductsControllerTest     # single test class
```

Controller tests use `@WebMvcTest` + `@MockBean` on the repository (see `ProductsControllerTest`) — they
do not hit a real database. Several negative-path tests deliberately trigger the controllers' error
logging, so `ERROR` lines in a green run are expected. Repository tests that exist
(e.g. `ProductsRepositoryTest`, `AttributeSetsRepositoryTest`) inject a mock `DataSource` through
`LoadDatabase.setDataSource(...)`.

## Architecture

### Database access

A HikariCP pool against the real w4cash Oracle schema, configured in `DataSourceConfig#w4cashDataSource`
from the legacy `AppConfig` credentials (see below). Feature repositories (e.g. `product/ProductsRepository`,
`attribute/AttributeSetsRepository`) hand-write SQL with `PreparedStatement`/`ResultSet` — no Spring Data,
no JPA. Borrow a connection per unit of work and **always** close it via try-with-resources:

```java
try (Connection conn = LoadDatabase.getConnection();
        PreparedStatement st = conn.prepareStatement(SQL)) { ... }
```

Anything that must be atomic — or that takes `LOCK TABLE` — has to hold **one** connection for its whole
duration and pass it down to helpers (see `KassenabschlussController#zReport`). Do not open a second
connection mid-transaction; it is a separate Oracle session and will not see the uncommitted work or
share the lock.

Controllers translate `SQLException` to a 500 per endpoint rather than relying on a `@ControllerAdvice`.

### JPA / H2 — vestigial, do not extend

`spring-boot-starter-data-jpa` and in-memory H2 are still on the classpath because three carried-over
classes are still annotated as JPA entities (`attribute/Attribute`, `ticketinfo/OrderItem`,
`ticketinfo/OrderLine`). Nothing reads or writes them through JPA — they are used as plain DTOs, and the
real Oracle key is `id_`. Hibernate just creates empty tables for them at startup. Write new persistence
against the Oracle pool.

`DataSourceConfig` declares **two** datasources, and both must stay declared: Boot's
`DataSourceAutoConfiguration` is `@ConditionalOnMissingBean(DataSource.class)`, so the moment the Oracle
pool exists the auto-configured H2 that Hibernate binds to disappears. `jpaDataSource` (H2, in-memory) is
therefore explicit and `@Primary`; `w4cashDataSource` is the Oracle pool reached through
`LoadDatabase.getConnection()`. Pool size and connection timeout are tunable via `w4cash.datasource.*` in
`application.properties`.

The Oracle pool sets `initializationFailTimeout=-1`, so the app still starts when the database is
unreachable (endpoints then fail per-request); the pool revalidates and replaces broken connections, so a
dropped session does not require a restart.

### Startup / configuration

`W4cashApplication` builds a static `AppConfig` (`com.openbravo.pos.forms.AppConfig`, from the legacy
`lib/w4cash.jar`) and calls `.load()` *before* Spring context startup — this is where `db.driver`,
`db.URL`, `db.user`, `db.password` (optionally `crypt:`-prefixed and decrypted with `AltEncrypter`) come
from. These are **not** read from `spring.datasource.*`; `DataSourceConfig#w4cashDataSource` pulls them
off `AppConfig`.

### The `lib/w4cash.jar` dependency

`pom.xml` depends on `lib/w4cash.jar` (Openbravo POS core: `com.openbravo.pos.forms.AppConfig`,
`com.openbravo.pos.util.AltEncrypter`, `com.openbravo.format.Formats`, the ticket/printer scripting
engine, ...) as a `system`-scoped jar. `spring-boot-maven-plugin` is configured with
`includeSystemScope=true` so it lands in the fat jar.

### Per-feature package convention

Each domain area lives under `w4cash.<feature>` with `Controller` + `Repository` — `attribute`,
`category`, `floors`, `kassenabschluss`, `person`, `places`, `print`, `printer`, `product`, `report`,
`taxcategory`.

### Printing subsystem (`print/`, `printer/`)

`TicketPrintService` renders kitchen/bar tickets and receipts two ways: ESC/POS byte sequences sent
directly to a `javax.print.PrintService` for real thermal printers, and a `Printable`-based fallback
(custom pagination/wrapping) when the target is a PDF printer. Printer names/indexes-per-category come
from `AppConfig` machine properties (`machine.printer`, `machine.printer.2`, ...) and the
`CATEGORIES.PRINTER` column. Every print attempt is recorded via `PrintJobRepository` (success/failure +
raw content), which is what `/print-jobs` lists and `/print-jobs/{id}/reprint` replays. The admin UI never
originates a print — it only lists and reprints — but the whole rendering path is here because reprint
re-renders from the stored job.

`ticketinfo/OrderItem` and `ticketinfo/OrderLine` are kept solely as the shapes `TicketPrintService`
renders from; the order/ticket REST endpoints that used them live in `w4cash_webserver`, not here.

### CORS

`WebConfig` allows all origins/methods/headers on `/**` — intentionally permissive for this internal API.
