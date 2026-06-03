package chechelpo.frplm.frameworks.entities.pseudo_services;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.data.QueryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

public abstract class EntityStore<R extends TableRecord<R>>
{
    private final static EnumSet<EntityTypes.Types> registeredStores = EnumSet.noneOf(EntityTypes.Types.class);

    protected final DSLContext ctx;
    protected final Table<R> main_table;
    private final EntityTypes.Types type;
    protected final Logger log;

    protected EntityStore(@NotNull DSLContext ctx, @NotNull Table<R> main_table, @NotNull EntityTypes.Types type) {
        if(registeredStores.contains(type))
            throw new IllegalStateException(type + " is already registered for store");

        registeredStores.add(type);
        this.ctx = ctx;
        this.main_table = main_table;
        this.type = type;

        this.log = (Logger) LoggerFactory.getLogger(type + "_Store");
        this.log.setLevel(type.getLoggerLevel());
    }

    public EntityTypes.Types getType(){
        return this.type;
    }

    public List<R> getAll(){
        List<R> records = ctx.selectFrom(main_table)
                .fetch();
        log.trace("Retrieved {} records on getAll call", records.size());

        return records;
    }

    public R get(@NotNull EntityKey<R> id){
        R record = ctx.selectFrom(main_table)
                .where(id.getEqualityConditions())
                .fetchOne();
        log.trace("Found {} record with id: {}", record, id);

        return record;
    }
    public List<R> getAllMatching(@NotNull EntityKey<R> id){
        return ctx.selectFrom(main_table)
                .where(id.getEqualityConditions())
                .fetch();
    }

    public List<R> query(@NotNull QueryObject<R> query){
        List<R> records = ctx.selectFrom(main_table)
                .where(query.getConditions())
                .fetch();
        log.trace("Query {} \n result: {}", query, records.size());

        return records;
    }

    public boolean update(@NotNull EntityKey<R> id, @NotNull EntityDataPayload<R> object) {
        log.trace("Updating record with id {} new values: {}", id, object);
        if (object.isEmpty()) {
            log.warn("Empty update order for id {}", id);
            return true;
        }

        return ctx.update(main_table)
                .set(object.assignments())
                .where(id.getEqualityConditions())
                .execute() == 1;
    }

    public boolean delete(@NotNull EntityKey<R> id) {
        log.trace("Deleting record with id {}", id);
        return ctx.deleteFrom(main_table)
                .where(id.getEqualityConditions())
                .execute() == 1;
    }

    public <T> T get(TableField<R, T> field, @NotNull EntityKey<R> id) {
        return ctx.selectFrom(main_table)
                .where(id.getPkCondition())
                .fetchOne(field);
    }

    public void create(@NotNull EntityDataPayload<R> initialData) {
        log.trace("Creating entity {}", initialData);
        ctx.insertInto(main_table)
                .set(initialData.assignments())
                .execute();
    }
    /**
     * Override in concrete stores to simulate triggers (auto-pointer, defaults, auditing, etc.).
     *
     * @param data the UpdateObject that will be inserted (mutable!). Functions as data carrier, not really as an update
     *             in HTTP sense
     * @apiNote {@link #incrementAndGet(TableField, EntityKey)} for those sequence numbers that give out IDs to child entities.
     */
    public @Nullable R createAndGet(@NotNull EntityDataPayload<R> data) {
        log.trace("Creating new record with values: {}", data);
        return ctx.insertInto(main_table)
                .set(data.assignments())
                .returning()
                .fetchOne();
    }
    public <T> @Nullable T createAndGet(EntityDataPayload<R> data, TableField<R,T> field){
        log.trace("Creating new entity with values {} and returning {}", data, field);
        return ctx.insertInto(main_table)
                .set(data.assignments())
                .returning()
                .fetchOne(field);
    }

    public <T extends Number> T incrementAndGet(TableField<R,T> field, @NotNull EntityKey<R> key){
        // Atomic: UPDATE parent SET counter = counter + 1 RETURNING counter + 1
        T newValue = ctx.update(main_table)
                .set(field, field.add(1))  // counter = counter + 1
                .where(key.getPkCondition())
                .returningResult(field)
                .fetchOne(field);

        if (newValue == null)
            throw new IllegalStateException("Something went wrong when updating field " + field);

        return newValue;
    }

    public boolean exists(@NotNull EntityKey<R> id){
        return ctx.fetchExists(
                ctx.selectOne()
                        .from(main_table)
                        .where(id.getEqualityConditions())
        );
    }

}
