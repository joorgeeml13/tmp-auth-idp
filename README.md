# Auth Microservice (IdP)

Microservicio centralizado de identidad y authenticacion (Identity Provider) diseñado como base agnostica y desacoplada para el ecosistema de mis proyectos personales.

Construido sobre **Quarkus** para priorizar arranque ultra-rapido, huella de memoria minima y soporte nativo con GraalVM.

---

## Stack Tecnico
- **Runtime:** Quarkus 3.40 LTS + Java 21 LTS (Temurin / GraalVM CE 21).
- **Seguridad:** SmallRye JWT / Security JPA
- **Persistencia:** Hibernate ORM con Panache
- **Base de Datos:** PostgreSQL, multitenant con un schema por proyecto (ver [docs/multitenancy.md](docs/multitenancy.md))
- **Criptografica:** Firma asimetrica (RS256 / Ed25519) + hashing Argon2/BCrypt
- **Entorno:** Docker / Docker compose
- **GitWorkflows:** CI/CD de empaquetado de la imagen docker y publicacion en el regitro de contenedores del git.