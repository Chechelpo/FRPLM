package io.github.chechelpo.frplm.core.entities.fields;

import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EntityDataPayload<R extends TableRecord<R>> implements DataPayload<R> {
    private final Map<TableField<R, ?>, Object> assignments;

    EntityDataPayload(Map<TableField<R, ?>, Object> assignments) {
        this.assignments = new HashMap<>(assignments);
    }

    public EntityDataPayload() {
        this.assignments = new HashMap<>();
    }

    public static <Rec extends TableRecord<Rec>> @NotNull Builder<Rec> builder() {
        return new Builder<>();
    }

    public static <Rec extends TableRecord<Rec>> @NotNull EntityDataPayload<Rec> fromValues(
            Map<TableField<Rec, ?>, Object> values
    ) {
        EntityDataPayload<Rec> obj = new EntityDataPayload<>();
        obj.setValues(values);
        return obj;
    }
    public static <Rec extends TableRecord<Rec>> @NotNull EntityDataPayload<Rec> of() {
        return EntityDataPayload.<Rec>builder().build();
    }
    public static <Rec extends TableRecord<Rec>, T> @NotNull EntityDataPayload<Rec> of(
            TableField<Rec, T> field,
            T value
    ) {
        return EntityDataPayload.<Rec>builder()
                .set(field, value)
                .build();
    }

    @Override
    public boolean isEmpty() {
        return assignments.isEmpty();
    }
    @Override
    public Map<TableField<R, ?>, Object> assignments() {
        return assignments;
    }

    @Override
    public <T> Assignment<R, T> getAssignment(TableField<R, T> field) {
        if (assignments.containsKey(field))
            //noinspection unchecked
            return Assignment.ofAssigned(field, (T) assignments.get(field));

        return Assignment.ofUnassigned(field);
    }

    @Override
    public boolean assigns(TableField<R, ?> field) {
        return assignments.containsKey(field);
    }

    @Override
    public void consume(Assignment<R, ?> assignment) {
        if (!assignment.isAssigned()) return;

        assignments.put(assignment.field, assignment.get());
    }

    public <T> void set(TableField<R, T> field, T value) {
        assignments.put(field, value);
    }

    void unsafeSetValue(TableField<R, ?> field, Object value) {
        assignments.put(field, value);
    }

    /**
     * @deprecated  Type checks at runtime, prefer {@link #set(TableField, Object)} if possible.
     */
    public void setValues(@NotNull Map<TableField<R, ?>, Object> values) {
        validateValues(values);
        assignments.putAll(values);
    }

    /** @deprecated use {@link #getAssignment(TableField)} instead **/
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> getValue(TableField<R, T> field) {
        return Optional.ofNullable((T) assignments.get(field));
    }


    /**
     * Converts every assignment into an equality condition.
     *
     * Example:
     *   ID = 10
     *   NAME = "Marco"
     */
    public List<Condition> asEqualityConditions() {
        return assignments.entrySet()
                .stream()
                .map(entry -> equalityCondition(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    /**
     * Combines every equality condition using AND.
     *
     * Example:
     *   ID = 10 AND NAME = "Marco"
     */
    public Condition asEqualityCondition() {
        return DSL.and(asEqualityConditions());
    }

    private static <Rec extends TableRecord<Rec>, T> Condition equalityCondition(
            TableField<Rec, T> field,
            Object value
    ) {
        if (value == null) {
            return field.isNull();
        }

        T convertedValue = field.getDataType().convert(value);

        return field.eq(convertedValue);
    }

    @Override
    public String toString() {
        return "Update object with assignments: \n " + assignments;
    }

    private static <Rec extends TableRecord<Rec>> void validateValues(
            @NotNull Map<TableField<Rec, ?>, Object> values
    ) {
        values.forEach((field, value) -> {
            if (value != null && !field.getType().isInstance(value)) {
                throw new IllegalArgumentException(
                        "Type mismatch for field " + field.getName() +
                                " expected " + field.getType().getName() +
                                " got " + value.getClass().getName()
                );
            }
        });
    }

    public static final class Builder<Rec extends TableRecord<Rec>> {
        private final Map<TableField<Rec, ?>, Object> assignments = new HashMap<>();

        private Builder() {
        }

        public <T> @NotNull Builder<Rec> set(TableField<Rec, T> field, T value) {
            assignments.put(field, value);
            return this;
        }
        @NotNull Builder<Rec> unsafeSet(TableField<Rec, ?> field, Object value) {
            assignments.put(field, value);
            return this;
        }

        /**
         * @apiNote Type checks at runtime, prefer {@link #set(TableField, Object)} if possible.
         */
        public @NotNull Builder<Rec> setValues(@NotNull Map<TableField<Rec, ?>, Object> values) {
            validateValues(values);
            assignments.putAll(values);
            return this;
        }

        public boolean assignsField(TableField<Rec, ?> field) {
            return assignments.containsKey(field);
        }

        public boolean isEmpty() {
            return assignments.isEmpty();
        }

        public @NotNull EntityDataPayload<Rec> build() {
            return new EntityDataPayload<>(assignments);
        }
    }


    public String prettyPrint() {
        if (assignments.isEmpty()) {
            return "EntityDataPayload {}";
        }

        StringBuilder out = new StringBuilder("EntityDataPayload {\n");

        assignments.forEach((key, value) -> out
                .append("  ")
                .append(key.getUnqualifiedName())
                .append(" = ")
                .append(formatValue(value))
                .append('\n'));

        return out.append('}').toString();
    }


    private static String formatValue(Object value) {
        return switch (value) {
            case null -> "null";
            case CharSequence charSequence -> "\"" + value + "\"";
            case Character c -> "'" + value + "'";
            default -> String.valueOf(value);
        };

    }
}