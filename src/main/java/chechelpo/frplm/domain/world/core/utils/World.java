package chechelpo.frplm.domain.world.core.utils;

import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;

public final class World extends Entity<WorldsRecord, WorldRepository> {
    World(EntityKey<WorldsRecord> key, WorldRepository repository) {
        super(key, repository);
    }
}
