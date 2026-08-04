package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class IntegerCoercer implements Coercer<Integer> {
    static final IntegerCoercer instance = new IntegerCoercer();

    private IntegerCoercer(){}

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Integer> coerce(
            @Nullable Object value
    ) {
        return switch (value) {
            case null -> Either.right(null);

            case Integer integerValue -> Either.right(integerValue);

            case Number number -> {
                long converted = number.longValue();

                if (converted < Integer.MIN_VALUE || converted > Integer.MAX_VALUE) {
                    yield error(value);
                }

                yield Either.right((int) converted);
            }

            case String string -> {
                try {
                    yield Either.right(Integer.parseInt(string));
                } catch (NumberFormatException exception) {
                    yield error(value);
                }
            }

            default -> error(value);
        };
    }

    private static @NotNull Either<CoerceError, Integer> error(
            @NotNull Object value
    ) {
        return Either.left(new CoerceError(
                "Cannot coerce '" + value + "' to INTEGER"
        ));
    }
}