package dev.jmatias.core.project.usecase;

import dev.jmatias.core.project.Project;
import dev.jmatias.core.project.ProjectRepository;
import dev.jmatias.core.project.ProjectSlugTakenException;
import dev.jmatias.shared.tenant.TenantMigrationService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * Registers a project and builds its schema. Not one transaction on purpose: Flyway runs on its own connections,
 * so the PROVISIONING row is committed first and the project is activated in a second transaction.
 * If the migration fails the project stays PROVISIONING (visible, not resolvable) and the error propagates.
 */
@ApplicationScoped
public class CreateProjectUseCase {

    private record Provisioning(UUID id, String schema) {}

    private final ProjectRepository repository;
    private final TenantMigrationService migrations;

    public CreateProjectUseCase(ProjectRepository repository, TenantMigrationService migrations) {
        this.repository = repository;
        this.migrations = migrations;
    }

    /** The slug must match the DB CHECK (project_slug_format); the endpoint validates it before calling. */
    public UUID execute(String slug, String name) {
        var provisioning = QuarkusTransaction.requiringNew().call(() -> {
            // shortcut: check-then-insert, two concurrent creates of one slug end in the unique constraint (a 500).
            if (repository.count("slug", slug) > 0) {
                throw new ProjectSlugTakenException(slug);
            }
            var project = new Project(slug, name);
            repository.persist(project);
            repository.flush(); // reads back the generated schema_name
            return new Provisioning(project.getId(), project.getSchemaName());
        });

        migrations.migrate(provisioning.schema());

        QuarkusTransaction.requiringNew().run(() -> repository.findByIdOptional(provisioning.id()).orElseThrow().activate());
        return provisioning.id();
    }
}
