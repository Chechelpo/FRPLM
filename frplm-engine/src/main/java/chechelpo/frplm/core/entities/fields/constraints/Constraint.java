package chechelpo.frplm.core.entities.fields.constraints;

import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Optional;

@Unmodifiable
public sealed abstract class Constraint<T, Primitive>
        permits BoolConstraint, FloatConstraint, NumberConstraint, StringConstraint {

    protected final boolean isReadOnly;
    protected final boolean isNullable;
    protected final FieldType fieldType;

    @Contract(pure = true)
    protected Constraint(@NotNull ABSConstraintsBuilder<?, ?> builder, FieldType fieldType) {
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

    public record ConstraintViolation<R extends TableRecord<R>> (TableField<R, ?> field, String message) {}
    protected abstract Optional<String> getInvalidValueReason(@NotNull Primitive value);
    /**
     * @param field field to get the name from
     * @param value value to check
     */
    @Contract(pure = true)
    public final <Rec extends TableRecord<Rec>> @NotNull Optional<ConstraintViolation<Rec>> validateConstraint(
            @NotNull TableField<Rec, ?> field,
            Object value,
            boolean editing
    ) {
        if (editing && isReadOnly)
            return Optional.of(new ConstraintViolation<>(field, "Tried to assign read-only field"));
        if (value == null)
            return isNullable ? Optional.empty()
                    : Optional.of(new ConstraintViolation<>(field,"Null found in" + field + " NotNull column"));

        Optional<String> invalidValueReason = getInvalidValueReason((Primitive) value);
        return invalidValueReason.map(s -> new ConstraintViolation<>(field, s));

    }

    public abstract static class ABSConstraintsBuilder<
            C extends Constraint<?, ?>,
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