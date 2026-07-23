package io.github.chechelpo.frplm.core.entities.pseudo_services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.events.crud.CRUDDraftEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.*;
import io.github.chechelpo.frplm.core.entities.fields.constraints.Constraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.utils.ValidationResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public abstract class EntityService<
        R extends TableRecord<R>,
        Store extends EntityStore<R>
        > implements EntityReader<R>, EntityUpdater<R> {
    private final static EnumSet<EntityConfigs.Types> REGISTERED_TYPES = EnumSet.noneOf(EntityConfigs.Types.class);
    protected final EventBus eventBus;
    // Fields
    private final Set<TableField<R, ?>> required_instantiation_fields = new HashSet<>();
    private final HashMap<TableField<R, ?>, Constraint<?,?>> constraints = new HashMap<>();
    private final Set<TableField<R, ?>> keys = new HashSet<>();
    private final HashMap<TableField<R, ?>, Object> defaultsOnCreate = new HashMap<>();

    protected final Store store;    
    protected final Logger log;
    private final EntityConfigs.Types entityType;

    public EntityService(@NotNull Store store, @NotNull EventBus eventBus) {
        EntityConfigs.Types types = store.getType();
        //if(REGISTERED_TYPES.contains(types))
        //    throw new IllegalStateException("Type " + types + " is already registered");

        REGISTERED_TYPES.add(types);
        this.entityType = types;
        this.eventBus = eventBus;
        this.store = store;
        log = (Logger) LoggerFactory.getLogger(types + "_Service");
        log.setLevel(Level.convertAnSLF4JLevel(types.getLoggerLevel()));
    }
    public EntityService(@NotNull Store store, @NotNull EventBus eventBus, boolean registerSingleton) {
        EntityConfigs.Types types = store.getType();
        //if(!registerSingleton && REGISTERED_TYPES.contains(types))
        //    throw new IllegalStateException("Type " + types + " is already registered");

        //if (!registerSingleton)  REGISTERED_TYPES.add(types);
        this.entityType = types;
        this.eventBus = eventBus;
        this.store = store;
        log = (Logger) LoggerFactory.getLogger(types + "_Service");
        log.setLevel(Level.convertAnSLF4JLevel(types.getLoggerLevel()));
    }

    public EntityConfigs.Types getType(){
        return this.entityType;
    }
    public boolean isKey(TableField<R, ?> field) {
        return keys.contains(field);
    }

    public EntityKey<R> keyOf(R record){
        Map<TableField<R, ?>, Object> assignments = new HashMap<>();
        keys.forEach(keyField -> assignments.put(keyField, record.get(keyField)));
        return new EntityKey<>(assignments, false);
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

    public Set<TableField<R, ?>> getKeyFields(){
        return keys;
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

    @Override
    public ValidationResult validateKeyStructure(EntityKey<R> key) {
        Objects.requireNonNull(key, "Key to validate is null");
        // Check for unknown fields
        for (TableField<R, ?> field : key.getValues().keySet())
            if (!keys.contains(field))
                return ValidationResult.error("Unknown field in key: " + field.getName());

        // Check if contains all fields
        for (TableField<R, ?> field : keys)
            if (!key.assignsField(field))
                return ValidationResult.error("Key is missing field: " + field.getName());

        return ValidationResult.success();
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
            for (Map.Entry<TableField<R, ?>, Object> defaultAssignment : defaultsOnCreate.entrySet()) {
                TableField<R, ?> field = defaultAssignment.getKey();
                if (!data.assignsField(field))
                    data.unsafeSetValue(field, defaultAssignment.getValue());
            }

            Optional<EntityKey<R>> initialKey = buildInitialKey(data);

            eventBus.publish(new CRUDDraftEvent.CreateEntityDraft<R>(this.entityType, operationID, initialKey, data));
            throwIfInvalidData(data, false);
        }

        /**
         * Builds an {@link EntityKey} from whatever key fields are already
         * assigned in {@code data} (after defaults have been applied).
         *
         * <p>Returns {@link Optional#empty()} when no key field is assigned
         * yet — e.g. when the primary key is auto-generated and won't be
         * known until after {@code store.createAndGet}. When only some key
         * fields are known (composite key with a missing component), the
         * returned key is a <em>partial</em> key containing just the
         * assigned fields.</p>
         */
        private Optional<EntityKey<R>> buildInitialKey(EntityDataPayload<R> data) {
            if (keys.isEmpty()) return Optional.empty();

            Map<TableField<R, ?>, Object> assignments = data.assignments();
            EntityKey.Builder<R> builder = EntityKey.builder();
            boolean anyAssigned = false;
            for (TableField<R, ?> keyField : keys) {
                if (assignments.containsKey(keyField)) {
                    builder.unsafeSet(keyField, assignments.get(keyField));
                    anyAssigned = true;
                }
            }

            return anyAssigned ? Optional.of(builder.build()) : Optional.empty();
        }

    @Transactional
    public @NotNull R createAndGet(EntityDataPayload<R> data) {
        Objects.requireNonNull(data, "data");
        long operationID = eventBus.nextOperationID();
        beforeCreate(data, operationID);
        R result = null;
        try{
            result = store.createAndGet(data);
        }catch (Exception e){
            log.error("Error creating entity with data {} \n {} \n Trace: \n", data.prettyPrint(), e.getMessage());
            e.printStackTrace();
            throw new UnexpectedException("New entity creation failed with data:  " + data, Severity.SYSTEM);
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
        Objects.requireNonNull(data);
        Objects.requireNonNull(field);
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

    @Override
    @Transactional(readOnly = true)
    public RecordFindResult<R> find(EntityKey<R> key){
        Objects.requireNonNull(key);
        long operationID = eventBus.nextOperationID();
        beforeRetrieve(key, true, operationID);
        log.debug("Finding record with id {}", key);

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
        throwIfInvalidKey(key, false);

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
    public OneMatchingResult<R> getOneMatching(EntityDataPayload<R> target){
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
    public <T> Optional<T> getValueOf(TableField<R, T> field, EntityKey<R> key) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(field);
        throwIfInvalidKey(key, true);
        if (!exists(key)) {
            log.error("Could not find {} with id {}", this.getType().getEntityType() ,key);
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(field, key));
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
    public boolean exists(EntityKey<R> k){
        Objects.requireNonNull(k);
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
    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    protected void beforeUpdate(@NotNull EntityKey<R> target, EntityDataPayload<R> data, long operationID) {
        throwIfInvalidKey(target, true);
        throwIfInvalidData(data, true);
        eventBus.publish(new CRUDDraftEvent.UpdateEntityDraft<R>(entityType, operationID, target, data));
        throwIfInvalidData(data, true);
    }

    @Transactional
    @Override
    public UpdateResult<R> update(EntityKey<R> target, EntityDataPayload<R> update) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(update);
        if (!exists(target)) return new UpdateResult.NoSuchEntity<>(RecordFindResult.notFound(target), update);

        long operationID = eventBus.nextOperationID();
        log.trace("Updating entity {} with new data {}", target, update);
        beforeUpdate(target, update, operationID);

        try{
            boolean success = store.update(target, update);
            if (!success) return new UpdateResult.Failure<>(target, update, new IllegalStateException("Unexpected exception"));
        }catch (Exception e){
            return new UpdateResult.Failure<>(target, update, e);
        }

        afterSuccessfulUpdate(target, update, operationID);
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

        /*
         * SELECT ... FOR UPDATE.
         *
         * This avoids the race in:
         *
         *     exists()
         *     get()
         *     update()
         *
         * Another transaction cannot modify or delete this row until the
         * current transaction completes.
         */
        Optional<T> currentValueResult =
                store.getNumericValueForUpdate(field, entityKey);

        if (currentValueResult.isEmpty()) {
            log.debug(
                    "{} with key {} was not found",
                    getType().getEntityType(),
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
        T approvedValue = draftUpdate.requireValue(field);

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
                entityKey,
                EntityDataPayload.of(field, actualValue),
                operationID
        );

        return Optional.of(actualValue);
    }

    @SuppressWarnings("unchecked")
    private static <R extends TableRecord<R> ,T extends Number> T adjustValue(
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
    protected void afterSuccessfulUpdate(EntityKey<R> key, EntityDataPayload<R> updated, long operationID) {
        eventBus.publish(new CRUDCommittedEvent.UpdatedEntity<R>(this.entityType, operationID, key, updated));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DELETE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected void beforeDelete(EntityKey<R> id, long operationID){
        Objects.requireNonNull(id);
        throwIfInvalidKey(id, true);
        eventBus.publish(new CRUDDraftEvent.DeleteEntityDraft<R>(this.entityType, operationID, id));
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
        eventBus.publish(new CRUDCommittedEvent.DeletedEntity<R>(this.entityType, operationID, id, record));
    }
}
