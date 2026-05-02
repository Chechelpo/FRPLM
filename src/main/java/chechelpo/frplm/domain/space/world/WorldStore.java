package chechelpo.frplm.domain.space.world;


import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.tables.Worlds.WORLDS;

@Component
final class WorldStore extends ABSEntityStore<WorldsRecord> {
    WorldStore(DSLContext ctx) {
        super(ctx, WORLDS, EntityTypes.Types.WORLDS);
    }
}
