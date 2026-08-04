package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class StringConstraint implements Constraint<String> {
    private final int minLength;
    private final int maxLength;

    @Contract(pure = true)
    private StringConstraint(@NotNull StringConstraintsBuilder builder) {
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<String> returnReasonIfInvalid(
            @NotNull String value
    ) {
        int length = value.length();

        if (length < minLength || length > maxLength) {
            return Optional.of(
                    "Value length must be between "
                            + minLength
                            + " and "
                            + maxLength
                            + ", but its length is "
                            + length
            );
        }

        return Optional.empty();
    }

    @Contract(" -> new")
    public static @NotNull StringConstraintsBuilder builder() {
        return new StringConstraintsBuilder();
    }

    public static final class StringConstraintsBuilder
            extends ABSConstraintsBuilder<
            String,
            StringConstraint,
            StringConstraintsBuilder
            > {

        private int minLength = 0;
        private int maxLength = Integer.MAX_VALUE;

        StringConstraintsBuilder() {
        }

        @Override
        protected @NotNull StringConstraintsBuilder self() {
            return this;
        }

        public @NotNull StringConstraintsBuilder setMinLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        public @NotNull StringConstraintsBuilder setMaxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        @Override
        @Contract(" -> new")
        public @NotNull StringConstraint build() {
            if (minLength < 0) {
                throw new IllegalStateException(
                        "Minimum string length cannot be negative: " + minLength
                );
            }

            if (minLength > maxLength) {
                throw new IllegalStateException(
                        "Invalid length range: ["
                                + minLength
                                + ","
                                + maxLength
                                + "]"
                );
            }

            return new StringConstraint(this);
        }
    }
}