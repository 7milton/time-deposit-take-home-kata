# XA Bank time deposits

Java 21 solution to the [Time Deposit Refactoring Kata](docs/ASSIGNMENT.md), built with Spring Boot, PostgreSQL, Spring Data JPA, Flyway, MapStruct, and Testcontainers.

The application uses a facade with explicit input and output ports. Plan-specific policies preserve the original calculator behavior and support additional plans.

| Operation | Endpoint | Result |
| --- | --- | --- |
| GET | `/api/time-deposits` | All deposits with their withdrawal history. |
| POST | `/api/time-deposits/update-balances` | One monthly interest increment for every eligible deposit; returns 204. |

## Run

Requires Java 21, Maven 3.9+, and Docker:

```sh
cd java
docker compose up -d --wait db
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run
```

The demo seeds three deposits and one historical withdrawal. The API listens on `http://localhost:8080`.

## Verify and review

From the repository root:

```sh
mvn -f java/pom.xml clean verify
```

- [Runbook, business assumptions, and architecture](java/README.md)
- [OpenAPI contract](java/openapi.yaml) and [Swagger UI instructions](java/README.md#swagger-ui)
- [AI workflow and three reusable prompts](docs/AI_WORKFLOW.md)
- [Original assignment](docs/ASSIGNMENT.md)

## CI and artifact delivery

[GitHub Actions](https://github.com/7milton/time-deposit-take-home-kata/actions/workflows/ci.yml) builds the application with Java 21, runs the unit and PostgreSQL Testcontainers tests, and validates OpenAPI on pull requests and pushes to `main`. It also supports manual runs.

Successful runs provide the executable JAR as a downloadable artifact. See the [pipeline instructions](java/README.md#ci-and-artifact-delivery) for reports and downloads.

This fork contains the Java solution; the unused language starters have been removed.
