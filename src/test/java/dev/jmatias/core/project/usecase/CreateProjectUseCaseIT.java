package dev.jmatias.core.project.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.jmatias.core.project.Project;
import dev.jmatias.core.project.ProjectRepository;
import dev.jmatias.core.project.ProjectSlugTakenException;
import dev.jmatias.core.project.ProjectStatus;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Not @TestTransaction: the use case commits in its own transactions, so the test cleans up by hand. */
@QuarkusTest
class CreateProjectUseCaseIT {

    @Inject
    CreateProjectUseCase useCase;

    @Inject
    ProjectRepository repository;

    @Inject
    DataSource ds;

    private final List<UUID> created = new ArrayList<>();

    @AfterEach
    void cleanUp() throws SQLException {
        for (var id : created) {
            var schema = "p_" + id.toString().replace("-", "");
            sql("drop schema if exists " + schema + " cascade");
            sql("delete from core.project where id = '" + id + "'");
        }
    }

    private String slug() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Project load(UUID id) {
        return QuarkusTransaction.requiringNew().call(() -> repository.findByIdOptional(id).orElseThrow());
    }

    @Test
    void createsTheProjectBuildsItsSchemaAndActivatesIt() throws SQLException {
        var slug = slug();

        var id = useCase.execute(slug, "Probe app");
        created.add(id);

        var project = load(id);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.getSlug()).isEqualTo(slug);
        assertThat(count("select count(*) from " + project.getSchemaName() + ".flyway_schema_history where version = '1'"))
                .isEqualTo(1);
        assertThat(repository.findActiveBySlug(slug)).isPresent();
    }

    @Test
    void rejectsADuplicateSlugWithoutCreatingAnything() throws SQLException {
        var slug = slug();
        created.add(useCase.execute(slug, "First"));

        assertThatThrownBy(() -> useCase.execute(slug, "Second")).isInstanceOf(ProjectSlugTakenException.class);

        assertThat(count("select count(*) from core.project where slug = '" + slug + "'")).isEqualTo(1);
    }

    private int count(String query) throws SQLException {
        try (var c = ds.getConnection(); var s = c.createStatement(); var r = s.executeQuery(query)) {
            r.next();
            return r.getInt(1);
        }
    }

    private void sql(String statement) throws SQLException {
        try (var c = ds.getConnection(); var s = c.createStatement()) {
            s.execute(statement);
        }
    }
}
