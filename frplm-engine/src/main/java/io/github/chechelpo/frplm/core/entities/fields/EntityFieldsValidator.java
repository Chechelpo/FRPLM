package io.github.chechelpo.frplm.core.entities.fields;

import org.jetbrains.annotations.NotNull;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.*;

public abstract class EntityFieldsValidator<R extends TableRecord<R>> implements FieldValidator<R>
{
    protected final Table<R> table;
    protected final Set<TableField<R, ?>> requiredInstantiationFields = new HashSet<>();
    protected final Map<TableField<R, ?>, FieldInfo<R, ?>> fieldInfoMap = new HashMap<>();
    protected final Set<TableField<R, ?>> keys = new HashSet<>();

    protected EntityFieldsValidator(Table<R> table) {
        this.table = table;
        getCustom().forEach(this::registerField);

        //noinspection unchecked
        Arrays.stream(getTable().fields())
                .map(TableField.class::cast)
                .filter(field -> !fieldInfoMap.containsKey(field))
                .forEach(field -> registerField(FieldInfo.builder(field).build()));
    }


    @Override
    public Table<R> getTable() {
        return this.table;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> FieldInfo<R, T> getInfoOf(TableField<R, T> field) {
        return (FieldInfo<R,T>) fieldInfoMap.get(field);
    }

    @Override
    public boolean isForeignKey(TableField<R, ?> field) {
        return fieldInfoMap.get(field).isForeignKey;
    }

    protected abstract List<FieldInfo<R, ?>> getCustom();

    protected <D extends DataPayload<R>> FieldActionResult<R, D> validateCustom(D payload) {
        return FieldActionResult.success(payload);
    }

    /**
     * Registers a field to be used by the service.
     *
     * @param info   metadata of field. For frontend.
     * @param <T>    The actual field type
     */
    protected <T> void registerField(@NotNull FieldInfo<R, ?> info) {
        Objects.requireNonNull(info, "Field payload is null");
        if (fieldInfoMap.containsKey(info.field))
            throw new IllegalStateException("Duplicate field: " + info.field);

        fieldInfoMap.put(info.field, info);
        if (info.isRequired) requiredInstantiationFields.add(info.field);
        if (info.isKey) keys.add(info.field);
    }

    @Override
    public Set<TableField<R, ?>> ignoreFieldsOnCreationOrder() {
        return Set.of();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Single Validators
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public <D extends DataPayload<R>> Optional<FieldActionResult.WrongValue<R, D, ?>> validateValueAssignment(
            TableField<R, ?> field,
            Object value,
            D payload,
            boolean isEditing
    ) {
        return fieldInfoMap.get(field)
                .validate(value, isEditing)
                .map(violation -> FieldActionResult.wrongValue(
                        "Constraint violation %s when checking field for %s".formatted(violation, payload.getClass()),
                        field,
                        value,
                        payload)
                );
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Validators
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Validates a data creationPayload by checking each field assignment against a set of allowed fields and validating the assigned value.
     *
     * @param payload         the data creationPayload containing field assignments to validate
     * @param allowedFieldSet the set of fields that are allowed to be present in the creationPayload
     * @param isEditing       a flag indicating whether the validation is performed during an editing operation
     * @return a field validation result indicating success if all assignments are valid, or an error result if a field is not allowed or its value is invalid
     */
    private <D extends DataPayload<R>> FieldActionResult<R, D> validatePayload(
            D payload,
            Set<TableField<R, ?>> allowedFieldSet,
            boolean isEditing
    ) {
        for (Map.Entry<TableField<R, ?>, Object> assignment : payload.assignments().entrySet()) {
            TableField<R, ?> field = assignment.getKey();
            Object value = assignment.getValue();

            if (!allowedFieldSet.contains(field))
                return FieldActionResult.unknownField(
                        "Unknown field %s when validating %s".formatted(field.getName(), payload.getClass()),
                        field,
                        payload
                );
            if (fieldInfoMap.get(field).isReadOnly)
                return FieldActionResult.readOnlyField(field, payload);
            
            Optional<FieldActionResult.WrongValue<R, D, ?>> valueAssignmentError =
                    validateValueAssignment(field, value, payload, isEditing);
            if (valueAssignmentError.isPresent()) return valueAssignmentError.get();
        }

        return validateCustom(payload);
    }

    /**
     * Checks if all required fields from the specified set are present in the given data creationPayload.
     *
     * @param payload          the data creationPayload to check for field assignments
     * @param requiredFieldSet the set of required fields that must be assigned in the creationPayload
     * @return an Optional containing the first required field that is missing from the creationPayload, or an empty Optional if all required fields are present
     */
    private Optional<TableField<R, ?>> checkAllFieldsPresent(DataPayload<R> payload, Set<TableField<R, ?>> requiredFieldSet) {
        for (TableField<R, ?> requiredField : requiredFieldSet)
            if (!payload.assigns(requiredField)) return Optional.of(requiredField);
        return Optional.empty();
    }

    @Override
    public FieldActionResult<R, EntityKey<R>> validateKey(EntityKey<R> key) {
        FieldActionResult<R, EntityKey<R>> dataValidationResult = validatePayload(key, keys, false);
        if (dataValidationResult.isFailure()) return dataValidationResult;

        return new FieldActionResult.Success<>("Valid key", key);
    }

    @Override
    public FieldActionResult<R, EntityKey<R>> validateFullKey(EntityKey<R> key) {
        FieldActionResult<R, EntityKey<R>> dataValidationError = validateKey(key);
        if (dataValidationError.isFailure()) return dataValidationError;

        Optional<TableField<R, ?>> missingKeyField = checkAllFieldsPresent(key, keys);
        if (missingKeyField.isPresent())
            return FieldActionResult.missingField(
                    "Full key misses field " + missingKeyField.get(),
                    missingKeyField.get(),
                    key
            );

        return FieldActionResult.success(key);
    }

    @Override
    public FieldActionResult<R, EntityDataPayload<R>> validateDataPayload(EntityDataPayload<R> payload) {
        FieldActionResult<R, EntityDataPayload<R>> dataValidationError = validatePayload(payload, fieldInfoMap.keySet(), true);
        if (dataValidationError.isFailure()) return dataValidationError;

        for (Map.Entry<TableField<R, ?>, Object> entry : payload.assignments().entrySet())
            if (keys.contains(entry.getKey()))
                return FieldActionResult.wrongValue(
                        "Non-creation data creationPayload assigns key field " + entry.getKey(),
                        entry.getKey(),
                        entry.getValue(),
                        payload
                );

        return FieldActionResult.success(payload);
    }

    @Override
    public FieldActionResult<R, EntityDataPayload<R>> validateDataCreationPayload(EntityDataPayload<R> payload) {
        FieldActionResult<R, EntityDataPayload<R>> validationResult = validatePayload(payload, fieldInfoMap.keySet(), false);
        if (validationResult.isFailure()) return validationResult;

        //Applies default values as needed
        fieldInfoMap.values()
                .forEach(fieldInfo -> payload.consumeIfAbsent(fieldInfo.getApplicationDefaultValue()));

        if (!payload.assignments().keySet().containsAll(requiredInstantiationFields))
            return FieldActionResult.missingField(
                    "Missing creation fields.\n Expected: %s \nGot: %s"
                            .formatted(requiredInstantiationFields, payload.assignments().keySet()),
                    requiredInstantiationFields.stream()
                            .filter(instField -> !payload.assigns(instField))
                            .findFirst().get(),
                    payload
            );

        return FieldActionResult.success(payload);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Utils
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    @Override
    public EntityKey<R> extractKeysFrom(EntityDataPayload<R> payload){
        EntityKey.Builder<R> builder = EntityKey.builder();

        keys.stream()
                .filter(payload::assigns)
                .forEach(key -> builder.unsafeSet(key, payload.require(key)));

        return builder.build();
    }

    @Override
    public boolean isKey(TableField<R, ?> field) {
        return keys.contains(field);
    }

    @Override
    public EntityKey<R> keyOf(R record) {
        EntityKey.Builder<R> builder = EntityKey.builder();
        keys.forEach(field -> builder.unsafeSet(field, record.get(field)));
        return builder.build();
    }

    public Set<TableField<R,?>> keyFields(){
        return keys;
    }

    @Override
    public Set<TableField<R,?>> instantiationFields(){
        return requiredInstantiationFields;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Register
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    void throwIfPresent(TableField<R, ?> field){
        if (
                requiredInstantiationFields.contains(field) || fieldInfoMap.containsKey(field) ||
                keys.contains(field)
        ) throw new IllegalStateException("Field " + field.getName() + " is registered twice");
    }


    public class FieldBuilder<T> {
        protected final TableField<R, T> column;
        protected FieldInfo<R, T> info = null;

        protected FieldBuilder(TableField<R, T> column) {
            this.column = column;
        }

        public TableField<R, T> getColumn() {
            return this.column;
        }

        public FieldBuilder<T> setInfo(FieldInfo<R, T> info) {
            this.info = info;
            return this;
        }
    }
}
