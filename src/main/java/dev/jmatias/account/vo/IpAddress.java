package dev.jmatias.account.vo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.regex.Pattern;

public record IpAddress(InetAddress value) {

    private static final Pattern IPV4 =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]+$");

    public IpAddress {
        Objects.requireNonNull(value, "value");
    }

    /** Accepts IP literals only: anything else would make InetAddress hit DNS. */
    public static IpAddress parse(String text) {
        if (text == null) {
            throw new InvalidValueException("account.ip.required");
        }
        String t = text.trim();
        boolean literal = IPV4.matcher(t).matches() || (t.contains(":") && IPV6_CHARS.matcher(t).matches());
        if (!literal) {
            throw new InvalidValueException("account.ip.invalid");
        }
        try {
            return new IpAddress(InetAddress.getByName(t));
        } catch (UnknownHostException e) {
            throw new InvalidValueException("account.ip.invalid", e);
        }
    }
}
