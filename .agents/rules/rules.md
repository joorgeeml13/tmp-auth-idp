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
│   ├── *Service.java             # Business logic and transactional boundary
│   ├── *Repository.java          # Persistence (implements PanacheRepository<Entity>)
│   ├── <Entity>.java             # JPA entity (if the domain requires persistence)
│   └── dto/                      # Input/output contracts (Java Records)
│       ├── *Request.java
│       └── *Response.java
│
├── security/                     # Cryptographic engine and IdP protocols (DB-agnostic)
└── shared/                       # Global exceptions, HTTP mappers, and base configuration
```
## 3. Unbreakable Code Rules
1. Repository Pattern: Always use `implements PanacheRepository<Entity>`. Extending `PanacheEntity` is strictly forbidden (avoids coupling entities to static methods).
2. Entity Boundary: No `@Entity` class may leak outside the `*Service` layer. JAX-RS endpoints must only accept and return Java Records located in `dto/` packages.
3. Zero Secrets in Logs: Logging plaintext passwords, hashes, refresh tokens, private keys, or `Authorization` headers is forbidden. Use `org.jboss.logging.Logger`, never `System.out.println`.
4. Error Handling: Never expose stack traces or internal SQL messages in HTTP responses. Map business exceptions to consistent JSON responses with their corresponding HTTP status codes (`400`, `401`, `403`, `409`).
## 4. Git Conventions
Follow the Conventional Commits standard: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`.
Committing .env files, certificates (`.pem`, `.key`, `.jks`), or temporary outputs is forbidden.
