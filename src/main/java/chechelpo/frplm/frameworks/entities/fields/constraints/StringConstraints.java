package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

///
/// # String constraints (Data carrier)
///
/// Specifies expected constraints of a string field
///
/// ## Defaults:
///
///  - minLength = `null`
///  - maxLength = `null`
///  - read_only = `false`
///  - allows_placeholders = `true`
///
public final class StringConstraints extends Constraints<FieldKind.StringKind, String> {
    private final Integer minLength;
    private final Integer maxLength;
    private final Set<String> allowedValues = new HashSet<>();
    private final boolean read_only;
    private final boolean allows_outlets;

    @Contract(pure = true)
    private StringConstraints(@NotNull StringConstraints.StringConstraintsBuilder builder) {
        super(builder, FieldType.STRING);
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.read_only = builder.read_only;
        this.allows_outlets = builder.allows_outlets;
        this.allowedValues.addAll(builder.possible_values);
    }

    @Override
    public String  coerce(Object value) {
        return (String) value;
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull StringConstraints.StringConstraintsBuilder builder() {
        return new StringConstraintsBuilder();
    }

    @Override
    protected void throwIfImpossibleValue(String value) throws InvalidValue {
        if (!this.allowedValues.isEmpty() && !this.allowedValues.contains(value))
            throw new InvalidValue("Value: " + value + " not in allow list \n Allow list: " + allowedValues);
        // TODO: Add allows outlets check
        super.throwIfImpossibleValue(value);
    }

    public static class StringConstraintsBuilder extends ABSConstraintsBuilder<StringConstraints, StringConstraintsBuilder> {
        private Integer minLength;
        private Integer maxLength;
        private Set<String> possible_values = new HashSet<>();
        private boolean read_only = false;
        private boolean allows_outlets = false;

        @Override
        protected StringConstraintsBuilder self() {
            return this;
        }

        public StringConstraintsBuilder setMinLength(@Nullable Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public StringConstraintsBuilder setMaxLength(@Nullable Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public StringConstraintsBuilder readOnly() {
            this.read_only = true;
            return this;
        }

        public StringConstraintsBuilder setPossibleValues(@NotNull String @NotNull ... possible_values) {
            Set<String> possible_values_set = new HashSet<>(possible_values.length);
            Collections.addAll(possible_values_set, possible_values);

            this.possible_values = possible_values_set;

            return this;
        }

        public StringConstraintsBuilder allows_outlets() {
            this.allows_outlets = true;
            return this;
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull StringConstraints build(){
            if (minLength != null && maxLength != null && maxLength <= minLength) {
                throw new IllegalArgumentException("Invalid range");
            }
            return new StringConstraints(this);
        }
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    @Override
    public FieldType type() {
        return FieldType.STRING;
    }

    @Override
    public boolean isReadOnly() {
        return read_only;
    }

    public boolean allowsOutlets() {
        return allows_outlets;
    }
}
