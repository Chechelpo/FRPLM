package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.utils.format.StandardFormats;
import org.jetbrains.annotations.*;
import org.jooq.Condition;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.jooq.impl.DSL.trueCondition;

/**
 * Record used for identifying a specific table entry.
 * @param <R> table record class which can be identified by an instance of this class
 */
public final class EntityKey<R extends TableRecord<R>>
{
    //Type checking done at FieldsABS, not here
    private final Map<TableField<R, ?>, Object> values;

    EntityKey(Map<TableField<R, ?>, Object> values, boolean mutable){
        this.values = values;
    }

    public @UnmodifiableView Map<TableField<R, ?>, Object> getValues() {
        return values;
    }
    public @NotNull <T> T requireValue(TableField<R, ?> field) {
        return (T) Objects.requireNonNull(values.get(field), field.getName() + " is not assigned by this key");
    }

    public boolean isEmpty(){
        return values.isEmpty();
    }

    public @NotNull Condition @NotNull [] getEqualityConditions() {
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
    public <T> @NotNull Optional<T> get(TableField<R, T> field){
        return (Optional<T>) Optional.ofNullable(values.get(field));
    }
    /**
     * @return id of the object in string format as specified by {@link StandardFormats#formatIDUnion(Map)}
     */
    public @NotNull String toFolderName(){
        return StandardFormats.formatIDUnion(values);
    }

    public boolean assignsField(TableField<R, ?> field){
        return values.containsKey(field);
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
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<TableField<R, ?>, Object> e : values.entrySet()) {
            sb.append(e.getKey().getName()).append(" = ").append(e.getValue()).append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    @Contract("-> new")
    public static <Rec extends TableRecord<Rec>> @NotNull Builder<Rec> builder(){
        return new Builder<>();
    }

    public static <Rec extends TableRecord<Rec>> EntityKey<Rec> of(){
        return EntityKey.<Rec>builder().build();
    }
    @CheckReturnValue
    public static <Rec extends TableRecord<Rec>, T> @Unmodifiable @NotNull EntityKey<Rec> of(
            @NotNull TableField<Rec, T> field,
            T value
    ) {
        return EntityKey.<Rec>builder()
                .set(field, value)
                .immutable()
                .build();
    }

    @Deprecated
    @CheckReturnValue
    public static <Rec extends TableRecord<Rec>> @Unmodifiable @NotNull EntityKey<Rec> ofValues(
            Map<TableField<Rec, ?>, Object> values
    ){
        return EntityKey.<Rec>builder()
                .immutable()
                .setAll(values)
                .build();
    }
    public static <Rec extends TableRecord<Rec>, T> @NotNull Builder<Rec> builder(
            @NotNull TableField<Rec, T> field,
            T value
    ) {
        return EntityKey.<Rec>builder().set(field, value);
    }

    public static class Builder<R extends TableRecord<R>> {
        private final Map<TableField<R, ?>, Object> values = new HashMap<>();
        private boolean mutable;
        public Builder(){}
        public Builder<R> setAll(@NotNull Map<TableField<R, ?>, Object> values){
            this.values.putAll(values);
            return this;
        }
        public <T> Builder<R> set(TableField<R, T> field, T value){
            values.put(field, value);
            return this;
        }
        public Builder<R> immutable(){
            mutable = true;
            return this;
        }

        @Deprecated
        Builder<R> unsafeSet(@NotNull TableField<R, ?> field, Object value){
            values.put(field, value);
            return this;
        }

        public EntityKey<R> build(){
            return new EntityKey<R>(this.values, mutable);
        }
    }
}
