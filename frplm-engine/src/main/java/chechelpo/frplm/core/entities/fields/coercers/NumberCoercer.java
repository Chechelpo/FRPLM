package chechelpo.frplm.core.entities.fields.coercers;


import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NumberCoercer extends Coercer<Number> {
    private NumberCoercer(FieldType type) {
        super(type);
    }

    @Contract(value = "_-> new",pure = true)
    public static @NotNull NumberCoercer create(FieldType type) {
        return new NumberCoercer(type);
    }

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Number> coerce(@Nullable Object value) {
        return switch (value) {
            case null -> Either.right(null);

            case Number n -> coerceNumber(n);

            case String s -> {
                try {
                    yield switch (this.type) {
                        case BYTE -> Either.right(Byte.parseByte(s));
                        case SHORT -> Either.right(Short.parseShort(s));
                        case INTEGER -> Either.right(Integer.parseInt(s));
                        case LONG -> Either.right(Long.parseLong(s));

                        default -> Either.left(new CoerceError(
                                "FieldType " + this.type + " is not a valid integer number type"
                        ));
                    };
                } catch (NumberFormatException ex) {
                    yield Either.left(new CoerceError(
                            "Cannot coerce '" + value + "' to " + this.type
                    ));
                }
            }

            default -> Either.left(new CoerceError(
                    "Cannot coerce '" + value + "' to " + this.type
            ));
        };
    }

    @Contract(value = "_ -> !null", pure = true)
    private @NotNull Either<CoerceError, Number> coerceNumber(@NotNull Number value) {
        long longValue = value.longValue();

        return switch (this.type) {
            case BYTE -> {
                if (longValue < Byte.MIN_VALUE || longValue > Byte.MAX_VALUE) {
                    yield Either.left(new CoerceError(
                            "Cannot coerce '" + value + "' to BYTE"
                    ));
                }

                yield Either.right((byte) longValue);
            }

            case SHORT -> {
                if (longValue < Short.MIN_VALUE || longValue > Short.MAX_VALUE) {
                    yield Either.left(new CoerceError(
                            "Cannot coerce '" + value + "' to SHORT"
                    ));
                }

                yield Either.right((short) longValue);
            }

            case INTEGER -> {
                if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                    yield Either.left(new CoerceError(
                            "Cannot coerce '" + value + "' to INTEGER"
                    ));
                }

                yield Either.right((int) longValue);
            }

            case LONG -> Either.right(longValue);

            default -> Either.left(new CoerceError(
                    "FieldType " + this.type + " is not a valid integer number type"
            ));
        };
    }

}
