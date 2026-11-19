package dev.jmatias.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.jmatias.account.vo.AccountId;
import dev.jmatias.account.vo.Email;
import dev.jmatias.account.vo.IpAddress;
import dev.jmatias.account.vo.PhoneNumber;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

/** Checks that Account maps to the V1 migration. Each test rolls back. */
@QuarkusTest
class AccountMappingIT {

    @Inject
    EntityManager em;

    @Test
    @TestTransaction
    void shouldPersistAndReloadEveryValueObject() throws Exception {
        var id = AccountId.generate();
        var account = new Account(id, new Email("  Ana@X.com "));
        // No mutators for these yet; reflection is fine before the entity is managed.
        set(account, "phoneNumber", new PhoneNumber("+34600111222"));
        set(account, "lastLoginIp", IpAddress.parse("2001:db8::1"));
        em.persist(account);
        em.flush();
        em.clear();

        var loaded = em.find(Account.class, id.value());

        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getId().value().version()).isEqualTo(7);
        assertThat(loaded.getEmail().value()).isEqualTo("ana@x.com");
        assertThat(loaded.getPhoneNumber().value()).isEqualTo("+34600111222");
        assertThat(loaded.getLastLoginIp().value().getHostAddress()).isEqualTo("2001:db8:0:0:0:0:0:1");
        assertThat(loaded.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(loaded.getTokenVersion()).isEqualTo(1);
        assertThat(loaded.getLocale()).isEqualTo("es-ES");
        assertThat(loaded.getZoneinfo()).isEqualTo("Europe/Madrid");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @TestTransaction
    void shouldPersistNullableColumnsAsNull() {
        var account = new Account(AccountId.generate(), new Email("nulls@x.com"));
        em.persist(account);
        em.flush();
        em.clear();

        var loaded = em.find(Account.class, account.getId().value());

        assertThat(loaded.getPhoneNumber()).isNull();
        assertThat(loaded.getLastLoginIp()).isNull();
        assertThat(loaded.getLockedUntil()).isNull();
        assertThat(loaded.getLastLogin()).isNull();
        assertThat(loaded.getDeletedAt()).isNull();
    }

    @Test
    @TestTransaction
    void shouldRejectDuplicateEmail() {
        em.persist(new Account(AccountId.generate(), new Email("dup@x.com")));
        em.persist(new Account(AccountId.generate(), new Email("DUP@x.com")));

        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
    }

    @Test
    @TestTransaction
    void shouldIncrementVersionOnUpdate() {
        var account = new Account(AccountId.generate(), new Email("ver@x.com"));
        em.persist(account);
        em.flush();

        account.markEmailVerified();
        em.flush();

        assertThat(account.getVersion()).isEqualTo(1);
    }

    @Test
    @TestTransaction
    void shouldFailOnConcurrentModification() {
        var account = new Account(AccountId.generate(), new Email("lock@x.com"));
        em.persist(account);
        em.flush();
        // Simulates another transaction having already updated the row.
        em.createNativeQuery("update account set version = 9 where id = :id")
                .setParameter("id", account.getId().value())
                .executeUpdate();

        account.markEmailVerified();

        assertThatThrownBy(() -> em.flush()).isInstanceOf(OptimisticLockException.class);
    }

    private static void set(Account account, String field, Object value) throws Exception {
        var f = Account.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(account, value);
    }
}
