package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

///
/// # Number constraints (Data carrier)
///
/// Specifies expected constraints of a number field
///
/// ## Defaults:
///
///  - min = `null`
///  - max = `null`
///  - read_only = `false`
///  - is_key = `false`
///
public final class FloatConstraints extends Constraints<FieldKind.FloatKind, Number> {
    private final Double min;
    private final Double max;
    private final boolean is_key;
    private final FieldType fieldType;

    private FloatConstraints(@NotNull FloatConstraintsBuilder builder) {
        super(builder, builder.fieldType);
        this.min = builder.min;
        this.max = builder.max;
        this.is_key = builder.is_key;
        this.fieldType = builder.fieldType;
    }

    public static FloatConstraintsBuilder builder(FieldType fieldType) {
        return new FloatConstraintsBuilder(fieldType);
    }

    public static class FloatConstraintsBuilder extends ABSConstraintsBuilder<FloatConstraints, FloatConstraintsBuilder>
    {
        private final FieldType fieldType;
        private Double min;
        private Double max;
        private boolean read_only = false;
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

        public FloatConstraintsBuilder readOnly() {
            this.read_only = true;
            return this;
        }

        public FloatConstraintsBuilder key() {
            this.is_key = true;
            return this;
        }

        public FloatConstraints build() {
            if (is_key && !read_only) {
                throw new IllegalArgumentException("Key field must be read only");
            }

            return new FloatConstraints(this);
        }
    }

    @Override
    public Number coerce(Object value) {
        return switch (value) {
            case null -> null;
            case Float f -> f;
            case Double d -> d;
            case Number n -> n.doubleValue();
            case String s -> fieldType == FieldType.FLOAT
                    ? Float.parseFloat(s)
                    : Double.parseDouble(s);
            default -> throw new IllegalArgumentException("Cannot coerce " + value + " to " + fieldType);
        };
    }

    @Override
    public FieldType type() {
        return fieldType;
    }
}