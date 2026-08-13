package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalDateTime;
import java.util.Optional;

@Unmodifiable
public final class LocalDateTimeConstraint
        implements Constraint<LocalDateTime> {

    private final @Nullable LocalDateTime minimum;
    private final @Nullable LocalDateTime maximum;

    private LocalDateTimeConstraint(@NotNull Builder builder) {
        this.minimum = builder.minimum;
        this.maximum = builder.maximum;
    }

    public @Nullable LocalDateTime minimum() {
        return minimum;
    }

    public @Nullable LocalDateTime maximum() {
        return maximum;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public @NotNull Optional<String> returnReasonIfInvalid(
            @NotNull LocalDateTime value
    ) {
        if (minimum != null && value.isBefore(minimum)) {
            return Optional.of(
                    "Date-time must not be before "
                            + minimum
                            + ", but was "
                            + value
            );
        }

        if (maximum != null && value.isAfter(maximum)) {
            return Optional.of(
                    "Date-time must not be after "
                            + maximum
                            + ", but was "
                            + value
            );
        }

        return Optional.empty();
    }

    public static final class Builder extends
            ABSConstraintsBuilder<
                    LocalDateTime,
                    LocalDateTimeConstraint,
                    Builder
                    > {

        private @Nullable LocalDateTime minimum;
        private @Nullable LocalDateTime maximum;

        private Builder() {
        }

        public @NotNull Builder minimum(
                @NotNull LocalDateTime minimum
        ) {
            this.minimum = minimum;
            return self();
        }

        public @NotNull Builder maximum(
                @NotNull LocalDateTime maximum
        ) {
            this.maximum = maximum;
            return self();
        }

        public @NotNull Builder between(
                @NotNull LocalDateTime minimum,
                @NotNull LocalDateTime maximum
        ) {
            this.minimum = minimum;
            this.maximum = maximum;
            return self();
        }

        @Override
        protected @NotNull Builder self() {
            return this;
        }

        @Override
        public @NotNull LocalDateTimeConstraint build() {
            if (
                    minimum != null
                            && maximum != null
                            && minimum.isAfter(maximum)
            ) {
                throw new IllegalStateException(
                        "minimum cannot be after maximum"
                );
            }

            return new LocalDateTimeConstraint(this);
        }
    }
}