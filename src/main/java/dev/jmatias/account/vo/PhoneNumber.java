package dev.jmatias.account.vo;

import java.util.regex.Pattern;

/** E.164 format. Only checks the shape, not that the number exists. */
public record PhoneNumber(String value) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    public PhoneNumber {
        if (value == null) {
            throw new InvalidValueException("account.phone.required");
        }
        value = value.trim();
        if (!E164.matcher(value).matches()) {
            throw new InvalidValueException("account.phone.invalid");
        }
    }
}
