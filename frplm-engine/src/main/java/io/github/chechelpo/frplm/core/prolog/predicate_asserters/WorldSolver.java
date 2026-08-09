package io.github.chechelpo.frplm.core.prolog.predicate_asserters;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import it.unibo.tuprolog.core.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.List;

final class WorldSolver implements FactProvider {
    @Override
    public ReservedPredicates getType() {
        return ReservedPredicates.WORLD;
    }

    @Contract("_ -> new")
    @Override
    public @NonNull @Unmodifiable List<Fact> getFacts(@NonNull SessionImpl session) {
        return List.of(Fact.of(
                Struct.of(
                        ReservedPredicates.WORLD.predicate,
                        Atom.of(
                                session.getWorld()
                                        .getName()
                        )
                )
        ));
    }
}
