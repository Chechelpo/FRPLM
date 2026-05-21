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
    private final int minLength;
    private final int maxLength;
    private final Set<String> allowedValues = new HashSet<>();
    private final boolean allows_outlets;

    @Contract(pure = true)
    private StringConstraints(@NotNull StringConstraints.StringConstraintsBuilder builder) {
        super(builder, FieldType.STRING);
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.allows_outlets = builder.allows_outlets;
        this.allowedValues.addAll(builder.possible_values);
    }

    @Override
    public FieldType type() {
        return FieldType.STRING;
    }

    public boolean allowsOutlets() {
        return allows_outlets;
    }

    @Override
    protected void throwIfImpossibleValue(@NotNull String value) throws InvalidValue {
        if (!this.allowedValues.isEmpty() && !this.allowedValues.contains(value))
            throw new InvalidValue("Value: " + value + " not in allow list \n Allow list: " + allowedValues);
        if (value.length() < minLength || value.length() > maxLength)
            throw new InvalidValue("Value: " + value + " must be between " + minLength + " and " + maxLength);
        // TODO: Add allows outlets check
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull StringConstraints.StringConstraintsBuilder builder() {
        return new StringConstraintsBuilder();
    }
    public static class StringConstraintsBuilder extends ABSConstraintsBuilder<StringConstraints, StringConstraintsBuilder> {
        private int minLength = 0;
        private int maxLength = Integer.MAX_VALUE;
        private Set<String> possible_values = new HashSet<>();
        private boolean allows_outlets = false;

        @Override
        protected StringConstraintsBuilder self() {
            return this;
        }

        public StringConstraintsBuilder setMinLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        public StringConstraintsBuilder setMaxLength(int maxLength) {
            this.maxLength = maxLength;
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
            if ( maxLength <= minLength) {
                throw new IllegalArgumentException("Invalid range");
            }
            return new StringConstraints(this);
        }
    }
}
