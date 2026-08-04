package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LongCoercer implements Coercer<Long> {
    static final LongCoercer instance = new LongCoercer();

    private LongCoercer(){}

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Long> coerce(
            @Nullable Object value
    ) {
        return switch (value) {
            case null -> Either.right(null);

            case Long longValue -> Either.right(longValue);

            case Number number -> Either.right(number.longValue());

            case String string -> {
                try {
                    yield Either.right(Long.parseLong(string));
                } catch (NumberFormatException exception) {
                    yield error(value);
                }
            }

            default -> error(value);
        };
    }

    private static @NotNull Either<CoerceError, Long> error(
            @NotNull Object value
    ) {
        return Either.left(new CoerceError(
                "Cannot coerce '" + value + "' to LONG"
        ));
    }
}