package io.github.chechelpo.frplm.core.prolog.predicate_asserters;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import it.unibo.tuprolog.core.Fact;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public sealed interface FactProviders permits FactProvidersImpl {
    @NonNull @Unmodifiable List<ReservedPredicates> getImplemented();
    @NonNull @Unmodifiable List<Fact> getFacts(SessionImpl session);
}
