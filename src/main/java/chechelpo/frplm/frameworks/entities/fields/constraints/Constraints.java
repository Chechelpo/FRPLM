package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

public sealed abstract class Constraints<T, R>
        permits BoolConstraints, FloatConstraints, NumberConstraints, StringConstraints {

    protected final boolean isReadOnly;
    protected final boolean isNullable;
    protected final FieldType fieldType;

    protected Constraints(ABSConstraintsBuilder<?, ?> builder, FieldType fieldType) {
        this.isReadOnly = builder.read_only;
        this.isNullable = builder.nullable;
        this.fieldType = fieldType;
    }

    public abstract R coerce(Object value) throws InvalidValue;

    protected void throwIfImpossibleValue(R value) throws InvalidValue {}

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isNullable() {
        return isNullable;
    }

    protected FieldType type() {
        return fieldType;
    }

    protected void violationMessage(@Nullable TableField<?, ?> field, Object value, InvalidValue exception) {
        assert field != null;
        System.err.println(
                "Field " + field.getName()
                        + " violated constraint with value: " + value  + " type " + fieldType + "\n"
                        + exception.getMessage()
        );
    }

    /**
     * @param field field to get the name from
     * @param value value to check
     * @return true if the value violates constraints
     */
    public boolean violatesConstraints(@Nullable TableField<?, ?> field, Object value) {
        if (value == null) {
            if (!isNullable) {
                violationMessage(field, null, new InvalidValue("Null found in NotNull column"));
                return true;
            }

            return false;
        }

        try{
            R coerced = this.coerce(value);
            throwIfImpossibleValue(coerced);

            return false;
        } catch (InvalidValue e) {
            violationMessage(field, value, e);
            return true;
        }
    }

    public abstract static class ABSConstraintsBuilder<
            C extends Constraints<?, ?>,
            B extends ABSConstraintsBuilder<C, B>
            > {
        protected boolean read_only = false;
        protected boolean nullable = false;

        protected abstract B self();

        public B readOnly() {
            this.read_only = true;
            return self();
        }

        public B nullable() {
            this.nullable = true;
            return self();
        }

        public abstract C build();
    }
}