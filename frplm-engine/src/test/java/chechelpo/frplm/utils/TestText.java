package chechelpo.frplm.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class TestText {
    private static final String DEFAULT_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";

    private TestText() {}

    public static @NotNull String randomText(long seed, int minLength, int maxLength) {
        return randomText(seed, minLength, maxLength, DEFAULT_ALPHABET);
    }

    public static @NotNull String randomText(
            long seed,
            int minLength,
            int maxLength,
            String alphabet
    ) {
        if (minLength < 0) {
            throw new IllegalArgumentException("minLength cannot be negative");
        }

        if (maxLength < minLength) {
            throw new IllegalArgumentException("maxLength cannot be smaller than minLength");
        }

        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("alphabet cannot be null or empty");
        }

        Random random = new Random(seed);

        int length = minLength + random.nextInt(maxLength - minLength + 1);

        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(alphabet.length());
            result.append(alphabet.charAt(index));
        }

        return result.toString();
    }
}