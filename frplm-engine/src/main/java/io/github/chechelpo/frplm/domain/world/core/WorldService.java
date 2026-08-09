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
        if (!data.assigns(WORLDS.LOREBOOK_ID)){
            EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
            lorebookData.set(LOREBOOKS.NAME, data.require(WORLDS.NAME));
            lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.WORLD_INFO.stable_id);

            data.set(
                    Worlds.WORLDS.LOREBOOK_ID,
                    lorebooks.createAndGet(
                            lorebookData,
                            Lorebooks.LOREBOOKS.ID
                    )
            );
        }

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<WorldsRecord> id, long operationID, WorldsRecord record) {
        lorebooks.delete(
                lorebooks.keyOf(lorebooks.getLorebookOf(record))
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }
}
