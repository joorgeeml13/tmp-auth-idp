package dev.jmatias.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.jmatias.account.vo.AccountId;
import dev.jmatias.account.vo.Email;
import dev.jmatias.account.vo.PhoneNumber;
import dev.jmatias.shared.exception.CustomException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
class AccountRepositoryIT {

    @Inject
    AccountRepository repository;

    @Inject
    EntityManager em;

    private Account persist(String email) {
        var account = new Account(AccountId.generate(), new Email(email));
        repository.persist(account);
        repository.flush();
        return account;
    }

    private void sql(String sql, Object... params) {
        var query = em.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        query.executeUpdate();
        em.clear();
    }

    @Test
    void findsByEmailIgnoringCaseAndSpaces() {
        var saved = persist("ana@x.com");

        assertThat(repository.findByEmail(new Email(" Ana@X.com "))).get().extracting(Account::getId).isEqualTo(saved.getId());
        assertThat(repository.findByEmail(new Email("other@x.com"))).isEmpty();
    }

    @Test
    void findsById() {
        var saved = persist("ana@x.com");

        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findById(AccountId.generate())).isEmpty();
    }

    @Test
    void existsByEmail() {
        persist("ana@x.com");

        assertThat(repository.existsByEmail(new Email("ana@x.com"))).isTrue();
        assertThat(repository.existsByEmail(new Email("other@x.com"))).isFalse();
    }

    @Test
    void existsByPhoneNumber() {
        var saved = persist("ana@x.com");
        sql("update account set phone_number = ?1 where id = ?2", "+34600111222", saved.getId().value());

        assertThat(repository.existsByPhoneNumber(new PhoneNumber("+34600111222"))).isTrue();
        assertThat(repository.existsByPhoneNumber(new PhoneNumber("+34600111333"))).isFalse();
    }

    @Test
    void findersSkipSoftDeletedAccounts() {
        var saved = persist("ana@x.com");
        sql("update account set deleted_at = now(), phone_number = ?1 where id = ?2", "+34600111222", saved.getId().value());

        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.findByEmail(new Email("ana@x.com"))).isEmpty();
        assertThat(repository.existsByEmail(new Email("ana@x.com"))).isFalse();
        assertThat(repository.existsByPhoneNumber(new PhoneNumber("+34600111222"))).isFalse();
    }

    // A corrupt row is a server problem: it must not surface as a 400 InvalidValueException.
    @Test
    void corruptEmailInDbIsNotAClientError() {
        var id = AccountId.generate();
        sql("insert into account (id, email) values (?1, ?2)", id.value(), "not-an-email");

        assertThatThrownBy(() -> repository.findById(id)).isNotInstanceOf(CustomException.class);
    }
}
