package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BoolCoercer implements Coercer<Boolean> {
    static final BoolCoercer instance = new BoolCoercer();
    private BoolCoercer(){}

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Boolean> coerce(@Nullable Object value) {
        return switch (value) {
            case null -> Either.right(null);

            case Boolean b -> Either.right(b);

            case String s -> switch (s.trim().toLowerCase()) {
                case "true" -> Either.right(true);
                case "false" -> Either.right(false);
                default -> Either.left(new CoerceError("Cannot coerce '" + value + "' to boolean"));
            };

            default -> Either.left(new CoerceError("Cannot coerce '" + value + "' to boolean"));
        };
    }
}
