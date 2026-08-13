package io.github.chechelpo.frplm.utils.matching;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class Outlet extends Macro {
    private static final String PREFIX = "outlet:";

    public Outlet(String name) {
        super(getNormalizedOutlet(name));
    }

    static String getNormalizedOutlet(@NonNull String name) {
        String normalized = getNormalized(
                Objects.requireNonNull(name)
        );

        return normalized.regionMatches(
                true,
                0,
                PREFIX,
                0,
                PREFIX.length()
        )
                ? PREFIX + normalized.substring(PREFIX.length())
                : PREFIX + normalized;
    }
}

