package dev.jmatias.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
class ProjectRepositoryIT {

    @Inject
    ProjectRepository repository;

    private Project persist(String slug) {
        var project = new Project(slug, "App " + slug);
        repository.persist(project);
        repository.flush();
        return project;
    }

    @Test
    void schemaNameIsReadBackFromTheGeneratedColumn() {
        var project = persist("my-app");

        assertThat(project.getSchemaName()).isEqualTo("p_" + project.getId().toString().replace("-", ""));
    }

    @Test
    void newProjectIsProvisioningAndNotResolvable() {
        persist("my-app");

        assertThat(repository.findActiveBySlug("my-app")).isEmpty();
    }

    @Test
    void activatedProjectIsFoundBySlug() {
        var project = persist("my-app");
        project.activate();
        repository.flush();

        assertThat(repository.findActiveBySlug("my-app")).get().extracting(Project::getId).isEqualTo(project.getId());
        assertThat(repository.findActiveBySlug("other-app")).isEmpty();
    }

    @Test
    void findAllActiveSkipsProjectsStillProvisioning() {
        var active = persist("active-app");
        active.activate();
        var provisioning = persist("new-app");
        repository.flush();

        assertThat(repository.findAllActive()).extracting(Project::getId)
                .contains(active.getId()).doesNotContain(provisioning.getId());
    }
}
