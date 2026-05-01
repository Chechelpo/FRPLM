package chechelpo.demo.frameworks.entities.microservices;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.events.EventManager;
import chechelpo.demo.events.types.DeletedEntity;
import chechelpo.demo.events.types.NewEntity;
import chechelpo.demo.events.types.UpdatedEntity;
import chechelpo.demo.exceptions.Severity;
import chechelpo.demo.exceptions.types.ExpectedField;
import chechelpo.demo.exceptions.types.InvalidID;
import chechelpo.demo.exceptions.types.NotFound;
import chechelpo.demo.exceptions.types.UnexpectedException;
import chechelpo.demo.frameworks.entities.data.QueryObject;
import chechelpo.demo.frameworks.entities.fields.constraints.Constraints;
import chechelpo.demo.frameworks.entities.fields.constraints.NumberConstraints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class ABSEntityService<
        Record extends TableRecord<Record>,
        Store extends ABSEntityStore<Record>
        > {
    private final static EnumSet<EntityTypes.Types> REGISTERED_TYPES = EnumSet.noneOf(EntityTypes.Types.class);

    private final Set<TableField<Record, ?>> required_instantiation_fields = new HashSet<>();
    private final HashMap<TableField<Record, ?>, Constraints<?,?>> constraints = new HashMap<>();
    private final Set<TableField<Record, ?>> keys = new HashSet<>();

    protected final Store store;
    private final Logger log;
    private final EntityTypes.Types entityType;

    public ABSEntityService(Store store, EntityTypes.Types types) {
        if(REGISTERED_TYPES.contains(types))
            throw new IllegalStateException("Type " + types + " is already registered");

        REGISTERED_TYPES.add(types);
        this.entityType = types;

        this.store = store;
        log = (Logger) LoggerFactory.getLogger(types + "_Service");
    }

    public boolean isKey(TableField<Record, ?> field) {
        return keys.contains(field);
    }
    public EntityKey<Record> keyOf(Record record){
        EntityKey.Builder<Record> builder = EntityKey.builder();
        for (TableField<Record, ?> field : keys) {
            builder.unsafeSet(field, record.getValue(field));
        }
        return builder.build();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // VALIDATORS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected void throwIfInvalid(@NotNull EntityKey<Record> key) {
        if (!key.getValues().keySet().equals(keys)) {
            log.error("Invalid key: {}", key);
            throw new InvalidID("Unknown ID", Severity.USER);
        }
    }
    protected void throwIfNotPartialKey(@NotNull EntityKey<Record> key) {
        if (!keys.containsAll(key.getValues().keySet())) {
            log.error("Invalid key: {}", key);
            throw new InvalidID("Invalid key", Severity.USER);
        }
    }

    private void throwOnConstraintsViolation(@Nullable EntityKey<Record> key, @Nullable EntityDataPayload<Record> payload) {
        if (key != null) {
            for (Map.Entry<TableField<Record, ?>, Object> entry : key.values().entrySet()) {
                if (constraints.containsKey(entry.getKey()) && constraints.get(entry.getKey()).violatesConstraints(entry.getKey(), entry.getValue())) {
                    throw new RuntimeException("Constraint violation");
                }
            }
        }
        if (payload != null && !payload.isEmpty()) {
            for (Map.Entry<TableField<Record, ?>, Object> entry : payload.values().entrySet()) {
                if (constraints.containsKey(entry.getKey())
                        &&
                        constraints.get(entry.getKey()).violatesConstraints(entry.getKey(), entry.getValue())) {
                    throw new RuntimeException("Constraint violation");
                }
            }
        }
    }

    private EntityKey<Record> coerceFullKey(EntityKey<Record> key) {
        throwIfInvalid(key);
        for (TableField<Record, ?> field : this.keys){
            Constraints<?, ?> constraint = constraints.get(field);
            key.setOrReplace(field, constraint.coerce(key.getValue(field)));
        }

        return key;
    }
    private EntityKey<Record> coercePartialKey(EntityKey<Record> key) {
        throwIfNotPartialKey(key);
        for (TableField<Record, ?> field : key.getValues().keySet()){
            Constraints<?, ?> constraint = constraints.get(field);
            key.setOrReplace(field, constraint.coerce(key.getValue(field)));
        }

        return key;
    }
    private EntityDataPayload<Record> coercePayload(EntityDataPayload<Record> data) {
        throwOnConstraintsViolation(null, data);
        for (TableField<Record, ?> field : data.values().keySet()){
            Constraints<?, ?> constraint = constraints.get(field);
            data.unsafeSetValue(field, constraint.coerce(data.getValue(field)));
        }

        return data;
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // FIELDS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    void registerField(TableField<Record, ?> field, boolean required, @Nullable Constraints<?,?> constraint) {
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
    // Operations
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public Optional<Record> find(EntityKey<Record> k){
        log.debug("Finding record with id {}", k);
        coerceFullKey(k);
        return Optional.ofNullable(store.get(k));
    }

    public List<Record> getAll() {
        return store.getAll();
    }
    public List<Record> getMatching(EntityKey<Record> k) {
        throwIfNotPartialKey(k);
        coercePartialKey(k);
        return store.getAllMatching(k);
    }

    protected EntityDataPayload<Record> beforeCreate(EntityDataPayload<Record> data) {
        checkForCreation(data);
        coercePayload(data);
        return data;
    }

    public @NotNull Record createAndGet(EntityDataPayload<Record> data) {
        data = beforeCreate(data);
        coercePayload(data);
        Record result = store.createAndGet(data);
        if (result == null) {
            log.error("Could not create new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }
        EventManager.emitEvent(new NewEntity(this.entityType, result));
        return result;
    }


    public <T> @NotNull T createAndGet(EntityDataPayload<Record> data, TableField<Record,T> field) {
        data = beforeCreate(data);
        coercePayload(data);
        Record result = store.createAndGet(data);
        if (result == null){
            log.error("Could not create and fetch new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }
        EventManager.emitEvent(new NewEntity(this.entityType, result));

        return result.get(field);
    }

    private void checkForCreation(EntityDataPayload<Record> data) {
        log.debug("Creating new entity with data {}", data);
        //Check instantiation fields and constraints
        if (!data.assignments().keySet().containsAll(required_instantiation_fields)) {
            log.error("Missing required instantiation fields");
            throw new ExpectedField("Expected fields", Severity.USER);
        }
        throwOnConstraintsViolation(null, data);

        for (TableField<Record, ?> field : required_instantiation_fields) {
            if (!data.assignsField(field)) {
                log.error("Missing required instantiation field {}", field);
                throw new ExpectedField("Field " + field.getName() + " is not assigned", Severity.USER);
            }
        }
    }

    public List<Record> query(QueryObject<Record> query) {
        log.debug("Querying for {}", query);
        return store.query(query);
    }

    public boolean update(EntityKey<Record> id, EntityDataPayload<Record> update) {
        log.debug("Updating entity {} with new data {}", id, update);
        coerceFullKey(id);
        coercePayload(update);

        boolean success = store.update(id, update);
        if (success) EventManager.emitEvent(new DeletedEntity(this.entityType, id));

        return success;
    }

    public boolean delete(EntityKey<Record> id) {
        log.setLevel(Level.TRACE);
        log.debug("Deleting entity {}", id);
        coerceFullKey(id);

        boolean success = store.delete(id);
        if (success) EventManager.emitEvent(new DeletedEntity(this.entityType, id));

        return success;
    }

    public <T extends Number> T getAndIncrement(TableField<Record,T> field, EntityKey<Record> entityKey) {
        coerceFullKey(entityKey);
        T number = store.getAndIncrement(field, entityKey);
        EventManager.emitEvent(new UpdatedEntity(this.entityType, EntityDataPayload.fromValues(Map.of(field, number))));
        return number;
    }
    /**
     * @param k key of the entity
     * @return true if registered in store, false otherwise
     * @implNote  does not throw {@link chechelpo.demo.exceptions.types.NotFound}, that's in charge of the caller
     */
    public boolean exists(EntityKey<Record> k){
        coerceFullKey(k);
        return store.exists(k);
    }

    public Record require(EntityKey<Record> id) throws NotFound {
        coerceFullKey(id);
        return find(id).orElseThrow(() ->
                new NotFound(
                        "Required entity missing: " + id,
                        Severity.SYSTEM
                )
        );
    }

    public Record getForUser(EntityKey<Record> id) {
        coerceFullKey(id);
        return find(id).orElseThrow(() ->
                new NotFound(
                        "No entity with id " + id,
                        Severity.USER
                )
        );
    }
}
