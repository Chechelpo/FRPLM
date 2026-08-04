package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface Coercer<T>
        permits BoolCoercer, ByteArrayCoercer, ByteCoercer, DoubleCoercer, FloatCoercer, IntegerCoercer, LocalDateTimeCoercer, LongCoercer, ShortCoercer, StringCoercer
{
    record CoerceError(String message) {}
    @Contract(pure = true)
    @NotNull Either<CoerceError, T> coerce(@Nullable Object value);
}
