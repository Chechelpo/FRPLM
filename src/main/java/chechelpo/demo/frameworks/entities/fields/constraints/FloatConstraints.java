package chechelpo.demo.frameworks.entities.fields.constraints;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
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
public final class FloatConstraints extends Constraints<FieldKind.FloatKind, Float> {
    private final Double min;
    private final Double max;
    private final boolean is_key;

    @Contract(pure = true)
    private FloatConstraints(@NotNull FloatConstraintsBuilder builder) {
        super(builder.read_only, FieldType.FLOAT);
        this.min = builder.min;
        this.max = builder.max;
        this.is_key = builder.is_key;
    }

    @Override
    public Float coerce(Object value) {
        return switch (value) {
            case null -> null;
            case Float v -> v;
            case String s -> Float.parseFloat(s);
            default -> throw new IllegalArgumentException("Cannot coerce " + value + " to " + FieldType.FLOAT);
        };
    }

    public static class FloatConstraintsBuilder {
        private Double min;
        private Double max;
        private boolean read_only = false;
        private boolean is_key = false;

        public FloatConstraintsBuilder setMin(@Nullable Double min) {
            this.min = min;
            return this;
        }

        public FloatConstraintsBuilder setMax(@Nullable Double max) {
            this.max = max;
            return this;
        }

        public FloatConstraintsBuilder setReadOnly(boolean read_only) {
            this.read_only = read_only;
            return this;
        }

        public FloatConstraintsBuilder setIsKey(boolean is_key) {
            this.is_key = is_key;
            return this;
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull FloatConstraints create(){
            if (is_key && !read_only) {
                throw new IllegalArgumentException("Key field must be read only");
            }
            return new FloatConstraints(this);
        }
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    public FieldType type() {
        return FieldType.FLOAT;
    }

    @Override
    public boolean isReadOnly() {
        return read_only;
    }

    public boolean isKey() {
        return is_key;
    }
}
