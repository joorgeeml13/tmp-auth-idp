package dev.jmatias.account.vo;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public AccountId {
        Objects.requireNonNull(value, "value");
    }

    // shortcut: UUIDv7 without monotonic counter, ids created in the same ms are not ordered; upgrade if strict ordering matters.
    public static AccountId generate() {
        long msb = (System.currentTimeMillis() << 16) | 0x7000L | (RANDOM.nextLong() & 0x0FFFL);
        long lsb = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new AccountId(new UUID(msb, lsb));
    }
}
