package chechelpo.frplm.frameworks.entities.repository;

import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import org.jooq.TableField;
import org.jooq.TableRecord;

public abstract class Entity<Record extends TableRecord<Record>, Repo extends EntityRepository<Record, ?>> {
    protected final EntityKey<Record> key;
    protected final Repo repository;

    protected Entity(EntityKey<Record> key, Repo repository) {
        this.key = key;
        this.repository = repository;
    }

    public final <T> T get(TableField<Record, T> field) {
        return repository.get(field, key);
    }
}
