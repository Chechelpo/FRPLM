package io.github.chechelpo.frplm.core.entities.pseudo_services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldActionResult;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.events.crud.CRUDDraftEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.*;
import io.github.chechelpo.frplm.utils.ValidationResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public abstract class EntityService<
        R extends TableRecord<R>,
        Store extends EntityStore<R>
        > implements EntityReader<R>, EntityUpdater<R>, EntityCreator<R> {
    protected final EventBus eventBus;

    protected final Store store;
    protected final Logger log;
    protected final FieldValidator<R> fieldValidator;

    private final Table<R> mainTable;

    public EntityService(@NotNull Store store, FieldValidator<R> validator, @NotNull EventBus eventBus) {
        this.mainTable = store.getMainTable();

        this.eventBus = eventBus;
        this.store = store;
        this.fieldValidator = validator;

        log = (Logger) LoggerFactory.getLogger(mainTable.getUnqualifiedName() + "_Service");
        log.setLevel(Level.convertAnSLF4JLevel(org.slf4j.event.Level.INFO));
    }

    @Override
    public Table<R> getTable(){
        return fieldValidator.getTable();
    }

    public FieldValidator<R> getFieldValidator() {
        return fieldValidator;
    }

    public EntityKey<R> keyOf(R record) {
        return fieldValidator.keyOf(record);
    }

    public void setLogLevel(Level level){
        this.log.setLevel(level);
    }

    public List<EntityKey<R>> keysOf(List<R> records) {
        return records.stream().map(this::keyOf).toList();
    }

    @Override
    public ValidationResult validateKeyStructure(EntityKey<R> key) {
        FieldActionResult<R, EntityKey<R>> error = fieldValidator.validateFullKey(key);
        if (error.isFailure()) return ValidationResult.error(error.debugString());
        return ValidationResult.success();
    }

    @Override
    public Set<TableField<R, ?>> ignoreFieldsOnCreationOrder() {
        return Set.of();
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CREATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Before entity creation hook. Meant as a way to inject mandatory data.
     *
     * @param data        the initial entity data
     * @param operationID of the creation event
     * @apiNote super() call is advised at the end of your override. This function also contains the event emit as well as validation logic
     */
    protected void beforeCreate(EntityDataPayload<R> data, long operationID) {
        EntityKey<R> initialKey = fieldValidator.extractKeysFrom(data);

        eventBus.publish(new CRUDDraftEvent.CreateEntityDraft<R>(mainTable, operationID, initialKey, data));
        fieldValidator.validateDataCreationPayload(data).orElseThrow();
    }

    @Transactional
    public @NotNull R createAndGet(EntityDataPayload<R> data) {
        Objects.requireNonNull(data, "data");
        long operationID = eventBus.nextOperationID();
        beforeCreate(data, operationID);
        R result = null;
        try {
            result = store.createAndGet(data);
        } catch (Exception e) {
            log.error("Error creating entity with data {} \n {} \n Trace: \n", data.tableString(), e.getMessage());
            throw new UnexpectedException("New entity creation failed with data:  \n" + data.tableString(), Severity.SYSTEM);
        }

        afterSuccessfulCreate(result, operationID);
        return result;
    }

    /**
     * @apiNote creates directly, no validation. Assumes caller has done it already
     */
    protected void unsafeCreate(EntityDataPayload<R> data) {
        this.store.create(data);
    }

    @Transactional
    public <T> @NotNull T createAndGet(EntityDataPayload<R> data, TableField<R, T> field) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(field);
        long operationID = eventBus.nextOperationID();
        beforeCreate(data, operationID);

        R result = store.createAndGet(data);
        if (result == null) {
            log.error("Could not create and fetch new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }

        afterSuccessfulCreate(result, operationID);
        return result.get(field);
    }

    protected void afterSuccessfulCreate(R data, long operationID) {
        eventBus.publish(
                new CRUDCommittedEvent.CreatedEntity<R>(operationID, keyOf(data), data)
        );
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // RETRIEVE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected void beforeRetrieve(@Nullable EntityKey<R> key, long operationID) {
        if (key != null)
            fieldValidator.validateFullKey(key)
                    .ifUnknownFieldThrow("Non key field")
                    .orElseThrow("Error while checking key while retrieving: \n" + key.tableString());
    }

    @Override
    @Transactional(readOnly = true)
    public RecordFindResult<R> find(EntityKey<R> key) {
        Objects.requireNonNull(key);
        long operationID = eventBus.nextOperationID();
        beforeRetrieve(key, operationID);
        log.debug("Finding record with id \n{}", key.tableString());

        R record = store.get(key);
        if (record == null) return new RecordFindResult.NotFound<>(key);
        afterRetrieve(List.of(record), operationID);

        return new RecordFindResult.Found<>(key, record);
    }

    @Transactional(readOnly = true)
    public Result<R> getAll() {
        Result<R> records = store.getAll();
        afterRetrieve(records, 0);
        return records;
    }

    @Transactional(readOnly = true)
    public Result<R> getMatching(EntityKey<R> key) {
        Objects.requireNonNull(key);
        fieldValidator.validateKey(key).orElseThrow("Error when fetching matching entity");

        Result<R> records = store.getAllMatching(key);
        afterRetrieve(records, 0);

        return records;
    }

    @Override
    public Result<R> getMatching(EntityDataPayload<R> target) {
        Objects.requireNonNull(target, "Target data is null");
        Result<R> records;

        try {
            records = store.getMatching(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        afterRetrieve(records, 0);
        return records;
    }

    @Override
    public OneMatchingResult<R> getOneMatching(EntityDataPayload<R> target) {
        return getOneMatchingResult(target);
    }

    @Override
    public <T> OneMatchingResult<R> getOneMatching(TableField<R, T> field, T value) {
        EntityDataPayload<R> target = EntityDataPayload.of(field, value);
        return getOneMatchingResult(target);
    }

    private EntityReader.OneMatchingResult<R> getOneMatchingResult(EntityDataPayload<R> target) {
        Result<R> result = store.getMatching(target);
        if (result.size() > 1)
            return new OneMatchingResult.MoreThanOne<>(target, result.size());

        return result.isEmpty() ?
                new OneMatchingResult.Empty<>(target) :
                new OneMatchingResult.Present<>(target, result.getFirst());
    }

    @Override
    public <T> Result<R> getMatching(TableField<R, T> field, T value) {
        return store.getMatching(field, value);
    }

    @Transactional(readOnly = true)
    public boolean exists(R record) {
        return exists(keyOf(record));
    }

    /**
     * @param k key of the entity
     * @return true if registered in store, false otherwise
     * @implNote does not throw {@link EntityNotFound}, that's in charge of the caller
     */
    @Transactional(readOnly = true)
    public boolean exists(EntityKey<R> k) {
        Objects.requireNonNull(k);
        fieldValidator.validateFullKey(k).orElseThrow("This key does not have a valid structure");
        return store.exists(k);
    }

    protected void afterRetrieve(List<R> records, long operationID) {
        //eventBus.publish(new CRUDCommittedEvent.RetrievedEntities<>(this.entityType, operationID, keys, records));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UPDATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Contract(mutates = "param2")
    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    protected void beforeUpdate(@NotNull EntityKey<R> target, EntityDataPayload<R> data, long operationID) {
        fieldValidator.validateFullKey(target).orElseThrow();
        eventBus.publish(new CRUDDraftEvent.UpdateEntityDraft<R>(mainTable, operationID, target, data));
        fieldValidator.validateFullKey(target).orElseThrow();
        fieldValidator.validateDataPayload(data).orElseThrow();
    }

    @Transactional
    public <T> UpdateResult<R> update(
            TableField<R,T> field,
            T value,
            R record
    ){
        return update(keyOf(record), EntityDataPayload.of(field, value));
    }
    @Transactional
    public UpdateResult<R> update(R target, EntityDataPayload<R> update) {
        return update(keyOf(target), update);
    }

    @Transactional
    @Override
    public UpdateResult<R> update(EntityKey<R> target, EntityDataPayload<R> update) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(update);
        if (!exists(target)) return new UpdateResult.NoSuchEntity<>(RecordFindResult.notFound(target), update);

        long operationID = eventBus.nextOperationID();
        log.debug("Updating entity {} with new data \n{}", target, update.tableString());
        beforeUpdate(target, update, operationID);

        R record = store.get(target);
        try {
            boolean success = store.update(target, update);
            if (!success)
                return new UpdateResult.Failure<>(target, record, update, new IllegalStateException("Unexpected exception"));
        } catch (Exception e) {
            return new UpdateResult.Failure<>(target, record, update, e);
        }

        afterSuccessfulUpdate(record, target, update, operationID);
        return new UpdateResult.Success<>(target, update);
    }

    @Transactional
    public <T extends Number> Optional<T> incrementAndGet(
            TableField<R, T> field,
            EntityKey<R> entityKey
    ) {
        return adjustAndGet(field, entityKey, 1);
    }

    @Transactional
    public <T extends Number> Optional<T> decrementAndGet(
            TableField<R, T> field,
            EntityKey<R> entityKey
    ) {
        return adjustAndGet(field, entityKey, -1);
    }

    private <T extends Number> Optional<T> adjustAndGet(
            TableField<R, T> field,
            EntityKey<R> entityKey,
            int delta
    ) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(entityKey, "entityKey");

        if (delta != 1 && delta != -1) {
            throw new IllegalArgumentException(
                    "Entity adjustment must be either +1 or -1"
            );
        }

        long operationID = eventBus.nextOperationID();

        Optional<T> currentValueResult =
                store.getNumericValueForUpdate(field, entityKey);

        if (currentValueResult.isEmpty()) {
            log.debug(
                    "Entity with key {} was not found",
                    entityKey
            );

            return Optional.empty();
        }

        T currentValue = currentValueResult.get();
        T expectedValue = adjustValue(field, currentValue, delta);

        EntityDataPayload<R> draftUpdate =
                EntityDataPayload.of(field, expectedValue);

        /*
         * Runs field validation and publishes the draft update event.
         */
        beforeUpdate(
                entityKey,
                draftUpdate,
                operationID
        );

        /*
         * An event subscriber can mutate EntityDataPayload. Increment and
         * decrement have fixed semantics, so subscribers must not replace
         * the calculated value.
         */
        T approvedValue = draftUpdate.require(field);
        R record = store.get(entityKey);
        if (!Objects.equals(approvedValue, expectedValue)) {
            throw new IllegalStateException(
                    "An update listener changed the result of a numeric " +
                            "adjustment for field " + field.getName() +
                            ": expected " + expectedValue +
                            ", received " + approvedValue
            );
        }

        T actualValue = store.adjustAndGet(
                field,
                entityKey,
                delta
        );

        log.trace(
                "{} field {}. Previous: {}, new: {}",
                delta > 0 ? "Incremented" : "Decremented",
                field.getUnqualifiedName(),
                currentValue,
                actualValue
        );

        afterSuccessfulUpdate(
                record,
                entityKey,
                EntityDataPayload.of(field, actualValue),
                operationID);

        return Optional.of(actualValue);
    }

    @SuppressWarnings("unchecked")
    private static <R extends TableRecord<R>, T extends Number> T adjustValue(
            @NotNull TableField<R, T> field,
            @NotNull T value,
            int delta
    ) {
        Class<?> type = field.getType();

        if (type == Integer.class) {
            return (T) Integer.valueOf(
                    Math.addExact(value.intValue(), delta)
            );
        }

        if (type == Long.class) {
            return (T) Long.valueOf(
                    Math.addExact(value.longValue(), delta)
            );
        }

        if (type == Short.class) {
            int adjusted = Math.addExact(
                    value.shortValue(),
                    delta
            );

            if (adjusted < Short.MIN_VALUE ||
                    adjusted > Short.MAX_VALUE) {
                throw new ArithmeticException(
                        "Short overflow while adjusting field " +
                                field.getName()
                );
            }

            return (T) Short.valueOf((short) adjusted);
        }

        if (type == Byte.class) {
            int adjusted = Math.addExact(
                    value.byteValue(),
                    delta
            );

            if (adjusted < Byte.MIN_VALUE ||
                    adjusted > Byte.MAX_VALUE) {
                throw new ArithmeticException(
                        "Byte overflow while adjusting field " +
                                field.getName()
                );
            }

            return (T) Byte.valueOf((byte) adjusted);
        }

        if (type == java.math.BigInteger.class) {
            return (T) ((java.math.BigInteger) value).add(
                    java.math.BigInteger.valueOf(delta)
            );
        }

        if (type == java.math.BigDecimal.class) {
            return (T) ((java.math.BigDecimal) value).add(
                    java.math.BigDecimal.valueOf(delta)
            );
        }

        if (type == Double.class) {
            double adjusted = value.doubleValue() + delta;

            if (!Double.isFinite(adjusted)) {
                throw new ArithmeticException(
                        "Non-finite double produced while adjusting field " +
                                field.getName()
                );
            }

            return (T) Double.valueOf(adjusted);
        }

        if (type == Float.class) {
            float adjusted = value.floatValue() + delta;

            if (!Float.isFinite(adjusted)) {
                throw new ArithmeticException(
                        "Non-finite float produced while adjusting field " +
                                field.getName()
                );
            }

            return (T) Float.valueOf(adjusted);
        }

        throw new IllegalArgumentException(
                "Cannot numerically adjust field " +
                        field.getName() +
                        " of type " +
                        type.getName()
        );
    }

    protected void afterSuccessfulUpdate(R previousData, EntityKey<R> key, EntityDataPayload<R> updated, long operationID) {
        eventBus.publish(new CRUDCommittedEvent.UpdatedEntity<>(previousData, operationID, key, updated));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DELETE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected void beforeDelete(EntityKey<R> id, long operationID) {
        Objects.requireNonNull(id);
        fieldValidator.validateFullKey(id).orElseThrow("Error when deleting key");
        eventBus.publish(new CRUDDraftEvent.DeleteEntityDraft<R>(mainTable, operationID, id));
    }

    @Transactional
    public boolean delete(R record){
        Objects.requireNonNull(record);
        return delete(keyOf(record));
    }

    @Transactional
    public boolean delete(EntityKey<R> id) {
        Objects.requireNonNull(id);
        log.debug("Deleting entity {}", id);

        long operationID = eventBus.nextOperationID();
        beforeDelete(id, operationID);
        Optional<R> staleRecord = this.find(id).found();
        if (staleRecord.isEmpty()) {
            log.error("No such entity to delete with ID {}", id);
            return false;
        }
        boolean success = store.delete(id);
        if (success) afterSuccessfulDelete(id, operationID, staleRecord.get());

        return success;
    }

    protected void afterSuccessfulDelete(EntityKey<R> id, long operationID, R record) {
        eventBus.publish(new CRUDCommittedEvent.DeletedEntity<R>(operationID, id, record));
    }
}
