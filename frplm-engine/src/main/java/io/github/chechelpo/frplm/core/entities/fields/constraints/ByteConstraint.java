package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class ByteConstraint implements Constraint<Byte> {
    private final Byte min;
    private final Byte max;

    @Contract(pure = true)
    private ByteConstraint(@NotNull ByteConstraintsBuilder builder) {
        this.min = builder.min;
        this.max = builder.max;
    }

    @Override
    @Contract(pure = true)
    public @NotNull Optional<String> returnReasonIfInvalid(
            @NotNull Byte value
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
    public static @NotNull ByteConstraintsBuilder builder() {
        return new ByteConstraintsBuilder();
    }

    public static final class ByteConstraintsBuilder
            extends ABSConstraintsBuilder<
                    Byte,
                    ByteConstraint,
                    ByteConstraintsBuilder
            > {

        private Byte min = Byte.MIN_VALUE;
        private Byte max = Byte.MAX_VALUE;

        ByteConstraintsBuilder() {
        }

        @Override
        protected @NotNull ByteConstraintsBuilder self() {
            return this;
        }

        public @NotNull ByteConstraintsBuilder setMin(@NotNull Byte min) {
            this.min = min;
            return this;
        }

        public @NotNull ByteConstraintsBuilder setMax(@NotNull Byte max) {
            this.max = max;
            return this;
        }

        @Override
        public @NotNull ByteConstraint build() {
            if (min > max) {
                throw new IllegalStateException(
                        "Invalid range: [" + min + "," + max + "]"
                );
            }

            return new ByteConstraint(this);
        }
    }
}