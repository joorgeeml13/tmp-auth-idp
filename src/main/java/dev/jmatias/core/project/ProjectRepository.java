package dev.jmatias.core.project;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Only use cases inject this. Finders return ACTIVE projects only: that is what can act as a tenant. */
@ApplicationScoped
public class ProjectRepository implements PanacheRepositoryBase<Project, UUID> {

    public Optional<Project> findActiveBySlug(String slug) {
        return find("slug = ?1 and status = ?2", slug, ProjectStatus.ACTIVE).firstResultOptional();
    }

    public List<Project> findAllActive() {
        return list("status", ProjectStatus.ACTIVE);
    }
}
