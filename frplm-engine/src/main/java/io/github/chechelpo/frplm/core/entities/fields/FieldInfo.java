package io.github.chechelpo.frplm.core.entities.fields;

import io.github.chechelpo.frplm.core.entities.fields.coercers.*;
import io.github.chechelpo.frplm.core.entities.fields.constraints.*;
import io.github.chechelpo.frplm.core.entities.pseudo_services.*;
import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jooq.UniqueKey;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;

/**
 *
 */
public final class FieldInfo<R extends TableRecord<R>, T> {
    public final @NotNull TableField<R, T> field;
    public final @NotNull Coercer<T> coercer;
    public final @NotNull Constraint<T> constraint;

    private final Set<T> allowedValues;
    private final List<Function<T, Optional<String>>> customConstraints;
    private final T defaultValue;

    private final boolean assignedDefaultValue;
    /**
     * Whether this field must be provided by payload when creating an entity via {@link EntityCreator#createAndGet(EntityDataPayload)}
     */
    public final boolean isRequired;
    /**
     * Whether this field can be targeted by a {@link EntityUpdater#update(EntityKey, EntityDataPayload)} call
     */
    public final boolean isReadOnly;
    /**
     * Whether this field at least partially identifies the entity (along with others). Forces read only if present
     */
    public final boolean isKey;
    /**
     * Whether this field can be set to null
     */
    public final boolean isNullable;

    @Contract(pure = true)
    private FieldInfo(@NotNull FieldInfo.Builder<R, T> builder) {
        this.field = Objects.requireNonNull(builder.field);
        this.coercer = Objects.requireNonNull(builder.coercer, "Coercer for field : " + field.getName());

        this.constraint = Objects.requireNonNull(builder.constraints);
        this.customConstraints = builder.customConstraints;

        this.allowedValues = builder.allowedValues == null ? null : Set.copyOf(builder.allowedValues);
        this.defaultValue = builder.defaultValue;

        this.isRequired = builder.require;
        this.isReadOnly = builder.key || builder.readOnly;
        this.isNullable = builder.isNullable;
        this.isKey = builder.key;
        this.assignedDefaultValue = builder.assignedDefaultValue;

        validateFieldInfo();
    }

    private void validateFieldInfo() {
        if (this.isKey && this.isNullable)
            throw new IllegalStateException("Nullable key detected in field " + field.getName());

        if (assignedDefaultValue){
            Optional<String> defaultValueError = this.validate(defaultValue, false);
            if (defaultValueError.isPresent())
                throw new IllegalStateException(
                        "Default value " + defaultValueError.get() + " doesn't pass validations on-create for field " + field.getName()
                );
        }
    }

    public DataPayload.Assignment<R, T> getDefaultValue() {
        if (!this.assignedDefaultValue) return DataPayload.Assignment.ofUnassigned(field);
        return DataPayload.Assignment.ofAssigned(field, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public @NonNull Optional<String> validate(Object value, boolean isEditing) {
        if (isEditing && this.isReadOnly)
            return Optional.of(field.getName() + " is read only ");
        if (value == null)
            return this.isNullable ? Optional.empty() : Optional.of(field.getName() + " is not nullable ");

        Class<?> expectedType = boxedType(field.getType());
        if (!expectedType.isInstance(value))
            return Optional.of(
                    "Wrong data type for %s value %s. Expected type %s Got %s".formatted(
                            field.getName(),
                            value,
                            field.getType(),
                            value.getClass()
                    )
            );
        T castedValue = (T) value;
        if (this.allowedValues != null && !allowedValues.contains(castedValue))
            return Optional.of("Value " + castedValue + " not allowed. Allowed values are: " + allowedValues);

        Optional<String> constraintError = constraint.returnReasonIfInvalid(castedValue);
        if (constraintError.isPresent()) return constraintError;

        return customConstraints.isEmpty() ?
                Optional.empty()
                :
                customConstraints.stream()
                        .map(customConstraint -> customConstraint.apply(castedValue))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .flatMap(Function.identity());
    }

    @Contract(pure = true)
    public @NonNull Either<Coercer.CoerceError, T> coerce(Object value) {
        return this.coercer.coerce(value);
    }

    public static <R extends TableRecord<R>, T> FieldInfo.Builder<R, T> builder(TableField<R, T> field) {
        return new Builder<>(field);
    }

    public static class Builder<R extends TableRecord<R>, T> {
        private final TableField<R, T> field;
        private final Coercer<T> coercer;
        private Constraint<T> constraints = null;
        private List<Function<T, Optional<String>>> customConstraints = new ArrayList<>();

        private Set<T> allowedValues = null;

        private boolean assignedDefaultValue = false;
        private T defaultValue = null;

        private boolean require = false;
        private boolean readOnly = false;
        private boolean key = false;
        private boolean isNullable = false;

        private Builder(TableField<R, T> field) {
            this.field = field;

            this.key = isPrimaryKeyField(field);
            this.coercer = CoercerCreator.getCoercerForClass(field.getType());
            this.isNullable = field.getDataType().nullable();
        }

        public TableField<R, T> field() {
            return this.field;
        }

        private void createAllowedValuesIfNull() {
            if (this.allowedValues == null)
                this.allowedValues = new HashSet<>();
        }

        public <C extends Constraint<T>> Builder<R, T> setConstraints(
                Constraint.@NotNull ABSConstraintsBuilder<T, C, ?> builder
        ) {
            return setConstraints(builder.build());
        }

        public Builder<R, T> setConstraints(Constraint<T> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder<R, T> addCustomConstraint(Function<T, Optional<String>> function) {
            this.customConstraints.add(function);
            return this;
        }

        public Builder<R, T> setDefaultValue(T value) {
            this.assignedDefaultValue = true;
            this.defaultValue = value;
            return this;
        }

        public Builder<R, T> addAllowedValues(T... values) {
            createAllowedValuesIfNull();
            allowedValues.addAll(List.of(values));
            return this;
        }

        public Builder<R, T> addAllowedValues(List<T> values) {
            createAllowedValuesIfNull();
            allowedValues.addAll(values);
            return this;
        }

        public Builder<R, T> addAllowedValue(T value) {
            createAllowedValuesIfNull();
            allowedValues.add(value);
            return this;
        }


        /**
         * Whether this field at least partially identifies the entity (along with others). Forces read only if present
         */
        public Builder<R, T> key() {
            this.key = true;
            return this;
        }

        /**
         * Whether this field can be targeted by a {@link EntityUpdater#update(EntityKey, EntityDataPayload)} call
         */
        public Builder<R, T> readOnly() {
            this.readOnly = true;
            return this;
        }

        /**
         * Whether this field must be provided by payload when creating an entity via {@link EntityCreator#createAndGet(EntityDataPayload)}
         */
        public Builder<R, T> requireOnCreate() {
            this.require = true;
            return this;
        }

        public Builder<R, T> nullable() {
            this.isNullable = true;
            return this;
        }

        public FieldInfo<R, T> build() {
            if (this.constraints == null)
                this.constraints = DefaultConstraints.getDefaultConstraintForClass(field.getType());

            return new FieldInfo<>(this);
        }
    }

    private static Class<?> boxedType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;

        throw new IllegalArgumentException("Unsupported primitive type: " + type);
    }


    private static boolean isPrimaryKeyField(TableField<?, ?> field) {
        var table = field.getTable();
        if (table == null) {
            return false;
        }

        var primaryKey = table.getPrimaryKey();
        return primaryKey != null
                && primaryKey.getFields().stream()
                .anyMatch(primaryKeyField -> primaryKeyField.equals(field));
    }
}