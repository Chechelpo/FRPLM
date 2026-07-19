package io.github.chechelpo.frplm.domain.prolog.predicates;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;

@Store
final class PrologPredicateStore extends EntityStore<PrologPredicateRecord> {
    PrologPredicateStore(@NotNull DSLContext ctx) {
        super(ctx, PROLOG_PREDICATE, EntityConfigs.Types.PROLOG_PREDICATE);
    }
}
