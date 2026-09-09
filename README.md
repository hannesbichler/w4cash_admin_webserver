# w4cash_admin_webserver

Spring Boot REST backend (Java 21) for the **w4cash_admin** Angular UI
(`C:\BTech\angular\w4cash_admin`).

Extracted from `w4cash_webserver` — it carries only the endpoints that admin UI
actually calls. The point-of-sale-facing parts of the original server
(ticket/order handling, payments, chat, OTP auth, settings, active-cash) are not
part of this project.

## Run

```
./mvnw spring-boot:run     # listens on https://w4cash.bichler.tech:3000
./mvnw test                # 105 tests
```

The Angular dev server proxies `/api/*` here — see `proxy.conf.json` in the UI
project (target `https://w4cash.bichler.tech:3000`). The backend accepts both `/products` and
`/api/products` style routes.

HTTPS is configured from the repo-root PEM files `w4cash.bichler.tech.pem` and `w4cash.bichler.tech-key.pem`.
The certificate SAN is `w4cash.bichler.tech`, so callers should resolve `w4cash.bichler.tech` to `217.154.223.125` rather than use the raw IP directly.

## Endpoints

| Area            | Endpoints                                                          | Angular service            |
| --------------- | ------------------------------------------------------------------ | -------------------------- |
| Products        | `/products`                                                        | `product.service.ts`       |
| Categories      | `/categories`                                                      | `category.service.ts`      |
| Attributes      | `/attributes`, `/attributes/{id}/values`, `/attribute-sets`         | `attribute.service.ts`     |
| Tax             | `/tax-categories`, `/tax-categories/{id}/rates`                     | `tax.service.ts`           |
| Floors & tables | `/floors`, `/places`, `/places/{floorId}`                           | `floor.service.ts`         |
| Print jobs      | `/print-jobs`, `/print-jobs/{id}/reprint`, `/persons`               | `print-job.service.ts`     |
| Printers        | `/printers/installed`, `/printers/config`                           | `printer.service.ts`       |
| Reports         | `/reports`, `/reports/{name}/run`                                   | `report.service.ts`        |
| Cash close      | `/kassenabschluss/{tabletId}`                                       | `kassenabschluss.service.ts` |

See `CLAUDE.md` for architecture notes.
