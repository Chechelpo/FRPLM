package chechelpo.frplm.domain.world.core;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.Worlds;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Service
public class WorldService extends EntityService<
        WorldsRecord,
        WorldStore
        > {
    private final LorebookService lorebooks;

    WorldService(WorldStore store, LorebookService lorebooks, EventBus eventBus) {
        super(store, eventBus);
        this.lorebooks = lorebooks;
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public WorldsRecord getWorldOf(@NotNull SessionsRecord record) throws EntityNotFound {
        return this.find(EntityKey.of(WORLDS.ID, record.getWorldId()))
                .orElseThrow(() -> new UnexpectedException("This session has no world, which should be impossible", Severity.SYSTEM));
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<WorldsRecord> data, long operationID) {
        EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
        lorebookData.set(LOREBOOKS.NAME, data.requireValue(WORLDS.NAME));
        lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.WORLD_INFO.stable_id);

        data.set(
                Worlds.WORLDS.LOREBOOK_ID,
                lorebooks.createAndGet(
                        lorebookData,
                        Lorebooks.LOREBOOKS.ID
                )
        );

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
