package io.github.chechelpo.frplm.domain.world.core;


import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
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
