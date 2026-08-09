package io.github.chechelpo.frplm.core.prolog.predicate_asserters;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import it.unibo.tuprolog.core.Fact;

import java.util.List;

interface FactProvider {
    ReservedPredicates getType();
    List<Fact> getFacts(SessionImpl session);
}
