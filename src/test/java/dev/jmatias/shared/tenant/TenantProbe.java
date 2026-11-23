package dev.jmatias.shared.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Test-only tenant entity: its table is created by TenantSchemaIT in each throwaway schema. */
@Entity
@Table(name = "probe")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantProbe {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String label;

    public TenantProbe(String label) {
        this.id = UUID.randomUUID();
        this.label = label;
    }
}
