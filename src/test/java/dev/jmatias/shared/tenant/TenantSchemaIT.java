package dev.jmatias.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.jmatias.core.project.Project;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantSchemaIT {

    @Inject
    DataSource ds;

    @Inject
    TenantContext tenant;

    @Inject
    EntityManager em;

    @Inject
    @PersistenceUnit("core")
    EntityManager coreEm;

    private String a;
    private String b;

    @BeforeEach
    void createSchemas() throws SQLException {
        a = newSchema();
        b = newSchema();
    }

    @AfterEach
    void dropSchemas() throws SQLException {
        for (var schema : new String[] {a, b}) {
            sql("drop schema " + schema + " cascade");
        }
    }

    @Test
    void eachTenantOnlySeesItsOwnRows() {
        insertProbeAs(a, "in-a");
        insertProbeAs(b, "in-b-1");
        insertProbeAs(b, "in-b-2");

        assertThat(countProbesAs(a)).isEqualTo(1);
        assertThat(countProbesAs(b)).isEqualTo(2);
    }

    @Test
    void withoutTenantTheQueryFailsInsteadOfHittingAnotherProject() {
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(this::countProbes))
                .hasMessageContaining("probe");
    }

    @Test
    void coreEntitiesStayInCoreWhileATenantIsActive() {
        tenant.set(a);
        QuarkusTransaction.begin();
        try {
            em.persist(new TenantProbe("in-a"));
            coreEm.persist(new Project("probe-app", "Probe app"));
            em.flush();
            coreEm.flush();

            assertThat(coreEm.createNativeQuery("select count(*) from core.project where slug = 'probe-app'")
                    .getSingleResult()).isEqualTo(1L);
            assertThat(countProbes()).isEqualTo(1);
        } finally {
            QuarkusTransaction.rollback(); // leaves no project row in core
        }
    }

    @Test
    void invalidSchemaNameIsRejected() {
        assertThatThrownBy(() -> tenant.set("public; drop schema core"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void insertProbeAs(String schema, String label) {
        tenant.set(schema);
        QuarkusTransaction.requiringNew().run(() -> em.persist(new TenantProbe(label)));
    }

    private long countProbesAs(String schema) {
        tenant.set(schema);
        return QuarkusTransaction.requiringNew().call(this::countProbes);
    }

    private long countProbes() {
        return em.createQuery("select count(p) from TenantProbe p", Long.class).getSingleResult();
    }

    private String newSchema() throws SQLException {
        var schema = "p_" + UUID.randomUUID().toString().replace("-", "");
        sql("create schema " + schema);
        sql("create table " + schema + ".probe (id uuid primary key, label varchar(50) not null)");
        return schema;
    }

    private void sql(String statement) throws SQLException {
        try (var c = ds.getConnection(); var s = c.createStatement()) {
            s.execute(statement);
        }
    }
}
