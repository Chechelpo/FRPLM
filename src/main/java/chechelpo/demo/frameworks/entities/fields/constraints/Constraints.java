package chechelpo.demo.frameworks.entities.fields.constraints;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

public sealed abstract class Constraints<T> permits BoolConstraints, FloatConstraints, NumberConstraints, StringConstraints{
    protected final boolean read_only;
    protected final FieldType fieldType;

    protected Constraints(boolean read_only, FieldType fieldType) {
        this.read_only = read_only;
        this.fieldType = fieldType;
    }

    public boolean isReadOnly(){
        return read_only;
    }
    protected FieldType type(){
        return fieldType;
    }

    protected void violationMessage(@Nullable TableField<?, ?> field, Object value){
        assert field != null;
        System.err.println("Field " + field.getName() + " violated constraint with value: " + value);
    }
    /**
     * @param field to get the name from
     * @param value the value you want to check
     * @return true if it violates constraints
     */
    public boolean violatesConstraints(@Nullable TableField<?, ?> field, Object value){
        switch (this.fieldType){
            case BOOLEAN:
                if (!(value instanceof Boolean)){
                    violationMessage(field, value);
                    return true;
                }
                break;
            case SHORT, BYTE, LONG, INTEGER:
                if (!(value instanceof Integer || value instanceof Long || value instanceof Byte || value instanceof Short)){
                    violationMessage(field, value);
                    return true;
                }
                break;
            case STRING:
                if (!(value instanceof String || value instanceof Character)){
                    violationMessage(field, value);
                    return true;
                }
                break;
            case FLOAT, DOUBLE:
                if (!(value instanceof Float || value instanceof Double)){
                    violationMessage(field, value);
                    return true;
                }
                break;
        }

        return false;
    }
}
