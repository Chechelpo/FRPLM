package chechelpo.frplm.domain.world.core.utils;

import chechelpo.frplm.annotations.Factory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Factory
public final class WorldFactory extends EntityFactory<WorldsRecord, World, WorldRepository> {
    WorldFactory(WorldRepository repository) {
        super(repository);
    }

    @Contract("_ -> new")
    @Override
    protected @NotNull World instantiate(@NotNull EntityKey<WorldsRecord> key) {
        return new World(key, repository);
    }
}
