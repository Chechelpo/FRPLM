package chechelpo.demo.domain.space.world;


import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.jooq.generated.tables.records.WorldsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.demo.jooq.generated.tables.Worlds.WORLDS;

@Component
final class WorldStore extends ABSEntityStore<WorldsRecord> {
    WorldStore(DSLContext ctx) {
        super(ctx, WORLDS, EntityTypes.Types.WORLDS);
    }
}
