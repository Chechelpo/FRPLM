package chechelpo.demo.frameworks.entities.fields.constraints;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

public sealed abstract class Constraints<T, R> permits BoolConstraints, FloatConstraints, NumberConstraints, StringConstraints{
    protected final boolean read_only;
    protected final FieldType fieldType;

    protected Constraints(boolean read_only, FieldType fieldType) {
        this.read_only = read_only;
        this.fieldType = fieldType;
    }
    public abstract R coerce(Object value);
    public boolean isReadOnly(){
        return read_only;
    }
    protected FieldType type(){
        return fieldType;
    }

    protected void violationMessage(@Nullable TableField<?, ?> field, Object value){
        assert field != null;
        System.err.println("Field " + field.getName() + " violated constraint with value: " + value + " expected " + fieldType);
    }
    /**
     * @param field to get the name from
     * @param value the value you want to check
     * @return true if it violates constraints
     */
    public boolean violatesConstraints(@Nullable TableField<?, ?> field, Object value) {
        // Null is always a violation (assuming columns are non-nullable)
        if (value == null) {
            violationMessage(field, value);
            return true;
        }

        switch (this.fieldType) {
            case BOOLEAN:
                if (value instanceof Boolean) {
                    return false;
                }
                // Try to parse as boolean from string
                if (value instanceof String) {
                    String s = (String) value;
                    if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
                        return false;
                    }
                }
                violationMessage(field, value);
                return true;

            case SHORT:
            case BYTE:
            case LONG:
            case INTEGER:
                // Already a numeric type? Accept it.
                if (value instanceof Number) {
                    return false;
                }
                // Try to parse the string representation as a long
                try {
                    Long.parseLong(value.toString());
                    return false; // parse succeeded
                } catch (NumberFormatException e) {
                    violationMessage(field, value);
                    return true;
                }

            case STRING:
                if (value instanceof String || value instanceof Character) {
                    return false;
                }
                violationMessage(field, value);
                return true;

            case FLOAT:
            case DOUBLE:
                // Already a floating-point numeric type? Accept it.
                if (value instanceof Float || value instanceof Double) {
                    return false;
                }
                // Try to parse as double
                try {
                    Double.parseDouble(value.toString());
                    return false; // parse succeeded
                } catch (NumberFormatException e) {
                    violationMessage(field, value);
                    return true;
                }
        }

        return false;
    }
}
