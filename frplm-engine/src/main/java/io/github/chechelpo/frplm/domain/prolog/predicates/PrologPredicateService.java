package io.github.chechelpo.frplm.domain.prolog.predicates;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;

@Service
public class PrologPredicateService
        extends EntityService<PrologPredicateRecord, PrologPredicateStore> {

    PrologPredicateService(
            @NotNull PrologPredicateStore store,
            FieldValidator<PrologPredicateRecord> validator,
            @NotNull EventBus eventBus
    ) {
        super(store, validator, eventBus);
    }

    @Override
    protected void beforeCreate(EntityDataPayload<PrologPredicateRecord> data, long operationID) {
        if (data.assigns(PROLOG_PREDICATE.ARITY))
            throw new InvalidValue("Can't assign arity on create, its a derived value");
        if (data.require(PROLOG_PREDICATE.NAME).trim().startsWith("frplm"))
            throw new InvalidValue("Can't use reserved prefix frplm_ for predicates");
        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<PrologPredicateRecord> target, EntityDataPayload<PrologPredicateRecord> data, long operationID) {
        if (data.require(PROLOG_PREDICATE.NAME).trim().startsWith("frplm"))
            throw new InvalidValue("Can't use reserved prefix frplm_ for predicates");
        super.beforeUpdate(target, data, operationID);
    }

    private static String signature(String name, short arity) {
        return name + "/" + arity;
    }
}