package dev.jmatias.account.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void shouldTrimAndLowercase() {
        assertThat(new Email("  Ana.Lopez+Tag@Example.COM ").value()).isEqualTo("ana.lopez+tag@example.com");
    }

    @Test
    void shouldTreatDifferentCasesAsEqual() {
        assertThat(new Email("A@x.com")).isEqualTo(new Email("a@X.com"));
    }

    @Test
    void shouldAcceptMaxLength() {
        String local = "a".repeat(255 - "@x.com".length());
        assertThat(new Email(local + "@x.com").value()).hasSize(255);
    }

    @Test
    void shouldRejectTooLong() {
        String local = "a".repeat(256 - "@x.com".length());
        assertThatThrownBy(() -> new Email(local + "@x.com")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "plain", "a@b", "@x.com", "a@", "a b@x.com", "a@@x.com", "a@x .com"})
    void shouldRejectInvalidFormat(String input) {
        assertThatThrownBy(() -> new Email(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
