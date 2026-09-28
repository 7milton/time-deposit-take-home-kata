# AI-assisted development workflow

Codex assisted with requirement analysis, policy extraction, the API and persistence implementation, tests, and documentation. Human review selected the stack, facade architecture, and [business assumptions](../java/README.md#business-assumptions). The prompts below summarize the working instructions as three reproducible stages.

## Purpose

AI was used to support requirement refinement, identify edge cases, and accelerate implementation and test development. Characterization tests provided evidence that the refactoring preserved the original calculator behavior, while integration tests verified the API and persistence. Human review guided design decisions, challenged unnecessary complexity, and checked the solution against the assignment.

## Setup

- Codex desktop with GPT-6, local repository and shell access, and access to official framework documentation.
- Java 21, Maven 3.9+, and Docker; dependency versions are configured in [`java/pom.xml`](../java/pom.xml).
- [`AGENTS.md`](../AGENTS.md) contains the repository rules used by the assistant. The three prompts below provide the task instructions; no separate agent framework or custom system prompt is required.
- Start from the original kata on your own fork. Inspect the diff and run the relevant checks after each stage.

## Prompt 1 — Preserve and clarify the interest rules

```text
Read docs/ASSIGNMENT.md (or the original kata README) and the original Java code.
Use Java 21. Keep the shared TimeDeposit class unchanged and preserve
TimeDepositCalculator.updateBalance(List<TimeDeposit>) and its behavior.
Characterize plan thresholds, unknown names, repeated accrual, and the original
half-cent rounding. Extract plan-specific InterestPolicy implementations and
keep the calculator readable. Use constructor injection for the policies.
Retain only the Java solution, run its tests, and review the diff.
```

## Prompt 2 — Implement the application and adapters

```text
Implement exactly GET /api/time-deposits and POST
/api/time-deposits/update-balances with Spring Boot. Use a facade behind an
input port and a repository output port implemented by a JPA adapter. Keep
interest rules independent of HTTP and persistence. Use PostgreSQL, Flyway,
Spring Data repositories, MapStruct, and Lombok entity builders. Perform
load, calculation, and save in one transaction, protecting existing deposits
from competing updates. Return withdrawals in GET. Add the OpenAPI contract,
a demo profile, and PostgreSQL integration tests. Preserve the original
calculator behavior and document the business assumptions.
```

## Prompt 3 — Review and prepare the submission

```text
Review the solution against the assignment and AGENTS.md. Check both HTTP
methods and paths, persistence mappings, transaction rollback, lock lifetime,
and all documented assumptions. Run mvn -f java/pom.xml clean verify and the
OpenAPI linter. Verify the packaged application with the documented demo flow.
Keep documentation focused on setup, design decisions, and evidence. Remove
stale IDE artifacts and group the reviewed solution into three coherent
commits. Report the checks actually run and any remaining limitations.
```

## Verification

The suite contains 26 tests covering the domain, the input port with an in-memory repository, and the real HTTP/PostgreSQL integration. Commands and verified results are recorded in the [runbook](../java/README.md#verification). The original [assignment](ASSIGNMENT.md) is retained for comparison.
