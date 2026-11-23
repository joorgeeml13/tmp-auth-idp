package dev.jmatias.core.project.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Rows go in by SQL with the status we need; the DB is shared with other ITs, so each test cleans up its own. */
@QuarkusTest
class MigrateAllProjectsUseCaseIT {

    @Inject
    MigrateAllProjectsUseCase useCase;

    @Inject
    DataSource ds;

    private final List<UUID> created = new ArrayList<>();

    @AfterEach
    void cleanUp() throws SQLException {
        for (var id : created) {
            sql("drop schema if exists " + schemaOf(id) + " cascade");
            sql("delete from core.project where id = '" + id + "'");
        }
    }

    @Test
    void migratesEveryActiveProjectAndSkipsTheOthers() throws SQLException {
        var a = insertProject("ACTIVE");
        var b = insertProject("ACTIVE");
        var provisioning = insertProject("PROVISIONING");

        assertThat(useCase.execute()).isEqualTo(2);

        assertThat(historyRows(a)).isEqualTo(1);
        assertThat(historyRows(b)).isEqualTo(1);
        assertThat(schemaExists(provisioning)).isFalse();
    }

    @Test
    void runningAgainChangesNothing() throws SQLException {
        var a = insertProject("ACTIVE");

        useCase.execute();
        useCase.execute();

        assertThat(historyRows(a)).isEqualTo(1);
    }

    @Test
    void aProjectThatCannotBeMigratedAbortsTheRun() throws SQLException {
        var broken = insertProject("ACTIVE");
        // A schema with tables but no Flyway history: Flyway refuses to touch it.
        sql("create schema " + schemaOf(broken));
        sql("create table " + schemaOf(broken) + ".junk (id int)");

        assertThatThrownBy(useCase::execute).isInstanceOf(FlywayException.class);
    }

    private UUID insertProject(String status) throws SQLException {
        var id = UUID.randomUUID();
        created.add(id);
        sql("insert into core.project (id, slug, name, status) values ('" + id + "', 'it-"
                + id.toString().substring(0, 8) + "', 'x', '" + status + "')");
        return id;
    }

    private String schemaOf(UUID id) {
        return "p_" + id.toString().replace("-", "");
    }

    private boolean schemaExists(UUID id) throws SQLException {
        return query("select count(*) from information_schema.schemata where schema_name = '" + schemaOf(id) + "'") == 1;
    }

    private int historyRows(UUID id) throws SQLException {
        return query("select count(*) from " + schemaOf(id) + ".flyway_schema_history where version is not null");
    }

    private int query(String sql) throws SQLException {
        try (var c = ds.getConnection(); var s = c.createStatement(); var r = s.executeQuery(sql)) {
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
