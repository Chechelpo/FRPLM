package chechelpo.frplm.frameworks.entities.microservices;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.events.crud.CRUDDraftEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.*;
import chechelpo.frplm.frameworks.entities.fields.constraints.Constraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
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
    private final HashMap<TableField<R, ?>, Constraints<?,?>> constraints = new HashMap<>();
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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // FIELDS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected  <T> void registerField(
            TableField<R, T> field,
            boolean required, @Nullable Constraints<?,?> constraint,
            @Nullable T defaultValue
    ){
        if (constraint != null)
            try{
                constraint.throwOnConstraintViolation(field, defaultValue, required);
            } catch (InvalidValue e) {
                log.error("Default value {} violates constraints \n {}", defaultValue, e.getMessage());
                throw new InvalidValue("Default value " + defaultValue + " violates constraints \n " + e.getMessage());
            }
        defaultsOnCreate.put(field,  defaultValue);
        registerField(field, required, constraint);
    }
    protected void registerField(TableField<R, ?> field, boolean required, @Nullable Constraints<?,?> constraint) {
        if (constraint != null) {
            constraints.put(field, constraint);
            if (constraint instanceof NumberConstraints){
                if (((NumberConstraints) constraint).isKey()) {
                    keys.add(field);
                }
            }
        }
        if (required) required_instantiation_fields.add(field);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // VALIDATORS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected final void throwIfUnknownField(TableField<R, ?> field) {
        if (!constraints.containsKey(field)){
            log.error("Unknown field: {}", field);
            throw new UnknownField("Unknown field: " + field );
        }
    }
    protected final void throwIfInvalidKey(@NotNull EntityKey<R> key, boolean isFullKey) {
        for (Map.Entry<TableField<R, ?>, Object> entry : key.values().entrySet()) {
            throwIfUnknownField(entry.getKey());
            if (!keys.contains(entry.getKey())){
                log.error("Invalid key field: {} found in {} key", entry.getKey(), this.entityType);
                throw new InvalidKey("Invalid field: " + entry.getKey() + " found in key ", Severity.USER);
            }
            Constraints<?, ?> con = constraints.get(entry.getKey());
            con.throwOnConstraintViolation(entry.getKey(), entry.getValue(), false);
        }

        if (isFullKey && !key.values().keySet().containsAll(keys)){
            throw new InvalidKey("Incomplete key", Severity.USER);
        }
    }

    /**
     * @param payload assignments of fields
     * @param isUpdate whether this is an update to an existing entity or a NEW entity
     */
    protected void throwIfInvalidData(@NotNull EntityDataPayload<R> payload, boolean isUpdate){
        TableField<R, ?> field;
        for (Map.Entry<TableField<R, ?>, Object> entry : payload.values().entrySet()) {
            field = entry.getKey();
            throwIfUnknownField(field);
            if (isUpdate && keys.contains(field)) {
                log.error("Key field: {} found in {} data payload", field, this.entityType);
                throw new InvalidValue("Tried to assign a key field");
            }
            Constraints<?, ?> con = constraints.get(field);
            con.throwOnConstraintViolation(field, entry.getValue(), isUpdate);
        }

        if (!isUpdate && !payload.values().keySet().containsAll(required_instantiation_fields)){
            throw new InvalidValue("Incomplete creation of required instantiation fields");
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
    @Transactional(readOnly = true)
    public Optional<R> find(EntityKey<R> k){
        throwIfInvalidKey(k, true);
        log.debug("Finding record with id {}", k);

        return Optional.ofNullable(store.get(k));
    }

    @Transactional(readOnly = true)
    public List<R> getAll() {
        return store.getAll();
    }

    @Transactional(readOnly = true)
    public List<R> getMatching(EntityKey<R> k) {
        throwIfInvalidKey(k, false);
        return store.getAllMatching(k);
    }

    @Transactional(readOnly = true)
    public <T> T getValueOf(TableField<R, T> field, EntityKey<R> key) {
        throwIfInvalidKey(key, true);
        return store.get(field, key);
    }

    public R require(EntityKey<R> id) throws NotFound {
        return find(id).orElseThrow(() ->
                new NotFound(
                        "Required entity missing: " + id,
                        Severity.SYSTEM
                )
        );
    }

    public R getForUser(EntityKey<R> id) {
        return find(id).orElseThrow(() ->
                new NotFound(
                        "No " + this.entityType + " with id " + id,
                        Severity.USER
                )
        );
    }
    /**
     * @param k key of the entity
     * @return true if registered in store, false otherwise
     * @implNote  does not throw {@link chechelpo.frplm.exceptions.types.NotFound}, that's in charge of the caller
     */
    @Transactional(readOnly = true)
    public boolean exists(EntityKey<R> k){
        throwIfInvalidKey(k, true);
        return store.exists(k);
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
    public <T extends Number> T getAndIncrement(TableField<R,T> field, EntityKey<R> entityKey) {
        long operationID = eventBus.nextOperationID();
        T newValue = store.get(field, entityKey);
        T expectedValue = increment(field, newValue);

        beforeUpdate(entityKey, EntityDataPayload.of(field, expectedValue), operationID);
        T number = store.incrementAndGet(field, entityKey);

        afterSuccessfulUpdate(entityKey, EntityDataPayload.of(field, number), operationID);

        return number;
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

        boolean success = store.delete(id);
        if (success) afterSuccessfulDelete(id, operationID);

        return success;
    }

    protected void afterSuccessfulDelete(EntityKey<R> id, long operationID) {
        eventBus.publish(new CRUDCommittedEvent.DeletedEntity<R>(this.entityType, operationID, id));
    }
}
