package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ShortCoercer implements Coercer<Short> {
    static final ShortCoercer instance = new ShortCoercer();
    private ShortCoercer(){}

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Short> coerce(
            @Nullable Object value
    ) {
        return switch (value) {
            case null -> Either.right(null);

            case Short shortValue -> Either.right(shortValue);

            case Number number -> {
                long converted = number.longValue();

                if (converted < Short.MIN_VALUE || converted > Short.MAX_VALUE) {
                    yield error(value);
                }

                yield Either.right((short) converted);
            }

            case String string -> {
                try {
                    yield Either.right(Short.parseShort(string));
                } catch (NumberFormatException exception) {
                    yield error(value);
                }
            }

            default -> error(value);
        };
    }

    private static @NotNull Either<CoerceError, Short> error(
            @NotNull Object value
    ) {
        return Either.left(new CoerceError(
                "Cannot coerce '" + value + "' to SHORT"
        ));
    }
}