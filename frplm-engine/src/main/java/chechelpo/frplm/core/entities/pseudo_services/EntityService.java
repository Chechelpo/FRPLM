package chechelpo.frplm.core.entities.pseudo_services;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.events.crud.CRUDDraftEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.*;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.InvalidKey;
import chechelpo.frplm.core.entities.fields.constraints.Constraint;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public abstract class EntityService<
        R extends TableRecord<R>,
        Store extends EntityStore<R>
        > {
    private final static EnumSet<EntityTypes.Types> REGISTERED_TYPES = EnumSet.noneOf(EntityTypes.Types.class);
    protected final EventBus eventBus;
    // Fields
    private final Set<TableField<R, ?>> required_instantiation_fields = new HashSet<>();
    private final HashMap<TableField<R, ?>, Constraint<?,?>> constraints = new HashMap<>();
    private final Set<TableField<R, ?>> keys = new HashSet<>();
    private final HashMap<TableField<R, ?>, Object> defaultsOnCreate = new HashMap<>();

    protected final Store store;
    protected final Logger log;
    private final EntityTypes.Types entityType;

    public EntityService(@NotNull Store store, @NotNull EventBus eventBus) {
        EntityTypes.Types types = store.getType();
        if(REGISTERED_TYPES.contains(types))
            throw new IllegalStateException("Type " + types + " is already registered");

        REGISTERED_TYPES.add(types);
        this.entityType = types;
        this.eventBus = eventBus;
        this.store = store;
        log = (Logger) LoggerFactory.getLogger(types + "_Service");
        log.setLevel(types.getLoggerLevel());
    }
    public EntityService(@NotNull Store store, @NotNull EventBus eventBus, boolean registerSingleton) {
        EntityTypes.Types types = store.getType();
        if(!registerSingleton && REGISTERED_TYPES.contains(types))
            throw new IllegalStateException("Type " + types + " is already registered");

        if (!registerSingleton)  REGISTERED_TYPES.add(types);
        this.entityType = types;
        this.eventBus = eventBus;
        this.store = store;
        log = (Logger) LoggerFactory.getLogger(types + "_Service");
        log.setLevel(types.getLoggerLevel());
    }

    public EntityTypes.Types getType(){
        return this.entityType;
    }
    public boolean isKey(TableField<R, ?> field) {
        return keys.contains(field);
    }
    public EntityKey<R> keyOf(R record){
        EntityKey.Builder<R> builder = EntityKey.builder();
        for (TableField<R, ?> field : keys) {
            builder.unsafeSet(field, record.getValue(field));
        }
        return builder.build();
    }
    public List<EntityKey<R>> keysOf(List<R> records){
        return records.stream().map(this::keyOf).toList();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // FIELDS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected  <T> void registerField(
            TableField<R, T> field,
            boolean required, @Nullable Constraint<?,?> constraint,
            @Nullable T defaultValue
    ){
        if (constraint != null) {
            Optional<Constraint.ConstraintViolation<R>> constraintViolation =
                    constraint.validateConstraint(field, defaultValue, required);
            if (constraintViolation.isPresent()) {
                log.error("Error when registering field {} \n {}", field, constraintViolation.get().message());
                throw new IllegalStateException("Error when registering field " + field + " \n " + constraintViolation.get().message());
            }
        }

        log.trace("Registering field {} with constraints {} and default value {}", field, constraint, defaultValue);
        defaultsOnCreate.put(field,  defaultValue);
        registerField(field, required, constraint);
    }

    protected void registerField(TableField<R, ?> field, boolean required, @Nullable Constraint<?,?> constraint) {
        if (constraint != null) {
            constraints.put(field, constraint);
            if (constraint instanceof NumberConstraint){
                if (((NumberConstraint) constraint).isKey()) {
                    keys.add(field);
                }
            }
        }
        log.trace("Registering field {} with constraints {} ", field, constraint);

        if (required) required_instantiation_fields.add(field);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // VALIDATORS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public void throwIfInvalidKey(@NotNull EntityKey<R> key, boolean isFullKey) {
        Optional<DataValidator.FieldValidationError<R>> error = DataValidator.getValidationError(
                constraints, keys, key.getValues(), isFullKey, true
        );

        if (error.isPresent()) {
            log.error("Error when validating key: \n {}", error.get().getMessage());
            throw new InvalidKey(error.get().getMessage(), Severity.SYSTEM);
        }
    }

    /**
     * @param payload assignments of fields
     * @param isUpdate whether this is an update to an existing entity or a NEW entity
     */
    public void throwIfInvalidData(@NotNull EntityDataPayload<R> payload, boolean isUpdate) throws InvalidValue {
        Optional<DataValidator.FieldValidationError<R>> error = DataValidator.getValidationError(
                constraints,
                required_instantiation_fields,
                payload.assignments(),
                !isUpdate,
                false
        );

        if (isUpdate)
            payload.assignments().forEach((field, value) -> {
                if(keys.contains(field)){
                    log.error("Found key field {} in update", field);
                    throw new UneditableField("Key field found in update " + field, Severity.SYSTEM);
                }
            });

        if (error.isPresent()) {
            log.error("Error when validating data: \n {}", error.get().getMessage());
            throw new InvalidValue(error.get().getMessage());
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CREATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Before entity creation hook. Meant as a way to inject mandatory data.
     * @param data the initial entity data
     * @param operationID of the creation event
     * @apiNote super() call is advised at the end of your override. This function also contains the event emit as well as validation logic
     */
    protected void beforeCreate(EntityDataPayload<R> data, long operationID) {
        throwIfInvalidData(data, false);

        for (Map.Entry<TableField<R, ?>, Object> defaultAssignment : defaultsOnCreate.entrySet()) {
            TableField<R, ?> field = defaultAssignment.getKey();
            if (!data.assignsField(field))
                data.unsafeSetValue(field, defaultAssignment.getValue());
        }

        eventBus.publish(new CRUDDraftEvent.CreateEntityDraft<R>(this.entityType, operationID, Optional.empty(), data));
        throwIfInvalidData(data, false);
    }

    @Transactional
    public @NotNull R createAndGet(EntityDataPayload<R> data) {
        long operationID = eventBus.nextOperationID();
        beforeCreate(data, operationID);

        R result = store.createAndGet(data);
        if (result == null) {
            log.error("Could not create new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }

        afterSuccessfulCreate(result, operationID);
        return result;
    }
    /** @apiNote creates directly, no validation. Assumes caller has done it already */
    protected void unsafeCreate(EntityDataPayload<R> data) {
        this.store.create(data);
    }
    @Transactional
    public <T> @NotNull T createAndGet(EntityDataPayload<R> data, TableField<R,T> field) {
        long operationID = eventBus.nextOperationID();
        beforeCreate(data, operationID);

        R result = store.createAndGet(data);
        if (result == null){
            log.error("Could not create and fetch new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }

        afterSuccessfulCreate(result, operationID);
        return result.get(field);
    }

    protected void afterSuccessfulCreate(R data, long operationID) {
        eventBus.publish(
                new CRUDCommittedEvent.CreatedEntity<R>(this.entityType, operationID, keyOf(data), data)
        );
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // RETRIEVE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected void beforeRetrieve(@Nullable EntityKey<R> key, boolean isFullKey, long operationID) {
        if (key != null) throwIfInvalidKey(key, isFullKey);
    }

    @Transactional(readOnly = true)
    public Optional<R> find(EntityKey<R> k){
        long operationID = eventBus.nextOperationID();
        beforeRetrieve(k, true, operationID);
        log.debug("Finding record with id {}", k);

        Optional<R> record = Optional.ofNullable(store.get(k));
        record.ifPresent(r -> afterRetrieve(List.of(r), operationID));

        return record;
    }

    @Transactional(readOnly = true)
    public List<R> getAll() {
        List<R> records = store.getAll();
        afterRetrieve(records, 0);
        return records;
    }

    @Transactional(readOnly = true)
    public List<R> getMatching(EntityKey<R> k) {
        throwIfInvalidKey(k, false);

        List<R> records = store.getAllMatching(k);
        afterRetrieve(records, 0);

        return records;
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> getValueOf(TableField<R, T> field, EntityKey<R> key) {
        throwIfInvalidKey(key, true);
        if (!exists(key)) {
            log.error("Could not find {} with id {}", this.getType().getEntityType() ,key);
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(field, key));
    }

    /**
     * @param k key of the entity
     * @return true if registered in store, false otherwise
     * @implNote  does not throw {@link EntityNotFound}, that's in charge of the caller
     */
    @Transactional(readOnly = true)
    public boolean exists(EntityKey<R> k){
        throwIfInvalidKey(k, true);
        return store.exists(k);
    }

    protected void afterRetrieve(List<R> records, long operationID) {
        //eventBus.publish(new CRUDCommittedEvent.RetrievedEntities<>(this.entityType, operationID, keys, records));
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UPDATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Contract(mutates = "param2")
    protected void beforeUpdate(@NotNull EntityKey<R> target, EntityDataPayload<R> data, long operationID) {
        throwIfInvalidKey(target, true);
        throwIfInvalidData(data, true);
        eventBus.publish(new CRUDDraftEvent.UpdateEntityDraft<R>(entityType, operationID, target, data));
        throwIfInvalidData(data, true);
    }

    @Transactional
    public boolean update(EntityKey<R> id, EntityDataPayload<R> update) {
        long operationID = eventBus.nextOperationID();
        log.trace("Updating entity {} with new data {}", id, update);
        beforeUpdate(id, update, operationID);

        boolean success = store.update(id, update);
        if (success) afterSuccessfulUpdate(id, update, operationID);

        return success;
    }
    @Transactional
    public <T extends Number> Optional<T> incrementAndGet(TableField<R,T> field, EntityKey<R> entityKey) {
        if (!exists(entityKey)) {
            log.error("{} with key {} not found", this.getType().getEntityType(), entityKey.toString());
            return Optional.empty();
        }
        long operationID = eventBus.nextOperationID();
        T newValue = store.get(field, entityKey);
        T expectedValue = increment(field, newValue);

        beforeUpdate(entityKey, EntityDataPayload.of(field, expectedValue), operationID);
        T number = store.incrementAndGet(field, entityKey);
        log.trace("Incremented field {} . Prev: {} , New: {}", field.getUnqualifiedName(), number, expectedValue);
        afterSuccessfulUpdate(entityKey, EntityDataPayload.of(field, number), operationID);

        return Optional.of(number);
    }

    private <T extends Number> T increment(@NotNull TableField<R, T> field, T value) {
        Class<?> type = field.getType();

        if (type == Integer.class) {
            return (T) Integer.valueOf(value.intValue() + 1);
        }

        if (type == Long.class) {
            return (T) Long.valueOf(value.longValue() + 1L);
        }

        if (type == Short.class) {
            return (T) Short.valueOf((short) (value.shortValue() + 1));
        }

        if (type == Byte.class) {
            return (T) Byte.valueOf((byte) (value.byteValue() + 1));
        }

        if (type == java.math.BigInteger.class) {
            return (T) ((java.math.BigInteger) value).add(java.math.BigInteger.ONE);
        }

        if (type == java.math.BigDecimal.class) {
            return (T) ((java.math.BigDecimal) value).add(java.math.BigDecimal.ONE);
        }

        throw new IllegalArgumentException(
                "Cannot increment numeric field " + field.getName() +
                        " of type " + type.getName()
        );
    }

    protected void afterSuccessfulUpdate(EntityKey<R> key, EntityDataPayload<R> updated, long operationID) {
        eventBus.publish(new CRUDCommittedEvent.UpdatedEntity<R>(this.entityType, operationID, key, updated));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DELETE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected void beforeDelete(EntityKey<R> id, long operationID){
        throwIfInvalidKey(id, true);
        eventBus.publish(new CRUDDraftEvent.DeleteEntityDraft<R>(this.entityType, operationID, id));
    }

    @Transactional
    public boolean delete(EntityKey<R> id) {
        log.debug("Deleting entity {}", id);

        long operationID = eventBus.nextOperationID();
        beforeDelete(id, operationID);
        Optional<R> staleRecord = this.find(id);
        if (staleRecord.isEmpty()) {
            log.error("No such entity to delete with ID {}", id);
            return false;
        }
        boolean success = store.delete(id);
        if (success) afterSuccessfulDelete(id, operationID, staleRecord.get());

        return success;
    }

    protected void afterSuccessfulDelete(EntityKey<R> id, long operationID, R record) {
        eventBus.publish(new CRUDCommittedEvent.DeletedEntity<R>(this.entityType, operationID, id, record));
    }
}
