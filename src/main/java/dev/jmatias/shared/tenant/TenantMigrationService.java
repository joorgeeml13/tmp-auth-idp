package dev.jmatias.shared.tenant;

import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Brings one project schema up to date with db/migration/tenant. Flyway creates the schema if it is missing,
 * so this is also how a new project gets its schema. Knows nothing about core: callers pass the name.
 */
@ApplicationScoped
public class TenantMigrationService {

    private final DataSource dataSource;

    public TenantMigrationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Idempotent. Throws FlywayException if a migration fails, so the caller decides whether that aborts. */
    public void migrate(String schema) {
        TenantContext.requireValid(schema); // the name is used as an identifier, never as a bind parameter
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration/tenant")
                .load()
                .migrate();
    }
}
