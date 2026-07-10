package io.github.chechelpo.frplm.core.entities.fields.constraints;

import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldKind;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

///
/// # String constraints (Data carrier)
///
/// Specifies expected constraints of a string field
///
/// ## Defaults:
///
///  - minLength = `Int.MinValue`
///  - maxLength = `Int.MaxValue`
///  - read_only = `false`
///  - allows_placeholders = `true`
///
public final class StringConstraint extends Constraint<FieldKind.StringKind, String> {
    private final int minLength;
    private final int maxLength;
    private final Set<String> allowedValues;
    private final boolean allows_outlets;

    @Contract(pure = true)
    private StringConstraint(@NotNull StringConstraint.StringConstraintsBuilder builder) {
        super(builder, FieldType.STRING);
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.allows_outlets = builder.allows_outlets;
        this.allowedValues = Set.of(builder.possible_values.toArray(String[]::new));
    }

    @Override
    public FieldType type() {
        return FieldType.STRING;
    }

    @Override
    @Contract(pure = true)
    protected @NotNull Optional<String> getInvalidValueReason(@NotNull String value) {
        if (!this.allowedValues.isEmpty() && !this.allowedValues.contains(value))
            return Optional.of("Value: " + value + " not in allow list \n Allow list: " + allowedValues);
        if (value.length() < minLength || value.length() > maxLength)
            return Optional.of("Value: " + value + " must be between " + minLength + " and " + maxLength);

        //TODO : ADD ALLOWED OUTLETS CHECK
        return Optional.empty();
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull StringConstraint.StringConstraintsBuilder builder() {
        return new StringConstraintsBuilder();
    }
    public static class StringConstraintsBuilder extends ABSConstraintsBuilder<StringConstraint, StringConstraintsBuilder> {
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
        public @NotNull StringConstraint build(){
            if ( maxLength <= minLength) {
                throw new IllegalArgumentException("Invalid range");
            }
            return new StringConstraint(this);
        }
    }
}
