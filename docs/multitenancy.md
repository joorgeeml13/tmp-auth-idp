# Multitenancy: un schema de PostgreSQL por proyecto

Estado: **decidido**, pendiente de implementar. Sustituye al modelo anterior de una única tabla `account` en `public`.

## Idea

Una sola base de datos PostgreSQL, un schema por cada proyecto cliente del IdP, más un schema `core` compartido.

```text
auth (database)
├── core                      # datos globales, no pertenecen a ningún proyecto
│   ├── project
│   └── signing_key
├── p_<uuid-sin-guiones>      # un schema por proyecto, todos con las mismas tablas
│   ├── account
│   ├── credentials
│   └── ...
└── p_<otro-uuid>
    └── ...
```

Un proyecto es una aplicación cliente (por ejemplo, una de mis apps personales). Sus cuentas, credenciales y demás datos viven aislados en su propio schema: una cuenta de un proyecto no existe para otro, y un `email` puede repetirse entre proyectos.

## Schema `core`

| Tabla | Para qué | Columnas clave |
|-------|----------|----------------|
| `project` | Registro de proyectos y de su schema | `id` UUID, `slug` (único, solo para URLs), `name`, `schema_name` (único), `status`, `created_at`, `updated_at` |
| `signing_key` | Claves **públicas** de firma de JWT, para publicar el JWKS y verificar tokens | `id`, `project_id` → `project`, `kid`, `algorithm`, `public_key`, `status`, `created_at`, `retire_at`; único `(project_id, kid)` |

Notas:

- `project.status`: `PROVISIONING` → `ACTIVE` → `SUSPENDED`. Solo los `ACTIVE` resuelven tenant.
- `project.schema_name` se **deriva del id** (`'p_' || replace(id::text, '-', '')`, 34 caracteres, límite de PostgreSQL: 63) y nunca de input del usuario. Es una columna generada (`GENERATED ALWAYS ... STORED`), así que no se puede escribir ni divergir del id. Motivo: un nombre de schema no se puede parametrizar en SQL, así que debe ser imposible que llegue contaminado.
- `signing_key.status`: `ACTIVE` (firma y verifica), `RETIRED` (solo verifica hasta `retire_at`, para no invalidar tokens vivos al rotar), `REVOKED` (no verifica).
- Se guarda **solo la parte pública**. La clave privada no entra en la base de datos (ver preguntas abiertas).

## Schema de proyecto

Todas las tablas del tenant (`account`, `credentials`, ...) son idénticas en cada schema y se definen **una sola vez** en `db/migration/tenant/`. Esas migraciones se aplican a cada schema de proyecto. Las tablas del tenant no llevan `project_id`: el aislamiento lo da el schema, no una columna.

Ninguna tabla de tenant tiene FK hacia `core` ni a otro schema. El vínculo es solo el nombre del schema en `core.project`.

## Resolución del tenant en cada petición

1. Un `TenantResolver` (Hibernate ORM `multitenant=SCHEMA`) obtiene el proyecto de la petición, lo busca en `core.project` (sin caché en la v1) y devuelve su `schema_name`.
2. Hibernate fija el `search_path` de la conexión a ese schema mientras dura la sesión y lo restablece al devolverla al pool.
3. El código de dominio (`Account`, `Credentials`, ...) trabaja sin saber en qué tenant está.

Dos persistence units, porque las entidades de `core` no son multitenant:

| PU | Entidades | Tenant |
|----|-----------|--------|
| `core` | `Project`, `SigningKey` | ninguno, schema `core` fijo |
| `tenant` (default) | `Account`, `Credentials`, ... | por petición |

## Conexión a PostgreSQL

V1: **un único usuario y una única conexión** para Flyway, el provisionado y la app. Ese usuario necesita `CREATE` sobre la base de datos (para crear los schemas de proyecto) y es propietario de `core` y de todos los `p_<uuid>`.

Más adelante se puede separar en un rol de migraciones (DDL) y otro de aplicación (solo DML), pero obliga a repartir permisos en cada schema nuevo y a tener un segundo datasource. No se hace en la v1.

## Migraciones (Flyway)

| Ubicación | Se aplica sobre | Cuándo |
|-----------|-----------------|--------|
| `db/migration/core/` | schema `core` | al arrancar |
| `db/migration/tenant/` | cada `p_<uuid>` | al crear un proyecto, y al arrancar para todos los proyectos `ACTIVE` (así una migración nueva llega a todos) |

Crear un proyecto:

1. Insertar fila en `core.project` con `status = PROVISIONING`.
2. `CREATE SCHEMA p_<uuid>`.
3. Ejecutar Flyway tenant sobre ese schema.
4. Pasar a `ACTIVE`.

Si falla en 2 o 3 el proyecto se queda en `PROVISIONING` (visible, no resoluble) y se puede reintentar o borrar.

## Estructura de paquetes (propuesta)

Mantiene el vertical slice de `.agents/rules/rules.md`.

```text
dev.jmatias/
├── core/              # dominio global: project/, signingkey/ (PU "core")
├── account/           # dominio de tenant (PU "tenant")
├── credential/        # dominio de tenant
├── security/          # firma y emisión de JWT, JWKS
└── shared/
    ├── exception/
    ├── i18n/
    └── tenant/        # TenantResolver, provisionado y migración de schemas
```

## Qué no se hace (por ahora)

- Un rol de PostgreSQL por tenant, o separar roles de migración y de aplicación: un único usuario accede a todos los schemas. Se puede endurecer más adelante.
- Particionar o sacar un proyecto grande a otra base de datos.
- Consultas que crucen proyectos. Si hacen falta (métricas), se hacen desde un job que itera los schemas.

## Preguntas abiertas

1. **¿Cómo se identifica el proyecto en la petición?** Opciones: prefijo de ruta `/{slug}/...`, cabecera, o subdominio. Recomendado: ruta, porque el `iss` y la URL del JWKS salen de ahí (`/{slug}/.well-known/jwks.json`).
2. **¿Dónde vive la clave privada de cada proyecto?** La base solo guarda las públicas. Opciones: fichero/secret por proyecto en el entorno, o cifrada en `core` con una clave maestra del entorno. Hay que decidirlo antes de implementar la firma.
3. **Escala:** un schema por proyecto está bien para decenas o cientos de proyectos. Con miles, las migraciones de arranque y el catálogo de PostgreSQL empiezan a pesar.
