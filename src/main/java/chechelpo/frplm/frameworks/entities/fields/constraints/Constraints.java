package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.NotNull;
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

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isNullable() {
        return isNullable;
    }

    protected FieldType type() {
        return fieldType;
    }


    protected abstract void throwIfImpossibleValue(@NotNull R value) throws InvalidValue;
    /**
     * @param field field to get the name from
     * @param value value to check
     */
    public final void throwOnConstraintViolation(@NotNull TableField<?, ?> field, Object value, boolean editing) throws InvalidValue {
        if (editing && isReadOnly)
            throw new InvalidValue("Tried to edit read-only field " + field.getName());
        if (value == null){
            if (!isNullable) throw new InvalidValue("Null found in" + field + " NotNull column");
            return;
        }
        try{
            throwIfImpossibleValue((R) value);
        } catch (InvalidValue ex) {
            throw new InvalidValue("Violation when checking " + field + ": \n" + ex.getMessage());
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