package io.github.chechelpo.frplm.domain.prolog.arguments;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.prolog.predicates.PrologPredicateService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.ExpectedField;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateArgumentRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;
import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE_ARGUMENT;

@Component
public class PrologArgumentService extends EntityService<PrologPredicateArgumentRecord, PrologArgumentStore> {
    private final PrologPredicateService prologPredicateService;

    PrologArgumentService(
            @NonNull PrologArgumentStore store,
            FieldValidator<PrologPredicateArgumentRecord> validator,
            @NotNull EventBus eventBus,
            PrologPredicateService prologPredicateService
    ) {
        super(store, validator, eventBus);
        this.prologPredicateService = prologPredicateService;
    }

    @Override
    protected void beforeCreate(@NonNull EntityDataPayload<PrologPredicateArgumentRecord> data, long operationID) {
        if (!data.assigns(PROLOG_PREDICATE_ARGUMENT.POSITION)) setDefaultPosition(data);
        super.beforeCreate(data, operationID);
    }
    private void setDefaultPosition(@NonNull EntityDataPayload<PrologPredicateArgumentRecord> data) {
        data.set(
                PROLOG_PREDICATE_ARGUMENT.POSITION,
                prologPredicateService.incrementAndGet(
                        PROLOG_PREDICATE.ARITY,
                        EntityKey.of(
                                PROLOG_PREDICATE.ID,
                                data.getAssignment(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID)
                                        .orElseThrow(a ->
                                                new ExpectedField("New prolog predicate argument has no predicate parent id", Severity.USER)
                                        )
                                )
                ).orElseThrow(() -> new EntityNotFound("Couldn't find parent predicate", Severity.USER))
        );
    }
    public EntityKey<PrologPredicateArgumentRecord> keyOf(int predicateId, short argumentId){
        return EntityKey.<PrologPredicateArgumentRecord>builder()
                .set(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID, predicateId)
                .set(PROLOG_PREDICATE_ARGUMENT.POSITION, argumentId)
                .build();
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<PrologPredicateArgumentRecord> id, long operationID, PrologPredicateArgumentRecord record) {
        prologPredicateService.decrementAndGet(
                PROLOG_PREDICATE.ARITY,
                EntityKey.of(PROLOG_PREDICATE.ID, id.require(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID))
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }

    @Transactional
    public boolean exchange(int predicateId, short argument1Id, short argument2Id) {
        if (!exists(keyOf(predicateId, argument1Id)))
            throw new EntityNotFound(
                    "No argument with (predicateId %s, argument %s) ".formatted(predicateId, argument1Id),
                    Severity.USER
            );
        if (!exists(keyOf(predicateId, argument2Id)))
            throw new EntityNotFound(
                    "No argument with (predicateId %s, argument %s".formatted(predicateId, argument2Id),
                    Severity.USER
            );

        return store.exchange(predicateId, argument1Id, argument2Id);
    }
}
