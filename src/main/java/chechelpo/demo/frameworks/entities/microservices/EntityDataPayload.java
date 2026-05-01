package chechelpo.demo.frameworks.entities.microservices;

import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.HashMap;
import java.util.Map;

public class EntityDataPayload<R extends TableRecord<R>> {
    private final Map<TableField<R,?>, Object> assignments;

    public EntityDataPayload(Map<TableField<R, ?>, Object> assignments) {
        this.assignments = assignments;
    }
    public EntityDataPayload() {
        this.assignments = new HashMap<>();
    }

    public static <Rec extends TableRecord<Rec>> @NotNull EntityDataPayload<Rec> fromValues(Map<TableField<Rec,?>, Object> values){
        EntityDataPayload<Rec> obj = new EntityDataPayload<>();
        obj.setValues(values);
        return obj;
    }

    public <T> void setValue(TableField<R,T> field, T value){
        assignments.put(field, value);
    }
    void unsafeSetValue(TableField<R,?> field, Object value){
        assignments.put(field, value);
    }
    /**
     * @apiNote Type checks at runtime, prefer {@link #setValue(TableField, Object)} if possible.
     */
    public void setValues(@NotNull Map<TableField<R,?>, Object> values) {
        values.forEach((field, value) -> {
            if (value != null && !field.getType().isInstance(value)) {
                throw new IllegalArgumentException(
                        "Type mismatch for field " + field.getName() +
                                " expected " + field.getType().getName() +
                                " got " + value.getClass().getName()
                );
            }
        });
        assignments.putAll(values);
    }

    public boolean assignsField(TableField<R,?> field) {
        return assignments.containsKey(field);
    }

    public Map<TableField<R,?>, Object> values() {
        return assignments;
    }

    public <T> T getValue(TableField<R,T> field) {
        if (assignments.containsKey(field))
            return (T) assignments.get(field);

        throw new IllegalArgumentException();
    }

    public final boolean isEmpty() {
        return assignments.isEmpty();
    }
    public boolean updatesField(TableField<R,?> field) {
        return assignments.containsKey(field);
    }

    public Map<TableField<R,?>, Object> assignments() {
        return assignments;
    }

    @Override
    public String toString() {
        return "Update object with assignments: \n " + assignments;
    }
}
