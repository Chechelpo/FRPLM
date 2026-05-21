package chechelpo.frplm.domain.world.core.microservices;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.Worlds;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import org.springframework.stereotype.Service;

import java.util.Map;

import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Service
public class WorldService extends EntityService<
        WorldsRecord,
        WorldStore
        > {
    private final LorebookService service;

    WorldService(WorldStore store, LorebookService lorebooks, EventBus eventBus) {
        super(store, eventBus);
        this.service = lorebooks;
    }

    @Override
    protected void beforeCreate(EntityDataPayload<WorldsRecord> data, long operationID) {
        EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
        lorebookData.set(LOREBOOKS.NAME, data.getValue(WORLDS.NAME));
        lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.WORLD_INFO.stable_id);

        data.set(
                Worlds.WORLDS.LOREBOOK_ID,
                service.createAndGet(
                        lorebookData,
                        Lorebooks.LOREBOOKS.ID
                )
        );

        super.beforeCreate(data, operationID);
    }
}
