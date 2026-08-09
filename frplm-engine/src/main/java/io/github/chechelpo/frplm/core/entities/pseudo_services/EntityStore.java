package io.github.chechelpo.frplm.core.entities.pseudo_services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

public abstract class EntityStore<R extends TableRecord<R>>
{
    private final static EnumSet<EntityConfigs.Types> registeredStores = EnumSet.noneOf(EntityConfigs.Types.class);

    protected final DSLContext ctx;
    protected final Table<R> main_table;
    private final EntityConfigs.Types type;
    protected final Logger log;

    protected EntityStore(@NotNull DSLContext ctx, @NotNull Table<R> main_table, @NotNull EntityConfigs.Types type) {
        //if(registeredStores.contains(type))
         //   throw new IllegalStateException(type + " is already registered for store");

        registeredStores.add(type);
        this.ctx = ctx;
        this.main_table = main_table;
        this.type = type;
        this.log = (Logger) LoggerFactory.getLogger(type + "_Store");
        this.log.setLevel(Level.convertAnSLF4JLevel(type.getLoggerLevel()));
    }
    protected EntityStore(@NotNull DSLContext ctx, @NotNull Table<R> main_table, @NotNull EntityConfigs.Types type, boolean registerSingleton) {
        if(registerSingleton && registeredStores.contains(type))
            throw new IllegalStateException(type + " is already registered for store");

        if (!registerSingleton) registeredStores.add(type);
        this.ctx = ctx;
        this.main_table = main_table;
        this.type = type;
        this.log = (Logger) LoggerFactory.getLogger(type + "_Store");
        this.log.setLevel(Level.convertAnSLF4JLevel(type.getLoggerLevel()));
    }

    public EntityConfigs.Types getType(){
        return this.type;
    }

    public Result<R> getAll(){
        Result<R> records = ctx.selectFrom(main_table)
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

    public <T> Result<R> getMatching(TableField<R,T> field, T value) {
        return ctx.selectFrom(main_table)
                .where(field.eq(value))
                .fetch();
    }

    public Result<R> getMatching(EntityDataPayload<R> data){
        return ctx.selectFrom(main_table)
                .where(data.asEqualityCondition())
                .fetch();
    }

    public Result<R> getAllMatching(@NotNull EntityKey<R> id){
        return ctx.selectFrom(main_table)
                .where(id.getEqualityConditions())
                .fetch();
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

    public <T extends Number> Optional<T> getNumericValueForUpdate(
            TableField<R, T> field,
            @NotNull EntityKey<R> key
    ) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(key, "key");

        Record1<T> row = ctx
                .select(field)
                .from(main_table)
                .where(key.getPkCondition())
                .forUpdate()
                .fetchOne();

        if (row == null) {
            return Optional.empty();
        }

        T value = row.value1();

        if (value == null) {
            throw new IllegalStateException(
                    "Cannot adjust null numeric field " +
                            field.getName() +
                            " on entity " +
                            key
            );
        }

        return Optional.of(value);
    }
    public <T extends Number> T adjustAndGet(
            TableField<R, T> field,
            @NotNull EntityKey<R> key,
            int delta
    ) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(key, "key");

        if (delta != 1 && delta != -1) {
            throw new IllegalArgumentException(
                    "Adjustment must be either +1 or -1"
            );
        }

        Field<T> adjustedValue = delta > 0
                ? field.add(delta)
                : field.sub(-delta);

        /*
         * SQL:
         *
         * UPDATE table
         * SET numeric_field = numeric_field +/- 1
         * WHERE ...
         * RETURNING numeric_field
         */
        T newValue = ctx
                .update(main_table)
                .set(field, adjustedValue)
                .where(key.getPkCondition())
                .returningResult(field)
                .fetchOne(field);

        if (newValue == null) {
            /*
             * Because the row was locked earlier in the same transaction,
             * disappearance here indicates an internal consistency problem.
             */
            throw new IllegalStateException(
                    "Numeric adjustment affected no row for entity " +
                            key +
                            " and field " +
                            field.getName()
            );
        }

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
