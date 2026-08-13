package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class ShortConstraint implements Constraint<Short> {
    private final Short min;
    private final Short max;

    @Contract(pure = true)
    private ShortConstraint(@NotNull ShortConstraintsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
    }

    public Short min() {
        return min;
    }

    public Short max() {
        return max;
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<String> returnReasonIfInvalid(
            @NotNull Short value
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
    public static @NotNull ShortConstraintsBuilder builder() {
        return new ShortConstraintsBuilder();
    }

    public static final class ShortConstraintsBuilder
            extends ABSConstraintsBuilder<
                    Short,
                    ShortConstraint,
                    ShortConstraintsBuilder
            > {

        private Short min = Short.MIN_VALUE;
        private Short max = Short.MAX_VALUE;

        ShortConstraintsBuilder() {
        }

        @Override
        protected @NotNull ShortConstraintsBuilder self() {
            return this;
        }

        public @NotNull ShortConstraintsBuilder setMin(@NotNull Short min) {
            this.min = min;
            return this;
        }

        public @NotNull ShortConstraintsBuilder setMax(@NotNull Short max) {
            this.max = max;
            return this;
        }

        @Override
        public @NotNull ShortConstraint build() {
            if (min > max) {
                throw new IllegalStateException(
                        "Invalid range: [" + min + "," + max + "]"
                );
            }

            return new ShortConstraint(this);
        }
    }
}