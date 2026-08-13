package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class FloatConstraint implements Constraint<Float> {
    private final Float min;
    private final Float max;

    @Contract(pure = true)
    private FloatConstraint(@NotNull FloatConstraintsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
    }

    public Float min() {
        return min;
    }

    public Float max() {
        return max;
    }

    @Override
    @Contract(pure = true)
    public Optional<String> returnReasonIfInvalid(@NotNull Float value) {
        if (value<min || value>max)
            return Optional.of("Value must be between " + min + " and " + max + " and its value is " + value);

        return Optional.empty();
    }

    @Contract(" -> new")
    public static @NotNull FloatConstraintsBuilder builder() {
        return new FloatConstraintsBuilder();
    }

    public static class FloatConstraintsBuilder extends ABSConstraintsBuilder<Float, FloatConstraint, FloatConstraintsBuilder>
    {
        private Float min = - Float.MAX_VALUE;
        private Float max = Float.MAX_VALUE;

        FloatConstraintsBuilder() {}

        @Override
        protected FloatConstraintsBuilder self() {
            return this;
        }

        public FloatConstraintsBuilder setMin(@Nullable Float min) {
            this.min = min;
            return this;
        }

        public FloatConstraintsBuilder setMax(@Nullable Float max) {
            this.max = max;
            return this;
        }

        public FloatConstraint build() {
            if (min >= max) throw new IllegalStateException("Invalid range: [" + min + "," + max + "]");
            return new FloatConstraint(this);
        }
    }
}