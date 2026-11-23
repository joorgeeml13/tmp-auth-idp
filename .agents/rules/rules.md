# Global Repository Rules ('auth-microservice')
All agents working in this repository MUST read and follow these rules.
## 1. Technical Stack
- **Language:** Java 21 (use `record`, pattern matching, and immutable collections `List.of`/`Set.of`).
- **Framework:** Quarkus 3.40 LTS (RESTEasy Reactive / Jackson, Hibernate ORM with Panache, SmallRye JWT).
- **Database:** PostgreSQL + versioned migrations with Flyway (`src/main/resources/db/migration/V{N}__{description}.sql`).
- **Dependency Injection:** Constructor injection (no `@Inject` on private fields) to facilitate testability and immutability (private final).
## 2. Architecture: Pragmatic Vertical Slice
Code in `src/main/java/dev/jmatias/` is organized strictly by functional domain, not by global technical layers (`controllers/`, `service/`, `repositories/` at the root).
```text
<base-package>/
├── {functional-domain}/          # One package per business capability
│   ├── *Resource.java            # JAX-RS endpoints (@Valid validation and delegation only)
│   ├── usecase/                  # One class per business operation (ALWAYS, even for plain CRUDs)
│   │   └── <Verb><Noun>UseCase.java   # Business logic and transactional boundary, single public method `execute`
│   ├── *Service.java             # Technical or shared logic only (hashing, token building, logic reused by several use cases)
│   ├── *Repository.java          # Persistence (implements PanacheRepositoryBase<Entity, ID>)
│   ├── <Entity>.java             # JPA entity (if the domain requires persistence)
│   └── dto/                      # Input/output contracts (Java Records)
│       ├── *Request.java
│       └── *Response.java
│
├── security/                     # Cryptographic engine and IdP protocols (DB-agnostic)
└── shared/                       # Global exceptions, HTTP mappers, and base configuration
```
Layer flow inside a functional domain:
```text
Resource (HTTP)
    │
    ▼
UseCase  (ALWAYS, even for plain CRUDs)
   /     \
  ▼       ▼
Repository  Service (technical or shared logic only)
  │
  ▼
Entity
```
- A `Resource` only calls a `UseCase`; it never calls a `Repository` or a `Service` directly.
- A `UseCase` orchestrates `Repository` and `Service`, and owns the transaction.
- A `Service` never calls a `UseCase`.
- One `UseCase` class per business operation, named `<Verb><Noun>UseCase` (e.g. `RegisterAccountUseCase`), with a single public method `execute`.
- Only `UseCase` classes may inject a `Repository`. It is `public` because use cases live in a different package (`usecase/`); this restriction is a convention, not enforced by the compiler. Other domains reach this domain's data through its use cases, never through its repository.
## 2.1 Multitenancy
PostgreSQL schema per project plus a shared `core` schema (`project`, `signing_key`). Full design in `docs/multitenancy.md`.
- Migrations: `db/migration/core/` for `core`, `db/migration/tenant/` for every project schema. Never put tenant tables in `core` or the reverse.
- Tenant tables have no `project_id` and no FK to other schemas; isolation comes from the schema.
- A schema name is never built from user input; it comes from `core.project.schema_name`.
## 3. Unbreakable Code Rules
1. Repository Pattern: Always use `implements PanacheRepositoryBase<Entity, ID>` (`PanacheRepository<Entity>` fixes the id to `Long`). Extending `PanacheEntity` is strictly forbidden (avoids coupling entities to static methods).
2. Entity Boundary: No `@Entity` class may leak outside the `*UseCase` layer. JAX-RS endpoints must only accept and return Java Records located in `dto/` packages.
3. Zero Secrets in Logs: Logging plaintext passwords, hashes, refresh tokens, private keys, or `Authorization` headers is forbidden. Use `org.jboss.logging.Logger`, never `System.out.println`.
4. Error Handling: Never expose stack traces or internal SQL messages in HTTP responses. Map business exceptions to consistent JSON responses with their corresponding HTTP status codes (`400`, `401`, `403`, `409`).
5. Atomic Scope (Max 5 Files per Turn): Never create, modify, or delete more than **5 files** in a single execution turn. Implement changes incrementally—one vertical slice or cohesive unit at a time. If a task requires touching more than 5 files, you MUST stop at the 5-file limit, verify that the project compiles (`./mvnw test-compile`), summarize the progress, and wait for explicit user confirmation before proceeding with the next batch.
## 4. Git Conventions
Follow the Conventional Commits standard: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`.
Committing .env files, certificates (`.pem`, `.key`, `.jks`), or temporary outputs is forbidden.
## 5. Testing Conventions & Structure
- **Two kinds of tests, split by what they need:**
  - **Unit (`*Test.java`):** pure logic only (hashing, token building, validators, mappers, services whose collaborators are external I/O boundaries). No Quarkus container, no database. JUnit 5 + Mockito/AssertJ with constructor injection. Mock only external I/O boundaries (HTTP clients, clock); never mock a repository to test persistence behavior.
  - **Integration (`*IT.java`):** anything that touches endpoints, persistence, Flyway or security filters. Use `@QuarkusTest` + REST Assured against real PostgreSQL (Dev Services/Testcontainers). H2 and SQLite are forbidden.
- **Mirror Structure:** `src/test/java/dev/jmatias/` MUST mirror the package hierarchy of `src/main/java/dev/jmatias/`. A class with logic gets `<ClassName>Test.java`; a class that only makes sense with the container (Resource, Repository) gets `<ClassName>IT.java`. Classes with neither logic nor I/O (records, plain DTOs) need no test.
- **Precedence:** if an agent file in `.agents/agents/` contradicts this section, this section wins.
- **Build:** `./mvnw verify` runs both: `*Test` with Surefire and `*IT` with Failsafe (`skipITs=false` in the `pom.xml`).
