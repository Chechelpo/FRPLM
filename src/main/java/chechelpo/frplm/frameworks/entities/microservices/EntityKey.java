package chechelpo.frplm.frameworks.entities.microservices;

import chechelpo.frplm.utils.format.StandardFormats;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.HashMap;
import java.util.Map;

import static org.jooq.impl.DSL.trueCondition;

/**
 * Record used for identifying a specific table entry.
 * @param <R> table record class which can be identified by an instance of this class
 */
public class EntityKey<R extends TableRecord<R>>
{
    //Type checking done at FieldsABS, not here
    private final Map<TableField<R, ?>, Object> values;
    private EntityKey(Map<TableField<R, ?>, Object> values){
        this.values = values;
    }

    @Contract("_ -> new")
    public static<Rec extends TableRecord<Rec>> @NotNull Builder<Rec> builder(){
        return new Builder<>();
    }

    public static class Builder<R extends TableRecord<R>> {
        private final Map<TableField<R, ?>, Object> values = new HashMap<>();
        public Builder(){}
        public Builder<R> setAll(@NotNull Map<TableField<R, ?>, Object> values){
            this.values.putAll(values);
            return this;
        }
        public <T> Builder<R> set(TableField<R, T> field, T value){
            values.put(field, value);
            return this;
        }
        public Builder<R> unsafeSet(@NotNull TableField<R, ?> field, Object value){
            values.put(field, value);
            return this;
        }

        public EntityKey<R> build(){
            return new EntityKey<R>(this.values);
        }
    }

    void setOrReplace(TableField<R, ?> field, Object value){
        values.put(field, value);
    }

    public Map<TableField<R, ?>, Object> getValues() {
        return values;
    }

    public Map<TableField<R, ?>, Object> values() {
        return values;
    }

    public @NotNull Condition[] getEqualityConditions() {
        return values.entrySet().stream()
                .map(e -> {
                    TableField field = (TableField) e.getKey();
                    Object value = e.getValue();
                    return value == null ? field.isNull() : field.eq(value);
                })
                .toArray(Condition[]::new);
    }

    public @NotNull Condition getPkCondition() {
        Condition c = trueCondition();
        for (Condition k : getEqualityConditions()) c = c.and(k);
        return c;
    }

    public <T> @NotNull T getValue(TableField<R, T> field){
        return (T) values.get(field);
    }
    /**
     * @return id of the object in string format as specified by {@link StandardFormats#formatIDUnion(Map)}
     */
    public @NotNull String toFolderName(){
        return StandardFormats.formatIDUnion(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (this == obj) return true;
        if (obj instanceof EntityKey) {
            try{
                EntityKey<R> other = (EntityKey<R>) obj;
                return values.equals(other.values);
            }catch(ClassCastException e){
                return false;
            }
        }

        return false;
    }

    public @NotNull String toString(){
        return values.toString();
    }
}
