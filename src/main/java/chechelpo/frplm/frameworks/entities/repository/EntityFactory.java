package chechelpo.frplm.frameworks.entities.repository;

import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.InvalidKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableRecord;

public abstract class EntityFactory<
        Record extends TableRecord<Record>,
        Ent extends Entity<Record, Repo>,
        Repo extends EntityRepository<Record, ?>
        > {
    protected final Repo repository;
    public EntityFactory(Repo repository) {
        this.repository = repository;
    }

    @Contract("_ -> new")
    public final Ent create(@NotNull EntityKey<Record> key) {
        if (!repository.exists(key))
            throw new InvalidKey("Entity doesn't exist when creating", Severity.SYSTEM);
        return instantiate(key);
    }
    @Contract("_ -> new")
    protected abstract Ent instantiate(@NotNull EntityKey<Record> key);
}
