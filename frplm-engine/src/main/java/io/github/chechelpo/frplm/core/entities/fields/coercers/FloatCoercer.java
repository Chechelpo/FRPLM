package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FloatCoercer extends Coercer<Float> {
    private FloatCoercer() {
        super(FieldType.FLOAT);
    }

    @Contract("-> new")
    public static @NotNull FloatCoercer create() {
        return new FloatCoercer();
    }

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Float> coerce(
            @Nullable Object value
    ) {
        return switch (value) {
            case null -> Either.right(null);

            case Float f -> validateFinite(f, value);

            case Number number -> {
                float converted = number.floatValue();
                yield validateFinite(converted, value);
            }

            case String string -> {
                try {
                    float parsed = Float.parseFloat(string);
                    yield validateFinite(parsed, value);
                } catch (NumberFormatException exception) {
                    yield failure(value);
                }
            }

            default -> failure(value);
        };
    }

    private @NotNull Either<CoerceError, Float> validateFinite(
            float value,
            @Nullable Object originalValue
    ) {
        if (!Float.isFinite(value)) {
            return failure(originalValue);
        }

        return Either.right(value);
    }

    private @NotNull Either<CoerceError, Float> failure(
            @Nullable Object value
    ) {
        return Either.left(new CoerceError(
                "Cannot coerce '" + value + "' to " + this.type
        ));
    }
}