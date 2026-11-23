package dev.jmatias.core.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

// No @Data / @EqualsAndHashCode: Lombok-generated equals on entities breaks Hibernate.
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    // Generated column in the DB (p_<id without dashes>): never written, read back after the insert.
    @Generated(event = EventType.INSERT)
    @Column(name = "schema_name", insertable = false, updatable = false)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Hibernate inserts explicit values, so the DB defaults never apply.
    public Project(String slug, String name) {
        this.id = UUID.randomUUID();
        this.slug = slug;
        this.name = name;
        this.status = ProjectStatus.PROVISIONING;
        this.createdAt = this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = ProjectStatus.ACTIVE;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
