package chechelpo.frplm.frameworks.entities.microservices;

import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.HashMap;
import java.util.Map;

public final class EntityDataPayload<R extends TableRecord<R>> {
    private final Map<TableField<R, ?>, Object> assignments;

    public EntityDataPayload(Map<TableField<R, ?>, Object> assignments) {
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

    public static <Rec extends TableRecord<Rec>, T> @NotNull EntityDataPayload<Rec> of(
            TableField<Rec, T> field,
            T value
    ) {
        return EntityDataPayload.<Rec>builder()
                .set(field, value)
                .build();
    }

    public <T> void set(TableField<R, T> field, T value) {
        assignments.put(field, value);
    }

    void unsafeSetValue(TableField<R, ?> field, Object value) {
        assignments.put(field, value);
    }

    /**
     * @apiNote Type checks at runtime, prefer {@link #set(TableField, Object)} if possible.
     */
    public void setValues(@NotNull Map<TableField<R, ?>, Object> values) {
        validateValues(values);
        assignments.putAll(values);
    }

    public boolean assignsField(TableField<R, ?> field) {
        return assignments.containsKey(field);
    }

    public Map<TableField<R, ?>, Object> values() {
        return assignments;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(TableField<R, T> field) {
        if (assignments.containsKey(field)) {
            return (T) assignments.get(field);
        }

        throw new IllegalArgumentException("Unknown field " + field.getName());
    }

    public final boolean isEmpty() {
        return assignments.isEmpty();
    }

    public boolean updatesField(TableField<R, ?> field) {
        return assignments.containsKey(field);
    }

    public Map<TableField<R, ?>, Object> assignments() {
        return assignments;
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
}