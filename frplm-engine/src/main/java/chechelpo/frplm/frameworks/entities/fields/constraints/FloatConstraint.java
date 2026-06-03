package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

///
/// # Number constraints (Data carrier)
///
/// Specifies expected constraints of a number field
///
/// ## Defaults:
///
///  - min = `Double.minvalue`
///  - max = `Double.maxValue`
///  - read_only = `false`
///  - is_key = `false`
///
@Unmodifiable
public final class FloatConstraint extends Constraint<FieldKind.FloatKind, Number> {
    private final Double min;
    private final Double max;
    private final boolean is_key;
    private final FieldType fieldType;

    @Contract(pure = true)
    private FloatConstraint(@NotNull FloatConstraintsBuilder builder) {
        super(builder, builder.fieldType);
        this.min = builder.min;
        this.max = builder.max;
        this.is_key = builder.is_key;
        this.fieldType = builder.fieldType;
    }

    @Override
    public FieldType type() {
        return fieldType;
    }

    @Override
    @Contract(pure = true)
    protected Optional<String> getInvalidValueReason(@NotNull Number value) {
        double doubleValue = value.doubleValue();
        if (doubleValue<min || doubleValue>max)
            return Optional.of("Value must be between " + min + " and " + max + " and its value is " + doubleValue);

        return Optional.empty();
    }

    @Contract("_ -> new")
    public static @NotNull FloatConstraintsBuilder builder(FieldType fieldType) {
        return new FloatConstraintsBuilder(fieldType);
    }
    public static class FloatConstraintsBuilder extends ABSConstraintsBuilder<FloatConstraint, FloatConstraintsBuilder>
    {
        private final FieldType fieldType;
        private Double min;
        private Double max;
        private boolean is_key = false;

        public FloatConstraintsBuilder(FieldType fieldType) {
            if (!fieldType.isValidFloat()) {
                throw new IllegalArgumentException("FieldType is not valid float");
            }

            this.fieldType = fieldType;
        }

        @Override
        protected FloatConstraintsBuilder self() {
            return this;
        }

        public FloatConstraintsBuilder setMin(@Nullable Double min) {
            this.min = min;
            return this;
        }

        public FloatConstraintsBuilder setMax(@Nullable Double max) {
            this.max = max;
            return this;
        }

        public FloatConstraintsBuilder key() {
            this.is_key = true;
            return this;
        }

        public FloatConstraint build() {
            if (is_key && !read_only) {
                throw new IllegalArgumentException("Key field must be read only");
            }

            return new FloatConstraint(this);
        }
    }
}