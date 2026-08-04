package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ByteCoercer implements Coercer<Byte> {
    static final ByteCoercer instance = new ByteCoercer();

    private ByteCoercer(){}

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Byte> coerce(
            @Nullable Object value
    ) {
        return switch (value) {
            case null -> Either.right(null);

            case Byte byteValue -> Either.right(byteValue);

            case Number number -> {
                long converted = number.longValue();

                if (converted < Byte.MIN_VALUE || converted > Byte.MAX_VALUE) {
                    yield error(value);
                }

                yield Either.right((byte) converted);
            }

            case String string -> {
                try {
                    yield Either.right(Byte.parseByte(string));
                } catch (NumberFormatException exception) {
                    yield error(value);
                }
            }

            default -> error(value);
        };
    }

    private static @NotNull Either<CoerceError, Byte> error(
            @NotNull Object value
    ) {
        return Either.left(new CoerceError(
                "Cannot coerce '" + value + "' to BYTE"
        ));
    }
}