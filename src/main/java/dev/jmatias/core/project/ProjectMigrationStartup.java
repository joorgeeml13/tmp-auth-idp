package dev.jmatias.core.project;

import dev.jmatias.core.project.usecase.MigrateAllProjectsUseCase;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/** Runs after Flyway has migrated core (that happens before StartupEvent), so core.project already exists. */
@ApplicationScoped
public class ProjectMigrationStartup {

    private static final Logger LOG = Logger.getLogger(ProjectMigrationStartup.class);

    private final MigrateAllProjectsUseCase migrateAll;

    public ProjectMigrationStartup(MigrateAllProjectsUseCase migrateAll) {
        this.migrateAll = migrateAll;
    }

    void onStart(@Observes StartupEvent event) {
        LOG.infof("Tenant schemas up to date: %d project(s)", migrateAll.execute());
    }
}
