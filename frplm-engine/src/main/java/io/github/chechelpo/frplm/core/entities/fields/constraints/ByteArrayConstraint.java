package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public final class ByteArrayConstraint implements Constraint<byte[]> {

    private final Integer minLength;
    private final Integer maxLength;

    private ByteArrayConstraint(Builder builder) {
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public @NotNull Optional<String> returnReasonIfInvalid(
            byte @NotNull [] value
    ) {
        int length = value.length;

        if (minLength != null && length < minLength) {
            return Optional.of(
                    "Byte array length must be at least "
                            + minLength
                            + ", but was "
                            + length
            );
        }

        if (maxLength != null && length > maxLength) {
            return Optional.of(
                    "Byte array length must be at most "
                            + maxLength
                            + ", but was "
                            + length
            );
        }

        return Optional.empty();
    }

    public static final class Builder extends
            ABSConstraintsBuilder<byte[], ByteArrayConstraint, Builder> {

        private Integer minLength;
        private Integer maxLength;

        private Builder() {
        }

        public @NotNull Builder minLength(int minLength) {
            if (minLength < 0) {
                throw new IllegalArgumentException(
                        "minLength cannot be negative"
                );
            }

            this.minLength = minLength;
            return self();
        }

        public @NotNull Builder maxLength(int maxLength) {
            if (maxLength < 0) {
                throw new IllegalArgumentException(
                        "maxLength cannot be negative"
                );
            }

            this.maxLength = maxLength;
            return self();
        }

        public @NotNull Builder lengthBetween(
                int minLength,
                int maxLength
        ) {
            return minLength(minLength).maxLength(maxLength);
        }

        @Override
        protected @NotNull Builder self() {
            return this;
        }

        @Override
        public @NotNull ByteArrayConstraint build() {
            if (
                    minLength != null
                            && maxLength != null
                            && minLength > maxLength
            ) {
                throw new IllegalStateException(
                        "minLength cannot be greater than maxLength"
                );
            }

            return new ByteArrayConstraint(this);
        }
    }
}