package dev.jmatias.account.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IpAddressTest {

    @Test
    void shouldParseIpv4() {
        assertThat(IpAddress.parse(" 192.168.1.10 ").value().getHostAddress()).isEqualTo("192.168.1.10");
    }

    @Test
    void shouldParseIpv6() {
        assertThat(IpAddress.parse("2001:db8::1").value().getHostAddress()).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void shouldTreatSameAddressAsEqual() {
        assertThat(IpAddress.parse("10.0.0.1")).isEqualTo(IpAddress.parse("10.0.0.1"));
    }

    // Hostnames and legacy short forms must be rejected before reaching InetAddress, which would resolve DNS.
    @ParameterizedTest
    @ValueSource(strings = {"", "localhost", "example.com", "1.2.3", "256.1.1.1", "1.2.3.4.5", "01.2.3.4", "::g", "fe80::1%eth0", "1.2.3.4 "
            + "x"})
    void shouldRejectNonLiterals(String input) {
        assertThatThrownBy(() -> IpAddress.parse(input))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("messageKey", "account.ip.invalid");
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> IpAddress.parse(null))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("messageKey", "account.ip.required");
    }
}
