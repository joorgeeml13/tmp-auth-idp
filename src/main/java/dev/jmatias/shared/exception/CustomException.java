package dev.jmatias.shared.exception;

import lombok.Getter;

/** Base for business exceptions: carries an i18n key, its args and the HTTP status the handler will answer with. */
@Getter
public abstract class CustomException extends RuntimeException {

    private final int status;
    private final String messageKey;
    private final transient Object[] args;

    protected CustomException(int status, String messageKey, Object... args) {
        this(status, messageKey, null, args);
    }

    // The cause goes to RuntimeException so the original stack trace reaches the logs.
    protected CustomException(int status, String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }
}
