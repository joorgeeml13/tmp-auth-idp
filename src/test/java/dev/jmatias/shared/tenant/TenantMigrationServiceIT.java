package dev.jmatias.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantMigrationServiceIT {

    @Inject
    TenantMigrationService service;

    @Inject
    DataSource ds;

    private final List<String> created = new ArrayList<>();

    @AfterEach
    void dropSchemas() throws SQLException {
        for (var schema : created) {
            sql("drop schema if exists " + schema + " cascade");
        }
    }

    @Test
    void createsTheSchemaAndAppliesTheTenantMigrations() throws SQLException {
        var schema = newName();

        service.migrate(schema);

        assertThat(schemaExists(schema)).isTrue();
        assertThat(appliedVersions(schema)).containsExactly("1");
    }

    @Test
    void migratingAgainChangesNothing() throws SQLException {
        var schema = newName();

        service.migrate(schema);
        service.migrate(schema);

        assertThat(appliedVersions(schema)).containsExactly("1");
    }

    @Test
    void eachSchemaKeepsItsOwnHistory() throws SQLException {
        var a = newName();
        var b = newName();

        service.migrate(a);

        assertThat(schemaExists(b)).isFalse();
        service.migrate(b);
        assertThat(appliedVersions(a)).containsExactly("1");
        assertThat(appliedVersions(b)).containsExactly("1");
    }

    @Test
    void rejectsNamesThatAreNotProjectSchemas() {
        assertThatThrownBy(() -> service.migrate("public")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.migrate("core")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.migrate("p_x; drop schema core cascade")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.migrate(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private String newName() {
        var schema = "p_" + UUID.randomUUID().toString().replace("-", "");
        created.add(schema);
        return schema;
    }

    private boolean schemaExists(String schema) throws SQLException {
        try (var c = ds.getConnection(); var s = c.prepareStatement(
                "select count(*) from information_schema.schemata where schema_name = ?")) {
            s.setString(1, schema);
            try (var r = s.executeQuery()) {
                r.next();
                return r.getInt(1) == 1;
            }
        }
    }

    private List<String> appliedVersions(String schema) throws SQLException {
        var versions = new ArrayList<String>();
        // version is null for Flyway's own bookkeeping rows, so they are skipped.
        try (var c = ds.getConnection(); var s = c.createStatement();
             var r = s.executeQuery("select version from " + schema + ".flyway_schema_history where version is not null and success order by installed_rank")) {
            while (r.next()) {
                versions.add(r.getString(1));
            }
        }
        return versions;
    }

    private void sql(String statement) throws SQLException {
        try (var c = ds.getConnection(); var s = c.createStatement()) {
            s.execute(statement);
        }
    }
}
