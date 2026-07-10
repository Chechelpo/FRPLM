package io.github.chechelpo.frplm.domain.prolog;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public enum PrologArgumentTypes {
    CHARACTER,
    LOCATION,
    WORLD,
    REGION,
    TAG,
    ENTRY,
    INTEGER,
    NUMBER,
    TEXT,
    BOOLEAN,
    TERM;

    @Contract(pure = true)
    public @NonNull String getTableValue() {
        return name();
    }
}