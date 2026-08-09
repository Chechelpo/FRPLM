package io.github.chechelpo.frplm.core.prolog;

import io.github.chechelpo.frplm.domain.prolog.arguments.PrologArgumentType;
import it.unibo.tuprolog.solve.Signature;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Defines the set of reserved Prolog predicates utilized by the system, including their
 * string identifiers, descriptions, and expected parameter types. This enumeration
 * standardizes core predicates related to world state, location visibility, and traversal.
 * It also provides utility methods to normalize arbitrary predicate names into valid
 * Prolog identifiers and to determine if a given predicate name is reserved by the system.
 */
public enum ReservedPredicates {
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Character predicates
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    CHARACTER_PRESENT(
            "character_present",
            "Whether this character is present at current tick, with user",
            new PrologArgumentType[]{PrologArgumentType.CHARACTER},
            false
    ),
    CHARACTER_IN_LOCATION(
            "character_at",
            "Whether this character is present at the location at the current tick",
            new PrologArgumentType[]{PrologArgumentType.CHARACTER, PrologArgumentType.LOCATION},
            true
    ),
    CHARACTER_HAS_TAG(
            "character_has_tag",
            "Character with a tag",
            new PrologArgumentType[]{PrologArgumentType.CHARACTER, PrologArgumentType.TAG},
            true
    ),


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Location predicates
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    CURRENT_LOCATION(
            "current_location",
            "The location of the user character at the current tick",
            new PrologArgumentType[]{PrologArgumentType.LOCATION},
            false
    ),
    LOCATION_TRAVERSABLE(
            "traversable",
            "traversable(A,B) means whether the path from A to B is traversable (if exists)",
            new PrologArgumentType[]{PrologArgumentType.LOCATION, PrologArgumentType.LOCATION},
            false
    ),
    LOCATION_VISIBLE(
            "location_visible",
            """
                    Whether the current location has:
                        A) A path to the this location.
                        B) Either name of destination or description of the edge is given.
                    """,
            new PrologArgumentType[]{PrologArgumentType.LOCATION, PrologArgumentType.LOCATION},
            false
    ),

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Extra predicates
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    WORLD(
            "world",
            """
                    Current world's name. ex.: world(eldoria)
                    """,
            new PrologArgumentType[]{PrologArgumentType.WORLD},
            false
    )
    ;

    private static final class Patterns { //Necessary cause of java enum initialization order
        private static final Pattern VALID_PREDICATE =
                Pattern.compile("[a-z][A-Za-z0-9_]*");
    }
    public final String predicate;
    public final String description;
    public final boolean varArg;
    public final PrologArgumentType[] params;

    ReservedPredicates(
            String predicate,
            String description,
            PrologArgumentType[] params,
            boolean varArg
    ){
        Objects.requireNonNull(params);
        Objects.requireNonNull(predicate);
        if (params.length == 0)
            throw new IllegalStateException("Empty params");

        this.predicate = normalizeSystemPredicate(predicate);
        this.params = params;
        this.description = description;
        this.varArg = varArg;
    }

    public int minArity(){
        return params.length;
    }

    @Contract(" -> new")
    public @NonNull Signature asSignature(){
        return new Signature(predicate, minArity(), varArg);
    }

    public static @NonNull String normalizeSystemPredicate(String base) {
        Objects.requireNonNull(base, "base");

        String normalized = base
                .trim()
                .replaceAll("\\s+", "_")
                .toLowerCase(Locale.ROOT);

        if (!Patterns.VALID_PREDICATE.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid Prolog predicate name: " + base
            );
        }

        return  normalized;
    }

    public static boolean isReservedPredicate(String predicate){
        if (predicate == null) return false;
        String normalized = normalizeSystemPredicate(predicate);
        return Arrays.stream(ReservedPredicates.values()).anyMatch(fact -> normalized.equals(fact.predicate));
    }
}
