# Working on the Java solution

## Start with the contract

1. Inspect `git status` and the affected code and tests; preserve unrelated work.
2. Before changing behavior, read `docs/ASSIGNMENT.md` and the business assumptions in `java/README.md`. For HTTP changes, also read `java/openapi.yaml`.
3. State the intended outcome and how it will be verified. Resolve ambiguities against the assignment and documented assumptions before implementing the change.

## Preserve compatibility

- Keep the shared `TimeDeposit` class unchanged and preserve the `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` signature and behavior. Establish existing behavior with characterization tests before refactoring calculations.
- Retain the original rate division by 12 and interest rounding through `new BigDecimal(double)` with `HALF_UP`. Replacing it with `BigDecimal.valueOf` can change half-cent results.
- Keep exactly the two application operations defined in `java/openapi.yaml`, including their response fields and status codes.

## Respect the architecture

- Keep `domain` independent of Spring, HTTP, and persistence. Add plan rules through `InterestPolicy` implementations registered in `InterestPolicyConfiguration`.
- The incoming web adapter calls `application.port.in.TimeDepositUseCase`. `TimeDepositFacade` implements that port and coordinates the use cases through `application.port.out.TimeDepositRepository`.
- Keep the application independent of adapter implementations and JPA entities. The outgoing persistence adapter owns Spring Data repositories, entity builders, and MapStruct mapping.
- Keep loading, calculation, and saving inside the facade's single accrual transaction. Preserve write locks until that transaction completes and rollback of the whole update on failure.
- Extend the existing policies and ports when they cover the requirement. Introduce another abstraction only when a concrete use case needs it.
- Before removing apparently unused code, check Lombok and MapStruct output, JPA mappings, and Spring registration as well as handwritten callers.

## Verify the change

Run commands from the repository root:

| Change | Required verification |
| --- | --- |
| Java code, dependencies, application configuration, or schema | `mvn -f java/pom.xml clean verify` with Java 21 and Docker. |
| HTTP behavior or OpenAPI contract | The Java suite plus `npx --yes @redocly/cli@1.34.5 lint java/openapi.yaml --config .redocly.yaml`; check methods, paths, status codes, and response fields against the controller. |
| Documentation only | Check referenced files, commands, and claims against the repository, then run `git diff --check`. |

Use domain tests for interest rules and PostgreSQL Testcontainers tests for persistence, transaction, and API behavior. Add regression coverage for changed behavior; retain the existing boundary, rounding, rollback, and locking checks. Report unavailable prerequisites and skipped checks explicitly.

## Review and hand off

- Explain new business assumptions in the relevant code comments and `java/README.md`. Record material changes to the AI workflow in `docs/AI_WORKFLOW.md`.
- Review the final diff for scope, compatibility, architecture boundaries, and generated or unrelated files. Commit only verified changes in coherent units.
- Work in the user's fork. Publishing changes requires an explicit push request.
- Summarize what changed, which checks ran and their results, and any remaining limitations. Distinguish local verification from remote CI or publication.
