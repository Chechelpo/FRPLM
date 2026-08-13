package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;

import static io.github.chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Store
final class ResponseMovementStore extends EntityStore<ResponseLocationChangesRecord> {
    ResponseMovementStore(@NotNull DSLContext ctx) {
        super(ctx, RESPONSE_LOCATION_CHANGES, EntityConfigs.Types.RESPONSE_MOVEMENTS);
    }

    void applyMovementsOfResponse(
            int sessionId,
            int tickNum,
            short responseNum
    ) {
        ctx.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);

            var earlierMovements = MOVEMENTS.as("earlier_movements");

            Field<Integer> previousMovementTick =
                    DSL.select(DSL.max(earlierMovements.AT_TICK))
                            .from(earlierMovements)
                            .where(
                                    earlierMovements.SESSION_ID.eq(
                                            MOVEMENTS.SESSION_ID
                                    )
                            )
                            .and(
                                    earlierMovements.SES_CHARACTER_ID.eq(
                                            MOVEMENTS.SES_CHARACTER_ID
                                    )
                            )
                            .and(
                                    earlierMovements.AT_TICK.lt(
                                            MOVEMENTS.AT_TICK
                                    )
                            )
                            .asField();

            tx.update(SESSION_CHARACTERS)
                    .set(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            MOVEMENTS.PREVIOUS_LOCATION_ID
                    )
                    .set(
                            SESSION_CHARACTERS.LAST_MOVED_TICK_NUM,
                            DSL.coalesce(
                                    previousMovementTick,
                                    DSL.inline(FIRST_MESSAGE_TICK_NUM)
                            )
                    )
                    .from(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionId))
                    .and(MOVEMENTS.AT_TICK.eq(tickNum))
                    .and(
                            SESSION_CHARACTERS.SESSION_ID.eq(
                                    MOVEMENTS.SESSION_ID
                            )
                    )
                    .and(
                            SESSION_CHARACTERS.ID.eq(
                                    MOVEMENTS.SES_CHARACTER_ID
                            )
                    )
                    .and(
                            SESSION_CHARACTERS.WORLD_ID.eq(
                                    MOVEMENTS.WORLD_ID
                            )
                    )
                    .execute();

            tx.update(SESSION_CHARACTERS)
                    .set(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            RESPONSE_LOCATION_CHANGES.LOCATION_ID
                    )
                    .set(
                            SESSION_CHARACTERS.LAST_MOVED_TICK_NUM,
                            tickNum
                    )
                    .from(RESPONSE_LOCATION_CHANGES)
                    .where(
                            RESPONSE_LOCATION_CHANGES.SESSION_ID.eq(sessionId)
                    )
                    .and(
                            RESPONSE_LOCATION_CHANGES.TICK_NUM.eq(tickNum)
                    )
                    .and(
                            RESPONSE_LOCATION_CHANGES.RESPONSE_NUM.eq(
                                    responseNum
                            )
                    )
                    .and(
                            SESSION_CHARACTERS.SESSION_ID.eq(
                                    RESPONSE_LOCATION_CHANGES.SESSION_ID
                            )
                    )
                    .and(
                            SESSION_CHARACTERS.ID.eq(
                                    RESPONSE_LOCATION_CHANGES
                                            .SESSION_CHARACTER_ID
                            )
                    )
                    .and(
                            SESSION_CHARACTERS.WORLD_ID.eq(
                                    RESPONSE_LOCATION_CHANGES.WORLD_ID
                            )
                    )
                    .execute();
        });
    }

}
