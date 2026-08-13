package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class LongConstraint implements Constraint<Long> {
    private final Long min;
    private final Long max;

    @Contract(pure = true)
    private LongConstraint(@NotNull LongConstraintsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
    }

    public Long min() {
        return min;
    }

    public Long max() {
        return max;
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<String> returnReasonIfInvalid(
            @NotNull Long value
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
    public static @NotNull LongConstraintsBuilder builder() {
        return new LongConstraintsBuilder();
    }

    public static final class LongConstraintsBuilder
            extends ABSConstraintsBuilder<
                    Long,
                    LongConstraint,
                    LongConstraintsBuilder
            > {

        private Long min = Long.MIN_VALUE;
        private Long max = Long.MAX_VALUE;

        LongConstraintsBuilder() {
        }

        @Override
        protected @NotNull LongConstraintsBuilder self() {
            return this;
        }

        public @NotNull LongConstraintsBuilder setMin(@NotNull Long min) {
            this.min = min;
            return this;
        }

        public @NotNull LongConstraintsBuilder setMax(@NotNull Long max) {
            this.max = max;
            return this;
        }

        @Override
        public @NotNull LongConstraint build() {
            if (min > max) {
                throw new IllegalStateException(
                        "Invalid range: [" + min + "," + max + "]"
                );
            }

            return new LongConstraint(this);
        }
    }
}