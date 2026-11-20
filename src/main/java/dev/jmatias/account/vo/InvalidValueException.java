package dev.jmatias.account.vo;

import dev.jmatias.shared.exception.CustomException;

/** A value object rejected its input (missing or malformed): answered as 400. */
public class InvalidValueException extends CustomException {

    public InvalidValueException(String messageKey) {
        super(400, messageKey);
    }

    public InvalidValueException(String messageKey, Throwable cause) {
        super(400, messageKey, cause);
    }
}
