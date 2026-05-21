package chechelpo.frplm.frameworks.entities.repository;

import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import org.jooq.TableField;
import org.jooq.TableRecord;

public abstract class EntityRepository<R extends TableRecord<R>, Ser extends EntityService<R,?>> {
    protected final Ser service;

    protected EntityRepository(Ser service) {
        this.service = service;
    }
    boolean exists(EntityKey<R> key){
        return service.exists(key);
    }
    public <T> T get(TableField<R,T> field, EntityKey<R> key){
        return service.getValueOf(field, key);
    }
}
