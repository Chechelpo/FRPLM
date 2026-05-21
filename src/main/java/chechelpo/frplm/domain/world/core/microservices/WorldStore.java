package chechelpo.frplm.domain.world.core.microservices;


import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.tables.Worlds.WORLDS;

@Component
final class WorldStore extends EntityStore<WorldsRecord> {
    WorldStore(DSLContext ctx) {
        super(ctx, WORLDS, EntityTypes.Types.WORLDS);
    }
}
