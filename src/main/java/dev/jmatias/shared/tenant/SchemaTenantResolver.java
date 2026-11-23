package dev.jmatias.shared.tenant;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Picks the schema for the default persistence unit. Without a tenant it falls back to "public",
 * which holds no tenant tables, so a forgotten tenant fails loudly instead of hitting another project.
 * The tenant is fixed when the Hibernate session opens: it is read once per transaction, not per query.
 */
@PersistenceUnitExtension
@ApplicationScoped
public class SchemaTenantResolver implements TenantResolver {

    static final String NO_TENANT = "public";

    private final TenantContext context;

    public SchemaTenantResolver(TenantContext context) {
        this.context = context;
    }

    @Override
    public String getDefaultTenantId() {
        return NO_TENANT;
    }

    @Override
    public String resolveTenantId() {
        // Startup tasks and other non-request code have no request context to read from.
        if (!Arc.container().requestContext().isActive()) {
            return NO_TENANT;
        }
        return context.get().orElse(NO_TENANT);
    }
}
