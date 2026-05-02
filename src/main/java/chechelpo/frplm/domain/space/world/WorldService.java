package chechelpo.frplm.domain.space.world;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.Worlds;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public final class WorldService extends ABSEntityService<
        WorldsRecord,
        WorldStore
        >
{
    private LorebookService service;
    WorldService(WorldStore store, LorebookService lorebooks) {
        super(store, EntityTypes.Types.WORLDS);
        this.service = lorebooks;
    }

    @Override
    public @NotNull WorldsRecord createAndGet(@NotNull EntityDataPayload<WorldsRecord> data) {
        data.setValue(
                Worlds.WORLDS.LOREBOOK_ID,
                service.createAndGet(EntityDataPayload.fromValues(Map.of(
                    Lorebooks.LOREBOOKS.NAME , data.getValue(Worlds.WORLDS.NAME)
                    ))
                    ,
                        Lorebooks.LOREBOOKS.ID
                )
        );
        return super.createAndGet(data);
    }
}
