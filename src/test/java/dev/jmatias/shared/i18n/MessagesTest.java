package dev.jmatias.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private final Messages messages = new Messages();

    @Test
    void resolvesSpanish() {
        assertThat(messages.get("error.internal", Locale.forLanguageTag("es"))).isEqualTo("Error interno del servidor");
    }

    @Test
    void resolvesEnglish() {
        assertThat(messages.get("error.internal", Locale.ENGLISH)).isEqualTo("Internal server error");
    }

    @Test
    void unknownLocaleFallsBackToDefaultBundleNotJvmLocale() {
        var original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("es"));
        try {
            assertThat(messages.get("error.internal", Locale.FRENCH)).isEqualTo("Internal server error");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void unknownKeyReturnsKey() {
        assertThat(messages.get("nope.nope", Locale.ENGLISH)).isEqualTo("nope.nope");
    }
}
