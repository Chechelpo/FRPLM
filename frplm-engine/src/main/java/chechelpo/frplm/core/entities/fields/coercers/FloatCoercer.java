package chechelpo.frplm.core.entities.fields.coercers;

import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FloatCoercer extends Coercer<Double> {
    private FloatCoercer(FieldType type) {
        super(type);
    }

    @Contract("_ -> new")
    public static @NotNull FloatCoercer create(FieldType type) {
        return new FloatCoercer(type);
    }

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Double> coerce(@Nullable Object value) {
        return switch (value) {
            case null -> Either.right(null);

            case Float f -> Either.right(f.doubleValue());

            case Double d -> Either.right(d);

            case Number n -> Either.right(n.doubleValue());

            case String s -> {
                try {
                    double parsed = this.type == FieldType.FLOAT
                            ? Float.parseFloat(s)
                            : Double.parseDouble(s);

                    yield Either.right(parsed);
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
}
