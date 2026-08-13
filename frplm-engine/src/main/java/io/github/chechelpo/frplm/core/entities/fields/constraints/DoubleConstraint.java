package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class DoubleConstraint implements Constraint<Double> {
    private final Double min;
    private final Double max;

    @Contract(pure = true)
    private DoubleConstraint(@NotNull DoubleConstraintsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
    }

    public Double min() {
        return min;
    }

    public Double max() {
        return max;
    }

    @Override
    @Contract(pure = true)
    public Optional<String> returnReasonIfInvalid(@NotNull Double value) {
        if (value < min || value > max)
            return Optional.of(
                    "Value must be between " + min + " and " + max
                            + " and its value is " + value
            );

        return Optional.empty();
    }

    @Contract(" -> new")
    public static @NotNull DoubleConstraintsBuilder builder() {
        return new DoubleConstraintsBuilder();
    }

    public static class DoubleConstraintsBuilder
            extends ABSConstraintsBuilder<
                    Double,
                    DoubleConstraint,
                    DoubleConstraintsBuilder
            > {
        private Double min = - Double.MAX_VALUE;
        private Double max = Double.MAX_VALUE;

        DoubleConstraintsBuilder() {}

        @Override
        protected DoubleConstraintsBuilder self() {
            return this;
        }

        public DoubleConstraintsBuilder setMin(@Nullable Double min) {
            this.min = min;
            return this;
        }

        public DoubleConstraintsBuilder setMax(@Nullable Double max) {
            this.max = max;
            return this;
        }

        public DoubleConstraint build() {
            if (min >= max)
                throw new IllegalStateException(
                        "Invalid range: [" + min + "," + max + "]"
                );

            return new DoubleConstraint(this);
        }
    }
}