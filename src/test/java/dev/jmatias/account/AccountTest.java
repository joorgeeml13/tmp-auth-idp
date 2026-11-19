package dev.jmatias.account;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jmatias.account.vo.AccountId;
import dev.jmatias.account.vo.Email;
import org.junit.jupiter.api.Test;

class AccountTest {

    private final AccountId id = AccountId.generate();
    private final Account account = new Account(id, new Email("ana@x.com"));

    @Test
    void shouldStartWithMigrationDefaults() {
        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getEmail()).isEqualTo(new Email("ana@x.com"));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(account.getTokenVersion()).isEqualTo(1);
        assertThat(account.getLocale()).isEqualTo("es-ES");
        assertThat(account.getZoneinfo()).isEqualTo("Europe/Madrid");
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.isPhoneVerified()).isFalse();
        assertThat(account.isMfaEnabled()).isFalse();
        assertThat(account.getPhoneNumber()).isNull();
        assertThat(account.getLockedUntil()).isNull();
        assertThat(account.getLastLogin()).isNull();
        assertThat(account.getLastLoginIp()).isNull();
        assertThat(account.getDeletedAt()).isNull();
    }

    @Test
    void shouldSetBothTimestampsOnCreate() {
        account.onCreate();
        assertThat(account.getCreatedAt()).isNotNull().isEqualTo(account.getUpdatedAt());
    }

    @Test
    void shouldOnlyMoveUpdatedAtOnUpdate() throws InterruptedException {
        account.onCreate();
        var created = account.getCreatedAt();
        Thread.sleep(2);
        account.onUpdate();
        assertThat(account.getCreatedAt()).isEqualTo(created);
        assertThat(account.getUpdatedAt()).isAfter(created);
    }

    @Test
    void shouldMarkEmailVerified() {
        account.markEmailVerified();
        assertThat(account.isEmailVerified()).isTrue();
    }
}
