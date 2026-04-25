package chechelpo.demo.frameworks.entities.data;

import chechelpo.demo.exceptions.types.UnknownOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.jooq.impl.DSL.trueCondition;

public class QueryObject<R extends TableRecord<R>> {
    private Map<TableField<R, ?>, FieldQuery<R,?>> queries = new HashMap<>();
    public QueryObject() {}

    public <T> void addQuery(@NotNull TableField<R, T> field, @NotNull String operatorName ,@NotNull T value){
        if (queries.containsKey(field)) throw new IllegalArgumentException("Field already has an active query");
        //No need to check for null, valueOf throws if not found
        ComparisonOperator operator;
        try{
            operator = ComparisonOperator.of(operatorName);
        }catch(IllegalArgumentException e){
            throw new UnknownOperator(operatorName);
        }

        queries.put(field, new FieldQuery<>(field, operator, value));
    }

    private enum ComparisonOperator{
        LIKE(String.class),
        EQUALS(null),
        GREATER_THAN(Integer.class, Float.class),
        LESS_THAN(Integer.class, Float.class),
        ;

        Set<Class<?>> supported_types = null;
        ComparisonOperator(@Nullable Class<?>... supported_types) {
            if (supported_types != null)
                this.supported_types = Set.of(supported_types);
        }

        boolean isSupported(@NotNull Class<?> type) {
            if (supported_types == null) return true;
            return supported_types.contains(type);
        }

        static ComparisonOperator of(@NotNull String name){
            return valueOf(name.toUpperCase().trim());
        }

    }

    private class FieldQuery <R extends TableRecord<R>, T>{
        private final TableField<R,T> field;
        private final ComparisonOperator operator;
        private final T value;

        FieldQuery(@NotNull TableField<R,T> field, @NotNull ComparisonOperator operator, T value){
            //Check if the operator is supported for this data type.
            Class<T> clazz = field.getType();
            if(!operator.isSupported(clazz)) throw new IllegalArgumentException("Unsupported comparison operator: " + operator);

            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        private Condition getCondition() {
            return switch (operator) {
                case EQUALS -> (value == null) ? field.isNull() : field.eq(value);

                case LIKE -> {
                    if (value == null) {
                        yield field.isNull();
                    }
                    if (!(value instanceof String s)) {
                        throw new IllegalStateException("LIKE requires a String value, got: " + value.getClass());
                    }
                    @SuppressWarnings("unchecked")
                    Field<String> stringField = (Field<String>) field;
                    yield stringField.like(s);
                }

                case GREATER_THAN -> {
                    if (value == null) {
                        throw new IllegalArgumentException("GREATER_THAN requires a non-null value for field: " + field.getName());
                    }
                    if (!(value instanceof Comparable)) {
                        throw new IllegalStateException("GREATER_THAN requires Comparable, got: " + value.getClass());
                    }
                    @SuppressWarnings("unchecked")
                    Field<Comparable<Object>> comparableField = (Field<Comparable<Object>>) field;
                    yield comparableField.gt((Comparable<Object>) value);
                }

                case LESS_THAN -> {
                    if (value == null) {
                        throw new IllegalArgumentException("LESS_THAN requires a non-null value for field: " + field.getName());
                    }
                    if (!(value instanceof Comparable)) {
                        throw new IllegalStateException("LESS_THAN requires Comparable, got: " + value.getClass());
                    }
                    @SuppressWarnings("unchecked")
                    Field<Comparable<Object>> comparableField = (Field<Comparable<Object>>) field;
                    yield comparableField.lt((Comparable<Object>) value);
                }
            };
        }
    }

    public Condition getConditions(){
        Condition c = trueCondition();
        for (Map.Entry<TableField<R, ?>, FieldQuery<R,?>> entry : queries.entrySet()){
            c = c.and(entry.getValue().getCondition());
        }
        return c;
    }
}
