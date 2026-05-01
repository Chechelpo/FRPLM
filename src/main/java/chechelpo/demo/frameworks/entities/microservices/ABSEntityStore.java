package chechelpo.demo.frameworks.entities.microservices;

import ch.qos.logback.classic.Logger;
import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.exceptions.Severity;
import chechelpo.demo.exceptions.types.NotFound;
import chechelpo.demo.frameworks.entities.data.QueryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

public abstract class ABSEntityStore<R extends TableRecord<R>>
{
    private final static EnumSet<EntityTypes.Types> registeredStores = EnumSet.noneOf(EntityTypes.Types.class);

    protected final DSLContext ctx;
    private final Table<R> main_table;
    private final Logger log;

    protected ABSEntityStore(@NotNull DSLContext ctx, @NotNull Table<R> main_table, @NotNull EntityTypes.Types type) {
        if(registeredStores.contains(type))
            throw new IllegalStateException(type + " is already registered for store");

        registeredStores.add(type);
        this.ctx = ctx;
        this.main_table = main_table;

        this.log = (Logger) LoggerFactory.getLogger(type + "_Store");
    }

    protected List<R> getAll(){
        List<R> records = ctx.selectFrom(main_table)
                .fetch();
        log.trace("Retrieved {} records on getAll call", records.size());

        return records;
    }

    protected R get(@NotNull EntityKey<R> id){
        R record = ctx.selectFrom(main_table)
                .where(id.getEqualityConditions())
                .fetchOne();
        log.trace("Found {} record with id: {}", record, id);

        return record;
    }
    protected List<R> getAllMatching(@NotNull EntityKey<R> id){
        return ctx.selectFrom(main_table)
                .where(id.getEqualityConditions())
                .fetch();
    }


    protected List<R> query(@NotNull QueryObject<R> query){
        List<R> records = ctx.selectFrom(main_table)
                .where(query.getConditions())
                .fetch();
        log.trace("Query {} \n result: {}", query, records.size());

        return records;
    }

    protected boolean update(@NotNull EntityKey<R> id, @NotNull EntityDataPayload<R> object) {
        log.trace("Updating record with id {} new values: {}", id, object);
        if (object.isEmpty()) {
            log.warn("Empty update order for id {}", id);
            return true;
        }
        if (!exists(id)) {
            log.error("No record with id {}", id);
            throw new NotFound(id.toFolderName(), Severity.USER);
        }

        return ctx.update(main_table)
                .set(object.assignments())
                .where(id.getEqualityConditions())
                .execute() == 1;
    }

    protected boolean delete(@NotNull EntityKey<R> id) {
        log.trace("Deleting record with id {}", id);
        if (!exists(id)) throw new NotFound(id.toFolderName(), Severity.USER);
        return ctx.deleteFrom(main_table)
                .where(id.getEqualityConditions())
                .execute() == 1;
    }

    /**
     * Override in concrete stores to simulate triggers (auto-pointer, defaults, auditing, etc.).
     *
     * @param data the UpdateObject that will be inserted (mutable!). Functions as data carrier, not really as an update
     *             in HTTP sense
     * @apiNote {@link #getAndIncrement(TableField, EntityKey)} for those sequence numbers that give out IDs to child entities.
     */
    protected @Nullable R createAndGet(@NotNull EntityDataPayload<R> data) {
        log.trace("Creating new record with values: {}", data);
        return ctx.insertInto(main_table)
                .set(data.assignments())
                .returning()
                .fetchOne();
    }
    protected <T> @Nullable T createAndGet(EntityDataPayload<R> data, TableField<R,T> field){
        log.trace("Creating new entity with values {} and returning {}", data, field);
        return ctx.insertInto(main_table)
                .set(data.assignments())
                .returning()
                .fetchOne(field);
    }

    protected <T extends Number> T getAndIncrement(TableField<R,T> field, @NotNull EntityKey<R> key){
        if (!exists(key)) throw new NotFound(key.toFolderName(), Severity.USER);

        // Atomic: UPDATE parent SET counter = counter + 1 RETURNING counter
        T newValue = ctx.update(main_table)
                .set(field, field.add(1))  // counter = counter + 1
                .where(key.getPkCondition())
                .returningResult(field)
                .fetchOne(field);

        if (newValue == null)
            throw new IllegalStateException("Something went wrong when updating field, record disparaged");

        return newValue;
    }

    protected boolean exists(@NotNull EntityKey<R> id){
        return ctx.fetchExists(
                ctx.selectOne()
                        .from(main_table)
                        .where(id.getEqualityConditions())
        );
    }

}
