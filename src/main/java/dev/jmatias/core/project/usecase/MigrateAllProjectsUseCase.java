package dev.jmatias.core.project.usecase;

import dev.jmatias.core.project.Project;
import dev.jmatias.core.project.ProjectRepository;
import dev.jmatias.shared.tenant.TenantMigrationService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Brings every ACTIVE project schema up to date, so a new tenant migration reaches all of them.
 * Fails fast: the first project that cannot be migrated aborts the run (and the startup), because
 * schemas out of sync with the code are worse than a failed boot.
 */
@ApplicationScoped
public class MigrateAllProjectsUseCase {

    private final ProjectRepository repository;
    private final TenantMigrationService migrations;

    public MigrateAllProjectsUseCase(ProjectRepository repository, TenantMigrationService migrations) {
        this.repository = repository;
        this.migrations = migrations;
    }

    /** Returns how many schemas were checked. shortcut: sequential, fine for tens of projects; parallelize beyond that. */
    public int execute() {
        // Names only, read inside the transaction: Flyway runs on its own connections, outside it.
        var schemas = QuarkusTransaction.requiringNew().call(
                () -> repository.findAllActive().stream().map(Project::getSchemaName).toList());
        schemas.forEach(migrations::migrate);
        return schemas.size();
    }
}
