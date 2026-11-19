package dev.jmatias.account.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountIdTest {

    @Test
    void shouldGenerateUuidV7WithRfcVariant() {
        var uuid = AccountId.generate().value();
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void shouldEmbedCurrentTimestamp() {
        long before = System.currentTimeMillis();
        long embedded = AccountId.generate().value().getMostSignificantBits() >>> 16;
        long after = System.currentTimeMillis();
        assertThat(embedded).isBetween(before, after);
    }

    @Test
    void shouldGenerateUniqueIds() {
        Set<AccountId> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(AccountId.generate());
        }
        assertThat(ids).hasSize(1000);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new AccountId(null)).isInstanceOf(NullPointerException.class);
    }
}
