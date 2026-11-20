package dev.jmatias.account;

import dev.jmatias.account.vo.AccountId;
import dev.jmatias.account.vo.Email;
import dev.jmatias.account.vo.PhoneNumber;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/** Only use cases inject this. Finders skip soft-deleted accounts so no caller has to remember to. */
@ApplicationScoped
public class AccountRepository implements PanacheRepositoryBase<Account, UUID> {

    public Optional<Account> findById(AccountId id) {
        return find("id = ?1 and deletedAt is null", id.value()).firstResultOptional();
    }

    public Optional<Account> findByEmail(Email email) {
        return find("email = ?1 and deletedAt is null", email).firstResultOptional();
    }

    public boolean existsByEmail(Email email) {
        return count("email = ?1 and deletedAt is null", email) > 0;
    }

    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        return count("phoneNumber = ?1 and deletedAt is null", phoneNumber) > 0;
    }
}
