package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.tables.Worlds.WORLDS;

@Component
final class WorldStore extends EntityStore<WorldsRecord> {
    WorldStore(DSLContext ctx) {
        super(ctx, WORLDS, EntityConfigs.Types.WORLDS);
    }
}
