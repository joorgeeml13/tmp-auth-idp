package dev.jmatias.shared.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Resolves a message key to text for a locale; falls back to messages.properties (en), then to the key. */
@ApplicationScoped
public class Messages {

    // No-fallback: an unknown locale must yield the default bundle, not the JVM's default locale.
    private static final ResourceBundle.Control CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    public String get(String key, Locale locale, Object... args) {
        try {
            var pattern = ResourceBundle.getBundle("messages", locale, CONTROL).getString(key);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
