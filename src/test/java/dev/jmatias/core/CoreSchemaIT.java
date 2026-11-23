package dev.jmatias.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CoreSchemaIT {

    @Inject
    DataSource ds;

    @Test
    void flywayCreatesTheCoreTables() throws SQLException {
        var tables = new ArrayList<String>();
        try (var c = ds.getConnection(); var s = c.createStatement();
             var r = s.executeQuery("select table_name from information_schema.tables where table_schema = 'core'")) {
            while (r.next()) {
                tables.add(r.getString(1));
            }
        }
        assertThat(tables).contains("project", "signing_key", "flyway_schema_history");
    }

    @Test
    void schemaNameIsDerivedFromTheId() throws SQLException {
        var id = UUID.randomUUID();
        inRollback(c -> {
            insertProject(c, id, "my-app");
            try (var s = c.prepareStatement("select schema_name from core.project where id = ?")) {
                s.setObject(1, id);
                try (var r = s.executeQuery()) {
                    r.next();
                    assertThat(r.getString(1)).isEqualTo("p_" + id.toString().replace("-", ""));
                }
            }
        });
    }

    @Test
    void invalidSlugIsRejected() throws SQLException {
        inRollback(c -> assertThatThrownBy(() -> insertProject(c, UUID.randomUUID(), "Bad Slug"))
                .isInstanceOf(SQLException.class).hasMessageContaining("project_slug_format"));
    }

    @Test
    void onlyOneActiveSigningKeyPerProject() throws SQLException {
        var projectId = UUID.randomUUID();
        inRollback(c -> {
            insertProject(c, projectId, "my-app");
            insertKey(c, projectId, "k1", "ACTIVE");
            insertKey(c, projectId, "k0", "RETIRED"); // retired keys do not count
            assertThatThrownBy(() -> insertKey(c, projectId, "k2", "ACTIVE"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("signing_key_one_active");
        });
    }

    private interface Body {
        void run(Connection c) throws SQLException;
    }

    /** Runs the body in a transaction that is always rolled back, so tests leave no rows behind. */
    private void inRollback(Body body) throws SQLException {
        try (var c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                body.run(c);
            } finally {
                c.rollback();
            }
        }
    }

    private void insertProject(Connection c, UUID id, String slug) throws SQLException {
        try (var s = c.prepareStatement("insert into core.project (id, slug, name) values (?, ?, 'x')")) {
            s.setObject(1, id);
            s.setString(2, slug);
            s.executeUpdate();
        }
    }

    private void insertKey(Connection c, UUID projectId, String kid, String status) throws SQLException {
        try (var s = c.prepareStatement(
                "insert into core.signing_key (id, project_id, kid, algorithm, public_key, status) values (?, ?, ?, 'RS256', 'pem', ?)")) {
            s.setObject(1, UUID.randomUUID());
            s.setObject(2, projectId);
            s.setString(3, kid);
            s.setString(4, status);
            s.executeUpdate();
        }
    }
}
