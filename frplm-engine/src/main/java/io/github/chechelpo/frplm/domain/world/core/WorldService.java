package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.Worlds;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Service
public class WorldService extends EntityService<
        WorldsRecord,
        WorldStore
        > {
    private final LorebookService lorebooks;

    WorldService(
            WorldStore store,
            FieldValidator<WorldsRecord> validator,
            LorebookService lorebooks,
            EventBus eventBus
    ) {
        super(store, validator, eventBus);
        this.lorebooks = lorebooks;
    }

    public EntityKey<WorldsRecord> keyOf(int worldId){
        return EntityKey.of(WORLDS.ID, worldId);
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public WorldsRecord getWorldOf(@NotNull SessionsRecord record) throws EntityNotFound {
        return this.find(EntityKey.of(WORLDS.ID, record.getWorldId()))
                .orElseThrow(notFound -> new UnexpectedException(
                        "This session has no world, which should be impossible " + notFound.toDebugString(),
                        Severity.SYSTEM
                        )
                );
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<WorldsRecord> data, long operationID) {
        data.ifUnassignedGet(
                WORLDS.LOREBOOK_ID,
                () -> lorebooks.createAndGet(
                            EntityDataPayload.<LorebooksRecord>builder()
                                    .set(LOREBOOKS.NAME, data.requireNonNull(WORLDS.NAME))
                                    .set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.WORLD_INFO.stable_id)
                                    .build(),
                            Lorebooks.LOREBOOKS.ID
                    )
        );

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void afterSuccessfulUpdate(WorldsRecord previousData, EntityKey<WorldsRecord> key, EntityDataPayload<WorldsRecord> updated, long operationID) {
        updated.getAssignment(WORLDS.NAME)
                .ifAssignedNotNull(
                        newWorldName -> lorebooks.update(
                                EntityKey.of(LOREBOOKS.ID, previousData.getLorebookId()),
                                EntityDataPayload.of(LOREBOOKS.NAME, newWorldName)
                        )
                );

        super.afterSuccessfulUpdate(previousData, key, updated, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<WorldsRecord> id, long operationID, WorldsRecord record) {
        lorebooks.delete(
                EntityKey.of(LOREBOOKS.ID, record.getLorebookId())
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }
}
