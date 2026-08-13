package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;

import static io.github.chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
import static io.github.chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSION_CHARACTERS;

@Store
final class MovementStore extends EntityStore<MovementsRecord> {
    MovementStore(@NotNull DSLContext ctx) {
        super(ctx, MOVEMENTS, EntityConfigs.Types.MOVEMENTS);
    }

    public void rollbackLatestMovementsAt(int sessionId, int tick) {
        ctx.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);

            List<MovementsRecord> movements = tx.selectFrom(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionId)
                            .and(MOVEMENTS.AT_TICK.eq(tick)))
                    .orderBy(MOVEMENTS.SES_CHARACTER_ID)
                    .forUpdate()
                    .fetch();

            for (MovementsRecord movement : movements) {
                Integer previousMovementTick = tx.selectFrom(MOVEMENTS)
                        .where(MOVEMENTS.SESSION_ID.eq(sessionId)
                                .and(MOVEMENTS.SES_CHARACTER_ID.eq(
                                        movement.getSesCharacterId()))
                                .and(MOVEMENTS.AT_TICK.lt(tick)))
                        .orderBy(MOVEMENTS.AT_TICK.desc())
                        .limit(1)
                        .fetchOne(MOVEMENTS.AT_TICK);

                if (previousMovementTick == null) {
                    previousMovementTick = FIRST_MESSAGE_TICK_NUM;
                }

                int updated = tx.update(SESSION_CHARACTERS)
                        .set(
                                SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                                movement.getPreviousLocationId()
                        )
                        .set(
                                SESSION_CHARACTERS.LAST_MOVED_TICK_NUM,
                                previousMovementTick
                        )
                        .where(SESSION_CHARACTERS.SESSION_ID.eq(sessionId)
                                .and(SESSION_CHARACTERS.ID.eq(
                                        movement.getSesCharacterId()))
                                .and(SESSION_CHARACTERS.WORLD_ID.eq(
                                        movement.getWorldId()))
                                .and(SESSION_CHARACTERS.LAST_MOVED_TICK_NUM.eq(tick)))
                        .execute();

                if (updated != 1) {
                    throw new IllegalStateException(
                            "Cannot rollback movement for session character "
                                    + movement.getSesCharacterId()
                                    + " at tick " + tick
                                    + ": it is not the character's latest movement"
                    );
                }
            }

            int deleted = tx.deleteFrom(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionId)
                            .and(MOVEMENTS.AT_TICK.eq(tick)))
                    .execute();

            if (deleted != movements.size()) {
                throw new IllegalStateException(
                        "Movement history changed concurrently during rollback"
                );
            }
        });
    }
}
