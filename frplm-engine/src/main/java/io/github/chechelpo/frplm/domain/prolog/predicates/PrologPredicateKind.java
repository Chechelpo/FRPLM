package io.github.chechelpo.frplm.domain.prolog.predicates;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public enum PrologPredicateKind {
    SYSTEM,
    USER,
    EXTENSION,
    BUILTIN
    ;

    @Contract(pure = true)
    @NonNull String getTableName(){
        return this.name();
    }
}
