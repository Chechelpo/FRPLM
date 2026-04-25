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
public final class NumberConstraints extends Constraints<FieldKind.NumberKind> {
    private final Long min;
    private final Long max;
    private final FieldType fieldType;
    private final boolean read_only;
    private final boolean is_key;

    @Contract(pure = true)
    private NumberConstraints(@NotNull NumberConstraintsBuilder builder) {
        super(builder.read_only, builder.fieldType);
        this.min = builder.min;
        this.max = builder.max;
        this.read_only = builder.read_only;
        this.is_key = builder.is_key;
        this.fieldType = builder.fieldType;
    }

    public static @NotNull NumberConstraintsBuilder builder(FieldType fieldType) {
        return new NumberConstraintsBuilder(fieldType);
    }

    public static class NumberConstraintsBuilder {
        private Long min;
        private Long max;
        private final FieldType fieldType;
        private boolean read_only = false;
        private boolean is_key = false;

        private NumberConstraintsBuilder(FieldType fieldType) {
            if (!fieldType.isValidNumber()) throw new IllegalArgumentException("FieldType is not valid number");
            this.fieldType = fieldType;
        }

        public NumberConstraintsBuilder setMin(@Nullable Long min) {
            this.min = min;
            return this;
        }

        public NumberConstraintsBuilder setMax(@Nullable Long max) {
            this.max = max;
            return this;
        }

        public NumberConstraintsBuilder readOnly() {
            this.read_only = true;
            return this;
        }

        public NumberConstraintsBuilder key() {
            this.is_key = true;
            return this;
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull NumberConstraints build(){
            if (is_key && !read_only) {
                throw new IllegalArgumentException("Key field must be read only");
            }
            return new NumberConstraints(this);
        }
    }

    public Long getMin() {
        return min;
    }

    public Long getMax() {
        return max;
    }

    @Override
    public FieldType type() {
        return fieldType;
    }

    @Override
    public boolean isReadOnly() {
        return read_only;
    }

    public boolean isKey() {
        return is_key;
    }
}
