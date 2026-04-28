package chechelpo.demo.frameworks.entities.microservices;

import ch.qos.logback.classic.Logger;
import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.exceptions.Severity;
import chechelpo.demo.exceptions.types.ExpectedField;
import chechelpo.demo.exceptions.types.InvalidID;
import chechelpo.demo.exceptions.types.NotFound;
import chechelpo.demo.exceptions.types.UnexpectedException;
import chechelpo.demo.frameworks.entities.data.EntityKey;
import chechelpo.demo.frameworks.entities.data.QueryObject;
import chechelpo.demo.frameworks.entities.data.EntityDataPayload;
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
    private final HashMap<TableField<Record, ?>, Constraints<?>> constraints = new HashMap<>();
    private final Set<TableField<Record, ?>> keys = new HashSet<>();

    protected final Store store;
    private final Logger log;

    public ABSEntityService(Store store, EntityTypes.Types types) {
        if(REGISTERED_TYPES.contains(types))
            throw new IllegalStateException("Type " + types + " is already registered");

        REGISTERED_TYPES.add(types);
        this.store = store;
        log = (Logger) LoggerFactory.getLogger(types + "_Service");
    }

    public boolean isKey(TableField<Record, ?> field) {
        return keys.contains(field);
    }
    public EntityKey<Record> keyOf(Record record){
        EntityKey.Builder<Record> builder = EntityKey.builder();
        for (TableField<Record, ?> field : keys) {
            builder.set(field, record.getValue(field));
        }
        return builder.build();
    }
    protected void throwIfInvalid(@NotNull EntityKey<Record> key) {
        if (!key.getValues().keySet().equals(keys)) {
            log.error("Invalid key: {}", key);
            throw new InvalidID("Unknown ID", Severity.USER);
        }
    }
    void registerField(TableField<Record, ?> field, boolean required, @Nullable Constraints<?> constraint) {
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

    public Optional<Record> find(EntityKey<Record> k){
        log.debug("Finding record with id {}", k);
        throwIfInvalid(k);
        return Optional.ofNullable(store.get(k));
    }

    public List<Record> getAll() {
        return store.getAll();
    }

    protected EntityDataPayload<Record> beforeCreate(EntityDataPayload<Record> data) {
        checkForCreation(data);
        return data;
    }

    public @NotNull Record createAndGet(EntityDataPayload<Record> data) {
        data = beforeCreate(data);

        Record result = store.createAndGet(data);
        if (result == null) {
            log.error("Could not create new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }

        return result;
    }


    public <T> @NotNull T createAndGet(EntityDataPayload<Record> data, TableField<Record,T> field) {
        data = beforeCreate(data);
        T result = store.createAndGet(data, field);
        if (result == null){
            log.error("Could not create and fetch new entity");
            throw new UnexpectedException("New entity creation failed with data: " + data, Severity.SYSTEM);
        }

        return result;
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
        throwIfInvalid(id);
        return store.update(id, update);
    }

    public boolean delete(EntityKey<Record> id) {
        log.debug("Deleting entity {}", id);
        return store.delete(id);
    }

    public <T extends Number> T getAndIncrement(TableField<Record,T> field, EntityKey<Record> entityKey) {
        return store.getAndIncrement(field, entityKey);
    }
    /**
     * @param k key of the entity
     * @return true if registered in store, false otherwise
     * @implNote  does not throw {@link chechelpo.demo.exceptions.types.NotFound}, that's in charge of the caller
     */
    public boolean exists(EntityKey<Record> k){
        return store.exists(k);
    }

    public Record require(EntityKey<Record> id) throws NotFound {
        return find(id).orElseThrow(() ->
                new NotFound(
                        "Required entity missing: " + id,
                        Severity.SYSTEM
                )
        );
    }

    public Record getForUser(EntityKey<Record> id) {
        return find(id).orElseThrow(() ->
                new NotFound(
                        "No entity with id " + id,
                        Severity.USER
                )
        );
    }
}
