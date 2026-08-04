package io.github.chechelpo.frplm.core.prolog.predicate_asserters;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import it.unibo.tuprolog.core.Atom;
import it.unibo.tuprolog.core.Fact;
import it.unibo.tuprolog.core.Struct;
import org.jspecify.annotations.NonNull;

import java.util.List;

final class CurrentLocation implements FactProvider {
    @Override
    public ReservedPredicates getType() {
        return ReservedPredicates.CURRENT_LOCATION;
    }

    @Override
    public List<Fact> getFacts(@NonNull SessionImpl session) {
        return List.of(Fact.of(
                Struct.of(
                        ReservedPredicates.CURRENT_LOCATION.predicate,
                        Atom.of(
                                session.getUserCharacter()
                                        .getCurrentLocation()
                                        .asReference()
                                        .encode()
                        )
                )
        ));
    }
}
