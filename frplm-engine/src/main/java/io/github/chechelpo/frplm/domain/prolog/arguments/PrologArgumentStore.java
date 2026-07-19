package io.github.chechelpo.frplm.domain.prolog.arguments;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateArgumentRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE_ARGUMENT;

@Store
final class PrologArgumentStore extends EntityStore<PrologPredicateArgumentRecord> {
    PrologArgumentStore(@NotNull DSLContext ctx) {
        super(ctx, PROLOG_PREDICATE_ARGUMENT, EntityConfigs.Types.PROLOG_ARGUMENTS);
    }

    public boolean exchange(int predicateId, short argument1Id, short argument2Id){
        return ctx.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);

            var arguments = tx.selectFrom(PROLOG_PREDICATE_ARGUMENT)
                    .where(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID.eq(predicateId))
                    .and(PROLOG_PREDICATE_ARGUMENT.POSITION.in((short) argument1Id, (short) argument2Id))
                    .forUpdate()
                    .fetch();

            PrologPredicateArgumentRecord argument1 = arguments.stream()
                    .filter(section -> section.getPosition().equals(argument1Id))
                    .findFirst()
                    .orElseThrow();

            PrologPredicateArgumentRecord argument2 = arguments.stream()
                    .filter(section -> section.getPosition().equals(argument2Id))
                    .findFirst()
                    .orElseThrow();

            short position1 = argument1.getPosition();
            short position2 = argument2.getPosition();

            short temporaryPosition;
            try{
                //noinspection DataFlowIssue
                temporaryPosition = (short) (ctx.select(DSL.min(PROLOG_PREDICATE_ARGUMENT.POSITION))
                        .from(PROLOG_PREDICATE_ARGUMENT)
                        .where(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID.eq(predicateId))
                        .fetchOne(0, short.class) - 1);
            } catch (NullPointerException e){
                throw new IllegalStateException("Couldn't get a temporary position");
            }

            tx.update(PROLOG_PREDICATE_ARGUMENT)
                    .set(PROLOG_PREDICATE_ARGUMENT.POSITION, temporaryPosition)
                    .where(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID.eq(predicateId))
                    .and(PROLOG_PREDICATE_ARGUMENT.POSITION.eq(argument1Id))
                    .execute();

            tx.update(PROLOG_PREDICATE_ARGUMENT)
                    .set(PROLOG_PREDICATE_ARGUMENT.POSITION, position1)
                    .where(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID.eq(predicateId))
                    .and(PROLOG_PREDICATE_ARGUMENT.POSITION.eq(argument2Id))
                    .execute();

            tx.update(PROLOG_PREDICATE_ARGUMENT)
                    .set(PROLOG_PREDICATE_ARGUMENT.POSITION, position2)
                    .where(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID.eq(predicateId))
                    .and(PROLOG_PREDICATE_ARGUMENT.POSITION.eq(argument1Id))
                    .execute();

            return true;
        });

    }
}
