package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

///
/// # Number constraints (Data carrier)
///
/// Specifies expected constraints of a number field
///
/// ## Defaults:
///
///  - min = `Long.MinValue`
///  - max = `Long.MaxValue`
///  - read_only = `false`
///  - is_key = `false`
///
public final class NumberConstraint extends Constraint<FieldKind.NumberKind, Number> {
    private final long min;
    private final long max;
    private final @NotNull FieldType fieldType;
    private final LongSet allowed_values;
    private final boolean read_only;
    private final boolean is_key;

    @Contract(pure = true)
    private NumberConstraint(@NotNull NumberConstraintsBuilder builder) {
        super(builder, builder.fieldType);

        this.min = builder.min;
        this.max = builder.max;

        this.read_only = builder.read_only;
        this.is_key = builder.is_key;

        this.allowed_values = builder.possible_values;
        this.fieldType = builder.fieldType;
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

    @Override
    protected Optional<String> getInvalidValueReason(@NotNull Number value) {
        if (!this.allowed_values.isEmpty() && !this.allowed_values.contains(value.longValue()))
            return Optional.of("Invalid value for field: " + value + " \n allowed values are " + allowed_values);
        if (value.longValue() < min || value.longValue() > max)
            return Optional.of(value + " must be between " + min + " and " + max);

        return Optional.empty();
    }

    public static @NotNull NumberConstraintsBuilder builder(FieldType fieldType) {
        return new NumberConstraintsBuilder(fieldType);
    }

    public static class NumberConstraintsBuilder extends ABSConstraintsBuilder<NumberConstraint, NumberConstraintsBuilder> {
        private Long min = Long.MIN_VALUE;
        private Long max = Long.MAX_VALUE;
        private final FieldType fieldType;
        private final LongSet possible_values = new LongOpenHashSet();
        private boolean read_only = false;
        private boolean is_key = false;

        private NumberConstraintsBuilder(FieldType fieldType) {
            if (!fieldType.isValidNumber()) throw new IllegalArgumentException("FieldType is not valid number");
            this.fieldType = fieldType;
        }

        @Override
        protected NumberConstraintsBuilder self(){
            return this;
        }

        public NumberConstraintsBuilder setMin(@Nullable Long min) {
            this.min = min;
            return this;
        }

        public NumberConstraintsBuilder setMax(@Nullable Long max) {
            this.max = max;
            return this;
        }

        public NumberConstraintsBuilder setPossibleValues(int @NotNull ... values){
            Arrays.stream(values).forEach(i -> possible_values.add((long) i));
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
        public @NotNull NumberConstraint build(){
            if (is_key && !read_only) {
                throw new IllegalArgumentException("Key field must be read only");
            }
            return new NumberConstraint(this);
        }
    }

    @Override
    public String toString() {
        return "NumberConstraints type: " + this.type() + " [min=" + min + ", max=" + max + "] allowed_values=" + Arrays.toString(allowed_values.toArray());
    }
}
