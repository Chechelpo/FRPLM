package io.github.chechelpo.frplm.core.prolog.predicate_solvers;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import it.unibo.tuprolog.solve.primitive.Primitive;

public interface PredicateSolver {
    ReservedPredicates getType();
    Primitive getPrimitive();
}
