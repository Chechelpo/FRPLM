package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class QualifiedNames {

    private static final String DELIMITER = ".";

    private QualifiedNames() {}

    /* =====================================================================
     * Two parts
     * ================================================================== */

    public static String qualify(
            String first,
            String second
    ) {
        return qualify(new String[]{first, second});
    }

    public static TwoParts splitTwo(String qualifiedName) {
        List<String> parts = split(qualifiedName, 2);

        return new TwoParts(
                parts.get(0),
                parts.get(1)
        );
    }

    public record TwoParts(
            String first,
            String second
    ) {}

    /* =====================================================================
     * Three parts
     * ================================================================== */

    public static String qualify(
            String first,
            String second,
            String third
    ) {
        return qualify(new String[]{first, second, third});
    }

    public static ThreeParts splitThree(String qualifiedName) {
        List<String> parts = split(qualifiedName, 3);

        return new ThreeParts(
                parts.get(0),
                parts.get(1),
                parts.get(2)
        );
    }

    public record ThreeParts(
            String first,
            String second,
            String third
    ) {}

    /* =====================================================================
     * N parts
     * ================================================================== */

    public static String qualify(String... parts) {
        Objects.requireNonNull(parts, "parts");

        if (parts.length == 0) {
            throw new IllegalArgumentException(
                    "At least one name part is required"
            );
        }

        return Arrays.stream(parts)
                .map(QualifiedNames::validatePart)
                .reduce((left, right) -> left + DELIMITER + right)
                .orElseThrow();
    }

    public static List<String> split(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Qualified name cannot be blank"
            );
        }

        return Arrays.stream(
                        qualifiedName.split("\\.", -1)
                )
                .map(QualifiedNames::validatePart)
                .toList();
    }

    public static List<String> split(
            String qualifiedName,
            int expectedParts
    ) {
        if (expectedParts < 1) {
            throw new IllegalArgumentException(
                    "expectedParts must be greater than zero"
            );
        }

        List<String> parts = split(qualifiedName);

        if (parts.size() != expectedParts) {
            throw new IllegalArgumentException(
                    "Expected "
                            + expectedParts
                            + " name parts, but received "
                            + parts.size()
            );
        }

        return parts;
    }

    private static String validatePart(String part) {
        Objects.requireNonNull(part, "Name part");

        String normalized = part.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Name parts cannot be blank"
            );
        }

        if (normalized.contains(DELIMITER)) {
            throw new IllegalArgumentException(
                    "Individual name parts cannot contain '.'"
            );
        }

        return normalized;
    }
}