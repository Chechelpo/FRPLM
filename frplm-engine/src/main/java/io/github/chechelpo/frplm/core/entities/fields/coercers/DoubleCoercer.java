package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DoubleCoercer extends Coercer<Double> {
    private DoubleCoercer() {
        super(FieldType.DOUBLE);
    }

    @Contract("-> new")
    public static @NotNull DoubleCoercer create() {
        return new DoubleCoercer();
    }

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Double> coerce(
            @Nullable Object value
    ) {
        return switch (value) {
            case null -> Either.right(null);

            case Double d -> validateFinite(d, value);

            case Number number -> {
                double converted = number.doubleValue();
                yield validateFinite(converted, value);
            }

            case String string -> {
                try {
                    double parsed = Double.parseDouble(string);
                    yield validateFinite(parsed, value);
                } catch (NumberFormatException exception) {
                    yield failure(value);
                }
            }

            default -> failure(value);
        };
    }

    private @NotNull Either<CoerceError, Double> validateFinite(
            double value,
            @Nullable Object originalValue
    ) {
        if (!Double.isFinite(value)) {
            return failure(originalValue);
        }

        return Either.right(value);
    }

    private @NotNull Either<CoerceError, Double> failure(
            @Nullable Object value
    ) {
        return Either.left(new CoerceError(
                "Cannot coerce '" + value + "' to " + this.type
        ));
    }
}