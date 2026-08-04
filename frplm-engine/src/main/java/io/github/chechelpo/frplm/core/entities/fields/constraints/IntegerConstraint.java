package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class IntegerConstraint implements Constraint<Integer> {
    private final Integer min;
    private final Integer max;

    @Contract(pure = true)
    private IntegerConstraint(@NotNull IntegerConstraintsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<String> returnReasonIfInvalid(
            @NotNull Integer value
    ) {
        if (value < min || value > max) {
            return Optional.of(
                    "Value must be between " + min + " and " + max
                            + " and its value is " + value
            );
        }

        return Optional.empty();
    }

    @Contract(" -> new")
    public static @NotNull IntegerConstraintsBuilder builder() {
        return new IntegerConstraintsBuilder();
    }

    public static final class IntegerConstraintsBuilder
            extends ABSConstraintsBuilder<
                    Integer,
                    IntegerConstraint,
                    IntegerConstraintsBuilder
            > {

        private Integer min = Integer.MIN_VALUE;
        private Integer max = Integer.MAX_VALUE;

        IntegerConstraintsBuilder() {
        }

        @Override
        protected @NotNull IntegerConstraintsBuilder self() {
            return this;
        }

        public @NotNull IntegerConstraintsBuilder setMin(
                @NotNull Integer min
        ) {
            this.min = min;
            return this;
        }

        public @NotNull IntegerConstraintsBuilder setMax(
                @NotNull Integer max
        ) {
            this.max = max;
            return this;
        }

        @Override
        public @NotNull IntegerConstraint build() {
            if (min > max) {
                throw new IllegalStateException(
                        "Invalid range: [" + min + "," + max + "]"
                );
            }

            return new IntegerConstraint(this);
        }
    }
}