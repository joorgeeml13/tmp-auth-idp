package dev.jmatias.account.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"+34600111222", "+12", "+123456789012345"})
    void shouldAcceptE164(String input) {
        assertThat(new PhoneNumber(input).value()).isEqualTo(input);
    }

    @Test
    void shouldTrim() {
        assertThat(new PhoneNumber(" +34600111222 ").value()).isEqualTo("+34600111222");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "600111222", "+0123456", "+1", "+34 600 111 222", "+34-600", "+1234567890123456", "+34abc"})
    void shouldRejectNonE164(String input) {
        assertThatThrownBy(() -> new PhoneNumber(input)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new PhoneNumber(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
