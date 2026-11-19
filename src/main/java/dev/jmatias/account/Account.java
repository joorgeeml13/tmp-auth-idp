package dev.jmatias.account;

import dev.jmatias.account.vo.AccountId;
import dev.jmatias.account.vo.Email;
import dev.jmatias.account.vo.IpAddress;
import dev.jmatias.account.vo.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// No @Data / @EqualsAndHashCode: Lombok-generated equals on entities breaks Hibernate.
@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    // Optimistic locking. Unrelated to tokenVersion, which invalidates issued JWTs.
    @Version
    private long version;

    // Hibernate forbids converters on @Id, so the key is a raw UUID and getId() wraps it.
    @Id
    private UUID id;

    @Column(nullable = false)
    private Email email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_number")
    private PhoneNumber phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String zoneinfo;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "last_login_ip")
    private IpAddress lastLoginIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Same defaults as the V1 migration: Hibernate inserts explicit values, so the DB defaults never apply.
    public Account(AccountId id, Email email) {
        this.id = id.value();
        this.email = email;
        this.status = AccountStatus.PENDING;
        this.tokenVersion = 1;
        this.locale = "es-ES";
        this.zoneinfo = "Europe/Madrid";
    }

    public AccountId getId() {
        return new AccountId(id);
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
