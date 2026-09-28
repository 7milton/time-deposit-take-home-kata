# Java solution

Java 21, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL, Flyway, MapStruct, and Lombok builders. The shared `TimeDeposit` class is unchanged. `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` preserves its signature and per-call calculation behavior.

## Run locally

Requires Java 21, Maven 3.9+, and Docker. Run from `java/`:

```sh
docker compose up -d --wait db
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

The demo profile inserts three deposits and one historical withdrawal when the database is empty. The default profile starts with an empty migrated database. PostgreSQL defaults are `localhost:5433`, database/user/password `time_deposit`; override them with `DB_URL`, `DB_USER`, and `DB_PASSWORD`.

```sh
curl -i http://localhost:8080/api/time-deposits
curl -i -X POST http://localhost:8080/api/time-deposits/update-balances
```

GET returns the deposits and their withdrawals ordered by ID. POST returns `204 No Content` after the transaction commits.

## Swagger UI

The [OpenAPI contract](openapi.yaml) describes the two application endpoints. With the demo running, launch Swagger UI in a second terminal from `java/`:

```sh
docker run --rm -p 8081:8080 -e SWAGGER_JSON=/spec/openapi.yaml \
  -v "$PWD/openapi.yaml:/spec/openapi.yaml:ro" swaggerapi/swagger-ui:v5.18.2
```

Open [Swagger UI](http://localhost:8081), expand GET or POST, then select **Try it out → Execute**. The demo profile permits the `http://localhost:8081` browser origin. The contract's `apiHost` server variable defaults to `localhost`.

## Business assumptions

- **Rates:** the assignment describes monthly interest, while the original calculator divides each rate by 12. Preserving that behavior takes precedence: each eligible call credits one-twelfth of the stated rate.
- **Eligibility:** no plan accrues during days 0–30. Basic uses 1% from day 31, student uses 3% from day 31 through day 365, and premium uses 5% from day 46. Unknown or differently cased plan names earn no interest.
- **Invocation:** every POST accrues again on the current balance. It does not advance `days`. The assignment supplies no accrual period or idempotency key, so retries are separate accruals.
- **Rounding:** the interest increment is rounded to cents with the original `new BigDecimal(double)` and `HALF_UP` behavior. The shared model retains `Double`; database amounts use `NUMERIC(19,2)`. Characterization tests cover half-cent cases where changing the decimal conversion would change results.
- **Withdrawals:** they are historical records already reflected in `balance`. GET includes them; accrual does not deduct them again. Creating withdrawals is outside the two-endpoint scope.
- **Database naming:** the assignment's logical fields map to unquoted PostgreSQL identifiers `time_deposits`, `plan_type`, and `time_deposit_id`. JSON retains the specified camelCase names. Flyway `V1` initializes an empty database; Hibernate validates the schema.
- **Access:** authentication and custom invalid-input handling are outside the assignment. Swagger CORS is enabled only by the demo profile.

## Architecture

| Package | Responsibility |
| --- | --- |
| `domain` | Calculator and interest policies, independent of Spring and persistence. |
| `application` | Transactional facade and immutable result views. |
| `application.port.in` | `TimeDepositUseCase`, implemented by the facade. |
| `application.port.out` | `TimeDepositRepository`, required by the facade. |
| `adapter.in.web` | REST controller calling the input port. |
| `adapter.out.persistence` | `JpaTimeDepositAdapter`, Spring Data repositories, entities, MapStruct mapper, and demo data. |
| `config` | Spring bean registration and demo CORS configuration. |

The Spring entry point and unchanged shared `TimeDeposit` class remain in the root package. The core has no dependency on adapter packages or JPA. Spring transaction annotations on the facade keep transaction management explicit without a separate decorator.

The facade loads deposits with pessimistic write locks, calculates interest, and saves balances in one transaction. A failed update rolls back the whole operation. The locks remain held until commit or rollback; they protect existing rows from competing writes. This all-deposit operation loads and locks the current rows, which suits the kata's scope. It does not deduplicate requests or prevent concurrent inserts.

Spring Data derives queries from method names; `@Lock` controls accrual locking, and `@EntityGraph` loads withdrawal history. MapStruct handles entity-to-domain and result mapping. Builders and `save`/`saveAll` handle persistence. Generated code stays in `target/`.

To add a plan, implement `InterestPolicy` and register it as a Spring bean. Each policy normally matches a distinct plan name. If policies intentionally overlap, set their order explicitly with Spring's `@Order`: the first match wins, including when it returns zero. The calculator, facade, and persistence adapter need no plan-specific edits.

## Verification

From the repository root:

```sh
mvn -f java/pom.xml clean verify
npx --yes @redocly/cli@1.34.5 lint java/openapi.yaml --config .redocly.yaml
```

The Java check requires Docker and builds the executable JAR. The tests use real PostgreSQL through Testcontainers and in-memory ports for the application test.

Verified on 2026-09-28: **26 tests passed** — 14 calculator cases, 1 facade test, 8 database/API tests, and 3 demo-profile tests. Coverage includes rate and day boundaries, legacy half-cent rounding, first-policy selection, repeated accrual, exactly two HTTP operations, withdrawal mapping, decimal persistence, rollback, lock lifetime, demo seeding, and CORS. OpenAPI lint passed without errors or warnings.

The packaged JAR was also started with the demo profile against a fresh PostgreSQL database. GET, POST, seeded withdrawals, resulting balances, unchanged days, and the Swagger CORS response were checked over HTTP.

The [AI workflow](../docs/AI_WORKFLOW.md) records the tools, repository instructions, and three prompts for reproducing the development stages.
