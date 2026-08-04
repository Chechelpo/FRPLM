package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Optional;

@Unmodifiable
public sealed interface Constraint<T> permits BoolConstraint, ByteArrayConstraint, ByteConstraint, DoubleConstraint, FloatConstraint, IntegerConstraint, LocalDateTimeConstraint, LongConstraint, ShortConstraint, StringConstraint {
    Optional<String> returnReasonIfInvalid(@NotNull T value);

    abstract class ABSConstraintsBuilder<T, C extends Constraint<T>, B extends ABSConstraintsBuilder<T, C, B>>
    {
        protected abstract B self();
        public abstract C build();
    }
}