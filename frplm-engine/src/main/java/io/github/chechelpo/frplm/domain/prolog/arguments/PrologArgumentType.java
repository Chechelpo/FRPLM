package io.github.chechelpo.frplm.domain.prolog.arguments;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public enum PrologArgumentType {
    CHARACTER("frplm_character"),
    LOCATION("frplm_location"),
    WORLD("frplm_world"),
    REGION("frplm_region"),
    TAG("frplm_tag"),
    ENTRY("frplm_entry"),
    INTEGER("integer"),
    NUMBER("number"),
    TEXT("frplm_check_text"),
    BOOLEAN("frplm_check_bool"),
    TERM(null);

    public final String checkerPredicate;

    PrologArgumentType(String checkerPredicate){
        this.checkerPredicate = checkerPredicate;
    }

    @Contract(pure = true)
    public @NonNull Optional<String> getCheckerPredicate(){
        return Optional.ofNullable(this.checkerPredicate);
    }

    public String createGuard(String argumentName) {
        return getCheckerPredicate()
                .map(predicate -> predicate + "(" + argumentName + ")")
                .orElse("true");
    }

    @Contract(pure = true)
    public @NonNull String getTableValue() {
        return name();
    }
}